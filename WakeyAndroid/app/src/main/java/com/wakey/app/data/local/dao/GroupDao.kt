// DAO：群組與群組成員的操作
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.GroupEntity
import com.wakey.app.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isJoined = 1 ORDER BY name")
    fun observeJoined(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isJoined = 0 ORDER BY name")
    fun observePending(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?

    @Query("SELECT * FROM groups WHERE cloudId = :cloudId")
    suspend fun getByCloudId(cloudId: String): GroupEntity?

    @Query("SELECT * FROM groups")
    suspend fun getAll(): List<GroupEntity>

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    // 成員關聯
    @Query("SELECT friendId FROM group_members WHERE groupId = :groupId")
    suspend fun getMemberIds(groupId: Long): List<Long>

    @Query("SELECT friendId FROM group_members WHERE groupId = :groupId")
    fun observeMemberIds(groupId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: GroupMemberEntity)

    @Delete
    suspend fun removeMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearMembers(groupId: Long)

    @Query("DELETE FROM group_members")
    suspend fun deleteAllMembers()

    @Transaction
    suspend fun replaceMembers(groupId: Long, friendIds: List<Long>) {
        clearMembers(groupId)
        friendIds.forEach { addMember(GroupMemberEntity(groupId, it)) }
    }
}
