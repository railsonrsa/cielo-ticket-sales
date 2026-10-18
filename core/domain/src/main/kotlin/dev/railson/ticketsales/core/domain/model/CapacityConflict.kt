package dev.railson.ticketsales.core.domain.model

/** Por que um item do Cart não cabe mais na Capacity atual do Event. Enquanto houver um, não se paga. */
sealed interface CapacityConflict {

    data object SoldOut : CapacityConflict

    data class OnlyLeft(val available: Int) : CapacityConflict
}
