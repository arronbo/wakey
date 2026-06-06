// 通知頁：群組邀請 + 共用鬧鐘邀請，可接受或拒絕
package com.wakey.app.ui.screen.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.local.entity.PendingAlarmEntity
import com.wakey.app.domain.model.Group
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.InboxViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(WColors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassIconButton(onClick = onBack) {
                        WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
                    }
                    Text("通知", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                }
            }

            // ── 群組邀請 ──────────────────────────────────────────────────
            if (state.groupInvites.isNotEmpty()) {
                item {
                    Text("群組邀請", fontSize = 11.sp, letterSpacing = 1.sp,
                        color = WColors.inkSoft,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(state.groupInvites, key = { "g-${it.cloudId}" }) { g ->
                    InviteCard(
                        group = g,
                        onAccept = { viewModel.acceptGroupInvite(g.cloudId) },
                        onReject = { viewModel.rejectGroupInvite(g.cloudId) }
                    )
                }
            }

            // ── 共用鬧鐘 ──────────────────────────────────────────────────
            if (state.pendingAlarms.isNotEmpty()) {
                item {
                    Text("共用鬧鐘邀請", fontSize = 11.sp, letterSpacing = 1.sp,
                        color = WColors.inkSoft,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(state.pendingAlarms, key = { "a-${it.id}" }) { a ->
                    SharedAlarmCard(
                        pending = a,
                        onAccept = { viewModel.acceptAlarm(a.id) },
                        onReject = { viewModel.rejectAlarm(a.id) }
                    )
                }
            }

            // ── 全空狀態 ───────────────────────────────────────────────
            if (!state.hasUnread) {
                item {
                    Text(
                        "目前沒有新的通知",
                        fontSize = 13.sp, color = WColors.inkSoft,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteCard(group: Group, onAccept: () -> Unit, onReject: () -> Unit) {
    GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GroupAvatar(
                    colors = listOf(group.color ?: "#FF8A6B"),
                    size = 48.dp,
                    monogram = group.name.firstOrNull()?.toString()
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = WColors.ink)
                    Text("邀請你加入這個群組", fontSize = 12.sp, color = WColors.inkSoft)
                    if (group.message.isNotBlank()) {
                        Text("「${group.message}」", fontSize = 12.sp,
                            color = WColors.inkSoft,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AcceptRejectRow(onAccept, onReject, acceptText = "加入")
        }
    }
}

@Composable
private fun SharedAlarmCard(
    pending: PendingAlarmEntity,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(WColors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    WakeyIcon(WIcon.alarmClock, size = 22.dp, tint = WColors.accentDeep)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "%02d:%02d".format(pending.timeHour, pending.timeMinute),
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WColors.ink
                    )
                    Text(
                        "${pending.senderName} 想共用這個鬧鐘給你",
                        fontSize = 12.sp, color = WColors.inkSoft
                    )
                    if (pending.label.isNotBlank()) {
                        Text("「${pending.label}」", fontSize = 12.sp,
                            color = WColors.inkSoft,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AcceptRejectRow(onAccept, onReject, acceptText = "加入")
        }
    }
}

@Composable
private fun AcceptRejectRow(
    onAccept: () -> Unit, onReject: () -> Unit,
    acceptText: String = "加入"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(WColors.ink.copy(alpha = 0.06f))
                .clickable { onReject() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("拒絕", fontSize = 14.sp, color = WColors.ink)
        }
        Box(
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(WColors.accent)
                .clickable { onAccept() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(acceptText, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
