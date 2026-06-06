// 從相簿選取的圖片中辨識 QR Code（ML Kit，靜態圖片）
package com.wakey.app.qr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 對使用者從相簿選取的圖片做 QR 辨識。
 * @return QR 內含的原始字串；找不到回傳 null。
 */
suspend fun scanQrFromImage(context: Context, uri: Uri): String? =
    suspendCancellableCoroutine { cont ->
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
        if (image == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes
                    .firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                    ?.rawValue
                cont.resume(value)
            }
            .addOnFailureListener { cont.resume(null) }
            .addOnCompleteListener { scanner.close() }
    }
