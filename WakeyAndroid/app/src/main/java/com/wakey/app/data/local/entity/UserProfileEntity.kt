// Room Entity：本機使用者資料（單筆，id 固定為 1）
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakey.app.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1,
    val uuid: String,
    val name: String,
    val handle: String,
    val color: String = "#FF8A6B",
    val message: String = "",
    val wakeWindowStart: String? = "06:00",
    val wakeWindowEnd: String? = "10:00",
    val refuseWake: Boolean = false,
    val fcmToken: String? = null,
    val photoUri: String? = null,
    val nextAlarmTime: String? = null,
    val wakeAudioPath: String? = null
) {
    fun toDomain() = UserProfile(
        id = id, uuid = uuid, name = name, handle = handle,
        color = color, message = message,
        wakeWindowStart = wakeWindowStart, wakeWindowEnd = wakeWindowEnd,
        refuseWake = refuseWake, fcmToken = fcmToken, photoUri = photoUri,
        nextAlarmTime = nextAlarmTime, wakeAudioPath = wakeAudioPath
    )

    companion object {
        fun fromDomain(p: UserProfile) = UserProfileEntity(
            id = p.id, uuid = p.uuid, name = p.name, handle = p.handle,
            color = p.color, message = p.message,
            wakeWindowStart = p.wakeWindowStart, wakeWindowEnd = p.wakeWindowEnd,
            refuseWake = p.refuseWake, fcmToken = p.fcmToken, photoUri = p.photoUri,
            nextAlarmTime = p.nextAlarmTime, wakeAudioPath = p.wakeAudioPath
        )
    }
}
