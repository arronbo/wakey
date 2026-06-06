// 雲端使用者目錄：以 Firestore 儲存每位使用者的 profile 與好友關聯
// 結構：
//   users/{uuid}                       使用者 profile 文件
//   users/{uuid}/friends/{friendUuid}  好友子集合（一筆 = 一個好友）
package com.wakey.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.wakey.app.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// 從 Firestore 讀到的對方 profile（精簡版，只保留好友卡片會用到的欄位）
data class RemoteProfile(
    val uuid: String,
    val name: String,
    val handle: String,
    val color: String,
    val message: String,
    val fcmToken: String?,
    val wakeWindowStart: String?,
    val wakeWindowEnd: String?,
    val refuseWake: Boolean,
    val nextAlarmTime: String?,
    val photoBase64: String?,
    val wakeAudioBase64: String?
)

// 雲端鬧鐘（對應 AlarmEntity，去掉本機 Long id）
data class RemoteAlarm(
    val cloudId: String,
    val timeHour: Int,
    val timeMinute: Int,
    val label: String,
    val repeatMode: String,
    val customDays: String,
    val ringtone: String,
    val vibrate: Boolean,
    val enabled: Boolean,
    val sharedFrom: String?
)

// 雲端群組（共享：成員與 pending 都能讀到）
data class RemoteGroup(
    val cloudId: String,
    val ownerUid: String,
    val name: String,
    val message: String,
    val color: String?,
    val memberUids: List<String>,
    val pendingUids: List<String> = emptyList(),
    val photoBase64: String? = null   // 群組頭像（所有成員共用），256 JPEG base64
)

