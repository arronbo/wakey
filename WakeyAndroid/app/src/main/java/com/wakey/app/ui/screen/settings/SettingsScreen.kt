// 設定頁：從個人頁右上齒輪進入。包含時間制式、早起時間、喚醒時段、喚醒鈴聲、
// 主題顏色、預設鬧鐘設定、登出/綁定 Google。
package com.wakey.app.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.datastore.ThemeMode
import com.wakey.app.ui.components.*
import com.wakey.app.ui.screen.profile.WakeAudioSheet
import com.wakey.app.viewmodel.AuthViewModel
import com.wakey.app.viewmodel.ProfileViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    val settings = state.settings

    var showWake by remember { mutableStateOf(false) }
    var showDefault by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showWakeAudio by remember { mutableStateOf(false) }
    var showEarlyWake by remember { mutableStateOf(false) }

    val isAnonymous by authViewModel.isAnonymous.collectAsState()
    val authUi by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val linkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> authViewModel.linkGoogleAccount(result.data) }

    LaunchedEffect(authUi.linkSuccessMessage, authUi.error) {
        authUi.linkSuccessMessage?.let {
            snackbarHostState.showSnackbar(it); authViewModel.clearMessages()
        }
        authUi.error?.let {
            snackbarHostState.showSnackbar(it); authViewModel.clearMessages()
        }
    }

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
                // 底部 padding 加大，登出/綁定按鈕可往上多滾，較好按
                .padding(top = 20.dp, bottom = 240.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 頂部列：返回 + 標題
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(onClick = onBack) {
                    WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
                }
                Text("設定", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
            }

            Spacer(Modifier.height(4.dp))

            SegmentedRow(
                icon = WIcon.sun, label = "時間制式",
                options = listOf(false to "12 小時", true to "24 小時"),
                value = settings.use24hFormat,
                onChange = { viewModel.setUse24h(it) }
            )

            // 早起時間（新）
            SettingRow(
                icon = { WakeyIcon(WIcon.alarmClock, size = 18.dp, tint = WColors.accentDeep) },
                label = "早起時間",
                detail = settings.earlyWakeTime ?: "未設定",
                onClick = { showEarlyWake = true }
            )

            SettingRow(
                icon = { WakeyIcon(WIcon.zap, size = 18.dp, tint = WColors.accentDeep) },
                label = "喚醒時段",
                detail = if (profile?.refuseWake == true) "拒絕喚醒"
                else "${profile?.wakeWindowStart ?: "--:--"} – ${profile?.wakeWindowEnd ?: "--:--"}",
                onClick = { showWake = true }
            )

            SettingRow(
                icon = { WakeyIcon(WIcon.music, size = 18.dp, tint = WColors.accentDeep) },
                label = "我的喚醒鈴聲",
                detail = if (profile?.wakeAudioPath.isNullOrBlank()) "未設定" else "已設定",
                onClick = { showWakeAudio = true }
            )

            SettingRow(
                icon = { WakeyIcon(WIcon.moon, size = 18.dp, tint = WColors.accentDeep) },
                label = "主題顏色",
                detail = themeModeLabel(settings.themeMode),
                onClick = { showTheme = true }
            )

            val ctx = androidx.compose.ui.platform.LocalContext.current
            SettingRow(
                icon = { WakeyIcon(WIcon.bell, size = 18.dp, tint = WColors.accentDeep) },
                label = "預設鬧鐘設定",
                detail = "${com.wakey.app.alarm.SystemRingtones.titleOf(ctx, settings.defaultRingtone)}　${if (settings.defaultVibrate) "震動開" else "無震動"}",
                onClick = { showDefault = true }
            )

            if (isAnonymous) {
                SettingRow(
                    icon = { WakeyIcon(WIcon.logOut, size = 18.dp, tint = WColors.accentDeep) },
                    label = "綁定 Google 帳號",
                    detail = if (authUi.loading) "處理中…" else "保留資料",
                    onClick = {
                        if (!authUi.loading) linkLauncher.launch(authViewModel.signInIntent())
                    }
                )
                Text(
                    "目前是訪客模式，換手機或重灌將無法取回資料。\n綁定 Google 帳號後可跨裝置同步。",
                    fontSize = 11.sp, color = WColors.inkSoft,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            } else {
                SettingRow(
                    icon = { WakeyIcon(WIcon.logOut, size = 18.dp, tint = WColors.accentDeep) },
                    label = "登出",
                    onClick = { authViewModel.signOut() }
                )
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }

    // ── BottomSheets ─────────────────────────────────────────────────────
    if (showWake) {
        WakeWindowSheet(
            start = profile?.wakeWindowStart ?: "06:00",
            end = profile?.wakeWindowEnd ?: "10:00",
            refuse = profile?.refuseWake ?: false,
            onConfirm = { s, e, r -> viewModel.saveWakeWindow(s, e, r); showWake = false },
            onDismiss = { showWake = false }
        )
    }
    if (showDefault) {
        DefaultAlarmSheet(
            ringtone = settings.defaultRingtone,
            vibrate = settings.defaultVibrate,
            onSave = { rt, vb ->
                viewModel.setDefaultRingtone(rt)
                viewModel.setDefaultVibrate(vb)
                showDefault = false
            },
            onDismiss = { showDefault = false }
        )
    }
    if (showTheme) {
        ThemeModeSheet(
            current = settings.themeMode,
            onPick = { viewModel.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false }
        )
    }
    if (showWakeAudio) {
        WakeAudioSheet(
            currentPath = profile?.wakeAudioPath,
            onPicked = { path -> viewModel.saveWakeAudio(path); showWakeAudio = false },
            onCleared = { viewModel.saveWakeAudio(null); showWakeAudio = false },
            onDismiss = { showWakeAudio = false }
        )
    }
    if (showEarlyWake) {
        TimeWheelSheet(
            value = settings.earlyWakeTime ?: "07:00",
            label = "早起時間",
            onConfirm = { v -> viewModel.setEarlyWakeTime(v); showEarlyWake = false },
            onDismiss = { showEarlyWake = false }
        )
    }
}

// ── 主題顏色相關 ─────────────────────────────────────────────────────────
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "淺色"
    ThemeMode.DARK -> "深色"
    ThemeMode.SYSTEM -> "跟隨系統"
    ThemeMode.TIME -> "隨時間"
}

