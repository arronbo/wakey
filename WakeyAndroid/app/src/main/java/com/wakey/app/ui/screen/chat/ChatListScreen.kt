// 聊天列表（inbox）：顯示所有對話，點擊進入
package com.wakey.app.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.domain.model.ChatType
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.ChatListViewModel
import com.wakey.app.viewmodel.ChatRow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    onOpenChat: (type: ChatType, localId: Long) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WColors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "聊天", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = WColors.ink,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        "還沒有對話，到好友或群組頁開始聊天吧",
                        fontSize = 14.sp, color = WColors.inkSoft,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(rows, key = { it.conversationId }) { row ->
                ChatRowItem(row) { onOpenChat(row.type, row.navLocalId) }
            }
        }
    }
}

@Composable
private fun ChatRowItem(row: ChatRow, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (row.type == ChatType.GROUP) {
                GroupAvatar(
                    colors = listOf(row.color), size = 48.dp,
                    monogram = row.title.firstOrNull()?.toString(), photoUri = row.photoUri
                )
            } else {
                Avatar(row.title, row.color, 48.dp, ring = true, photoUri = row.photoUri)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = WColors.ink, modifier = Modifier.weight(1f)
                    )
                    if (row.lastAtMillis > 0) {
                        Text(formatRelative(row.lastAtMillis), fontSize = 11.sp, color = WColors.inkSoft)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.lastText.ifBlank { "尚無訊息" },
                        fontSize = 13.sp,
                        color = if (row.unread) WColors.ink else WColors.inkSoft,
                        fontWeight = if (row.unread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (row.unread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).background(WColors.accent)
                        )
                    }
                }
            }
        }
    }
}

// 今天顯示時間、昨天顯示「昨天」、更早顯示日期
private fun formatRelative(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (yesterday) return "昨天"
    return SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(millis))
}
