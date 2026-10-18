package dev.railson.ticketsales.di

import dev.railson.ticketsales.payment.cielolio.di.cieloLioModule

/** Flavor `cielo`: a Cielo LIO de verdade. */
internal val paymentModule = cieloLioModule
