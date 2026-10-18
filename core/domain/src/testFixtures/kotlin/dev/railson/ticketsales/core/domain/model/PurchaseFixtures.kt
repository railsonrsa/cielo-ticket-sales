package dev.railson.ticketsales.core.domain.model

/** Uma Cielo Order autorizada; o teste muda só o que importa para ele. */
fun aCieloOrder(
    authorizedAmount: Money,
    orderId: String = "cielo-order-1",
    cieloCode: String = "123456",
    authCode: String = "654321",
) = CieloOrder(orderId, authorizedAmount, cieloCode, authCode)

/** Uma Purchase de um item só; o teste muda só o que importa para ele. */
fun aPurchase(
    id: String,
    status: PurchaseStatus = PurchaseStatus.Approved,
    createdAtEpochMillis: Long = 1_792_000_000_000,
    pendingRefund: Boolean = false,
    inconclusiveChecks: Int = 0,
) = Purchase(
    id = id,
    status = status,
    items = listOf(PurchaseItem(eventId = "event-1", eventName = "Show de teste", quantity = 1, unitPrice = Money(cents = 5_000))),
    total = Money(cents = 5_000),
    createdAtEpochMillis = createdAtEpochMillis,
    pendingRefund = pendingRefund,
    inconclusiveChecks = inconclusiveChecks,
)
