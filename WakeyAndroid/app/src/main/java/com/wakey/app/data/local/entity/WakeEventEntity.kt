// Room Entity：FCM 喚醒事件（誰叫誰起床）
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_events")
data class WakeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val direction: String,    // "out" = 我喚醒對方；"in" = 對方喚醒我
    val peerUid: String       // 對方 uid（out=被喚醒者；in=喚醒者）
)
