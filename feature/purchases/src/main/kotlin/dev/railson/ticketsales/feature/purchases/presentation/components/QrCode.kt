package dev.railson.ticketsales.feature.purchases.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * O [content] num QR, sempre preto no branco (no tema escuro também: leitores esperam esse contraste).
 * O ZXing gera a matriz com um módulo por célula; o Canvas a escala sem perder nitidez.
 */
@Composable
internal fun QrCode(content: String, contentDescription: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to QUIET_ZONE_MODULES))
    }
    Canvas(modifier.aspectRatio(1f).semantics { this.contentDescription = contentDescription }) {
        val cell = size.width / matrix.width
        drawRect(Color.White)
        // Um Path só: as células vizinhas se fundem, sem as frestas do antialias entre retângulos separados.
        drawPath(matrix.toPath(cell), Color.Black)
    }
}

private fun BitMatrix.toPath(cell: Float) = Path().apply {
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (get(x, y)) addRect(Rect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell))
        }
    }
}

/** A margem branca mínima que a norma do QR pede é 4 módulos. */
private const val QUIET_ZONE_MODULES = 4
