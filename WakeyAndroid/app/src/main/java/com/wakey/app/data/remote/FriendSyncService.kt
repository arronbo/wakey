// 好友同步服務：把雲端 (Firestore) 的好友清單與每位好友的 profile
// 持續同步到本機 Room（單向：雲端 → 本機）
//
// 流程：
//   1. 訂閱 users/{我的uuid}/friends 子集合 → 拿到目前所有好友 UUID
//   2. 對每個好友 UUID 訂閱 users/{friendUuid} → 對方 profile 變動即更新本機
//   3. 若雲端少了某個好友，從本機 Room 刪除
//   4. profile 一有變動，upsert 進 FriendDao（以 userId 比對既有列）
package com.wakey.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.wakey.app.data.local.entity.FriendEntity
import com.wakey.app.data.repository.AlarmRepository
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.UserProfileRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendSyncService @Inject constructor(
    private val directory: FirestoreUserDirectory,
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val alarmRepository: AlarmRepository,
    private val avatarStore: AvatarStore,
    private val audioStore: AudioStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var rootJob: Job? = null

    // 每個好友 UUID 對應一個 profile 訂閱 Job
    private val profileJobs = mutableMapOf<String, Job>()

    fun start() {
        if (rootJob?.isActive == true) {
            Log.d(TAG, "start() already running")
            return
        }
        rootJob = scope.launch {
            try {
                val profile = userProfileRepository.getOrCreate()
                Log.d(TAG, "start() myUuid=${profile.uuid}")
                if (profile.uuid.isBlank()) return@launch

                // 主動拉一次 FCM token 並 push 至 Firestore，
                // 處理「app 已裝過 token 已存在，onNewToken 不會再觸發」的情境。
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    Log.d(TAG, "FCM token fetched: ${token.take(20)}…")
                    if (token != profile.fcmToken) {
                        userProfileRepository.updateFcmToken(token)
                    }
                }.onFailure { Log.e(TAG, "fetch/push FCM token failed", it) }

                // 計算 / 重新整理「下次響鈴時間」，順便也會更新 profile 並 push 至 Firestore
                runCatching { alarmRepository.refreshNextAlarmStatus() }
                    .onFailure { Log.e(TAG, "refreshNextAlarmStatus failed", it) }

                // 再保底 push 一次 profile（refreshNextAlarmStatus 內若值未變不會 save，故仍需這一次）
                val freshProfile = userProfileRepository.get() ?: profile
                runCatching { directory.pushProfile(freshProfile) }
                    .onSuccess { Log.d(TAG, "ensured profile on Firestore") }
                    .onFailure { Log.e(TAG, "ensure profile push failed", it) }

                directory.observeFriendUuids(profile.uuid).collectLatest { uuids ->
                    Log.d(TAG, "remote friend uuids=$uuids (count=${uuids.size})")
                    syncFriendList(uuids)
                }
            } catch (e: Exception) {
                Log.e(TAG, "FriendSyncService crashed", e)
            }
        }
    }

    companion object { private const val TAG = "FriendSyncService" }

    fun stop() {
        rootJob?.cancel()
        rootJob = null
        profileJobs.values.forEach { it.cancel() }
        profileJobs.clear()
    }

    private suspend fun syncFriendList(remoteUuids: List<String>) {
        val remoteSet = remoteUuids.toSet()

        // 1. 新增/維持：對每個遠端 UUID 啟動 profile 訂閱
        for (uuid in remoteSet) {
            if (profileJobs[uuid]?.isActive == true) continue
            profileJobs[uuid] = scope.launch {
                directory.observeProfile(uuid).collectLatest { remote ->
                    if (remote != null) {
                        upsertLocal(remote)
                    }
                }
            }
        }

        // 2. 移除：雲端已不存在的好友，停掉訂閱並從 Room 刪除
        val removed = profileJobs.keys - remoteSet
        for (uuid in removed) {
            profileJobs[uuid]?.cancel()
            profileJobs.remove(uuid)
            friendRepository.deleteByUserId(uuid)
        }
    }

    private suspend fun upsertLocal(remote: RemoteProfile) {
        val existing = friendRepository.getByUserId(remote.uuid)
        Log.d(TAG, "upsertLocal uuid=${remote.uuid} name=${remote.name} existingId=${existing?.id}")
        val photoPath = avatarStore.saveIncoming(remote.uuid, remote.photoBase64)
        val audioPath = audioStore.saveIncoming(remote.uuid, remote.wakeAudioBase64)
        val entity = FriendEntity(
            id = existing?.id ?: 0,
            userId = remote.uuid,
            name = remote.name,
            handle = remote.handle,
            color = remote.color,
            message = remote.message,
            nextAlarmTime = remote.nextAlarmTime,
            wakeWindowStart = remote.wakeWindowStart,
            wakeWindowEnd = remote.wakeWindowEnd,
            refuseWake = remote.refuseWake,
            fcmToken = remote.fcmToken,
            photoUri = photoPath,
            wakeAudioPath = audioPath
        )
        friendRepository.save(entity.toDomain())
    }
}
