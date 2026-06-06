// 領域模型：本機使用者資料
package com.wakey.app.domain.model

data class UserProfile(
    val id: Long = 1,
    val uuid: String,            // 用於 QR Code 配對
    val name: String,
    val handle: String,
    val color: String = "#FF8A6B",
    val message: String = "",
    val wakeWindowStart: String? = "06:00",
    val wakeWindowEnd: String? = "10:00",
    val refuseWake: Boolean = false,
    val fcmToken: String? = null,
    val photoUri: String? = null,  // 使用者自選頭像（內部儲存路徑）
    val nextAlarmTime: String? = null,  // 自己下一顆鬧鐘 "HH:mm"，由 AlarmRepository 計算後 push
    val wakeAudioPath: String? = null  // 我的喚醒鈴聲本機路徑（被好友叫醒時對方裝置會用到的音檔）
)
