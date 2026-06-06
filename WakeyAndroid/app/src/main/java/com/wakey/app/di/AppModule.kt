// Hilt 模組：提供 FCM 推播發送 Helper 的依賴
package com.wakey.app.di

import android.content.Context
import com.wakey.app.data.remote.FcmSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFcmSender(@ApplicationContext ctx: Context): FcmSender = FcmSender()
}
