// 個人資料畫面：頭像、姓名、個人留言，以及右上齒輪 → 設定頁
package com.wakey.app.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.repository.DailyWake
import com.wakey.app.domain.model.Friend
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.InboxViewModel
import com.wakey.app.viewmodel.ProfileViewModel
import com.wakey.app.viewmodel.WakeStatsUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    inboxViewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    val inbox by inboxViewModel.uiState.collectAsState()
    val hasUnread = inbox.hasUnread
    val stats by viewModel.wakeStats.collectAsState()

    var editName by remember { mutableStateOf(false) }
    var editMsg by remember { mutableStateOf(false) }
    var nameInput by remember(profile?.name) { mutableStateOf(profile?.name ?: "") }
    var msgInput by remember(profile?.message) { mutableStateOf(profile?.message ?: "") }
    var confirmRemovePhoto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 頂部列：標題 + 右上通知 + 設定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(onClick = onOpenNotifications) {
                        Box {
                            WakeyIcon(WIcon.bell, size = 22.dp, tint = WColors.ink)
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD06A6A))
                                )
                            }
                        }
                    }
                    GlassIconButton(onClick = onOpenSettings) {
                        WakeyIcon(WIcon.settings, size = 22.dp, tint = WColors.ink)
                    }
                }
            }

            // 使用者頭卡（含照片選擇）
            GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhotoPicker(
                        photoUri = profile?.photoUri,
                        onPicked = { viewModel.savePhoto(it) },
                        size = 88.dp
                    ) {
                        Avatar(
                            profile?.name ?: "我",
                            profile?.color ?: "#FF8A6B",
                            88.dp, ring = true,
                            photoUri = profile?.photoUri
                        )
                    }
                    if (!profile?.photoUri.isNullOrBlank()) {
                        Text(
                            "移除頭像",
                            fontSize = 12.sp, color = Color(0xFFA0353A),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFD85A6A).copy(alpha = 0.12f))
                                .clickable { confirmRemovePhoto = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    if (editName) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            EditField(nameInput, { nameInput = it }, 160.dp)
                            Text("儲存", color = WColors.accent, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    viewModel.saveName(nameInput); editName = false
                                })
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { editName = true }
                        ) {
                            Text(profile?.name ?: "我", fontSize = 24.sp,
                                fontWeight = FontWeight.Bold, color = WColors.ink)
                            WakeyIcon(WIcon.pencil, size = 14.dp, tint = WColors.inkSoft)
                        }
                    }
                    Text("@${profile?.handle ?: ""}", fontSize = 14.sp, color = WColors.inkSoft)
                }
            }

            // 個人留言
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("個人留言", fontSize = 11.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                        Box(modifier = Modifier.clickable {
                            if (editMsg) viewModel.saveMessage(msgInput)
                            editMsg = !editMsg
                        }) { WakeyIcon(WIcon.pencil, size = 14.dp, tint = WColors.inkSoft) }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (editMsg) {
                        EditField(msgInput, { msgInput = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape).background(WColors.accent)
                                .clickable { viewModel.saveMessage(msgInput); editMsg = false }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) { Text("儲存", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    } else {
                        Text("「${profile?.message ?: ""}」", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = WColors.ink)
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WakeyIcon(WIcon.messageCircle, size = 11.dp, tint = WColors.inkSoft)
                        Text("此留言會同步顯示在別人的好友卡上",
                            fontSize = 11.sp, color = WColors.inkSoft)
                    }
                }
            }

            // ── 早起 / 喚醒 統計區 ─────────────────────────────────────
            StreakCard(stats.consecutiveEarlyDays)
            RecentWakesCard(stats.recentDailyWakes)
            AvgSnoozeCard(stats.averageSnoozeMs)
            TopPeersCard(
                title = "最常喚醒的對象",
                emptyText = "還沒叫醒過任何人",
                pairs = stats.topOutgoing
            )
            TopPeersCard(
                title = "最常被誰喚醒",
                emptyText = "還沒被任何人叫醒過",
                pairs = stats.topIncoming
            )
        }
    }

    if (confirmRemovePhoto) {
        WakeyBottomSheet(onDismiss = { confirmRemovePhoto = false }) {
            Text("移除頭像", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                modifier = Modifier.padding(bottom = 8.dp))
            Text("確定要移除目前的頭像照片嗎？", fontSize = 14.sp, color = WColors.inkSoft,
                modifier = Modifier.padding(bottom = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { confirmRemovePhoto = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("取消", fontSize = 14.sp, color = WColors.ink) }
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFD85A6A))
                        .clickable {
                            viewModel.savePhoto(null); confirmRemovePhoto = false
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("確定移除", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── 統計卡片 ────────────────────────────────────────────────────────────
@Composable
private fun StreakCard(days: Int) {
    GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(WColors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { WakeyIcon(WIcon.zap, size = 26.dp, tint = WColors.accentDeep) }
            Column(modifier = Modifier.weight(1f)) {
                Text("連續早起紀錄", fontSize = 11.sp, letterSpacing = 1.sp,
                    color = WColors.inkSoft)
                Row(verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$days", fontSize = 36.sp,
                        fontWeight = FontWeight.Bold, color = WColors.ink)
                    Text("天", fontSize = 14.sp, color = WColors.inkSoft,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    if (days == 0) "在「早起時間」前後 2 分鐘關掉鬧鐘就算成功"
                    else "繼續加油！",
                    fontSize = 11.sp, color = WColors.inkSoft
                )
            }
        }
    }
}

@Composable
private fun RecentWakesCard(wakes: List<DailyWake>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("最近 7 天起床時間", fontSize = 11.sp, letterSpacing = 1.sp,
                color = WColors.inkSoft, modifier = Modifier.padding(bottom = 8.dp))
            wakes.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(if (d.wasEarly) Color(0xFF7FD3B5) else WColors.inkSoft.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(formatDayLabel(d.dayKey), fontSize = 13.sp,
                        color = WColors.ink, modifier = Modifier.weight(1f))
                    Text(
                        d.wakeMillis?.let { formatHm(it) } ?: "—",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (d.wasEarly) WColors.accentDeep else WColors.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun AvgSnoozeCard(avgMs: Long) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(WColors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { WakeyIcon(WIcon.alarmClock, size = 22.dp, tint = WColors.accentDeep) }
            Column(modifier = Modifier.weight(1f)) {
                Text("平均關掉鬧鐘時間", fontSize = 11.sp, letterSpacing = 1.sp,
                    color = WColors.inkSoft)
                Text(
                    if (avgMs <= 0) "—" else formatDuration(avgMs),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WColors.ink
                )
                Text("從鬧鐘響起到按下關閉的平均秒數",
                    fontSize = 11.sp, color = WColors.inkSoft)
            }
        }
    }
}

@Composable
private fun TopPeersCard(
    title: String,
    emptyText: String,
    pairs: List<Pair<Friend, Int>>
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 11.sp, letterSpacing = 1.sp,
                color = WColors.inkSoft, modifier = Modifier.padding(bottom = 8.dp))
            if (pairs.isEmpty()) {
                Text(emptyText, fontSize = 13.sp, color = WColors.inkSoft,
                    modifier = Modifier.padding(vertical = 6.dp))
            } else {
                pairs.forEach { (friend, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Avatar(friend.name, friend.color, 36.dp,
                            ring = true, photoUri = friend.photoUri)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.name, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = WColors.ink)
                            Text("@${friend.handle}", fontSize = 11.sp, color = WColors.inkSoft)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(WColors.accent.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("$count 次", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = WColors.accentDeep)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDayLabel(dayKey: String): String {
    val parts = dayKey.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return dayKey
    val cal = Calendar.getInstance().apply {
        set(parts[0], parts[1] - 1, parts[2], 12, 0, 0)
    }
    val today = Calendar.getInstance()
    val daysDiff = ((today.timeInMillis - cal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
    return when (daysDiff) {
        0 -> "今天"
        1 -> "昨天"
        else -> SimpleDateFormat("M/d (E)", Locale.TAIWAN).format(cal.time)
    }
}

private fun formatHm(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.TAIWAN).format(java.util.Date(millis))

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return when {
        m == 0L -> "${s} 秒"
        s == 0L -> "${m} 分"
        else -> "${m} 分 ${s} 秒"
    }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, width: Any) {
    val mod = when (width) {
        is Dp -> Modifier.width(width)
        is Modifier -> width
        else -> Modifier
    }
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = TextStyle(color = WColors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(WColors.accent),
        modifier = mod.clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
