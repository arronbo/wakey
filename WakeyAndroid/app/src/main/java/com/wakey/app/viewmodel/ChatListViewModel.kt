// ViewModel：聊天列表（inbox）。把雲端對話與本機好友/群組資料結合成顯示列。
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.ChatRemote
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.domain.model.ChatType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 一列聊天摘要（給列表畫面顯示與導航用）
data class ChatRow(
    val conversationId: String,
    val type: ChatType,
    val navLocalId: Long,      // direct=friend.id；group=group.id
    val title: String,
    val color: String,
    val photoUri: String?,
    val lastText: String,
    val lastAtMillis: Long,
    val lastSenderName: String,  // 群組顯示「誰說的」
    val unread: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRemote: ChatRemote,
    private val friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    val rows: StateFlow<List<ChatRow>> =
        userProfileRepository.profile.flatMapLatest { me ->
            val myUid = me?.uuid
            if (myUid.isNullOrBlank()) flowOf(emptyList())
            else combine(
                chatRemote.observeConversations(myUid),
                friendRepository.friends,
                groupRepository.groups
            ) { conversations, friends, groups ->
                conversations.mapNotNull { conv ->
                    when (conv.type) {
                        ChatType.DIRECT -> {
                            val otherUid = conv.otherUid(myUid) ?: return@mapNotNull null
                            val friend = friends.firstOrNull { it.userId == otherUid }
                                ?: return@mapNotNull null
                            ChatRow(
                                conversationId = conv.id,
                                type = ChatType.DIRECT,
                                navLocalId = friend.id,
                                title = friend.name,
                                color = friend.color,
                                photoUri = friend.photoUri,
                                lastText = conv.lastText,
                                lastAtMillis = conv.lastAtMillis,
                                lastSenderName = "",
                                unread = conv.hasUnread(myUid)
                            )
                        }
                        ChatType.GROUP -> {
                            val group = groups.firstOrNull { it.cloudId == conv.groupCloudId }
                                ?: return@mapNotNull null
                            ChatRow(
                                conversationId = conv.id,
                                type = ChatType.GROUP,
                                navLocalId = group.id,
                                title = group.name,
                                color = group.color ?: "#FF8A6B",
                                photoUri = group.photoUri,
                                lastText = conv.lastText,
                                lastAtMillis = conv.lastAtMillis,
                                lastSenderName = "",
                                unread = conv.hasUnread(myUid)
                            )
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
