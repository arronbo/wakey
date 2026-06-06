// 鬧鐘列表畫面：完全對應 React 版 AlarmScreen + AlarmCard 的視覺設計
package com.wakey.app.ui.screen.alarm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.domain.model.Alarm
import com.wakey.app.domain.model.RepeatMode
import com.wakey.app.ui.components.*
import java.util.concurrent.TimeUnit
import com.wakey.app.viewmodel.AlarmViewModel

@Composable
fun AlarmScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // 多選模式：selectedIds 非空 = 進入選取模式
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selectedIds.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }
    // 列表變動時，清掉已不存在的選取項
    LaunchedEffect(state.alarms) {
        val ids = state.alarms.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(ids)
    }

    // 每秒更新一次，讓倒數能顯示實時秒數
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    // 計算下個鬧鐘（以剩餘秒數排序）
    val nextAlarm = state.alarms
        .filter { it.enabled }
        .map { it to secsUntil(it.timeHour, it.timeMinute, nowMs) }
        .minByOrNull { it.second }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                top = 20.dp, start = 20.dp, end = 20.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 標題列 ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectionMode) "已選 ${selectedIds.size} 個" else "鬧鐘",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = WColors.ink
                    )
                    if (selectionMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 刪除選取
                            GlassIconButton(onClick = { confirmDelete = true }) {
                                WakeyIcon(WIcon.trash, size = 20.dp, tint = Color(0xFFA0353A))
                            }
                            // 取消選取
                            GlassIconButton(onClick = { selectedIds = emptySet() }) {
                                WakeyIcon(WIcon.x, size = 20.dp, tint = WColors.ink)
                            }
                        }
                    } else {
                        GlassIconButton(onClick = onAdd) {
                            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                        }
                    }
                }
            }

            // ── 倒數卡片 ─────────────────────────────────────────────────
            item {
                GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                    if (nextAlarm != null) {
                        val (alarm, secs) = nextAlarm
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "距離下個鬧鐘響鈴還有",
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = WColors.inkSoft
                            )
                            Text(
                                text = formatCountdown(secs),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = WColors.ink,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            val timeStr = "%02d:%02d".format(alarm.timeHour, alarm.timeMinute)
                            val infoStr = listOf(
                                alarm.label.takeIf { it.isNotBlank() },
                                timeStr,
                                alarm.repeatMode.toLabel(alarm)
                            ).filterNotNull().joinToString(" · ")
                            Text(
                                text = infoStr,
                                fontSize = 14.sp,
                                color = WColors.inkSoft,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("當前沒有鬧鐘", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WColors.ink)
                            Text("按右上角 + 來新增一個吧", fontSize = 14.sp, color = WColors.inkSoft, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // ── 鬧鐘列表 ─────────────────────────────────────────────────
            items(state.alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    selectionMode = selectionMode,
                    selected = alarm.id in selectedIds,
                    onToggle = { viewModel.toggleAlarm(alarm.id, !alarm.enabled) },
                    onClick = {
                        if (selectionMode) {
                            selectedIds = if (alarm.id in selectedIds)
                                selectedIds - alarm.id else selectedIds + alarm.id
                        } else onEdit(alarm.id)
                    },
                    onLongPress = { selectedIds = selectedIds + alarm.id }
                )
            }
        }
    }

    // 多選刪除確認
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除鬧鐘？") },
            text = { Text("確定要刪除已選的 ${selectedIds.size} 個鬧鐘嗎？此動作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAlarms(selectedIds)
                    selectedIds = emptySet()
                    confirmDelete = false
                }) {
                    Text("刪除", color = Color(0xFFA0353A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

// ── 鬧鐘卡片（對應 React AlarmCard）─────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    selectionMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Box(modifier = if (!alarm.enabled) Modifier.fillMaxWidth().background(Color.Transparent) else Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .then(
                        if (!alarm.enabled) Modifier.background(Color.Transparent) else Modifier
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 時間 + sharedFrom 標籤
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "%02d:%02d".format(alarm.timeHour, alarm.timeMinute),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.enabled) WColors.ink else WColors.ink.copy(alpha = 0.4f)
                        )
                        alarm.sharedFrom?.let { from ->
                            Text(
                                text = "$from 共用",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WColors.accentDeep,
                                modifier = Modifier
                                    .background(WColors.accent.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .align(Alignment.Bottom)
                            )
                        }
                    }
                    // 標籤 + 重複
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val parts = buildList {
                            if (alarm.label.isNotBlank()) add(alarm.label)
                            add(alarm.repeatMode.toLabel(alarm))
                            if (alarm.vibrate) add("震動")
                        }
                        Text(
                            text = parts.joinToString(" · "),
                            fontSize = 13.sp,
                            color = WColors.inkSoft
                        )
                    }
                }

                if (selectionMode) {
                    // 選取圓圈（取代 Toggle）
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (selected) WColors.accent else WColors.ink.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) WakeyIcon(WIcon.check, size = 16.dp, tint = Color.White)
                    }
                } else {
                    // 自訂 Toggle
                    WakeToggle(
                        checked = alarm.enabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            // 停用時加半透明遮罩
            if (!alarm.enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
            // 選取時加珊瑚色高亮邊框
            if (selected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(WColors.accent.copy(alpha = 0.12f))
                )
            }
        }
    }
}


// ── 工具函式 ──────────────────────────────────────────────────────────────
// 計算距某時刻還有幾「秒」（基準時間由外部每秒傳入，確保實時更新）
private fun secsUntil(hour: Int, minute: Int, nowMs: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= nowMs) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    return TimeUnit.MILLISECONDS.toSeconds(cal.timeInMillis - nowMs)
}

// 不到 1 分鐘 → 顯示實時秒數；其餘維持時/分
private fun formatCountdown(secs: Long): String {
    if (secs < 60) return "${secs} 秒"
    val totalMin = secs / 60
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h == 0L) "${m} 分鐘" else "${h} 小時 ${m} 分"
}

private fun RepeatMode.toLabel(alarm: Alarm): String = when (this) {
    RepeatMode.ONCE -> "僅一次"
    RepeatMode.DAILY -> "每天"
    RepeatMode.WEEKDAYS -> "周一至五"
    RepeatMode.CUSTOM -> {
        val names = mapOf(1 to "日", 2 to "一", 3 to "二", 4 to "三", 5 to "四", 6 to "五", 7 to "六")
        if (alarm.customDays.isEmpty()) "自定"
        else alarm.customDays.sorted().mapNotNull { names[it] }.joinToString("、")
    }
}
