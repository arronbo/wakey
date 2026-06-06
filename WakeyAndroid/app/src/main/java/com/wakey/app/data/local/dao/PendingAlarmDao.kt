// DAO：待確認共用鬧鐘
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.PendingAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAlarmDao {
    @Query("SELECT * FROM pending_alarms ORDER BY receivedMillis DESC")
    fun observeAll(): Flow<List<PendingAlarmEntity>>

    @Query("SELECT * FROM pending_alarms WHERE id = :id")
    suspend fun getById(id: Long): PendingAlarmEntity?

    @Insert
    suspend fun insert(entity: PendingAlarmEntity): Long

    @Query("DELETE FROM pending_alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_alarms")
    suspend fun deleteAll()
}
