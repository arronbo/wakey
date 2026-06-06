// 頭像編解碼：把本機頭像檔壓縮成 Base64 上傳，或把收到的 Base64 寫回本機檔
// 用於跨裝置同步使用者頭像（透過 Firestore profile 文件夾帶 photoBase64 欄位）
package com.wakey.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dir: File
        get() = File(context.filesDir, "friend_avatars").apply { mkdirs() }

    // 本機頭像檔（512 方圖）→ 壓縮成 256 JPEG → Base64（供上傳 Firestore）
    fun encodeForUpload(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return try {
            val bmp = BitmapFactory.decodeFile(path) ?: return null
            val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 收到的 Base64 → 寫入本機檔，回傳路徑供 Avatar 顯示
    // 檔名帶內容 hash：頭像變了會產生新檔（Coil 自動刷新），同時清掉同一好友的舊檔
    // base64 為 null/空 → 刪除該好友所有頭像並回傳 null
    fun saveIncoming(uuid: String, base64: String?): String? {
        val safeUuid = uuid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        if (base64.isNullOrBlank()) {
            dir.listFiles { f -> f.name.startsWith("fa_${safeUuid}_") }?.forEach { it.delete() }
            return null
        }
        return try {
            val hash = base64.hashCode()
            val target = File(dir, "fa_${safeUuid}_$hash.jpg")
            if (!target.exists()) {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                target.outputStream().use { it.write(bytes) }
                // 清理同一好友的舊頭像
                dir.listFiles { f ->
                    f.name.startsWith("fa_${safeUuid}_") && f.name != target.name
                }?.forEach { it.delete() }
            }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
