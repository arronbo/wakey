// Room Entity：待確認的共用鬧鐘
// 朋友透過 FCM 共用鬧鐘來，先存在這張表，使用者到通知頁接受才會轉成正式 alarm
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_alarms")
data class PendingAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeHour: Int,
    val timeMinute: Int,
    val label: String,
    val ringtone: String,
    val vibrate: Boolean,
    val senderName: String,    // 共用者顯示名稱
    val receivedMillis: Long   // 收到時間（排序用）
)
