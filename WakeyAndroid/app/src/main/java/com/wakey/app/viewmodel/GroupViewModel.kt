// ViewModel：群組列表、新增／編輯／刪除、觸發群組起床（FCM 推播）
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.AudioStore
import com.wakey.app.data.remote.AvatarStore
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.data.repository.WakeAnalyticsRepository
import com.wakey.app.domain.model.Friend
import com.wakey.app.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val wakeResult: WakeResult? = null
)

sealed class WakeResult {
    data class Sent(val count: Int) : WakeResult()
    data class NoWakeable(val total: Int) : WakeResult()
}

// 群組成員（可能是好友，也可能是非好友；非好友資料取自雲端 profile）
data class GroupMember(
    val uid: String,
    val name: String,
    val handle: String,
    val color: String,
    val photoUri: String?,
    val isFriend: Boolean,
    val friendId: Long?,     // 為好友時的本機 id（點擊進好友頁用）
    val canWake: Boolean,    // 僅好友有意義（非好友無法被喚醒）
    val isOwner: Boolean
)

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val fcmSender: FcmSender,
    private val directory: FirestoreUserDirectory,
    private val avatarStore: AvatarStore,
    private val wakeAnalytics: WakeAnalyticsRepository
) : ViewModel() {

    // 非好友成員的雲端 profile 快取（避免每次重組都打 Firestore）
    private val remoteProfileCache = mutableMapOf<String, com.wakey.app.data.remote.RemoteProfile?>()

    val pendingInvites: StateFlow<List<Group>> =
        groupRepository.pendingInvites.stateIn(
            viewModelScope, SharingStarted.Eagerly, emptyList()
        )

    private val _uiState = MutableStateFlow(GroupUiState(isLoading = true))
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.groups.collect { list ->
                _uiState.update { it.copy(groups = list, isLoading = false) }
            }
        }
    }

    fun saveGroup(group: Group) {
        viewModelScope.launch { groupRepository.save(group) }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch { groupRepository.delete(id) }
    }

    fun leaveGroup(id: Long) {
        viewModelScope.launch { groupRepository.leave(id) }
    }

    fun acceptInvite(cloudId: String) {
        viewModelScope.launch { groupRepository.acceptInvite(cloudId) }
    }

    fun rejectInvite(cloudId: String) {
        viewModelScope.launch { groupRepository.rejectInvite(cloudId) }
    }

    // 掃描 QR 加入；回傳是否成功
    suspend fun joinByCloudId(cloudId: String): Boolean = groupRepository.joinByCloudId(cloudId)

    // 錄音叫醒群組：上傳一份音檔給每位可喚醒成員
    suspend fun wakeGroupWithAudio(groupId: Long, audioBase64: String): Int {
        val group = groupRepository.getById(groupId) ?: return 0
        val profile = userProfileRepository.get() ?: return 0
        var sentCount = 0
        group.memberUids.forEach { uid ->
            val friend = friendRepository.getByUserId(uid) ?: return@forEach
            if (!friend.canWake) return@forEach
            val token = friend.fcmToken ?: return@forEach
            val messageId = runCatching {
                directory.uploadWakeMessage(profile.uuid, uid, audioBase64)
            }.getOrNull() ?: return@forEach
            val ok = fcmSender.sendWake(token, profile.name, profile.uuid, messageId)
            if (ok) {
                sentCount++
                runCatching { wakeAnalytics.logOutgoing(uid) }
            }
        }
        return sentCount
    }

    fun getGroup(id: Long): Flow<Group?> =
        groupRepository.groups.map { it.firstOrNull { g -> g.id == id } }

    // 取得群組成員清單（以雲端 memberUids 為準）。
    // 好友 → 直接用本機 Friend；非好友 → 從雲端抓 profile 顯示（仍可點進去加好友）。
    fun getGroupMembers(groupId: Long): Flow<List<GroupMember>> =
        kotlinx.coroutines.flow.combine(
            groupRepository.groups,
            friendRepository.friends
        ) { groups, friends -> groups to friends }
            .map { (groups, friends) ->
                val group = groups.firstOrNull { it.id == groupId } ?: return@map emptyList()
                val ownerUid = group.ownerUid
                group.memberUids.map { uid ->
                    val friend = friends.firstOrNull { it.userId == uid }
                    if (friend != null) {
                        GroupMember(
                            uid = uid, name = friend.name, handle = friend.handle,
                            color = friend.color, photoUri = friend.photoUri,
                            isFriend = true, friendId = friend.id,
                            canWake = friend.canWake, isOwner = uid == ownerUid
                        )
                    } else {
                        // 非好友：抓（並快取）雲端 profile
                        val rp = if (remoteProfileCache.containsKey(uid)) remoteProfileCache[uid]
                        else runCatching { directory.fetchProfile(uid) }.getOrNull()
                            .also { remoteProfileCache[uid] = it }
                        val photo = avatarStore.saveIncoming("member_$uid", rp?.photoBase64)
                        GroupMember(
                            uid = uid, name = rp?.name ?: "Wakey 用戶",
                            handle = rp?.handle ?: "", color = rp?.color ?: "#FF8A6B",
                            photoUri = photo, isFriend = false, friendId = null,
                            canWake = false, isOwner = uid == ownerUid
                        )
                    }
                }
            }

    // 按下「起床！」按鈕：對群組內可喚醒的成員發送 FCM
    fun wakeGroup(groupId: Long) {
        viewModelScope.launch {
            val group = groupRepository.getById(groupId) ?: return@launch
            val profile = userProfileRepository.get() ?: return@launch
            val senderName = profile.name

            // 走 memberUids 避免 friend 還沒同步好時對應不到本機 friendId
            var sentCount = 0
            group.memberUids.forEach { uid ->
                val friend = friendRepository.getByUserId(uid) ?: return@forEach
                if (!friend.canWake) return@forEach
                val token = friend.fcmToken ?: return@forEach
                val ok = fcmSender.sendWake(token, senderName, profile.uuid)
                if (ok) {
                    sentCount++
                    runCatching { wakeAnalytics.logOutgoing(uid) }
                }
            }

            _uiState.update {
                it.copy(
                    wakeResult = if (sentCount > 0)
                        WakeResult.Sent(sentCount)
                    else
                        WakeResult.NoWakeable(group.memberUids.size)
                )
            }
        }
    }

    fun clearWakeResult() {
        _uiState.update { it.copy(wakeResult = null) }
    }
}
