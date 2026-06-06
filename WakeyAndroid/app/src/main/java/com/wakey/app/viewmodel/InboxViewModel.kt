// ViewModel：通知頁聚合（群組邀請 + 共用鬧鐘 + 之後可能的其他邀請）
// 同時也是 ProfileScreen 紅點來源
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakey.app.data.local.entity.PendingAlarmEntity
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.PendingAlarmRepository
import com.wakey.app.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val groupInvites: List<Group> = emptyList(),
    val pendingAlarms: List<PendingAlarmEntity> = emptyList()
) {
    val totalUnread: Int get() = groupInvites.size + pendingAlarms.size
    val hasUnread: Boolean get() = totalUnread > 0
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val pendingAlarmRepository: PendingAlarmRepository
) : ViewModel() {

    val uiState: StateFlow<InboxUiState> = combine(
        groupRepository.pendingInvites,
        pendingAlarmRepository.pending
    ) { invites, alarms ->
        InboxUiState(groupInvites = invites, pendingAlarms = alarms)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InboxUiState())

    fun acceptGroupInvite(cloudId: String) {
        viewModelScope.launch { groupRepository.acceptInvite(cloudId) }
    }

    fun rejectGroupInvite(cloudId: String) {
        viewModelScope.launch { groupRepository.rejectInvite(cloudId) }
    }

    fun acceptAlarm(id: Long) {
        viewModelScope.launch { pendingAlarmRepository.accept(id) }
    }

    fun rejectAlarm(id: Long) {
        viewModelScope.launch { pendingAlarmRepository.reject(id) }
    }
}
