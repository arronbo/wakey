// Room Entity：好友資料表
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wakey.app.domain.model.Friend

@Entity(
    tableName = "friends",
    indices = [Index(value = ["userId"], unique = true)]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val handle: String,
    val color: String = "#FF8A6B",
    val message: String = "",
    val nextAlarmTime: String? = null,
    val wakeWindowStart: String? = null,
    val wakeWindowEnd: String? = null,
    val refuseWake: Boolean = false,
    val fcmToken: String? = null,
    val photoUri: String? = null,
    val wakeAudioPath: String? = null
) {
    fun toDomain() = Friend(
        id = id, userId = userId, name = name, handle = handle,
        color = color, message = message, nextAlarmTime = nextAlarmTime,
        wakeWindowStart = wakeWindowStart, wakeWindowEnd = wakeWindowEnd,
        refuseWake = refuseWake, fcmToken = fcmToken, photoUri = photoUri,
        wakeAudioPath = wakeAudioPath
    )

    companion object {
        fun fromDomain(f: Friend) = FriendEntity(
            id = f.id, userId = f.userId, name = f.name, handle = f.handle,
            color = f.color, message = f.message, nextAlarmTime = f.nextAlarmTime,
            wakeWindowStart = f.wakeWindowStart, wakeWindowEnd = f.wakeWindowEnd,
            refuseWake = f.refuseWake, fcmToken = f.fcmToken, photoUri = f.photoUri,
            wakeAudioPath = f.wakeAudioPath
        )
    }
}
