package dev.railson.ticketsales.core.domain.model

/** Uma Cielo Order autorizada; o teste muda só o que importa para ele. */
fun aCieloOrder(
    authorizedAmount: Money,
    orderId: String = "cielo-order-1",
    cieloCode: String = "123456",
    authCode: String = "654321",
) = CieloOrder(orderId, authorizedAmount, cieloCode, authCode)
