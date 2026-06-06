// Repository：群組（共享，邀請制；與所有成員/被邀請者同步）
// 雲端存在全域 groups/{cloudId}；本機 Room 為 cache
package com.wakey.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.wakey.app.data.local.dao.GroupDao
import com.wakey.app.data.local.entity.GroupEntity
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.remote.RemoteGroup
import com.wakey.app.domain.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val dao: GroupDao,
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val directory: FirestoreUserDirectory,
    private val fcmSender: FcmSender,
    private val auth: FirebaseAuth
) {
    // 群組列表只顯示「已加入」的群組；被邀請尚未接受的歸通知頁
    val groups: Flow<List<Group>> = dao.observeJoined().map { entities ->
        entities.map { entity ->
            val memberIds = dao.getMemberIds(entity.id)
            entity.toDomain(memberIds)
        }
    }

    // 群組邀請列表（用於通知頁）
    val pendingInvites: Flow<List<Group>> = dao.observePending().map { entities ->
        entities.map { entity ->
            val memberIds = dao.getMemberIds(entity.id)
            entity.toDomain(memberIds)
        }
    }

    suspend fun getById(id: Long): Group? {
        val entity = dao.getById(id) ?: return null
        return entity.toDomain(dao.getMemberIds(id))
    }

    suspend fun getByCloudId(cloudId: String): Group? {
        val entity = dao.getByCloudId(cloudId) ?: return null
        return entity.toDomain(dao.getMemberIds(entity.id))
    }

    // 建立/更新群組。邀請制：要新加的成員會被放進 pendingUids，不會直接成為 members。
    // group.memberIds：UI 上勾選的好友 friend.id 列表；視為「要邀請的人」
    suspend fun save(group: Group): Long {
        val existing = if (group.id != 0L) dao.getById(group.id) else null
        val cloudId = existing?.cloudId?.takeIf { it.isNotBlank() }
            ?: group.cloudId.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val ownerUid = existing?.ownerUid?.takeIf { it.isNotBlank() }
            ?: group.ownerUid.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.uid
            ?: ""
        // 把勾選的 friend.id 轉成 uid
        val selectedUids = group.memberIds
            .mapNotNull { friendRepository.getById(it)?.userId }
            .toSet()
        // 既有成員 + 既有 pending 都不動；新勾的若不在這兩集合內 → 加進 pending；
        // 既不在勾選且仍在 pending 的 → 視為「取消邀請」要從 pending 拿掉
        val existingMembers = existing?.toDomain(emptyList())?.memberUids?.toSet() ?: emptySet()
        val existingPending = existing?.toDomain(emptyList())?.pendingUids?.toSet() ?: emptySet()
        // 勾選但還不是 member 的人都列入 pending
        val finalPending = selectedUids.filter { it !in existingMembers }.toSet()
        // 對「本次新增的邀請」推 FCM 通知（不重複通知既有 pending）
        val newlyInvited = finalPending - existingPending

        val entity = GroupEntity(
            id = group.id, cloudId = cloudId, ownerUid = ownerUid,
            name = group.name, message = group.message,
            color = group.color, photoUri = group.photoUri,
            memberUidsJoined = existingMembers.joinToString(","),
            pendingUidsJoined = finalPending.joinToString(","),
            isJoined = true  // 自己建/編輯的群組一定是已加入
        )
        val newId = dao.upsertGroup(entity)
        // 本機 group_members 暫存 friend.id（給離線/UI 兼容）
        val friendIdsForExistingMembers = existingMembers
            .mapNotNull { friendRepository.getByUserId(it)?.id }
        dao.replaceMembers(newId, friendIdsForExistingMembers)
        // 推上雲端（包含 pendingUids）
        pushFullGroup(entity.copy(id = newId))
        // 對「本次新增的邀請」每位送 FCM（彈出系統通知 + 觸發紅點）
        if (newlyInvited.isNotEmpty()) {
            val me = userProfileRepository.get()
            if (me != null) {
                newlyInvited.forEach { invitedUid ->
                    val token = runCatching { directory.fetchProfile(invitedUid)?.fcmToken }
                        .getOrNull() ?: return@forEach
                    runCatching {
                        fcmSender.sendGroupInvite(token, me.name, group.name, cloudId)
                    }
                }
            }
        }
        return newId
    }

    suspend fun delete(id: Long) {
        val entity = dao.getById(id) ?: return
        if (entity.ownerUid == auth.currentUser?.uid && entity.cloudId.isNotBlank()) {
            runCatching { directory.deleteGroup(entity.cloudId) }
        }
        dao.deleteGroupById(id)
    }

    // 退出群組（非 owner 用）
    suspend fun leave(id: Long) {
        val entity = dao.getById(id) ?: return
        val myUid = auth.currentUser?.uid ?: return
        if (entity.cloudId.isNotBlank()) {
            runCatching { directory.leaveGroup(entity.cloudId, myUid) }
        }
        dao.deleteGroupById(id)
    }

    // 接受邀請
    suspend fun acceptInvite(cloudId: String) {
        val myUid = auth.currentUser?.uid ?: return
        runCatching { directory.acceptGroupInvite(cloudId, myUid) }
    }

    // 拒絕邀請
    suspend fun rejectInvite(cloudId: String) {
        val myUid = auth.currentUser?.uid ?: return
        runCatching { directory.rejectGroupInvite(cloudId, myUid) }
    }

    // 透過 QR 加入群組
    suspend fun joinByCloudId(cloudId: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        val remote = runCatching { directory.fetchGroup(cloudId) }.getOrNull() ?: return false
        if (remote.memberUids.contains(myUid)) return true  // 已是成員
        runCatching { directory.joinGroupByQr(cloudId, myUid) }
        return true
    }

    // 邀請成員（owner 用，現場加人）
    suspend fun invite(groupId: Long, friendId: Long) {
        val entity = dao.getById(groupId) ?: return
        val friend = friendRepository.getById(friendId) ?: return
        if (entity.cloudId.isBlank()) return
        runCatching { directory.inviteToGroup(entity.cloudId, friend.userId) }
    }

    // ── 雲端同步 ───────────────────────────────────────────────────────
    private suspend fun pushFullGroup(entity: GroupEntity) {
        val members = entity.memberUidsJoined.toUidList()
        val pending = entity.pendingUidsJoined.toUidList()
        // 確保 owner 自己也在 memberUids（owner 透過 whereArrayContains 訂閱）
        val ownerInMembers = if (entity.ownerUid.isNotBlank() && entity.ownerUid !in members) {
            members + entity.ownerUid
        } else members
        runCatching {
            directory.pushGroup(
                RemoteGroup(
                    cloudId = entity.cloudId, ownerUid = entity.ownerUid,
                    name = entity.name, message = entity.message,
                    color = entity.color, memberUids = ownerInMembers,
                    pendingUids = pending
                )
            )
        }
    }

    // 雲端 → 本機
    suspend fun applyRemote(remotes: List<RemoteGroup>) {
        val myUid = auth.currentUser?.uid
        dao.deleteAllMembers()
        dao.deleteAllGroups()
        remotes.forEach { r ->
            val isJoined = myUid != null && r.memberUids.contains(myUid)
            val others = r.memberUids.filter { it != myUid }
            val entity = GroupEntity(
                id = 0, cloudId = r.cloudId, ownerUid = r.ownerUid,
                name = r.name, message = r.message, color = r.color, photoUri = null,
                memberUidsJoined = others.joinToString(","),
                pendingUidsJoined = r.pendingUids.joinToString(","),
                isJoined = isJoined
            )
            val newId = dao.upsertGroup(entity)
            val friendIds = others.mapNotNull { friendRepository.getByUserId(it)?.id }
            dao.replaceMembers(newId, friendIds)
        }
    }

    suspend fun clearLocal() {
        dao.deleteAllMembers()
        dao.deleteAllGroups()
    }

    // 首次登入時上傳本機群組（極少用到，保留兼容）
    suspend fun pushAllToCloud() {
        val myUid = auth.currentUser?.uid ?: return
        dao.getAll().forEach { e ->
            val cloudId = e.cloudId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val ownerUid = e.ownerUid.takeIf { it.isNotBlank() } ?: myUid
            val withCloud = if (cloudId != e.cloudId || ownerUid != e.ownerUid) {
                e.copy(cloudId = cloudId, ownerUid = ownerUid).also { dao.upsertGroup(it) }
            } else e
            pushFullGroup(withCloud)
        }
    }
}

private fun String.toUidList(): List<String> =
    if (isBlank()) emptyList()
    else split(",").map { it.trim() }.filter { it.isNotBlank() }
