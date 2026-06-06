// Room Entity：群組成員關聯表（多對多）
package com.wakey.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "friendId"],
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FriendEntity::class, parentColumns = ["id"], childColumns = ["friendId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("groupId"), Index("friendId")]
)
data class GroupMemberEntity(
    val groupId: Long,
    val friendId: Long
)
