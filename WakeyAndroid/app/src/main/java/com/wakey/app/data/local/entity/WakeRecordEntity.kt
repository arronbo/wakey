// Room Entity：鬧鐘關閉紀錄
// 一次紀錄 = 一次鬧鐘響起後使用者關掉的事件
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_records")
data class WakeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmStartMillis: Long,    // 鬧鐘服務啟動時間（≈ 鬧鐘觸發時刻）
    val dismissedMillis: Long,     // 使用者按下「關閉」的時間
    val snoozeMs: Long,            // dismissedMillis - alarmStartMillis（賴床時長）
    val source: String,            // "scheduled"（自己的鬧鐘）/ "remote"（朋友叫醒）
    val wasEarly: Boolean          // 是否在 earlyWakeTime ± 2 分鐘內關掉
)
