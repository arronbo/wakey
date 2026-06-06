// 群組列表畫面：完全對應 React 版 GroupScreen + GroupCard
package com.wakey.app.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.ContextCompat
import com.wakey.app.domain.model.Group
import com.wakey.app.ui.components.*
import com.wakey.app.ui.screen.friend.QrScannerView
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupScreen(
    onGroupClick: (Long) -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
    friendViewModel: FriendViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val friendState by friendViewModel.uiState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("群組", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(onClick = { showScan = true }) {
                            WakeyIcon(WIcon.scanLine, size = 22.dp, tint = WColors.ink)
                        }
                        GlassIconButton(onClick = { showCreate = true }) {
                            WakeyIcon(WIcon.plus, size = 22.dp, tint = WColors.ink)
                        }
                    }
                }
            }

            if (state.groups.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "還沒有群組，按右上 + 來建立第一個吧",
                        fontSize = 14.sp, color = WColors.inkSoft,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(state.groups, key = { it.id }) { group ->
                GroupCard(group) { onGroupClick(group.id) }
            }
        }
    }

    if (showCreate) {
        GroupEditSheet(
            group = null,
            allFriends = friendState.friends,
            onSave = { g -> viewModel.saveGroup(g); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }

    if (showScan) {
        GroupScanSheet(
            onResult = { raw ->
                val cloudId = raw.removePrefix("wakey-group:").trim()
                if (cloudId.isBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar("無效的群組 QR Code")
                    }
                } else {
                    scope.launch {
                        val ok = viewModel.joinByCloudId(cloudId)
                        snackbarHostState.showSnackbar(
                            if (ok) "已加入群組！" else "加入失敗，請確認 QR 是否正確"
                        )
                    }
                }
                showScan = false
            },
            onDismiss = { showScan = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp)
        )
    }
}

@Composable
private fun GroupScanSheet(onResult: (String) -> Unit, onDismiss: () -> Unit) {
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

    WakeyBottomSheet(onDismiss = onDismiss) {
        Text("掃描群組 QR Code", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = WColors.ink,
            modifier = Modifier.padding(bottom = 4.dp))
        Text("掃描後會直接加入群組，不需邀請", fontSize = 12.sp,
            color = WColors.inkSoft, modifier = Modifier.padding(bottom = 16.dp))
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
                    QrScannerView(onResult = onResult)
                }
                Text("對準群組的 QR Code", fontSize = 14.sp,
                    color = WColors.inkSoft, modifier = Modifier.padding(top = 12.dp))
            } else {
                Text("需要相機權限才能掃描", fontSize = 14.sp, color = WColors.inkSoft)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GroupAvatar(
                colors = listOf(group.color ?: "#FF8A6B"),
                size = 60.dp,
                monogram = group.name.firstOrNull()?.toString(),
                photoUri = group.photoUri
            )
            Text(
                group.name,
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text("${group.memberUids.size} 位成員", fontSize = 10.sp, color = WColors.inkSoft)
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WColors.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .heightIn(min = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "「${group.message.ifBlank { "（沒有留言）" }}」",
                    fontSize = 11.sp, color = Color_A0533A, fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private val Color_A0533A = androidx.compose.ui.graphics.Color(0xFFA0533A)
