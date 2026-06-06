// 鬧鐘編輯畫面：完全對應 React 版 AlarmEditScreen
// 滾輪選時器、設定列（重複/名稱/鈴聲/震動/共用）、底部滑入選單、刪除確認
package com.wakey.app.ui.screen.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.wakey.app.viewmodel.AlarmViewModel
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.GroupViewModel
import com.wakey.app.viewmodel.ProfileViewModel

@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    onBack: () -> Unit,
    alarmViewModel: AlarmViewModel = hiltViewModel(),
    friendViewModel: FriendViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val existingAlarm by produceState<Alarm?>(null, alarmId) {
        if (alarmId != null) {
            alarmViewModel.getAlarm(alarmId).collect { value = it }
        }
    }
    val friendState by friendViewModel.uiState.collectAsState()
    val groupState by groupViewModel.uiState.collectAsState()

    val isNew = alarmId == null

    // 狀態初始化
    var hh by remember { mutableIntStateOf(7) }
    var mm by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("新鬧鐘") }
    var repeat by remember { mutableStateOf("僅一次") }
    var customDays by remember { mutableStateOf(setOf<Int>()) }
    var vibrate by remember { mutableStateOf(true) }
    var ringtone by remember { mutableStateOf("default") }
    var sharedTo by remember { mutableStateOf(setOf<String>()) }
    var confirmDel by remember { mutableStateOf(false) }

    // 顯示哪個 BottomSheet
    var sheet by remember { mutableStateOf<String?>(null) } // "repeat"|"name"|"sound"|"share"

    // 新增鬧鐘時，套用「個人頁的預設鬧鐘設定」(鈴聲 + 震動)，僅套用一次
    var appliedDefaults by remember { mutableStateOf(false) }
    LaunchedEffect(isNew, profileState.isLoading, profileState.settings) {
        // 等 DataStore 真正載入完成才套用，避免被初始佔位值蓋掉
        if (isNew && !appliedDefaults && !profileState.isLoading) {
            ringtone = profileState.settings.defaultRingtone
            vibrate = profileState.settings.defaultVibrate
            appliedDefaults = true
        }
    }

    // 載入既有鬧鐘資料
    LaunchedEffect(existingAlarm) {
        existingAlarm?.let { a ->
            hh = a.timeHour; mm = a.timeMinute; label = a.label
            repeat = a.repeatMode.toRepeatLabel(a); customDays = a.customDays
            vibrate = a.vibrate; ringtone = a.ringtone
            sharedTo = (a.sharedToFriendIds.map { "f:$it" } +
                        a.sharedToGroupIds.map { "g:$it" }).toSet()
        }
    }

    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    val repeatLabel = if (repeat == "自定") {
        val dayNames = listOf("日","一","二","三","四","五","六")
        if (customDays.isEmpty()) "自定"
        else customDays.sorted().joinToString("、") { dayNames.getOrElse(it) { "" } }
    } else repeat

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
                // 底部 padding 加大，讓使用者可以把刪除按鈕滾到螢幕中段更好按
                .padding(top = 20.dp, bottom = 240.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 頂部列：取消 | 標題 | 儲存 ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 取消按鈕（玻璃圓角）
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.32f))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WColors.ink)
                }

                Text(
                    text = if (isNew) "新增鬧鐘" else "編輯鬧鐘",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WColors.ink
                )

                // 儲存按鈕（珊瑚色）
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WColors.accent)
                        .clickable {
                            val repeatMode = repeat.toRepeatMode()
                            alarmViewModel.saveAlarm(Alarm(
                                id = alarmId ?: 0L,
                                timeHour = hh, timeMinute = mm, label = label,
                                repeatMode = repeatMode, customDays = customDays,
                                ringtone = ringtone, vibrate = vibrate, enabled = true,
                                sharedToFriendIds = sharedTo
                                    .filter { it.startsWith("f:") }
                                    .mapNotNull { it.removePrefix("f:").toLongOrNull() },
                                sharedToGroupIds = sharedTo
                                    .filter { it.startsWith("g:") }
                                    .mapNotNull { it.removePrefix("g:").toLongOrNull() }
                            ))
                            onBack()
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("儲存", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // ── 滾輪時間選擇器 ───────────────────────────────────────────
            GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 中央高亮條（置於滾輪後面）
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.85f)
                            .height(52.dp)
                            .background(WColors.accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, WColors.accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WheelPicker(
                            items = hours,
                            selectedIndex = hh,
                            onIndexSelected = { hh = it }
                        )
                        Text(
                            ":",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = WColors.ink,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        WheelPicker(
                            items = minutes,
                            selectedIndex = mm,
                            onIndexSelected = { mm = it }
                        )
                    }
                }
            }

            // ── 設定列 ───────────────────────────────────────────────────
            // 重複
            SettingRow(
                icon = { Text("↺", color = WColors.accentDeep, fontSize = 16.sp) },
                label = "重複",
                detail = repeatLabel,
                onClick = { sheet = "repeat" }
            )

            // 自訂星期選擇（僅 repeat == "自定" 時顯示）
            if (repeat == "自定") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("選擇重複日", fontSize = 11.sp, color = WColors.inkSoft, modifier = Modifier.padding(bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("日","一","二","三","四","五","六").forEachIndexed { i, d ->
                                val active = i in customDays
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (active) WColors.accent else Color.White.copy(alpha = 0.4f))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            customDays = if (active) customDays - i else customDays + i
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        d,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else WColors.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 鬧鐘名稱
            SettingRow(
                icon = { Text("✏", color = WColors.accentDeep, fontSize = 15.sp) },
                label = "鬧鐘名稱",
                detail = label,
                onClick = { sheet = "name" }
            )

            // 鈴聲（顯示系統鈴聲名稱）
            val ctx = androidx.compose.ui.platform.LocalContext.current
            SettingRow(
                icon = { Text("♪", color = WColors.accentDeep, fontSize = 18.sp) },
                label = "鈴聲",
                detail = com.wakey.app.alarm.SystemRingtones.titleOf(ctx, ringtone),
                onClick = { sheet = "sound" }
            )

            // 震動
            SettingRow(
                icon = { Text("📳", color = WColors.accentDeep, fontSize = 15.sp) },
                label = "震動",
                toggle = vibrate,
                onToggle = { vibrate = !vibrate }
            )

            // 共用對象
            SettingRow(
                icon = { Text("⇧", color = WColors.accentDeep, fontSize = 18.sp) },
                label = "共用對象",
                detail = if (sharedTo.isEmpty()) "不共用" else "${sharedTo.size} 個",
                onClick = { sheet = "share" }
            )

            // ── 刪除按鈕（僅編輯模式）───────────────────────────────────
            if (!isNew) {
                Spacer(Modifier.height(4.dp))
                if (confirmDel) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { confirmDel = false }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("取消", fontSize = 14.sp, color = WColors.ink)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD85A6A))
                                .clickable {
                                    alarmId?.let { alarmViewModel.deleteAlarm(it) }
                                    onBack()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("確定刪除鬧鐘", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFD85A6A).copy(alpha = 0.10f))
                            .clickable { confirmDel = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕  刪除這個鬧鐘", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFA0353A))
                    }
                }
            }
        }

        // ── Bottom Sheets ────────────────────────────────────────────────
        if (sheet != null) {
            WakeyBottomSheet(onDismiss = { sheet = null }) {
                when (sheet) {
                    "repeat" -> RepeatSheet(value = repeat) { repeat = it; sheet = null }
                    "name"   -> NameSheet(value = label) { label = it; sheet = null }
                    "sound"  -> SoundSheet(value = ringtone) { ringtone = it; sheet = null }
                    "share"  -> ShareSheet(
                        friends = friendState.friends,
                        groups = groupState.groups,
                        selected = sharedTo,
                        onChange = { sharedTo = it },
                        onClose = { sheet = null }
                    )
                }
            }
        }
    }
}

