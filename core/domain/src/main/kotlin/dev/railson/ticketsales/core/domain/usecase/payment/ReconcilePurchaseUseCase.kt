package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.OrdersPage
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A Reconciliation (ADR 0001): descobre, pela lista de Cielo Orders, o desfecho da Purchase `AwaitingPayment` cujo
 * callback nunca chegou. Roda na abertura a frio ([onColdStart]) e no "Verificar pagamento" ([invoke]), **nunca no
 * resume**: a pessoa pode estar no meio do pagamento.
 *
 * A porta só dispara (ADR 0002): cada página chega depois, em [onOrdersPage], e o use case pede a próxima.
 * A busca vive em memória, então a instância tem que ser única no app. Se o processo morrer no meio, a busca se
 * perde sem contar como inconclusiva, e a próxima abertura a frio recomeça.
 * Só marca `Failed` com certeza: consulta que falhou, que não respondeu ou que estourou o limite de páginas
 * deixa a Purchase `AwaitingPayment` e conta uma verificação inconclusiva.
 */
class ReconcilePurchaseUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val paymentTerminal: PaymentTerminal,
    private val scope: CoroutineScope,
) {

    private val mutex = Mutex()
    private var search: Search? = null
    private var coldStartHandled = false

    private val running = MutableStateFlow(false)

    /** Há uma busca em andamento: o "Verificar pagamento" e o "Desistir" ficam indisponíveis. */
    val isRunning: StateFlow<Boolean> = running.asStateFlow()

    suspend operator fun invoke(): StartReconciliationResult = mutex.withLock { start() }

    /** A primeira abertura do app neste processo. Uma rotação ou uma volta para o app não contam. */
    suspend fun onColdStart() {
        mutex.withLock {
            if (coldStartHandled) return
            coldStartHandled = true
            start()
        }
    }

    /** Uma página sem busca em andamento (processo recriado, resposta atrasada) é ignorada. */
    suspend fun onOrdersPage(page: OrdersPage) {
        mutex.withLock {
            val current = search ?: return
            when (val next = current.plan.accept(page)) {
                is OrdersSearch.Next.Read -> request(current, next.page)
                OrdersSearch.Next.Finished -> conclude(current)
                OrdersSearch.Next.Inconclusive -> giveUp(current)
            }
        }
    }

    /** A consulta voltou com erro ou ilegível. */
    suspend fun onOrdersFailed() {
        mutex.withLock { search?.let { giveUp(it) } }
    }

    private suspend fun start(): StartReconciliationResult {
        if (search != null) return StartReconciliationResult.AlreadyRunning
        val purchase = purchaseRepository.findAwaitingPayment() ?: return StartReconciliationResult.NothingToReconcile
        val started = Search(purchase, OrdersSearch(purchase.id, purchase.createdAtEpochMillis, CREATED_AT_SLACK_MILLIS, MAX_PAGES))
        search = started
        running.value = true
        request(started, page = 1)
        return StartReconciliationResult.Started
    }

    private fun request(current: Search, page: Int) {
        current.watchdog?.cancel()
        // Sem resposta (a Cielo não chamou o callback), a busca termina inconclusiva em vez de travar o botão.
        current.watchdog = scope.launch {
            delay(PAGE_TIMEOUT_MILLIS)
            mutex.withLock {
                if (search !== current) return@withLock
                current.watchdog = null
                giveUp(current)
            }
        }
        paymentTerminal.requestOrders(page)
    }

    private suspend fun conclude(current: Search) {
        finish(current)
        // Um callback pode ter chegado durante a busca: ele vence, e a Reconciliation não mexe em nada.
        val purchase = purchaseRepository.find(current.purchase.id)
        if (purchase?.status != PurchaseStatus.AwaitingPayment) return

        val matches = current.plan.matches
        val paid = matches.filter { it.isPaid }
        if (matches.isEmpty()) return purchaseRepository.fail(purchase.id, FailureReason.NotStarted)
        if (paid.isEmpty()) return purchaseRepository.fail(purchase.id, FailureReason.NotCompleted)

        // Cobrança duplicada: fica a primeira que bate com o total, e as outras vão para estorno.
        val kept = paid.firstOrNull { it.cieloOrder.authorizedAmount == purchase.total } ?: paid.first()
        val extraOrderIds = (paid - kept).map { it.cieloOrder.orderId }
        if (kept.cieloOrder.authorizedAmount == purchase.total) {
            purchaseRepository.approve(purchase.id, kept.cieloOrder, extraOrderIds)
        } else {
            purchaseRepository.fail(purchase.id, FailureReason.AmountMismatch, kept.cieloOrder, pendingRefund = true, extraOrderIds)
        }
    }

    private suspend fun giveUp(current: Search) {
        finish(current)
        if (purchaseRepository.find(current.purchase.id)?.status == PurchaseStatus.AwaitingPayment) {
            purchaseRepository.recordInconclusiveCheck(current.purchase.id)
        }
    }

    private fun finish(current: Search) {
        current.watchdog?.cancel()
        search = null
        running.value = false
    }

    private class Search(val purchase: Purchase, val plan: OrdersSearch) {
        var watchdog: Job? = null
    }

    companion object {
        /** Quanto esperar por uma página. A consulta da Cielo é silenciosa e leva ~40 ms no emulador. */
        const val PAGE_TIMEOUT_MILLIS = 15_000L

        /** A folga em torno da hora da Purchase no atalho de parada: o relógio é o mesmo, mas a Order nasce depois. */
        const val CREATED_AT_SLACK_MILLIS = 10 * 60_000L

        const val MAX_PAGES = 20
    }
}

/** O que aconteceu ao pedir uma Reconciliation. O desfecho chega depois, pelo banco. */
enum class StartReconciliationResult {
    Started,
    AlreadyRunning,

    /** Não há Purchase `AwaitingPayment`. */
    NothingToReconcile,
}
