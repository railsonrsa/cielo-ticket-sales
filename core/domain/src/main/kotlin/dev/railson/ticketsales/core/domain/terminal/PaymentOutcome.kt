package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason

/** A resposta da maquininha, já traduzida para o domínio. */
sealed interface PaymentOutcome {

    /** A Cielo criou uma Order para o [reference]; aprovar depende de a soma autorizada bater com o total. */
    data class Paid(val reference: String, val cieloOrder: CieloOrder) : PaymentOutcome

    /** Falha da maquininha (códigos 1–4). Não traz `reference`: vale para a única `AwaitingPayment`. */
    data class Failed(val reason: FailureReason) : PaymentOutcome

    /** Resposta ausente ou inválida: é como se não tivesse chegado. */
    data object Unreadable : PaymentOutcome
}
