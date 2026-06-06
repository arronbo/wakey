// ViewModel：鬧鐘列表管理、新增／編輯／刪除、計算距下個鬧鐘時間
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.repository.AlarmRepository
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.domain.model.Alarm
import com.wakey.app.domain.model.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AlarmUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarmCountdown: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
    private val userProfileRepository: UserProfileRepository,
    private val fcmSender: FcmSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState(isLoading = true))
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            alarmRepository.alarms.collect { list ->
                _uiState.update {
                    it.copy(
                        alarms = list,
                        nextAlarmCountdown = computeCountdown(list),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newId = alarmRepository.save(alarm)
            // FCM 發送涉及 OAuth + HTTP 通常需要 1-3 秒；UI 在 save 後立刻 popBackStack
            // → ViewModel 被銷毀 → viewModelScope 取消 → FCM 發送被中途砍掉。
            // 用 NonCancellable 確保即使 ViewModel 已銷毀也跑完，邀請才會送到。
            withContext(NonCancellable) {
            val profile = userProfileRepository.get()
            if (profile == null) {
                android.util.Log.w(TAG, "saveAlarm: no profile, skip share FCM")
                return@withContext
            }
            val senderName = profile.name
            android.util.Log.d(
                TAG,
                "saveAlarm: sharedToFriendIds=${alarm.sharedToFriendIds} " +
                    "sharedToGroupIds=${alarm.sharedToGroupIds}"
            )

            // 共用給好友
            alarm.sharedToFriendIds.forEach { friendId ->
                val friend = friendRepository.getById(friendId)
                if (friend == null) {
                    android.util.Log.w(TAG, "  friendId=$friendId not found locally"); return@forEach
                }
                val token = friend.fcmToken
                if (token.isNullOrBlank()) {
                    android.util.Log.w(TAG, "  ${friend.name} has no fcmToken"); return@forEach
                }
                val ok = runCatching {
                    fcmSender.sendSharedAlarm(token, alarm.copy(id = newId), senderName)
                }.getOrElse { e ->
                    android.util.Log.e(TAG, "  sendSharedAlarm crashed for ${friend.name}", e); false
                }
                android.util.Log.d(TAG, "  → ${friend.name} sendSharedAlarm ok=$ok")
            }

            // 共用給群組（對每位成員）
            // 走 memberUids（雲端權威清單），避免 memberIds 本機映射延遲／為空
            // 導致對應不到成員、整個群組都收不到（與 GroupViewModel.wakeGroup 一致）
            alarm.sharedToGroupIds.forEach { groupId ->
                val group = groupRepository.getById(groupId) ?: return@forEach
                android.util.Log.d(TAG, "  group=${group.name} memberUids=${group.memberUids}")
                group.memberUids.forEach { uid ->
                    val member = friendRepository.getByUserId(uid)
                    if (member == null) {
                        android.util.Log.w(TAG, "    uid=$uid not found locally"); return@forEach
                    }
                    val token = member.fcmToken
                    if (token.isNullOrBlank()) {
                        android.util.Log.w(TAG, "    ${member.name} has no fcmToken"); return@forEach
                    }
                    val ok = runCatching {
                        fcmSender.sendSharedAlarm(token, alarm.copy(id = newId), senderName)
                    }.getOrElse { e ->
                        android.util.Log.e(TAG, "    sendSharedAlarm crashed for ${member.name}", e); false
                    }
                    android.util.Log.d(TAG, "    → ${member.name} sendSharedAlarm ok=$ok")
                }
            }
            } // withContext(NonCancellable)
        }
    }

    companion object { private const val TAG = "AlarmViewModel" }

    fun toggleAlarm(id: Long, enabled: Boolean) {
        viewModelScope.launch { alarmRepository.toggle(id, enabled) }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch { alarmRepository.delete(id) }
    }

    fun getAlarm(id: Long): Flow<Alarm?> = alarmRepository.alarms.map { list ->
        list.firstOrNull { it.id == id }
    }

    // 計算距下一個啟用鬧鐘的倒數字串（例："距下個鬧鐘 3 小時 20 分"）
    private fun computeCountdown(alarms: List<Alarm>): String {
        val now = System.currentTimeMillis()
        val next = alarms
            .filter { it.enabled }
            .mapNotNull { alarm ->
                try {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.timeHour)
                        set(Calendar.MINUTE, alarm.timeMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                } catch (e: Exception) { null }
            }
            .minOrNull() ?: return "尚無鬧鐘"

        val diffMs = next - now
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
        return when {
            hours > 0 -> "距下個鬧鐘 ${hours}h ${minutes}m"
            else -> "距下個鬧鐘 ${minutes} 分鐘"
        }
    }
}
