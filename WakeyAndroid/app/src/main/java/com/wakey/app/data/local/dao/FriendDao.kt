// DAO：好友的 CRUD 操作
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id")
    suspend fun getById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE userId = :userId")
    suspend fun getByUserId(userId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity): Long

    @Update
    suspend fun update(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM friends WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()

    @Query("UPDATE friends SET fcmToken = :token WHERE id = :id")
    suspend fun updateFcmToken(id: Long, token: String)
}