@Singleton
class FirestoreUserDirectory @Inject constructor(
    private val avatarStore: AvatarStore,
    private val audioStore: AudioStore
) {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun userDoc(uuid: String) = db.collection("users").document(uuid)
    private fun friendsCol(uuid: String) = userDoc(uuid).collection("friends")

    // ── 推送自己 profile ────────────────────────────────────────────────
    suspend fun pushProfile(profile: UserProfile) {
        if (profile.uuid.isBlank()) return
        val data = mapOf(
            "name" to profile.name,
            "handle" to profile.handle,
            "color" to profile.color,
            "message" to profile.message,
            "fcmToken" to profile.fcmToken,
            "wakeWindowStart" to profile.wakeWindowStart,
            "wakeWindowEnd" to profile.wakeWindowEnd,
            "refuseWake" to profile.refuseWake,
            "nextAlarmTime" to profile.nextAlarmTime,
            "photoBase64" to avatarStore.encodeForUpload(profile.photoUri),
            "wakeAudioBase64" to audioStore.encodeForUpload(profile.wakeAudioPath),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc(profile.uuid).set(data, SetOptions.merge()).await()
    }

    // ── 讀取對方 profile（單次） ────────────────────────────────────────
    suspend fun fetchProfile(uuid: String): RemoteProfile? {
        if (uuid.isBlank()) return null
        val snap = userDoc(uuid).get().await()
        if (!snap.exists()) return null
        return snap.toRemoteProfile(uuid)
    }

    // ── 訂閱對方 profile（即時更新好友卡片資料） ───────────────────────
    fun observeProfile(uuid: String): Flow<RemoteProfile?> = callbackFlow {
        if (uuid.isBlank()) {
            trySend(null); close(); return@callbackFlow
        }
        val reg: ListenerRegistration = userDoc(uuid).addSnapshotListener { snap, err ->
            if (err != null) { trySend(null); return@addSnapshotListener }
            trySend(snap?.takeIf { it.exists() }?.toRemoteProfile(uuid))
        }
        awaitClose { reg.remove() }
    }

    // ── 建立雙向好友關聯 ──────────────────────────────────────────────
    // 同時寫 users/{me}/friends/{other} 與 users/{other}/friends/{me}
    suspend fun addBidirectionalFriendship(myUuid: String, otherUuid: String) {
        if (myUuid.isBlank() || otherUuid.isBlank() || myUuid == otherUuid) {
            Log.w(TAG, "addBidirectionalFriendship skipped (me=$myUuid other=$otherUuid)")
            return
        }
        val batch = db.batch()
        val now = FieldValue.serverTimestamp()
        batch.set(friendsCol(myUuid).document(otherUuid), mapOf("addedAt" to now))
        batch.set(friendsCol(otherUuid).document(myUuid), mapOf("addedAt" to now))
        batch.commit().await()
        Log.d(TAG, "friendship written: $myUuid <-> $otherUuid")
    }

    companion object { private const val TAG = "FirestoreDir" }

    // ── 解除雙向好友關聯 ──────────────────────────────────────────────
    suspend fun removeBidirectionalFriendship(myUuid: String, otherUuid: String) {
        if (myUuid.isBlank() || otherUuid.isBlank()) return
        val batch = db.batch()
        batch.delete(friendsCol(myUuid).document(otherUuid))
        batch.delete(friendsCol(otherUuid).document(myUuid))
        batch.commit().await()
    }

    // ── 訂閱我的好友 UUID 清單 ─────────────────────────────────────────
    fun observeFriendUuids(myUuid: String): Flow<List<String>> = callbackFlow {
        if (myUuid.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val reg: ListenerRegistration = friendsCol(myUuid).addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snap?.documents?.map { it.id } ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    private fun alarmsCol(uid: String) = userDoc(uid).collection("alarms")
    // 群組改為全域 collection，文件存 ownerUid + memberUids，每位成員都看得到
    private fun groupsCol() = db.collection("groups")

    // ── 帳號是否已有資料（profile 文件存在且有名稱）──────────────────────
    suspend fun accountExists(uid: String): Boolean {
        if (uid.isBlank()) return false
        val snap = userDoc(uid).get().await()
        return snap.exists() && !snap.getString("name").isNullOrBlank()
    }

    // ── 鬧鐘 ───────────────────────────────────────────────────────────
    suspend fun pushAlarm(uid: String, a: RemoteAlarm) {
        if (uid.isBlank() || a.cloudId.isBlank()) return
        alarmsCol(uid).document(a.cloudId).set(
            mapOf(
                "timeHour" to a.timeHour, "timeMinute" to a.timeMinute,
                "label" to a.label, "repeatMode" to a.repeatMode,
                "customDays" to a.customDays, "ringtone" to a.ringtone,
                "vibrate" to a.vibrate, "enabled" to a.enabled,
                "sharedFrom" to a.sharedFrom
            )
        ).await()
    }

    suspend fun deleteAlarm(uid: String, cloudId: String) {
        if (uid.isBlank() || cloudId.isBlank()) return
        alarmsCol(uid).document(cloudId).delete().await()
    }

    suspend fun fetchAlarms(uid: String): List<RemoteAlarm> {
        if (uid.isBlank()) return emptyList()
        return alarmsCol(uid).get().await().documents.map { d ->
            RemoteAlarm(
                cloudId = d.id,
                timeHour = (d.getLong("timeHour") ?: 0).toInt(),
                timeMinute = (d.getLong("timeMinute") ?: 0).toInt(),
                label = d.getString("label") ?: "",
                repeatMode = d.getString("repeatMode") ?: "ONCE",
                customDays = d.getString("customDays") ?: "",
                ringtone = d.getString("ringtone") ?: "default",
                vibrate = d.getBoolean("vibrate") ?: true,
                enabled = d.getBoolean("enabled") ?: true,
                sharedFrom = d.getString("sharedFrom")
            )
        }
    }

    // ── 群組（全域共享） ─────────────────────────────────────────────
    suspend fun pushGroup(g: RemoteGroup) {
        if (g.cloudId.isBlank() || g.ownerUid.isBlank()) return
        groupsCol().document(g.cloudId).set(
            mapOf(
                "ownerUid" to g.ownerUid,
                "name" to g.name, "message" to g.message,
                "color" to g.color,
                "memberUids" to g.memberUids,
                "pendingUids" to g.pendingUids,
                "photoBase64" to g.photoBase64,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteGroup(cloudId: String) {
        if (cloudId.isBlank()) return
        groupsCol().document(cloudId).delete().await()
    }

    // 邀請成員（owner 用）：把 invitedUid 加到 pendingUids
    suspend fun inviteToGroup(cloudId: String, invitedUid: String) {
        if (cloudId.isBlank() || invitedUid.isBlank()) return
        groupsCol().document(cloudId).update(
            "pendingUids", FieldValue.arrayUnion(invitedUid)
        ).await()
    }

    // 取消邀請（owner 用）
    suspend fun cancelInvite(cloudId: String, invitedUid: String) {
        if (cloudId.isBlank() || invitedUid.isBlank()) return
        groupsCol().document(cloudId).update(
            "pendingUids", FieldValue.arrayRemove(invitedUid)
        ).await()
    }

    // 接受邀請（被邀請者用）：原子地把自己從 pendingUids 移到 memberUids
    suspend fun acceptGroupInvite(cloudId: String, myUid: String) {
        if (cloudId.isBlank() || myUid.isBlank()) return
        groupsCol().document(cloudId).update(
            mapOf(
                "pendingUids" to FieldValue.arrayRemove(myUid),
                "memberUids" to FieldValue.arrayUnion(myUid)
            )
        ).await()
    }

    // 拒絕邀請：把自己從 pendingUids 移除
    suspend fun rejectGroupInvite(cloudId: String, myUid: String) {
        if (cloudId.isBlank() || myUid.isBlank()) return
        groupsCol().document(cloudId).update(
            "pendingUids", FieldValue.arrayRemove(myUid)
        ).await()
    }

    // 退出群組：把自己從 memberUids 移除（owner 不能退出，需用刪除群組）
    suspend fun leaveGroup(cloudId: String, myUid: String) {
        if (cloudId.isBlank() || myUid.isBlank()) return
        groupsCol().document(cloudId).update(
            "memberUids", FieldValue.arrayRemove(myUid)
        ).await()
    }

    // 掃 QR 直接加入群組：把自己加入 memberUids（無須 pending）
    suspend fun joinGroupByQr(cloudId: String, myUid: String) {
        if (cloudId.isBlank() || myUid.isBlank()) return
        groupsCol().document(cloudId).update(
            "memberUids", FieldValue.arrayUnion(myUid)
        ).await()
    }

    // 一次性取得群組（QR 掃描後檢查群組是否存在用）
    suspend fun fetchGroup(cloudId: String): RemoteGroup? {
        if (cloudId.isBlank()) return null
        val snap = groupsCol().document(cloudId).get().await()
        if (!snap.exists()) return null
        return snap.toRemoteGroup()
    }

    // 訂閱「memberUids 或 pendingUids 含有我」的所有群組
    // pendingUids 也包含進來：通知頁要能顯示「我被邀請的群組」
    fun observeGroupsForMember(myUid: String): Flow<List<RemoteGroup>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val acc = mutableMapOf<String, RemoteGroup>()
        fun emit() = trySend(acc.values.toList())
        val regMember = groupsCol()
            .whereArrayContains("memberUids", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.documentChanges?.forEach { ch ->
                    when (ch.type) {
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED ->
                            acc.remove(ch.document.id)
                        else -> acc[ch.document.id] = ch.document.toRemoteGroup()
                    }
                }
                emit()
            }
        val regPending = groupsCol()
            .whereArrayContains("pendingUids", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.documentChanges?.forEach { ch ->
                    when (ch.type) {
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            // 別誤刪：若另一查詢仍有就保留
                            // 簡化：只在這份 query 內無此 doc 時才刪。不易判斷，先保留。
                        }
                        else -> acc[ch.document.id] = ch.document.toRemoteGroup()
                    }
                }
                emit()
            }
        awaitClose { regMember.remove(); regPending.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toRemoteGroup() = RemoteGroup(
        cloudId = id,
        ownerUid = getString("ownerUid") ?: "",
        name = getString("name") ?: "",
        message = getString("message") ?: "",
        color = getString("color"),
        memberUids = (get("memberUids") as? List<String>) ?: emptyList(),
        pendingUids = (get("pendingUids") as? List<String>) ?: emptyList(),
        photoBase64 = getString("photoBase64")
    )

    // ── 喚醒語音訊息（臨時，一次性） ──────────────────────────────────
    // 結構：wake_messages/{messageId} = { senderUid, targetUid, audioBase64, createdAt }
    // 用途：A 按「錄音叫醒」B 時上傳，B 收到 FCM 後抓下來播，播完刪
    private fun wakeMsgCol() = db.collection("wake_messages")

    suspend fun uploadWakeMessage(
        senderUid: String,
        targetUid: String,
        audioBase64: String
    ): String {
        val doc = wakeMsgCol().document()
        doc.set(
            mapOf(
                "senderUid" to senderUid,
                "targetUid" to targetUid,
                "audioBase64" to audioBase64,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return doc.id
    }

    suspend fun fetchWakeMessage(messageId: String): String? {
        if (messageId.isBlank()) return null
        val snap = wakeMsgCol().document(messageId).get().await()
        if (!snap.exists()) return null
        return snap.getString("audioBase64")
    }

    suspend fun deleteWakeMessage(messageId: String) {
        if (messageId.isBlank()) return
        runCatching { wakeMsgCol().document(messageId).delete().await() }
    }

    // ── 設定（存在 users/{uid} 的 settings map 欄位）─────────────────────
    suspend fun pushSettings(uid: String, settings: Map<String, Any?>) {
        if (uid.isBlank()) return
        userDoc(uid).set(mapOf("settings" to settings), SetOptions.merge()).await()
    }

    suspend fun fetchSettings(uid: String): Map<String, Any?>? {
        if (uid.isBlank()) return null
        val snap = userDoc(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        return snap.get("settings") as? Map<String, Any?>
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRemoteProfile(uuid: String) =
        RemoteProfile(
            uuid = uuid,
            name = getString("name") ?: "朋友",
            handle = getString("handle") ?: "",
            color = getString("color") ?: "#FF8A6B",
            message = getString("message") ?: "",
            fcmToken = getString("fcmToken"),
            wakeWindowStart = getString("wakeWindowStart"),
            wakeWindowEnd = getString("wakeWindowEnd"),
            refuseWake = getBoolean("refuseWake") ?: false,
            nextAlarmTime = getString("nextAlarmTime"),
            photoBase64 = getString("photoBase64"),
            wakeAudioBase64 = getString("wakeAudioBase64")
        )
}
