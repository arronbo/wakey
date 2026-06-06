// 對話畫面：訊息泡泡 + 底部輸入列（一對一與群組共用）
package com.wakey.app.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.domain.model.ChatMessage
import com.wakey.app.domain.model.ChatType
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    chatType: ChatType,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    // 有新訊息時自動捲到底
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    // 進入或收到新訊息時標記已讀
    LaunchedEffect(messages.size, ui.ready) {
        if (ui.ready) viewModel.markRead()
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
                .imePadding()
        ) {
            // 頂部列
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassIconButton(onClick = onBack) {
                    WakeyIcon(WIcon.chevronLeft, size = 22.dp, tint = WColors.ink)
                }
                Text(
                    ui.title.ifBlank { "聊天" },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                    modifier = Modifier.weight(1f)
                )
            }

            // 訊息列表
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "開始你們的對話吧 👋",
                            fontSize = 14.sp, color = WColors.inkSoft,
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                items(messages, key = { it.id.ifBlank { it.createdAtMillis.toString() } }) { msg ->
                    MessageBubble(
                        msg = msg,
                        mine = msg.isMine(ui.myUid),
                        showSender = chatType == ChatType.GROUP && !msg.isMine(ui.myUid)
                    )
                }
            }

            // 輸入列
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(WColors.raised.copy(alpha = if (WColors.isDark) 0.9f else 0.85f))
                        .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (input.isEmpty()) {
                        Text("輸入訊息…", fontSize = 15.sp, color = WColors.inkSoft.copy(alpha = 0.6f))
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = WColors.ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(WColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val canSend = input.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(if (canSend) WColors.accent else WColors.ink.copy(alpha = 0.15f))
                        .clickable(enabled = canSend) {
                            viewModel.send(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    WakeyIcon(
                        WIcon.send, size = 20.dp,
                        tint = if (canSend) Color.White else WColors.inkSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, mine: Boolean, showSender: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (showSender) {
            Text(
                msg.senderName, fontSize = 11.sp, color = WColors.inkSoft,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (mine) 18.dp else 4.dp,
                            bottomEnd = if (mine) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (mine) WColors.accent
                        else WColors.raised.copy(alpha = if (WColors.isDark) 0.85f else 0.9f)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    msg.text,
                    fontSize = 15.sp,
                    color = if (mine) Color.White else WColors.ink
                )
            }
        }
        Text(
            formatTime(msg.createdAtMillis),
            fontSize = 9.sp, color = WColors.inkSoft.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
        )
    }
}

private fun formatTime(millis: Long): String =
    if (millis <= 0) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
