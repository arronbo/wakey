// ViewModel：用 uid 檢視某使用者的公開資料（從雲端 profile），供群組非好友成員加好友用
package com.wakey.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.remote.AvatarStore
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val loading: Boolean = true,
    val uid: String = "",
    val name: String = "",
    val handle: String = "",
    val color: String = "#FF8A6B",
    val message: String = "",
    val photoUri: String? = null,
    val isFriend: Boolean = false,
    val isSelf: Boolean = false,
    val notFound: Boolean = false
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val directory: FirestoreUserDirectory,
    private val friendRepository: FriendRepository,
    private val userProfileRepository: UserProfileRepository,
    private val avatarStore: AvatarStore
) : ViewModel() {

    private val uid: String = savedStateHandle["uid"] ?: ""

    // 一次性載入的雲端資料；isFriend 跟好友清單即時連動（加完好友按鈕會即時變更）
    private val loaded = MutableStateFlow(false)
    private val name = MutableStateFlow("")
    private val handle = MutableStateFlow("")
    private val color = MutableStateFlow("#FF8A6B")
    private val message = MutableStateFlow("")
    private val photo = MutableStateFlow<String?>(null)
    private val isSelf = MutableStateFlow(false)
    private val notFound = MutableStateFlow(false)

    val uiState: StateFlow<UserProfileUiState> =
        combine(friendRepository.friends, loaded) { friends, isLoaded ->
            UserProfileUiState(
                loading = !isLoaded,
                uid = uid,
                name = name.value.ifBlank { "Wakey 用戶" },
                handle = handle.value,
                color = color.value,
                message = message.value,
                photoUri = photo.value,
                isFriend = friends.any { it.userId == uid },
                isSelf = isSelf.value,
                notFound = notFound.value
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfileUiState())

    init {
        viewModelScope.launch {
            isSelf.value = userProfileRepository.get()?.uuid == uid
            val rp = runCatching { directory.fetchProfile(uid) }.getOrNull()
            if (rp != null) {
                name.value = rp.name
                handle.value = rp.handle
                color.value = rp.color
                message.value = rp.message
                photo.value = avatarStore.saveIncoming("member_$uid", rp.photoBase64)
            } else {
                notFound.value = true
            }
            loaded.value = true
        }
    }
}
