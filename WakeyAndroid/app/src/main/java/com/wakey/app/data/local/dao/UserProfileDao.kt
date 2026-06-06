// DAO：本機使用者資料（單筆）
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET fcmToken = :token WHERE id = 1")
    suspend fun updateFcmToken(token: String)

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
