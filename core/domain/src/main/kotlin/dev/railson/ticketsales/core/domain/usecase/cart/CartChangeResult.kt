package dev.railson.ticketsales.core.domain.usecase.cart

/** O que aconteceu com um pedido de mudança no Cart. */
enum class CartChangeResult {
    Applied,

    /** A quantidade pedida passa da Capacity atual do Event; nada foi gravado. */
    ExceedsCapacity,
}
