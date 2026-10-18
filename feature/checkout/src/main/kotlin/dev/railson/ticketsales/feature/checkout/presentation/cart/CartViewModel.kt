package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AcknowledgeSoldOutUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseResult
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveAwaitingPaymentUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveLateApprovalsUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveSoldOutNoticesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O desfecho do pagamento chega pelo banco, nunca pelo callback: o ViewModel guarda só o id da Purchase que
 * disparou (no [SavedStateHandle], para sobreviver à recriação) e reage quando ela sai de `AwaitingPayment`.
 * Os desfechos fora de hora (aprovação atrasada, esgotado após o pagamento) também chegam pelo banco.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val changeQuantity: ChangeCartQuantityUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
    private val startPurchase: StartPurchaseUseCase,
    observeAwaitingPayment: ObserveAwaitingPaymentUseCase,
    observePurchase: ObservePurchaseUseCase,
    private val reconcilePurchase: ReconcilePurchaseUseCase,
    private val abandonPurchase: AbandonPurchaseUseCase,
    observeLateApprovals: ObserveLateApprovalsUseCase,
    observeSoldOutNotices: ObserveSoldOutNoticesUseCase,
    private val acknowledgeSoldOut: AcknowledgeSoldOutUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val effectChannel = Channel<CartEffect>(Channel.BUFFERED)
    val effects: Flow<CartEffect> = effectChannel.receiveAsFlow()

    private val trackedPurchase: Flow<Purchase?> = savedStateHandle.getStateFlow<String?>(TRACKED_PURCHASE_ID, null)
        .flatMapLatest { purchaseId -> if (purchaseId == null) flowOf(null) else observePurchase(purchaseId) }

    private val awaitingPayment: Flow<Purchase?> = observeAwaitingPayment()

    /** A checagem prévia barrou o "Pagar": não há Purchase para observar, então o aviso fica aqui. */
    private val terminalUnavailable = savedStateHandle.getStateFlow(TERMINAL_UNAVAILABLE, false)

    private val confirmingAbandon = savedStateHandle.getStateFlow(CONFIRMING_ABANDON, false)

    /** O "Pagar" esbarrou em aprovações atrasadas: o dialog fica aberto até "Revisar carrinho" ou "Continuar e pagar". */
    private val reviewingLateApprovals = savedStateHandle.getStateFlow(REVIEWING_LATE_APPROVALS, false)

    private val screenFlags: Flow<ScreenFlags> =
        combine(terminalUnavailable, confirmingAbandon, reviewingLateApprovals, reconcilePurchase.isRunning, ::ScreenFlags)

    private val lateOutcomes: Flow<LateOutcomes> = combine(observeLateApprovals(), observeSoldOutNotices(), ::LateOutcomes)

    val uiState: StateFlow<CartUiState> =
        combine(observeCart(), awaitingPayment, trackedPurchase, lateOutcomes, screenFlags) { cart, awaiting, tracked, late, flags ->
            val canAbandon = awaiting?.canBeAbandoned == true && !flags.checkingPayment
            val dialog = tracked?.dialog()
                ?: late.soldOutNotices.firstOrNull()?.let(CartDialog::SoldOutAfterPayment)
                ?: CartDialog.TerminalUnavailable.takeIf { flags.terminalUnavailable }
                ?: CartDialog.ConfirmAbandon.takeIf { flags.confirmingAbandon && canAbandon }
                ?: CartDialog.LateApprovalsToReview(late.approvals).takeIf { flags.reviewingLateApprovals && late.approvals.isNotEmpty() }
            CartUiState(
                isLoading = false,
                cart = cart,
                awaitingPayment = awaiting != null,
                isCheckingPayment = awaiting != null && flags.checkingPayment,
                canAbandon = canAbandon,
                hasLateApprovals = late.approvals.isNotEmpty(),
                dialog = dialog,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        viewModelScope.launch {
            trackedPurchase.collect { purchase ->
                val effect = purchase?.oneOffEffect()
                when {
                    effect != null -> sendAndStopTracking(effect)
                    // Falhou sem dialog (Reconciliation sem cobrança, desistência): uma aprovação que chegue depois é
                    // atrasada, e aparece pelo aviso e pelo dialog do próximo "Pagar", não como esta venda aprovada.
                    purchase?.status is PurchaseStatus.Failed && purchase.dialog() == null -> stopTracking()
                }
            }
        }
    }

    fun onAction(action: CartAction) {
        viewModelScope.launch {
            when (action) {
                is CartAction.ChangeQuantity -> changeQuantity(action.eventId, action.quantity)
                is CartAction.Remove -> removeFromCart(action.eventId)
                CartAction.Pay -> pay()
                CartAction.Retry -> {
                    stopTracking()
                    pay()
                }
                // Também é o "Revisar carrinho": o aceite não é gravado, e o próximo "Pagar" pergunta de novo.
                CartAction.DismissDialog -> {
                    stopTracking()
                    savedStateHandle[TERMINAL_UNAVAILABLE] = false
                    savedStateHandle[CONFIRMING_ABANDON] = false
                    savedStateHandle[REVIEWING_LATE_APPROVALS] = false
                }
                // O desfecho chega pelo banco: o banner some ou ganha o aviso de verificação inconclusiva.
                CartAction.CheckPayment -> reconcilePurchase()
                CartAction.AskToAbandon -> savedStateHandle[CONFIRMING_ABANDON] = true
                CartAction.ConfirmAbandon -> {
                    savedStateHandle[CONFIRMING_ABANDON] = false
                    awaitingPayment.first()?.let { abandonPurchase(it.id) }
                }
                is CartAction.ContinueAndPay -> pay(acceptedLateApprovals = action.lateApprovalIds)
                is CartAction.AcknowledgeSoldOut -> {
                    stopTracking()
                    acknowledgeSoldOut(action.purchaseId)
                }
            }
        }
    }

    private suspend fun pay(acceptedLateApprovals: Set<String> = emptySet()) {
        val result = startPurchase(acceptedLateApprovals)
        savedStateHandle[REVIEWING_LATE_APPROVALS] = result is StartPurchaseResult.LateApprovalsToReview
        // Conflito de Capacity e pagamento em andamento já aparecem na tela pelo próprio estado.
        when (result) {
            is StartPurchaseResult.Started -> savedStateHandle[TRACKED_PURCHASE_ID] = result.purchaseId
            StartPurchaseResult.TerminalUnavailable -> savedStateHandle[TERMINAL_UNAVAILABLE] = true
            else -> Unit
        }
    }

    private suspend fun sendAndStopTracking(effect: CartEffect) {
        stopTracking()
        effectChannel.send(effect)
    }

    private fun stopTracking() {
        savedStateHandle[TRACKED_PURCHASE_ID] = null
    }

    private fun Purchase.oneOffEffect(): CartEffect? = when (status) {
        PurchaseStatus.Approved -> CartEffect.PurchaseApproved(id).takeUnless { isLateApprovalToAccept }
        PurchaseStatus.Failed(FailureReason.CanceledByOperator) -> CartEffect.PaymentCanceled
        else -> null
    }

    private fun Purchase.dialog(): CartDialog? = when (val status = status) {
        is PurchaseStatus.Failed -> when (status.reason) {
            FailureReason.PaymentError, FailureReason.Declined -> CartDialog.RetryablePaymentFailure(status.reason)
            FailureReason.TerminalAuthError -> CartDialog.TerminalAuthError
            FailureReason.TerminalUnavailable -> CartDialog.TerminalUnavailable
            FailureReason.AmountMismatch -> CartDialog.AmountMismatch
            else -> null
        }
        else -> null
    }

    private companion object {
        const val TRACKED_PURCHASE_ID = "tracked_purchase_id"
        const val TERMINAL_UNAVAILABLE = "terminal_unavailable"
        const val CONFIRMING_ABANDON = "confirming_abandon"
        const val REVIEWING_LATE_APPROVALS = "reviewing_late_approvals"
    }
}

private data class ScreenFlags(
    val terminalUnavailable: Boolean,
    val confirmingAbandon: Boolean,
    val reviewingLateApprovals: Boolean,
    val checkingPayment: Boolean,
)

private data class LateOutcomes(val approvals: List<Purchase>, val soldOutNotices: List<Purchase>)
