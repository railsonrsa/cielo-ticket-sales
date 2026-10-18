package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * O "Pagar": transforma o Cart numa Purchase `AwaitingPayment`, grava e só então dispara o pagamento (ADR 0001).
 * A trava "uma `AwaitingPayment` por vez" mora aqui, não na tela: duplo toque, rotação e retry caem nela.
 * O [mutex] cobre a corrida entre checar e gravar, então a instância tem que ser única no app.
 *
 * Uma aprovação atrasada não esvazia o Cart, que pode repetir os mesmos ingressos: o "Pagar" só segue depois de o
 * Operator aceitar cada uma ([acceptedLateApprovals]). O aceite fica gravado; uma que chegue depois do dialog aberto
 * ainda precisa ser aceita.
 */
class StartPurchaseUseCase(
    private val observeCart: ObserveCartUseCase,
    private val purchaseRepository: PurchaseRepository,
    private val paymentTerminal: PaymentTerminal,
    private val newPurchaseId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val mutex = Mutex()

    suspend operator fun invoke(acceptedLateApprovals: Set<String> = emptySet()): StartPurchaseResult = mutex.withLock {
        if (purchaseRepository.findAwaitingPayment() != null) return@withLock StartPurchaseResult.PaymentInProgress
        val cart = observeCart().first()
        if (cart.items.isEmpty()) return@withLock StartPurchaseResult.EmptyCart
        // Revalida sem reservar: a Capacity só muda na aprovação.
        val conflicts = cart.items.filter { it.capacityConflict != null }.map { it.event.id }
        if (conflicts.isNotEmpty()) return@withLock StartPurchaseResult.CapacityConflicts(conflicts)
        val lateApprovals = purchaseRepository.observeAll().first().filter(Purchase::isLateApprovalToAccept)
        val (accepted, toReview) = lateApprovals.partition { it.id in acceptedLateApprovals }
        accepted.forEach { purchaseRepository.acknowledge(it.id) }
        if (toReview.isNotEmpty()) {
            return@withLock StartPurchaseResult.LateApprovalsToReview(toReview.sortedBy(Purchase::createdAtEpochMillis))
        }
        if (!paymentTerminal.isAvailable()) return@withLock StartPurchaseResult.TerminalUnavailable

        val purchase = Purchase(
            id = newPurchaseId(),
            status = PurchaseStatus.AwaitingPayment,
            items = cart.items.map { PurchaseItem(it.event.id, it.event.name, it.quantity, it.event.unitPrice) },
            total = cart.total,
            createdAtEpochMillis = now(),
        )
        purchaseRepository.create(purchase)
        paymentTerminal.requestPayment(purchase)
        StartPurchaseResult.Started(purchase.id)
    }
}
