// 好友列表畫面：完全對應 React 版 FriendsScreen + FriendCard + AddFriendSheet
package com.wakey.app.ui.screen.friend

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.domain.model.Friend
import com.wakey.app.qr.QrCodeGenerator
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.AddFriendResult
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.ProfileViewModel

@Composable
fun FriendScreen(
    onFriendClick: (Long) -> Unit,
    viewModel: FriendViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.addResult) {
        state.addResult?.let { result ->
            val msg = when (result) {
                is AddFriendResult.Success -> "已新增 ${result.friend.name}"
                is AddFriendResult.AlreadyExists -> "${result.friend.name} 已在好友列表"
                is AddFriendResult.Error -> result.message
            }
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAddResult()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("好友", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                    GlassIconButton(onClick = { showAdd = true }) {
                        WakeyIcon(WIcon.plus, size = 22.dp, tint = WColors.ink)
                    }
                }
            }

            if (state.friends.isEmpty()) {
                item {
                    Text(
                        "還沒有好友，按右上 + 掃描 QR 加好友",
                        fontSize = 14.sp, color = WColors.inkSoft,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            items(state.friends, key = { it.id }) { friend ->
                FriendCard(friend) { onFriendClick(friend.id) }
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }

    if (showAdd) {
        AddFriendSheet(
            userName = profileState.profile?.name ?: "我",
            userHandle = profileState.profile?.handle ?: "",
            userColor = profileState.profile?.color ?: "#FF8A6B",
            userId = profileState.profile?.uuid ?: "",
            onScanResult = { uid -> viewModel.addFriendFromQr(uid); showAdd = false },
            onClose = { showAdd = false }
        )
    }
}

// ── 好友卡片（2×2 grid 佈局）─────────────────────────────────────────────
@Composable
private fun FriendCard(friend: Friend, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 左上：頭像 + 名稱
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Avatar(friend.name, friend.color, 44.dp, ring = true, photoUri = friend.photoUri)
                    Column {
                        Text(friend.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                        Text("@${friend.handle}", fontSize = 11.sp, color = WColors.inkSoft)
                    }
                }
                // 右上：下次響鈴
                Column(horizontalAlignment = Alignment.End) {
                    Text("下次響鈴", fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft.copy(alpha = 0.7f))
                    Text(
                        friend.nextAlarmTime ?: "--:--",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 左下：留言
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WakeyIcon(WIcon.messageCircle, size = 12.dp, tint = WColors.inkSoft)
                    Text(
                        "「${friend.message}」",
                        fontSize = 12.sp, color = WColors.inkSoft, fontStyle = FontStyle.Italic,
                        maxLines = 1
                    )
                }
                // 右下：可喚醒狀態
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (friend.canWake) Color(0xFF7FD3B5) else Color(0xFFD88A8A))
                    )
                    Text(
                        "可喚醒 · ${if (friend.canWake) "是" else "否"}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (friend.canWake) Color(0xFF1F8A5B) else Color(0xFFA04040)
                    )
                }
            }
        }
    }
}

// ── 加好友 BottomSheet（我的 QR / 掃描 兩個分頁）────────────────────────
@Composable
private fun AddFriendSheet(
    userName: String,
    userHandle: String,
    userColor: String,
    userId: String,
    onScanResult: (String) -> Unit,
    onClose: () -> Unit
) {
    var mode by remember { mutableStateOf("qr") } // "qr" | "scan"

    WakeyBottomSheet(onDismiss = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("加好友", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
            Box(modifier = Modifier.clickable(onClick = onClose)) {
                WakeyIcon(WIcon.x, size = 20.dp, tint = WColors.inkSoft)
            }
        }

        // 分頁切換
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(WColors.ink.copy(alpha = 0.08f))
                .padding(4.dp)
                .padding(bottom = 0.dp)
        ) {
            listOf("qr" to "我的 QR", "scan" to "掃描").forEach { (k, label) ->
                val sel = mode == k
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (sel) Color.White else Color.Transparent)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { mode = k }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (sel) WColors.accent else WColors.inkSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (mode == "qr") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userId.isBlank()) {
                    CircularProgressIndicator()
                } else {
                    val qr = remember(userId) { QrCodeGenerator().generate(userId) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Image(qr.asImageBitmap(), "我的 QR", modifier = Modifier.size(180.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Avatar(userName, userColor, 48.dp, ring = true)
                    Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                        modifier = Modifier.padding(top = 8.dp))
                    Text("@$userHandle", fontSize = 14.sp, color = WColors.inkSoft)
                    Text("請朋友掃描以加為好友", fontSize = 12.sp, color = WColors.inkSoft,
                        modifier = Modifier.padding(top = 12.dp))

                    // 分享連結（傳給沒同框的朋友）
                    val ctx = LocalContext.current
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(WColors.accent.copy(alpha = 0.14f))
                            .clickable {
                                // 用 https 中轉頁，聊天 App 才會把連結變成可點；
                                // 網頁再自動轉跳 wakey://add-friend 喚起 App。
                                val link = "https://arronbo.github.io/wakey/?type=friend&uid=$userId"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT,
                                        "$userName 邀請你加為 Wakey 好友：$link")
                                }
                                runCatching {
                                    ctx.startActivity(
                                        android.content.Intent.createChooser(intent, "分享加好友連結")
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WakeyIcon(WIcon.share, size = 14.dp, tint = WColors.accentDeep)
                        Text("分享加好友連結",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = WColors.accentDeep)
                    }
                }
            }
        } else {
            ScanTab(onScanResult)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ScanTab(onScanResult: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (granted) {
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1B1730))
            ) {
                QrScannerView(onResult = onScanResult)
            }
            Text("對準朋友的 QR Code", fontSize = 14.sp, color = WColors.inkSoft,
                modifier = Modifier.padding(top = 12.dp))
        } else {
            Text("需要相機權限才能掃描", fontSize = 14.sp, color = WColors.inkSoft)
        }
    }
}
