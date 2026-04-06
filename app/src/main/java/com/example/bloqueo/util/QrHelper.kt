package com.example.bloqueo.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object QrHelper {

    /**
     * Genera un token único para el QR de desactivación del modo estricto.
     */
    fun generateToken(): String = UUID.randomUUID().toString().replace("-", "")

    /**
     * Genera un Bitmap con el código QR que codifica el texto dado.
     * @param text texto a codificar (p. ej. el token)
     * @param sizePx tamaño del bitmap (ancho y alto)
     */
    fun encodeToQrBitmap(text: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Int>().apply {
                put(EncodeHintType.MARGIN, 1)
            }
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
