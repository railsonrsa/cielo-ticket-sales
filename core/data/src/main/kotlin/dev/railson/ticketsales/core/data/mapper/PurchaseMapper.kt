package dev.railson.ticketsales.core.data.mapper

import dev.railson.ticketsales.core.data.local.entity.PurchaseEntity
import dev.railson.ticketsales.core.data.local.entity.PurchaseItemEntity
import dev.railson.ticketsales.core.data.local.entity.PurchaseWithItems
import dev.railson.ticketsales.core.data.local.entity.TicketEntity
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.Ticket

internal const val AWAITING_PAYMENT = "AWAITING_PAYMENT"
internal const val APPROVED = "APPROVED"
internal const val FAILED = "FAILED"

internal fun PurchaseStatus.toColumn(): String = when (this) {
    PurchaseStatus.AwaitingPayment -> AWAITING_PAYMENT
    PurchaseStatus.Approved -> APPROVED
    is PurchaseStatus.Failed -> FAILED
}

internal fun Purchase.toEntity() = PurchaseEntity(
    id = id,
    status = status.toColumn(),
    failureReason = (status as? PurchaseStatus.Failed)?.reason?.name,
    totalCents = total.cents,
    createdAtEpochMillis = createdAtEpochMillis,
    orderId = cieloOrder?.orderId,
    authorizedAmountCents = cieloOrder?.authorizedAmount?.cents,
    cieloCode = cieloOrder?.cieloCode,
    authCode = cieloOrder?.authCode,
    pendingRefund = pendingRefund,
    extraOrderIds = extraOrderIds.toColumn(),
    inconclusiveChecks = inconclusiveChecks,
    needsAcknowledgment = needsAcknowledgment,
)

internal fun List<String>.toColumn(): String = joinToString(separator = ",")

internal fun Purchase.itemEntities() = items.mapIndexed { position, item ->
    PurchaseItemEntity(
        purchaseId = id,
        eventId = item.eventId,
        position = position,
        eventName = item.eventName,
        quantity = item.quantity,
        unitPriceCents = item.unitPrice.cents,
    )
}

internal fun PurchaseWithItems.toDomain() = Purchase(
    id = purchase.id,
    status = when (purchase.status) {
        AWAITING_PAYMENT -> PurchaseStatus.AwaitingPayment
        APPROVED -> PurchaseStatus.Approved
        FAILED -> PurchaseStatus.Failed(FailureReason.valueOf(checkNotNull(purchase.failureReason)))
        else -> error("Unknown purchase status: ${purchase.status}")
    },
    items = items.sortedBy { it.position }.map { PurchaseItem(it.eventId, it.eventName, it.quantity, Money(it.unitPriceCents)) },
    total = Money(purchase.totalCents),
    createdAtEpochMillis = purchase.createdAtEpochMillis,
    cieloOrder = purchase.orderId?.let { orderId ->
        CieloOrder(
            orderId = orderId,
            authorizedAmount = Money(checkNotNull(purchase.authorizedAmountCents)),
            cieloCode = checkNotNull(purchase.cieloCode),
            authCode = checkNotNull(purchase.authCode),
        )
    },
    pendingRefund = purchase.pendingRefund,
    extraOrderIds = purchase.extraOrderIds.split(',').filter(String::isNotEmpty),
    inconclusiveChecks = purchase.inconclusiveChecks,
    needsAcknowledgment = purchase.needsAcknowledgment,
)

internal fun TicketEntity.toDomain() = Ticket(id = id, purchaseId = purchaseId, eventId = eventId)
