// 群組詳情：玻璃風格，與「我」頁一致。
// 頂部 back/設定按鈕 + 群組頭像/名稱 + 留言卡 + 成員列表卡 + 起床按鈕
package com.wakey.app.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.asImageBitmap
import com.wakey.app.domain.model.Friend
import com.wakey.app.qr.QrCodeGenerator
import com.wakey.app.ui.components.*
import com.wakey.app.ui.screen.friend.WakeRecordSheet
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.GroupViewModel
import com.wakey.app.viewmodel.WakeResult
import kotlinx.coroutines.launch

@Composable
fun GroupDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onMemberClick: (Long) -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
    friendViewModel: FriendViewModel = hiltViewModel()
) {
    val group by viewModel.getGroup(groupId).collectAsState(initial = null)
    val members by viewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showRecord by remember { mutableStateOf(false) }
    var sendingRecord by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val friendState by friendViewModel.uiState.collectAsState()

    LaunchedEffect(state.wakeResult) {
        state.wakeResult?.let { result ->
            val msg = when (result) {
                is WakeResult.Sent -> "已喚醒 ${result.count} 位成員！"
                is WakeResult.NoWakeable -> "目前沒有可喚醒的成員（共 ${result.total} 位）"
            }
            snackbarHostState.showSnackbar(msg)
            viewModel.clearWakeResult()
        }
    }

    val g = group
    val wakeableCount = members.count { it.canWake }

    Box(modifier = Modifier.fillMaxSize().background(WColors.bg)) {
        // 主滾動內容：全部按鈕都放在 LazyColumn 內、整頁順向滾動，避免任何手機底部重疊
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 76.dp, start = 20.dp, end = 20.dp, bottom = 240.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 群組頭像 + 名稱
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GroupAvatar(
                        colors = listOf(g?.color ?: "#FF8A6B"),
                        size = 86.dp,
                        monogram = g?.name?.firstOrNull()?.toString(),
                        photoUri = g?.photoUri
                    )
                    Text(
                        g?.name ?: "群組",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "${members.size} 位成員",
                        fontSize = 13.sp, color = WColors.inkSoft
                    )
                }
            }

            // 群組留言
            if (!g?.message.isNullOrBlank()) {
                item {
                    GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("群組留言", fontSize = 10.sp, letterSpacing = 1.sp,
                                color = WColors.inkSoft)
                            Text(
                                "「${g?.message}」",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = WColors.ink, fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 成員列表
            item {
                Text(
                    "成員（$wakeableCount / ${members.size} 可喚醒）",
                    fontSize = 11.sp, letterSpacing = 1.sp, color = WColors.inkSoft,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(members, key = { it.id }) { member ->
                MemberCard(
                    member = member,
                    isOwner = g?.ownerUid?.isNotBlank() == true && member.userId == g?.ownerUid
                ) { onMemberClick(member.id) }
            }

            if (members.isEmpty() && g != null) {
                item {
                    Text(
                        if ((g?.pendingUids?.size ?: 0) > 0)
                            "等待被邀請的成員加入…（${g?.pendingUids?.size} 人）"
                        else "這個群組還沒有成員，按上方設定邀請朋友吧",
                        fontSize = 13.sp, color = WColors.inkSoft,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // ── 動作按鈕區（QR / 錄音 / 起床） ────────────────────────
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WColors.accent.copy(alpha = 0.12f))
                        .clickable { showQr = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WakeyIcon(WIcon.scanLine, size = 16.dp, tint = WColors.accentDeep)
                    Spacer(Modifier.size(8.dp))
                    Text("顯示群組 QR Code", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = WColors.accentDeep)
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (wakeableCount > 0) WColors.accent.copy(alpha = 0.12f)
                            else WColors.ink.copy(alpha = 0.06f)
                        )
                        .clickable(enabled = wakeableCount > 0) { showRecord = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WakeyIcon(
                        WIcon.music, size = 16.dp,
                        tint = if (wakeableCount > 0) WColors.accentDeep else WColors.inkSoft
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "錄音叫醒群組",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (wakeableCount > 0) WColors.accentDeep else WColors.inkSoft
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (wakeableCount > 0) WColors.accent else WColors.ink.copy(alpha = 0.18f))
                        .clickable(enabled = wakeableCount > 0) { viewModel.wakeGroup(groupId) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WakeyIcon(
                            WIcon.bellRing, size = 18.dp,
                            tint = if (wakeableCount > 0) Color.White else WColors.inkSoft
                        )
                        Text(
                            if (wakeableCount > 0) "起床！喚醒 $wakeableCount 位"
                            else "目前無人可喚醒",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (wakeableCount > 0) Color.White else WColors.inkSoft
                        )
                    }
                }
            }

            // 危險區：退出（非 owner）/ 刪除整個群組（owner）
            g?.let { groupVal ->
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isOwner = myUid != null && groupVal.ownerUid == myUid
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (isOwner) confirmDelete = true else confirmLeave = true
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WakeyIcon(
                            if (isOwner) WIcon.trash else WIcon.logOut,
                            size = 16.dp, tint = Color(0xFFA04040)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            if (isOwner) "刪除整個群組" else "退出群組",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA04040)
                        )
                    }
                }
            }
        }

        // 頂部列：返回 + 設定
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(onClick = onBack) {
                WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
            }
            GlassIconButton(onClick = { showSettings = true }) {
                WakeyIcon(WIcon.settings, size = 20.dp, tint = WColors.ink)
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )
    }

    if (showSettings) {
        g?.let { group ->
            GroupEditSheet(
                group = group,
                allFriends = friendState.friends,
                onSave = { updated ->
                    viewModel.saveGroup(updated)
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }
    }

    // 群組 QR Code 顯示
    if (showQr && g != null) {
        val cid = g?.cloudId ?: ""
        WakeyBottomSheet(onDismiss = { showQr = false }) {
            Text("群組 QR Code", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = WColors.ink,
                modifier = Modifier.padding(bottom = 8.dp))
            Text("讓朋友掃描以直接加入群組（不需邀請）", fontSize = 12.sp,
                color = WColors.inkSoft, modifier = Modifier.padding(bottom = 16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (cid.isBlank()) {
                    Text("群組尚未同步，請稍候", color = WColors.inkSoft)
                } else {
                    val qr = remember(cid) {
                        QrCodeGenerator().generate("wakey-group:$cid")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Image(
                            qr.asImageBitmap(), "群組 QR",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(g?.name ?: "", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = WColors.ink)

                    // 分享連結（傳給沒同框的朋友）
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val groupName = g?.name ?: "群組"
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(WColors.accent.copy(alpha = 0.14f))
                            .clickable {
                                val link = "wakey://join-group?cloudId=$cid"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT,
                                        "邀請你加入 Wakey 群組「$groupName」：$link")
                                }
                                runCatching {
                                    ctx.startActivity(
                                        android.content.Intent.createChooser(intent, "分享加群組連結")
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WakeyIcon(WIcon.share, size = 14.dp, tint = WColors.accentDeep)
                        Text("分享加群組連結",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = WColors.accentDeep)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // 錄音叫醒群組
    if (showRecord && g != null) {
        WakeRecordSheet(
            friendName = g?.name ?: "群組",
            sending = sendingRecord,
            onDismiss = { if (!sendingRecord) showRecord = false },
            onSend = { audioBase64 ->
                sendingRecord = true
                scope.launch {
                    val sent = viewModel.wakeGroupWithAudio(groupId, audioBase64)
                    sendingRecord = false
                    showRecord = false
                    snackbarHostState.showSnackbar(
                        if (sent > 0) "已用錄音叫醒 $sent 位！"
                        else "目前沒有可喚醒的成員"
                    )
                }
            }
        )
    }

    // 退出群組確認
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("退出群組？") },
            text = { Text("退出後將不再收到此群組的喚醒，且需要被重新邀請才能再加入。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.leaveGroup(groupId)
                    onBack()
                }) {
                    Text("退出", color = Color(0xFFA04040), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("取消") }
            }
        )
    }

    // 刪除整個群組（owner）確認
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除整個群組？") },
            text = { Text("此操作無法復原，所有成員都會失去此群組。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteGroup(groupId)
                    onBack()
                }) {
                    Text("刪除", color = Color(0xFFA04040), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MemberCard(member: Friend, isOwner: Boolean = false, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(member.name, member.color, 44.dp, ring = true, photoUri = member.photoUri)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(member.name, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = WColors.ink)
                    if (isOwner) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WColors.accent.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("管理員", fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, color = WColors.accentDeep)
                        }
                    }
                }
                Text("@${member.handle}", fontSize = 11.sp, color = WColors.inkSoft)
            }
            // 可喚醒狀態指示燈
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (member.canWake) Color(0xFF7FD3B5) else Color(0xFFD88A8A))
                )
                Text(
                    if (member.canWake) "可喚醒" else "勿擾",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (member.canWake) Color(0xFF1F8A5B) else Color(0xFFA04040)
                )
            }
        }
    }
}
