// 以 uid 檢視某使用者（多為群組中的非好友成員），可直接加為好友
package com.wakey.app.ui.screen.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.AddFriendResult
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel(),
    friendViewModel: FriendViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val friendState by friendViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(friendState.addResult) {
        friendState.addResult?.let { result ->
            adding = false
            val msg = when (result) {
                is AddFriendResult.Success -> "已加 ${result.friend.name} 為好友！"
                is AddFriendResult.AlreadyExists -> "${result.friend.name} 已是好友"
                is AddFriendResult.Error -> result.message
            }
            snackbarHostState.showSnackbar(msg)
            friendViewModel.clearAddResult()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(WColors.bg).statusBarsPadding()
    ) {
        if (ui.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 80.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(ui.name, ui.color, 86.dp, ring = true, photoUri = ui.photoUri)
                Text(ui.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                    modifier = Modifier.padding(top = 12.dp))
                if (ui.handle.isNotBlank())
                    Text("@${ui.handle}", fontSize = 14.sp, color = WColors.inkSoft)

                Spacer(Modifier.height(8.dp))

                if (ui.message.isNotBlank()) {
                    GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("個人留言", fontSize = 10.sp, letterSpacing = 1.sp, color = WColors.inkSoft)
                            Text("「${ui.message}」", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = WColors.ink, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                when {
                    ui.notFound -> Text(
                        "找不到這位使用者的資料",
                        fontSize = 14.sp, color = WColors.inkSoft
                    )
                    ui.isSelf -> Text(
                        "這是你自己 🙂",
                        fontSize = 14.sp, color = WColors.inkSoft
                    )
                    ui.isFriend -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WColors.ink.copy(alpha = 0.06f))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WakeyIcon(WIcon.check, size = 18.dp, tint = WColors.inkSoft)
                            Text("已是好友", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = WColors.inkSoft)
                        }
                    }
                    else -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WColors.accent)
                            .clickable(enabled = !adding) {
                                adding = true
                                friendViewModel.addFriendFromQr(ui.uid)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (adding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White
                                )
                            } else {
                                WakeyIcon(WIcon.plus, size = 18.dp, tint = Color.White)
                            }
                            Text(if (adding) "加好友中…" else "加為好友",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 返回鍵
        GlassIconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp, start = 20.dp)
        ) {
            WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
        )
    }
}