@Composable
private fun ThemeModeSheet(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val c = WColors
    val options = listOf(
        ThemeMode.LIGHT to ("淺色" to "始終使用明亮配色"),
        ThemeMode.DARK to ("深色" to "始終使用深色配色"),
        ThemeMode.SYSTEM to ("跟隨系統" to "依手機的深色模式設定"),
        ThemeMode.TIME to ("隨時間" to "白天 6:00–18:00 淺色，夜晚深色")
    )
    WakeyBottomSheet(onDismiss = onDismiss) {
        Text("主題顏色", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink,
            modifier = Modifier.padding(bottom = 4.dp))
        Text("選擇 App 的明暗配色方式", fontSize = 12.sp, color = c.inkSoft,
            modifier = Modifier.padding(bottom = 12.dp))
        options.forEach { (mode, text) ->
            val (title, desc) = text
            val sel = mode == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (sel) c.accent.copy(alpha = 0.14f)
                        else c.raisedSoft.copy(alpha = if (c.isDark) 0.06f else 0.04f)
                    )
                    .clickable { onPick(mode) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(c.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    WakeyIcon(
                        when (mode) {
                            ThemeMode.LIGHT -> WIcon.sun
                            ThemeMode.DARK -> WIcon.moon
                            ThemeMode.SYSTEM -> WIcon.settings
                            ThemeMode.TIME -> WIcon.alarmClock
                        }, size = 18.dp, tint = c.accentDeep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    Text(desc, fontSize = 11.sp, color = c.inkSoft)
                }
                if (sel) WakeyIcon(WIcon.check, size = 18.dp, tint = c.accent)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── 時間制式 segmented row ───────────────────────────────────────────────
@Composable
private fun SegmentedRow(
    icon: String, label: String,
    options: List<Pair<Boolean, String>>, value: Boolean, onChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(WColors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { WakeyIcon(icon, size = 18.dp, tint = WColors.accentDeep) }
            Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = WColors.ink)
            Row(
                modifier = Modifier.clip(CircleShape)
                    .background(WColors.ink.copy(alpha = 0.08f)).padding(2.dp)
            ) {
                options.forEach { (k, l) ->
                    val sel = value == k
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .background(if (sel) Color.White else Color.Transparent)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onChange(k) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(l, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (sel) WColors.accent else WColors.inkSoft)
                    }
                }
            }
        }
    }
}

// ── 喚醒時段 sheet ──────────────────────────────────────────────────────
@Composable
private fun WakeWindowSheet(
    start: String, end: String, refuse: Boolean,
    onConfirm: (String?, String?, Boolean) -> Unit, onDismiss: () -> Unit
) {
    var s by remember { mutableStateOf(start) }
    var e by remember { mutableStateOf(end) }
    var r by remember { mutableStateOf(refuse) }
    var picker by remember { mutableStateOf<String?>(null) }

    WakeyBottomSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", fontSize = 14.sp, color = WColors.inkSoft, modifier = Modifier.clickable(onClick = onDismiss))
            Text("喚醒時段", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
            Text("儲存", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.accent,
                modifier = Modifier.clickable {
                    onConfirm(if (r) null else s, if (r) null else e, r)
                })
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFD88A8A).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { WakeyIcon(WIcon.x, size = 18.dp, tint = Color(0xFFA04040)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("拒絕喚醒", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WColors.ink)
                    Text("朋友的「起床」功能將無法喚醒你", fontSize = 11.sp, color = WColors.inkSoft)
                }
                WakeToggle(checked = r, onCheckedChange = { r = it })
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.then(if (r) Modifier.alpha(0.4f) else Modifier)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("可喚醒時段", fontSize = 11.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeButton("起始", s, Modifier.weight(1f)) { if (!r) picker = "start" }
                        Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.inkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp))
                        TimeButton("終止", e, Modifier.weight(1f)) { if (!r) picker = "end" }
                    }
                    WakeWindowRing(s, e, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (picker != null) {
        val cur = if (picker == "start") s else e
        TimeWheelSheet(
            value = cur,
            label = if (picker == "start") "起始時間" else "終止時間",
            onConfirm = { v -> if (picker == "start") s = v else e = v; picker = null },
            onDismiss = { picker = null }
        )
    }
}

@Composable
private fun TimeButton(label: String, time: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
            Text(time, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
        }
    }
}

