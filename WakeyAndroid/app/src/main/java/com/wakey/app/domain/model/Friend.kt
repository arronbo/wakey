// 領域模型：好友
package com.wakey.app.domain.model

import java.time.LocalTime
import java.time.format.DateTimeParseException

data class Friend(
    val id: Long = 0,
    val userId: String,              // 對方的 UUID（掃 QR 取得）
    val name: String,
    val handle: String,
    val color: String = "#FF8A6B",
    val message: String = "",
    val nextAlarmTime: String? = null,    // 對方下一顆鬧鐘 "HH:mm"，由對方 push 上 Firestore
    val wakeWindowStart: String? = null,  // "HH:mm"
    val wakeWindowEnd: String? = null,
    val refuseWake: Boolean = false,      // 由對方設定，是否拒絕被叫醒
    val fcmToken: String? = null,
    val photoUri: String? = null,         // 同步自雲端的頭像（本機落地路徑）
    val wakeAudioPath: String? = null     // 對方設定的喚醒鈴聲檔（本機落地路徑），叫醒對方時使用
) {
    // 動態計算：現在是否能叫醒這位好友
    // = 沒勾「拒絕喚醒」 且 有設定喚醒時段 且 現在時間落在時段內
    val canWake: Boolean
        get() = isCurrentlyWakeable(refuseWake, wakeWindowStart, wakeWindowEnd, LocalTime.now())
}

private fun isCurrentlyWakeable(
    refuseWake: Boolean,
    start: String?,
    end: String?,
    now: LocalTime
): Boolean {
    if (refuseWake) return false
    if (start.isNullOrBlank() || end.isNullOrBlank()) return false
    val s = parseHm(start) ?: return false
    val e = parseHm(end) ?: return false
    return if (s <= e) {
        now in s..e
    } else {
        // 跨午夜（例如 22:00 – 09:00）
        now >= s || now <= e
    }
}

private fun parseHm(s: String): LocalTime? = try {
    LocalTime.parse(s)
} catch (_: DateTimeParseException) { null }
