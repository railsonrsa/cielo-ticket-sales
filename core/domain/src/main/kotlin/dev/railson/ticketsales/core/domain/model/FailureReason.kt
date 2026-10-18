package dev.railson.ticketsales.core.domain.model

/** Por que uma Purchase terminou `Failed`. Lista fechada: o texto para o Operator mora no `strings.xml` da feature. */
enum class FailureReason {
    CanceledByOperator,
    PaymentError,
    Declined,
    TerminalAuthError,
    TerminalUnavailable,
    NotCompleted,
    NotStarted,
    AmountMismatch,
    SoldOutAfterPayment,
    Abandoned,
}
