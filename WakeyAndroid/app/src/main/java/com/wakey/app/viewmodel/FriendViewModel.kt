// ViewModel：好友列表、新增（QR 掃描後）、刪除、狀態顯示
package com.wakey.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.AudioStore
import com.wakey.app.data.remote.AvatarStore
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.domain.model.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendUiState(
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = false,
    val addResult: AddFriendResult? = null
)

sealed class AddFriendResult {
    data class Success(val friend: Friend) : AddFriendResult()
    data class AlreadyExists(val friend: Friend) : AddFriendResult()
    data class Error(val message: String) : AddFriendResult()
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val directory: FirestoreUserDirectory,
    private val avatarStore: AvatarStore,
    private val audioStore: AudioStore
) : ViewModel() {

    companion object { private const val TAG = "FriendViewModel" }

    private val _uiState = MutableStateFlow(FriendUiState(isLoading = true))
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            friendRepository.friends.collect { list ->
                _uiState.update { it.copy(friends = list, isLoading = false) }
            }
        }
    }

    // QR 掃描完成後呼叫；scannedUuid 為對方 UUID
    // 流程：
    //   1. 從 Firestore 讀對方 profile（名稱/handle/顏色/fcmToken…）
    //   2. 在 Firestore 寫雙向 friendship（users/{me}/friends/{other} + 反向）
    //   3. 本機立刻 upsert 一筆，提供即時 UI 回饋（之後 FriendSyncService 也會持續更新）
    fun addFriendFromQr(scannedUuid: String) {
        viewModelScope.launch {
            val me = userProfileRepository.getOrCreate()
            if (scannedUuid.isBlank()) {
                _uiState.update {
                    it.copy(addResult = AddFriendResult.Error("無效的 QR Code"))
                }
                return@launch
            }
            if (scannedUuid == me.uuid) {
                _uiState.update {
                    it.copy(addResult = AddFriendResult.Error("不能加自己為好友"))
                }
                return@launch
            }

            val existing = friendRepository.getByUserId(scannedUuid)
            if (existing != null) {
                _uiState.update {
                    it.copy(addResult = AddFriendResult.AlreadyExists(existing))
                }
                return@launch
            }

            // 1. 查雲端對方 profile
            val fetchResult = runCatching { directory.fetchProfile(scannedUuid) }
            if (fetchResult.isFailure) {
                val err = fetchResult.exceptionOrNull()
                Log.e(TAG, "fetchProfile failed for uuid=$scannedUuid", err)
                val msg = err?.message?.take(120) ?: "未知錯誤"
                _uiState.update {
                    it.copy(addResult = AddFriendResult.Error("讀取對方資料失敗：$msg"))
                }
                return@launch
            }
            val remote = fetchResult.getOrNull()
            if (remote == null) {
                _uiState.update {
                    it.copy(addResult = AddFriendResult.Error("找不到使用者，請確認對方已開啟 App（uuid: ${scannedUuid.take(8)}…）"))
                }
                return@launch
            }

            // 2. 雲端雙向加好友
            val friendshipResult = runCatching {
                directory.addBidirectionalFriendship(me.uuid, scannedUuid)
            }
            if (friendshipResult.isFailure) {
                val err = friendshipResult.exceptionOrNull()
                Log.e(TAG, "addBidirectionalFriendship failed", err)
                val msg = err?.message?.take(120) ?: "未知錯誤"
                _uiState.update {
                    it.copy(addResult = AddFriendResult.Error("加好友失敗：$msg"))
                }
                return@launch
            }

            // 3. 本機立刻 upsert（帶上既有 id 避免與 FriendSyncService 同時寫入產生重複）
            val current = friendRepository.getByUserId(remote.uuid)
            val photoPath = avatarStore.saveIncoming(remote.uuid, remote.photoBase64)
            val audioPath = audioStore.saveIncoming(remote.uuid, remote.wakeAudioBase64)
            val friend = Friend(
                id = current?.id ?: 0,
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
            val id = friendRepository.save(friend)
            _uiState.update {
                it.copy(addResult = AddFriendResult.Success(friend.copy(id = id)))
            }
        }
    }

    // 錄音叫醒：上傳 base64 到 wake_messages，回傳 messageId
    suspend fun uploadWakeMessage(targetUid: String, audioBase64: String): String? {
        val me = userProfileRepository.get()?.uuid ?: return null
        return runCatching {
            directory.uploadWakeMessage(me, targetUid, audioBase64)
        }.getOrNull()
    }

    fun deleteFriend(id: Long) {
        viewModelScope.launch {
            val friend = friendRepository.getById(id) ?: return@launch
            val me = userProfileRepository.get()
            // 雲端先解除雙向關聯；FriendSyncService 收到後會自動清掉本機，
            // 但為了 UI 即時更新，這裡也直接刪本機。
            if (me != null) {
                runCatching { directory.removeBidirectionalFriendship(me.uuid, friend.userId) }
            }
            friendRepository.delete(id)
        }
    }

    fun clearAddResult() {
        _uiState.update { it.copy(addResult = null) }
    }

    fun getFriend(id: Long): Flow<Friend?> =
        friendRepository.friends.map { it.firstOrNull { f -> f.id == id } }
}
