// Room Entity：鬧鐘資料表
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakey.app.domain.model.Alarm
import com.wakey.app.domain.model.RepeatMode

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cloudId: String = "",   // 跨裝置穩定 ID（雲端 alarms 子集合文件 ID）
    val timeHour: Int,
    val timeMinute: Int,
    val label: String = "",
    val repeatMode: String = RepeatMode.ONCE.name,
    val customDays: String = "",          // 逗號分隔整數，如 "2,3,4"
    val ringtone: String = "default",
    val vibrate: Boolean = true,
    val enabled: Boolean = true,
    val sharedFrom: String? = null,
    val sharedToFriendIds: String = "",   // 逗號分隔 Long
    val sharedToGroupIds: String = ""
) {
    fun toDomain() = Alarm(
        id = id,
        timeHour = timeHour,
        timeMinute = timeMinute,
        label = label,
        repeatMode = RepeatMode.valueOf(repeatMode),
        customDays = customDays.toIntSet(),
        ringtone = ringtone,
        vibrate = vibrate,
        enabled = enabled,
        sharedFrom = sharedFrom,
        sharedToFriendIds = sharedToFriendIds.toLongList(),
        sharedToGroupIds = sharedToGroupIds.toLongList()
    )

    companion object {
        fun fromDomain(a: Alarm) = AlarmEntity(
            id = a.id,
            timeHour = a.timeHour,
            timeMinute = a.timeMinute,
            label = a.label,
            repeatMode = a.repeatMode.name,
            customDays = a.customDays.joinToString(","),
            ringtone = a.ringtone,
            vibrate = a.vibrate,
            enabled = a.enabled,
            sharedFrom = a.sharedFrom,
            sharedToFriendIds = a.sharedToFriendIds.joinToString(","),
            sharedToGroupIds = a.sharedToGroupIds.joinToString(",")
        )
    }
}

private fun String.toIntSet(): Set<Int> =
    if (isBlank()) emptySet() else split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

private fun String.toLongList(): List<Long> =
    if (isBlank()) emptyList() else split(",").mapNotNull { it.trim().toLongOrNull() }
