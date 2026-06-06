// Hilt 模組：提供 Room 資料庫、DAO、Repository 的依賴注入綁定
package com.wakey.app.di

import android.content.Context
import com.wakey.app.data.local.AppDatabase
import com.wakey.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        AppDatabase.getInstance(ctx)

    @Provides @Singleton
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides @Singleton
    fun provideFriendDao(db: AppDatabase): FriendDao = db.friendDao()

    @Provides @Singleton
    fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()

    @Provides @Singleton
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides @Singleton
    fun provideWakeRecordDao(db: AppDatabase): WakeRecordDao = db.wakeRecordDao()

    @Provides @Singleton
    fun provideWakeEventDao(db: AppDatabase): WakeEventDao = db.wakeEventDao()

    @Provides @Singleton
    fun providePendingAlarmDao(db: AppDatabase): PendingAlarmDao = db.pendingAlarmDao()
}