@Composable
private fun TimeWheelSheet(
    value: String, label: String,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit
) {
    var hh by remember { mutableIntStateOf(value.split(":").getOrNull(0)?.toIntOrNull() ?: 0) }
    var mm by remember { mutableIntStateOf(value.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    WakeyBottomSheet(onDismiss = onDismiss) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
            modifier = Modifier.padding(bottom = 12.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f).height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WColors.accent.copy(alpha = 0.10f))
            )
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                WheelPicker(hours, hh, { hh = it })
                Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                    modifier = Modifier.padding(horizontal = 4.dp))
                WheelPicker(minutes, mm, { mm = it })
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(WColors.accent)
                .clickable { onConfirm("%02d:%02d".format(hh, mm)) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) { Text("確定", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
    }
}

// ── 預設鬧鐘設定 sheet ──────────────────────────────────────────────────
@Composable
private fun DefaultAlarmSheet(
    ringtone: String, vibrate: Boolean,
    onSave: (String, Boolean) -> Unit, onDismiss: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var rt by remember { mutableStateOf(ringtone) }
    var vb by remember { mutableStateOf(vibrate) }
    val opts = remember { com.wakey.app.alarm.SystemRingtones.list(ctx) }
    val preview = rememberRingtonePreview()

    WakeyBottomSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", fontSize = 14.sp, color = WColors.inkSoft, modifier = Modifier.clickable(onClick = onDismiss))
            Text("預設鬧鐘設定", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
            Text("儲存", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.accent,
                modifier = Modifier.clickable { onSave(rt, vb) })
        }
        Text("新建鬧鐘時會自動套用這些設定", fontSize = 12.sp, color = WColors.inkSoft,
            modifier = Modifier.padding(bottom = 12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(WColors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { WakeyIcon(WIcon.vibrate, size = 18.dp, tint = WColors.accentDeep) }
                Text("震動", modifier = Modifier.weight(1f), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = WColors.ink)
                WakeToggle(checked = vb, onCheckedChange = { vb = it })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("預設鈴聲", fontSize = 11.sp, color = WColors.inkSoft, modifier = Modifier.padding(bottom = 6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.55f))
        ) {
            opts.forEach { o ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rt = o.uri }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WakeyIcon(WIcon.music, size = 16.dp, tint = WColors.accent)
                    Text(o.title, fontSize = 14.sp, color = WColors.ink, modifier = Modifier.weight(1f))
                    if (rt == o.uri) WakeyIcon(WIcon.check, size = 18.dp, tint = WColors.accent)
                    val playing = preview.playingUri == o.uri
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (playing) WColors.accent else WColors.accent.copy(alpha = 0.14f))
                            .clickable { preview.toggle(o.uri) },
                        contentAlignment = Alignment.Center
                    ) {
                        WakeyIcon(
                            if (playing) WIcon.pause else WIcon.play,
                            size = 15.dp,
                            tint = if (playing) Color.White else WColors.accentDeep
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
