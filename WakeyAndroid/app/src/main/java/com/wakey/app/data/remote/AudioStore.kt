// 喚醒音檔編解碼：本機 m4a → Base64 上傳；下載 Base64 → 本機檔。
// 用於跨裝置同步「我的喚醒鈴聲」（透過 Firestore profile 的 wakeAudioBase64 欄位）。
package com.wakey.app.data.remote

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 自己錄製 / 挑選的音檔放這個目錄（給上傳用）
    private val myDir: File get() = File(context.filesDir, "my_audio").apply { mkdirs() }

    // 好友同步下來的音檔放這個目錄（供鬧鐘服務讀取）
    private val friendDir: File get() = File(context.filesDir, "friend_audio").apply { mkdirs() }

    // Firestore profile 文件約 1MB 上限，扣掉其他欄位保留約 700KB（Base64 後）給音檔
    private val maxBase64Size = 700_000

    /** 我的音檔路徑（持久檔名，覆蓋寫入） */
    fun myAudioFile(): File = File(myDir, "wake_audio.m4a")

    /** 從外部 Uri（檔案選擇器）複製到本機；回傳檔案路徑或 null */
    fun importFromUri(uri: android.net.Uri): String? = try {
        val target = myAudioFile()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        if (target.length() > 0) target.absolutePath else null
    } catch (e: Exception) {
        e.printStackTrace(); null
    }

    /** 將本機音檔編碼成 Base64；超過大小上限或不存在則回傳 null */
    fun encodeForUpload(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.exists() || f.length() == 0L) return null
        return try {
            val bytes = f.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (b64.length > maxBase64Size) null else b64
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** 將 Base64 寫入本機 friend_audio/{uid}.m4a；null/blank → 刪除原檔並回傳 null */
    fun saveIncoming(uuid: String, base64: String?): String? {
        val safeUuid = uuid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val target = File(friendDir, "fa_$safeUuid.m4a")
        if (base64.isNullOrBlank()) {
            target.delete()
            return null
        }
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            target.outputStream().use { it.write(bytes) }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** 把雲端 Base64 寫入「我的」目錄（applyRemote 登入下載時用） */
    fun saveMyFromBase64(base64: String?): String? {
        val target = myAudioFile()
        if (base64.isNullOrBlank()) {
            target.delete()
            return null
        }
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            target.outputStream().use { it.write(bytes) }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** 將臨時喚醒訊息音檔（Base64）寫到 cache，回傳本機路徑 */
    fun saveTransientWakeMessage(base64: String): String? {
        return try {
            val cacheDir = File(context.cacheDir, "wake_msgs").apply { mkdirs() }
            val target = File(cacheDir, "wm_${System.currentTimeMillis()}.m4a")
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            target.outputStream().use { it.write(bytes) }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** 取得好友音檔的本機路徑（若存在） */
    fun friendAudioPath(uuid: String): String? {
        val safeUuid = uuid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val f = File(friendDir, "fa_$safeUuid.m4a")
        return if (f.exists() && f.length() > 0) f.absolutePath else null
    }
}
