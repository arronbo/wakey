// Repository：本機使用者資料（單筆）
// 任何寫入除了存進 Room，也會 push 一份到 Firestore 的 users/{uuid}
// 讓掃描的好友能即時拿到最新 profile（姓名、handle、顏色、fcmToken…）
package com.wakey.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.wakey.app.data.local.dao.UserProfileDao
import com.wakey.app.data.local.entity.UserProfileEntity
import com.wakey.app.data.remote.AudioStore
import com.wakey.app.data.remote.AvatarStore
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.remote.RemoteProfile
import com.wakey.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val directory: FirestoreUserDirectory,
    private val auth: FirebaseAuth,
    private val avatarStore: AvatarStore,
    private val audioStore: AudioStore
) {
    val profile: Flow<UserProfile?> = dao.observe().map { it?.toDomain() }

    suspend fun get(): UserProfile? = dao.get()?.toDomain()

    suspend fun save(profile: UserProfile) {
        dao.upsert(UserProfileEntity.fromDomain(profile))
        runCatching { directory.pushProfile(profile) }
            .onFailure { Log.e(TAG, "pushProfile failed (uuid=${profile.uuid})", it) }
            .onSuccess { Log.d(TAG, "pushProfile ok (uuid=${profile.uuid})") }
    }

    suspend fun updateFcmToken(token: String) {
        dao.updateFcmToken(token)
        // 重新讀回完整 profile，再 push 一次，確保 Firestore 上 fcmToken 也同步
        get()?.let {
            runCatching { directory.pushProfile(it) }
                .onFailure { e -> Log.e(TAG, "pushProfile (fcm) failed", e) }
        }
    }

    companion object { private const val TAG = "UserProfileRepo" }

    // 取得 / 建立本機 profile。
    // 已登入時：身份一律對齊 Firebase uid（本機若是別的 uuid 會被改綁到目前帳號）。
    // 未登入時（理論上 gate 後不會發生）：維持舊的隨機 UUID 行為。
    suspend fun getOrCreate(): UserProfile {
        val uid = auth.currentUser?.uid
        val existing = get()
        if (uid != null) {
            if (existing != null && existing.uuid == uid) return existing
            val base = existing?.copy(uuid = uid) ?: UserProfile(
                id = 1,
                uuid = uid,
                name = auth.currentUser?.displayName ?: "使用者",
                handle = "user_${uid.take(6)}"
            )
            save(base)
            return base
        }
        return existing ?: UserProfile(
            id = 1,
            uuid = UUID.randomUUID().toString(),
            name = "使用者",
            handle = "user_${(1000..9999).random()}"
        ).also { save(it) }
    }

    // 以雲端 profile 覆蓋本機（登入下載用）。保留本機 fcmToken（per-device）。
    suspend fun applyRemote(remote: RemoteProfile) {
        val localToken = get()?.fcmToken
        val photoPath = avatarStore.saveIncoming(remote.uuid, remote.photoBase64)
        val audioPath = audioStore.saveMyFromBase64(remote.wakeAudioBase64)
        val profile = UserProfile(
            id = 1, uuid = remote.uuid, name = remote.name, handle = remote.handle,
            color = remote.color, message = remote.message,
            wakeWindowStart = remote.wakeWindowStart, wakeWindowEnd = remote.wakeWindowEnd,
            refuseWake = remote.refuseWake, fcmToken = localToken ?: remote.fcmToken,
            photoUri = photoPath, nextAlarmTime = remote.nextAlarmTime,
            wakeAudioPath = audioPath
        )
        dao.upsert(UserProfileEntity.fromDomain(profile))
    }

    suspend fun clearLocal() = dao.deleteAll()

    // 登入成功後呼叫：確保本機 profile 綁定目前帳號（帶入 Google 顯示名作初始）
    suspend fun ensureForUser(uid: String, displayName: String?): UserProfile {
        val existing = get()
        if (existing != null && existing.uuid == uid) return existing
        val profile = UserProfile(
            id = 1,
            uuid = uid,
            name = displayName ?: existing?.name ?: "使用者",
            handle = "user_${uid.take(6)}"
        )
        save(profile)
        return profile
    }
}
