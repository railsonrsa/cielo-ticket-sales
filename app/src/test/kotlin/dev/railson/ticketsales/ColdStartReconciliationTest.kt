package dev.railson.ticketsales

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * A Reconciliation da órfã roda na abertura a frio e **nunca no resume** (ADR 0001): voltar da tela da Cielo, ou de
 * qualquer outro app, não pode marcar `Failed` um pagamento que ainda está em andamento.
 */
@RunWith(RobolectricTestRunner::class)
class ColdStartReconciliationTest {

    @Test
    fun `only the cold start checks the orphan, never a resume or a rotation`() {
        // O Application já subiu o Koin; a maquininha vira uma que só anota o que foi pedido.
        val purchaseRepository: PurchaseRepository = get(PurchaseRepository::class.java)
        val terminal = RecordingPaymentTerminal(purchaseRepository)
        loadKoinModules(module { single<PaymentTerminal> { terminal } })
        runBlocking { purchaseRepository.create(orphan()) }

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        awaitMainLooper { terminal.requestedPages.isNotEmpty() }
        assertThat(terminal.requestedPages).containsExactly(1)

        // A pessoa vai para a tela da Cielo e volta; depois gira o aparelho.
        activity.pause().stop()
        activity.start().resume()
        activity.recreate()
        repeat(20) { awaitMainLooper { true } }

        assertThat(terminal.requestedPages).containsExactly(1)
    }

    private fun awaitMainLooper(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("a condição não se cumpriu")
    }

    private fun orphan() = Purchase(
        id = "orphan",
        status = PurchaseStatus.AwaitingPayment,
        items = listOf(PurchaseItem(SEEDED_EVENT_ID, "Noite de Choro no Coreto", quantity = 1, unitPrice = Money(4_000))),
        total = Money(4_000),
        createdAtEpochMillis = 0,
    )

    private companion object {
        const val SEEDED_EVENT_ID = "7e8e3497-5e2a-420d-9826-9bb0ebf57f9c"
    }
}
