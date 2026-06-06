// ViewModel：群組列表、新增／編輯／刪除、觸發群組起床（FCM 推播）
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.AudioStore
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

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val fcmSender: FcmSender,
    private val directory: FirestoreUserDirectory,
    private val wakeAnalytics: WakeAnalyticsRepository
) : ViewModel() {

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

    // 取得群組成員的 Friend 物件列表
    // 以「雲端 memberUids」為準，動態跟好友列表 combine：
    // 好友清單同步進度晚於群組時，成員會逐步補齊；對不到的 uid 暫時不顯示。
    fun getGroupMembers(groupId: Long): Flow<List<Friend>> =
        kotlinx.coroutines.flow.combine(
            groupRepository.groups,
            friendRepository.friends
        ) { groups, friends ->
            val group = groups.firstOrNull { it.id == groupId } ?: return@combine emptyList()
            group.memberUids.mapNotNull { uid ->
                friends.firstOrNull { it.userId == uid }
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
