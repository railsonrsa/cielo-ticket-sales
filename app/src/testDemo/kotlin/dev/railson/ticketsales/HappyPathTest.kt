package dev.railson.ticketsales

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.ui.format.formatBrl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * O caminho feliz de ponta a ponta, com a `FakePaymentTerminal` do flavor `demo` e o Room de verdade:
 * Cart → Pagar → aprovada → Minhas compras.
 */
@RunWith(RobolectricTestRunner::class)
class HappyPathTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `paying the cart issues the ticket and shows it in my purchases`() {
        waitForText("Noite de Choro no Coreto")
        compose.onAllNodesWithText("Adicionar").onFirst().performClick()
        compose.onNodeWithContentDescription("Abrir carrinho").performClick()
        waitForText("Pagar")
        compose.onNodeWithText("Pagar").performClick()

        // A maquininha só dispara: a "tela da Cielo" é outra Activity, que o Robolectric não abre sozinho.
        val terminalIntent = awaitStartedActivity()
        assertThat(terminalIntent.component?.className).endsWith("FakePaymentActivity")
        ActivityScenario.launch<Activity>(terminalIntent).use {
            waitForText("Aprovar")
            compose.onNodeWithText("Aprovar").performClick()
            // O desfecho entra pelo use case e chega à tela do app pelo banco.
            waitForText("Pagamento aprovado")
        }
        compose.onNodeWithText("1 ingresso emitido").assertExists()
        compose.onNodeWithText("Nova venda").performClick()

        // A Capacity baixou e o Cart esvaziou.
        waitForText("149 lugares")
        compose.onNodeWithContentDescription("Minhas compras").performClick()

        waitForText("1 ingresso")
        compose.onNodeWithText("Noite de Choro no Coreto").assertExists()
        compose.onNodeWithText(formatBrl(4_000)).assertExists()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(TIMEOUT_MILLIS) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitStartedActivity(): Intent {
        val application = shadowOf(ApplicationProvider.getApplicationContext<Application>())
        var intent: Intent? = null
        compose.waitUntil(TIMEOUT_MILLIS) {
            // O Room responde em outra thread e retoma a coroutine pelo main looper, que no Robolectric só anda à mão.
            shadowOf(Looper.getMainLooper()).idle()
            intent = application.nextStartedActivity
            intent != null
        }
        return checkNotNull(intent)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
