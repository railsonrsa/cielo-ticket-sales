package dev.railson.ticketsales.core.ui.format

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private val brazilianPortuguese = Locale("pt", "BR")

/** Centavos em reais, como "R$ 45,50". */
fun formatBrl(cents: Long): String =
    NumberFormat.getCurrencyInstance(brazilianPortuguese).format(cents / 100.0)

/** Epoch millis UTC na data/hora do fuso do aparelho, como "07/11/2026 20:00". */
fun formatDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, brazilianPortuguese).format(Date(epochMillis))
