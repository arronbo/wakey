// ViewModel：單一對話（一對一或群組）的訊息收發
package com.wakey.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.ChatRemote
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.domain.model.ChatMessage
import com.wakey.app.domain.model.ChatType
import com.wakey.app.domain.model.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val title: String = "",
    val myUid: String = "",
    val ready: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRemote: ChatRemote,
    private val friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
    private val userProfileRepository: UserProfileRepository,
    private val directory: FirestoreUserDirectory,
    private val fcmSender: FcmSender
) : ViewModel() {

    private val type: String = savedStateHandle["type"] ?: "direct"
    private val localId: Long = savedStateHandle["id"] ?: -1L

    private val _ui = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _ui.asStateFlow()

    // 解析後的對話資訊
    private val _conversationId = MutableStateFlow("")
    private var senderName: String = "我"
    private var isGroup: Boolean = type == "group"
    private var recipientTokens: List<String> = emptyList()  // 推播對象（不含自己）
    private var groupNameForPush: String? = null

    val messages: StateFlow<List<ChatMessage>> =
        _conversationId.flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else chatRemote.observeMessages(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { resolveConversation() }
    }

    private suspend fun resolveConversation() {
        val me = userProfileRepository.getOrCreate()
        senderName = me.name
        val myUid = me.uuid

        if (isGroup) {
            val group = groupRepository.getById(localId) ?: return
            val members = (group.memberUids + group.ownerUid + myUid)
                .filter { it.isNotBlank() }
                .distinct()
            val convId = ConversationId.group(group.cloudId)
            groupNameForPush = group.name
            chatRemote.ensureConversation(convId, ChatType.GROUP, members, group.cloudId)
            // 推播對象：所有成員的 token（扣掉自己）
            recipientTokens = members.filter { it != myUid }.mapNotNull { uid ->
                runCatching { directory.fetchProfile(uid)?.fcmToken }.getOrNull()
            }
            _ui.value = ChatUiState(title = group.name, myUid = myUid, ready = true)
            _conversationId.value = convId
        } else {
            val friend = friendRepository.getById(localId) ?: return
            val otherUid = friend.userId
            val convId = ConversationId.direct(myUid, otherUid)
            chatRemote.ensureConversation(convId, ChatType.DIRECT, listOf(myUid, otherUid))
            recipientTokens = listOfNotNull(friend.fcmToken)
            _ui.value = ChatUiState(title = friend.name, myUid = myUid, ready = true)
            _conversationId.value = convId
        }
        markRead()
    }

    fun send(text: String) {
        val body = text.trim()
        if (body.isBlank()) return
        val convId = _conversationId.value
        if (convId.isBlank()) return
        val myUid = _ui.value.myUid
        viewModelScope.launch {
            runCatching {
                chatRemote.sendMessage(convId, myUid, senderName, body)
            }
            // 推播給收件人（背景，失敗不影響送出）
            recipientTokens.forEach { token ->
                runCatching {
                    fcmSender.sendChatMessage(
                        targetToken = token,
                        senderName = senderName,
                        text = body,
                        conversationId = convId,
                        groupName = if (isGroup) groupNameForPush else null
                    )
                }
            }
        }
    }

    fun markRead() {
        val convId = _conversationId.value
        val myUid = _ui.value.myUid
        if (convId.isBlank() || myUid.isBlank()) return
        viewModelScope.launch { chatRemote.markRead(convId, myUid) }
    }
}
