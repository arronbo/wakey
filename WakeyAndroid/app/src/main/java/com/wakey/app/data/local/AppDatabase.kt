// Room 資料庫入口，整合所有 Entity 與 DAO
package com.wakey.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.wakey.app.data.local.dao.*
import com.wakey.app.data.local.entity.*

@Database(
    entities = [
        AlarmEntity::class,
        FriendEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        UserProfileEntity::class,
        WakeRecordEntity::class,
        WakeEventEntity::class,
        PendingAlarmEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun wakeRecordDao(): WakeRecordDao
    abstract fun wakeEventDao(): WakeEventDao
    abstract fun pendingAlarmDao(): PendingAlarmDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wakey_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
