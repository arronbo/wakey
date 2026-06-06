// Application 入口：初始化 Hilt，依登入狀態啟動 / 停止雲端同步
package com.wakey.app

import android.app.Application
import com.wakey.app.data.remote.AuthRepository
import com.wakey.app.data.remote.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WakeyApplication : Application() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var syncManager: SyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 登入 → 決定下載/上傳並開始同步；登出 → 停止同步並清本機
        appScope.launch {
            authRepository.authState.collectLatest { user ->
                if (user != null) {
                    syncManager.onSignedIn(user.uid, user.displayName)
                } else {
                    syncManager.onSignedOut()
                }
            }
        }
    }
}
