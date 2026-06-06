// 同步協調器：登入後依帳號雲端狀態決定「下載覆蓋本機」或「上傳本機作初始」；登出清本機
package com.wakey.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.wakey.app.data.datastore.SettingsDataStore
import com.wakey.app.data.repository.AlarmRepository
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.data.repository.WakeAnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val directory: FirestoreUserDirectory,
    private val userProfileRepository: UserProfileRepository,
    private val alarmRepository: AlarmRepository,
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository,
    private val settingsDataStore: SettingsDataStore,
    private val friendSyncService: FriendSyncService,
    private val groupSyncService: GroupSyncService,
    private val wakeAnalytics: WakeAnalyticsRepository,
    private val pendingAlarmRepository: com.wakey.app.data.repository.PendingAlarmRepository,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onSignedIn(uid: String, displayName: String?) {
        scope.launch {
            try {
                // 必須在 ensureForUser 之前判斷，否則 profile 一上雲就被誤判為舊帳號
                val exists = directory.accountExists(uid)
                if (exists) {
                    Log.d(TAG, "account exists → pullAll")
                    pullAll(uid)
                } else {
                    Log.d(TAG, "new account → ensure + pushAll")
                    userProfileRepository.ensureForUser(uid, displayName)
                    pushAll(uid)
                }
                // 刷新本機 FCM token 上雲（覆蓋下載來的舊裝置 token）
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    userProfileRepository.updateFcmToken(token)
                }
                friendSyncService.start()
                groupSyncService.start()
            } catch (e: Exception) {
                Log.e(TAG, "onSignedIn sync failed", e)
            }
        }
    }

    fun onSignedOut() {
        friendSyncService.stop()
        groupSyncService.stop()
        scope.launch {
            runCatching {
                alarmRepository.clearLocal()
                groupRepository.clearLocal()
                friendRepository.clearLocal()
                userProfileRepository.clearLocal()
                settingsDataStore.clearLocal()
                wakeAnalytics.clearLocal()
                pendingAlarmRepository.clearLocal()
            }.onFailure { Log.e(TAG, "clearLocal failed", it) }
        }
    }

    // 下載覆蓋本機（friends / groups 由各自 SyncService 實時訂閱，這裡不重複）
    private suspend fun pullAll(uid: String) {
        directory.fetchProfile(uid)?.let { userProfileRepository.applyRemote(it) }
        alarmRepository.applyRemote(directory.fetchAlarms(uid))
        directory.fetchSettings(uid)?.let { settingsDataStore.applyRemote(it) }
    }

    // 上傳本機作為帳號初始資料（profile 已由 ensureForUser push）
    private suspend fun pushAll(uid: String) {
        alarmRepository.pushAllToCloud()
        groupRepository.pushAllToCloud()
        settingsDataStore.pushSettings()
    }

    companion object { private const val TAG = "SyncManager" }
}
