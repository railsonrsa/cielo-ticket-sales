package dev.railson.ticketsales.payment.cielolio.callback

import android.util.Base64

/** Uma resposta capturada no emulador, como a Cielo a manda: em Base64. */
internal fun fixtureBase64(name: String): String =
    base64(checkNotNull(object {}.javaClass.getResource("/fixtures/$name")) { "fixture $name" }.readText())

internal fun base64(json: String): String = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
