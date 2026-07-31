package com.honoka.deepseekwatch.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? = try {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) for (y in 0 until sizePx) {
        bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
    }
    bitmap
} catch (_: Exception) { null }
