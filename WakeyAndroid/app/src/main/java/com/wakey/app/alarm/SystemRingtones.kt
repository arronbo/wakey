// 讀取系統內建鈴聲清單（鬧鐘 + 一般鈴聲），供鈴聲選擇與播放使用
package com.wakey.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

data class RingtoneOption(val title: String, val uri: String)

object SystemRingtones {

    // 列出系統可用鈴聲，第一項固定為「預設鬧鈴」
    fun list(context: Context): List<RingtoneOption> {
        val result = mutableListOf<RingtoneOption>()
        result.add(RingtoneOption("預設鬧鈴", "default"))

        try {
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
            val cursor = rm.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = rm.getRingtoneUri(cursor.position)
                if (title != null && uri != null) {
                    result.add(RingtoneOption(title, uri.toString()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // 由 uri 字串解析顯示名稱
    fun titleOf(context: Context, uriStr: String?): String {
        if (uriStr.isNullOrBlank() || uriStr == "default") return "預設鬧鈴"
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(uriStr))?.getTitle(context)
                ?: "自訂鈴聲"
        } catch (e: Exception) {
            "預設鬧鈴"
        }
    }

    // 取得實際可播放的 Uri（"default" → 系統預設鬧鈴；絕對路徑 → file:// URI）
    fun resolveUri(uriStr: String?): Uri? {
        return try {
            when {
                uriStr.isNullOrBlank() || uriStr == "default" ->
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                uriStr.startsWith("/") -> Uri.fromFile(java.io.File(uriStr))
                else -> Uri.parse(uriStr)
            }
        } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
}
