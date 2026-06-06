// DAO：鬧鐘關閉紀錄
package com.wakey.app.data.local.dao

import androidx.room.*
import com.wakey.app.data.local.entity.WakeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeRecordDao {
    @Query("SELECT * FROM wake_records ORDER BY dismissedMillis DESC")
    fun observeAll(): Flow<List<WakeRecordEntity>>

    @Insert
    suspend fun insert(record: WakeRecordEntity): Long

    @Query("DELETE FROM wake_records")
    suspend fun deleteAll()
}
