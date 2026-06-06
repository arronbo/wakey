// QR Code 產生器：將使用者 UUID 編碼為 Bitmap，供「我的 QR」畫面顯示
package com.wakey.app.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeGenerator @Inject constructor() {

    // 產生包含 userId 的 QR Code Bitmap（預設 512×512）
    fun generate(userId: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val matrix = writer.encode(userId, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
