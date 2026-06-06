// ViewModel：個人資料讀取與儲存、喚醒時段、App 設定（DataStore）
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.datastore.AppSettings
import com.wakey.app.data.datastore.SettingsDataStore
import com.wakey.app.data.datastore.ThemeMode
import com.wakey.app.data.local.entity.WakeEventEntity
import com.wakey.app.data.local.entity.WakeRecordEntity
import com.wakey.app.data.repository.DailyWake
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.data.repository.WakeAnalyticsRepository
import com.wakey.app.domain.model.Friend
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

// 早起 / 喚醒統計顯示用 state
data class WakeStatsUi(
    val consecutiveEarlyDays: Int = 0,
    val recentDailyWakes: List<DailyWake> = emptyList(),
    val averageSnoozeMs: Long = 0,
    val topOutgoing: List<Pair<Friend, Int>> = emptyList(),
    val topIncoming: List<Pair<Friend, Int>> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val settingsDataStore: SettingsDataStore,
    private val wakeAnalytics: WakeAnalyticsRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userProfileRepository.profile,
        settingsDataStore.settings
    ) { profile, settings ->
        ProfileUiState(profile = profile, settings = settings, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    // 早起／喚醒統計
    val wakeStats: StateFlow<WakeStatsUi> = combine(
        wakeAnalytics.records,
        wakeAnalytics.events,
        friendRepository.friends
    ) { records: List<WakeRecordEntity>, events: List<WakeEventEntity>, friends: List<Friend> ->
        fun resolve(pairs: List<Pair<String, Int>>): List<Pair<Friend, Int>> =
            pairs.mapNotNull { (uid, n) ->
                friends.firstOrNull { it.userId == uid }?.let { it to n }
            }
        WakeStatsUi(
            consecutiveEarlyDays = wakeAnalytics.consecutiveEarlyDays(records),
            recentDailyWakes = wakeAnalytics.recentWakeups(records, 7),
            averageSnoozeMs = wakeAnalytics.averageSnoozeMs(records),
            topOutgoing = resolve(wakeAnalytics.topPeers(events, "out", 3)),
            topIncoming = resolve(wakeAnalytics.topPeers(events, "in", 3))
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WakeStatsUi())

    fun logOutgoingWake(peerUid: String) {
        viewModelScope.launch {
            runCatching { wakeAnalytics.logOutgoing(peerUid) }
        }
    }

    init {
        // 確保使用者資料存在
        viewModelScope.launch { userProfileRepository.getOrCreate() }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { userProfileRepository.save(profile) }
    }

    fun saveName(name: String) {
        viewModelScope.launch {
            val current = userProfileRepository.get() ?: return@launch
            userProfileRepository.save(current.copy(name = name))
        }
    }

    fun savePhoto(path: String?) {
        viewModelScope.launch {
            val current = userProfileRepository.get() ?: return@launch
            userProfileRepository.save(current.copy(photoUri = path))
        }
    }

    // 設定我的喚醒鈴聲（傳 null 清除）
    fun saveWakeAudio(path: String?) {
        viewModelScope.launch {
            val current = userProfileRepository.get() ?: return@launch
            userProfileRepository.save(current.copy(wakeAudioPath = path))
        }
    }

    fun saveMessage(message: String) {
        viewModelScope.launch {
            val current = userProfileRepository.get() ?: return@launch
            userProfileRepository.save(current.copy(message = message))
        }
    }

    fun saveWakeWindow(start: String?, end: String?, refuse: Boolean) {
        viewModelScope.launch {
            val current = userProfileRepository.get() ?: return@launch
            userProfileRepository.save(
                current.copy(
                    wakeWindowStart = start,
                    wakeWindowEnd = end,
                    refuseWake = refuse
                )
            )
        }
    }

    // DataStore 設定
    fun setUse24h(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setUse24hFormat(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDefaultRingtone(value: String) {
        viewModelScope.launch { settingsDataStore.setDefaultRingtone(value) }
    }

    fun setDefaultVibrate(value: Boolean) {
        viewModelScope.launch { settingsDataStore.setDefaultVibrate(value) }
    }

    fun setEarlyWakeTime(value: String?) {
        viewModelScope.launch { settingsDataStore.setEarlyWakeTime(value) }
    }
}
