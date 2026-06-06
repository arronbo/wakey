// Room Entity：群組資料表
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wakey.app.domain.model.Group

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cloudId: String = "",
    val ownerUid: String = "",
    val name: String,
    val message: String = "",
    val color: String? = null,
    val photoUri: String? = null,
    // 已加入成員 uid（雲端 source-of-truth；不含 owner 自己）
    val memberUidsJoined: String = "",
    // 待邀請成員 uid（雲端 source-of-truth）
    val pendingUidsJoined: String = "",
    // 我是否已是這個群組的成員（含 owner）。false = 只是被邀請。
    val isJoined: Boolean = true
) {
    fun toDomain(memberIds: List<Long>): Group {
        return Group(
            id = id, cloudId = cloudId, ownerUid = ownerUid,
            name = name, message = message,
            color = color, photoUri = photoUri,
            memberIds = memberIds,
            memberUids = memberUidsJoined.toUidList(),
            pendingUids = pendingUidsJoined.toUidList()
        )
    }
}

private fun String.toUidList(): List<String> =
    if (isBlank()) emptyList()
    else split(",").map { it.trim() }.filter { it.isNotBlank() }