// ── RepeatSheet ──────────────────────────────────────────────────────────
@Composable
private fun RepeatSheet(value: String, onChange: (String) -> Unit) {
    val opts = listOf("僅一次", "每天", "周一至五", "自定")
    Text("重複", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
        modifier = Modifier.padding(bottom = 12.dp))
    opts.forEach { opt ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(opt) }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(opt, fontSize = 16.sp, color = WColors.ink)
            if (value == opt) Text("✓", color = WColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))
    }
}

// ── NameSheet ────────────────────────────────────────────────────────────
@Composable
private fun NameSheet(value: String, onChange: (String) -> Unit) {
    var v by remember { mutableStateOf(value) }
    Text("鬧鐘名稱", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
        modifier = Modifier.padding(bottom = 12.dp))
    TextField(
        value = v,
        onValueChange = { v = it },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.6f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WColors.accent)
            .clickable { onChange(v) }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("儲存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ── SoundSheet ───────────────────────────────────────────────────────────
@Composable
private fun SoundSheet(value: String, onChange: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val opts = remember { com.wakey.app.alarm.SystemRingtones.list(ctx) }
    Text("鈴聲", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
        modifier = Modifier.padding(bottom = 12.dp))
    opts.forEach { opt ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(opt.uri) }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("♪", color = WColors.accent, fontSize = 16.sp)
                Text(opt.title, fontSize = 16.sp, color = WColors.ink)
            }
            if (value == opt.uri) Text("✓", color = WColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))
    }
}

// ── ShareSheet ───────────────────────────────────────────────────────────
@Composable
private fun ShareSheet(
    friends: List<com.wakey.app.domain.model.Friend>,
    groups: List<com.wakey.app.domain.model.Group>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
    onClose: () -> Unit
) {
    fun toggle(id: String) {
        onChange(if (id in selected) selected - id else selected + id)
    }

    Text("共用對象", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
        modifier = Modifier.padding(bottom = 12.dp))

    if (groups.isNotEmpty()) {
        Text("群組", fontSize = 11.sp, color = WColors.inkSoft, modifier = Modifier.padding(bottom = 4.dp))
        groups.forEach { g ->
            val id = "g:${g.id}"; val sel = id in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) WColors.accent.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { toggle(id) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(g.name, fontSize = 15.sp, color = WColors.ink)
                if (sel) Text("✓", color = WColors.accent, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (friends.isNotEmpty()) {
        Text("好友", fontSize = 11.sp, color = WColors.inkSoft, modifier = Modifier.padding(bottom = 4.dp))
        friends.forEach { f ->
            val id = "f:${f.id}"; val sel = id in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) WColors.accent.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { toggle(id) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(f.name, fontSize = 15.sp, color = WColors.ink)
                if (sel) Text("✓", color = WColors.accent, fontSize = 16.sp)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WColors.accent)
            .clickable { onClose() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("完成", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ── 轉換工具 ─────────────────────────────────────────────────────────────
private fun RepeatMode.toRepeatLabel(alarm: Alarm): String = when (this) {
    RepeatMode.ONCE -> "僅一次"
    RepeatMode.DAILY -> "每天"
    RepeatMode.WEEKDAYS -> "周一至五"
    RepeatMode.CUSTOM -> "自定"
}

private fun String.toRepeatMode(): RepeatMode = when (this) {
    "每天" -> RepeatMode.DAILY
    "周一至五" -> RepeatMode.WEEKDAYS
    "自定" -> RepeatMode.CUSTOM
    else -> RepeatMode.ONCE
}
