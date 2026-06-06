// 好友個人資料畫面：對應 React 版 FriendProfileScreen
package com.wakey.app.ui.screen.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun FriendProfileScreen(
    friendId: Long,
    onBack: () -> Unit,
    onMessage: () -> Unit = {},
    viewModel: FriendViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val friend by viewModel.getFriend(friendId).collectAsState(initial = null)
    val profileState by profileViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRecordSheet by remember { mutableStateOf(false) }
    var sendingRecord by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
            .statusBarsPadding()
    ) {
        val f = friend
        if (f == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // 主內容：單一可垂直滾動 Column；矮螢幕內容會自然往下展開、可滑動，
            // 不再用「上 align TopCenter / 下 align BottomCenter」分上下兩塊造成重疊。
            // 底部 padding 加大，讓使用者可以把刪除好友按鈕往上多滾一點、更好按
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 80.dp, bottom = 240.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 頭像 + 名稱（置中區塊）
                Avatar(f.name, f.color, 86.dp, ring = true, photoUri = f.photoUri)
                Text(f.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                    modifier = Modifier.padding(top = 12.dp))
                Text("@${f.handle}", fontSize = 14.sp, color = WColors.inkSoft)

                Spacer(Modifier.height(8.dp))

                // 個人留言卡
                GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("個人留言", fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                        Text("「${f.message}」", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = WColors.ink, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                // 下次響鈴 / 可喚醒時段
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("下次響鈴", fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                            Text(f.nextAlarmTime ?: "--:--", fontSize = 28.sp,
                                fontWeight = FontWeight.Bold, color = WColors.ink)
                        }
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("可喚醒時段", fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                            Text(
                                if (f.wakeWindowStart != null && f.wakeWindowEnd != null)
                                    "${f.wakeWindowStart} – ${f.wakeWindowEnd}" else "拒絕喚醒",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                // 叫他起床
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (f.canWake) WColors.accent else WColors.ink.copy(alpha = 0.18f))
                        .clickable(enabled = f.canWake) {
                            scope.launch {
                                val token = f.fcmToken
                                val myName = profileState.profile?.name ?: "朋友"
                                if (token != null) {
                                    val ok = FcmSender().sendWake(token, myName, profileState.profile?.uuid)
                                    if (ok) profileViewModel.logOutgoingWake(f.userId)
                                    snackbarHostState.showSnackbar(if (ok) "已叫醒 ${f.name}！" else "發送失敗")
                                } else {
                                    snackbarHostState.showSnackbar("無法取得好友推播 token")
                                }
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WakeyIcon(WIcon.bellRing, size = 18.dp,
                            tint = if (f.canWake) Color.White else WColors.inkSoft)
                        Text(
                            if (f.canWake) "叫他起床" else "目前無法喚醒",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (f.canWake) Color.White else WColors.inkSoft
                        )
                    }
                }

                // 錄音叫他起床（次要按鈕）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (f.canWake) WColors.accent.copy(alpha = 0.12f)
                            else WColors.ink.copy(alpha = 0.06f)
                        )
                        .clickable(enabled = f.canWake) { showRecordSheet = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WakeyIcon(
                        WIcon.music, size = 16.dp,
                        tint = if (f.canWake) WColors.accentDeep else WColors.inkSoft
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "錄音叫他起床",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (f.canWake) WColors.accentDeep else WColors.inkSoft
                    )
                }

                // 刪除好友（destructive，需確認）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WakeyIcon(WIcon.trash, size = 16.dp, tint = Color(0xFFA04040))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "刪除好友",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA04040)
                    )
                }
            }

            // 頂部 back / message 按鈕：浮動在內容上方
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassIconButton(onClick = onBack) {
                    WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
                }
                GlassIconButton(onClick = onMessage) {
                    WakeyIcon(WIcon.messageCircle, size = 20.dp, tint = WColors.ink)
                }
            }

            if (showRecordSheet) {
                WakeRecordSheet(
                    friendName = f.name,
                    sending = sendingRecord,
                    onDismiss = { if (!sendingRecord) showRecordSheet = false },
                    onSend = { audioBase64 ->
                        sendingRecord = true
                        scope.launch {
                            val token = f.fcmToken
                            val myName = profileState.profile?.name ?: "朋友"
                            val myUid = profileState.profile?.uuid
                            if (token == null) {
                                sendingRecord = false
                                showRecordSheet = false
                                snackbarHostState.showSnackbar("無法取得好友推播 token")
                                return@launch
                            }
                            val messageId = viewModel.uploadWakeMessage(f.userId, audioBase64)
                            if (messageId == null) {
                                sendingRecord = false
                                showRecordSheet = false
                                snackbarHostState.showSnackbar("上傳錄音失敗")
                                return@launch
                            }
                            val ok = FcmSender().sendWake(token, myName, myUid, messageId)
                            if (ok) profileViewModel.logOutgoingWake(f.userId)
                            sendingRecord = false
                            showRecordSheet = false
                            snackbarHostState.showSnackbar(
                                if (ok) "已用錄音叫醒 ${f.name}！" else "發送失敗"
                            )
                        }
                    }
                )
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("刪除好友？") },
                    text = {
                        Text("確定要刪除 ${f.name} 嗎？對方的好友列表也會一併移除你。")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteFriend(f.id)
                            onBack()
                        }) {
                            Text("刪除", color = Color(0xFFA04040), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                    }
                )
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
            )
        }
    }
}
