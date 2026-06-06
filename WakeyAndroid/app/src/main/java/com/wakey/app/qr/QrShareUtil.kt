// QR 圖片的儲存（存到相簿）與分享（透過 FileProvider）工具
package com.wakey.app.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrShareUtil {

    /**
     * 把 QR 圖片存進系統相簿（Pictures/Wakey）。
     * @return 成功與否
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Wakey")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)

    /**
     * 透過 FileProvider 分享 QR 圖片（存到 cache/shared_qr 再用 ACTION_SEND）。
     */
    fun shareImage(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String, text: String? = null) {
        runCatching {
            val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val file = File(dir, "$fileName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                text?.let { putExtra(Intent.EXTRA_TEXT, it) }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
    }
}

/**
 * 從掃描/辨識結果取出對方 uid。
 * 支援兩種格式：純 uid，或加好友連結（wakey://add-friend?uid=xxx、https://.../?type=friend&uid=xxx）。
 */
fun extractFriendUid(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.contains("://") && !trimmed.contains("?")) return trimmed
    return runCatching {
        android.net.Uri.parse(trimmed).getQueryParameter("uid")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: trimmed
}
