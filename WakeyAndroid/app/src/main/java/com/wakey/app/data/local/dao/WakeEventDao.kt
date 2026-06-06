// DAO：FCM 喚醒事件
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.WakeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeEventDao {
    @Query("SELECT * FROM wake_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<WakeEventEntity>>

    @Insert
    suspend fun insert(event: WakeEventEntity): Long

    @Query("DELETE FROM wake_events")
    suspend fun deleteAll()
}
