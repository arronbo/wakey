// 領域模型：鬧鐘，供 UI 層與 ViewModel 使用（與 Room Entity 分離）
package com.wakey.app.domain.model

data class Alarm(
    val id: Long = 0,
    val timeHour: Int,           // 0–23
    val timeMinute: Int,         // 0–59
    val label: String = "",
    val repeatMode: RepeatMode = RepeatMode.ONCE,
    val customDays: Set<Int> = emptySet(), // Calendar.MONDAY..SUNDAY
    val ringtone: String = "default",
    val vibrate: Boolean = true,
    val enabled: Boolean = true,
    val sharedFrom: String? = null,     // 好友名稱（若為別人分享的）
    val sharedToFriendIds: List<Long> = emptyList(),
    val sharedToGroupIds: List<Long> = emptyList()
)

enum class RepeatMode { ONCE, DAILY, WEEKDAYS, CUSTOM }
