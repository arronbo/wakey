// 群組新增／編輯 Sheet：對應 React 版 NewGroupSheet / GroupSettingsSheet
package com.wakey.app.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.wakey.app.domain.model.Friend
import com.wakey.app.domain.model.Group
import com.wakey.app.ui.components.*

@Composable
fun GroupEditSheet(
    group: Group?,
    allFriends: List<Friend>,
    onSave: (Group) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var message by remember { mutableStateOf(group?.message ?: "") }
    var photoUri by remember { mutableStateOf(group?.photoUri) }
    var members by remember { mutableStateOf(group?.memberIds?.toSet() ?: emptySet()) }

    // 不再強制至少一人；只要名字非空就能建立
    val canSave = name.isNotBlank()

    WakeyBottomSheet(onDismiss = onDismiss) {
        // 頂部列
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", fontSize = 14.sp, color = WColors.inkSoft,
                modifier = Modifier.clickable(onClick = onDismiss))
            Text(if (group == null) "新增群組" else "群組設定",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
            Text(
                if (group == null) "建立" else "儲存",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (canSave) WColors.accent else WColors.inkSoft.copy(alpha = 0.4f),
                modifier = Modifier.clickable(enabled = canSave) {
                    onSave(
                        Group(
                            id = group?.id ?: 0L,
                            name = name.trim(),
                            message = message.trim(),
                            photoUri = photoUri,
                            memberIds = members.toList()
                        )
                    )
                }
            )
        }

        // 預覽
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoPicker(
                photoUri = photoUri,
                onPicked = { photoUri = it },
                size = 64.dp
            ) {
                GroupAvatar(
                    colors = listOf("#FF8A6B"),
                    size = 64.dp,
                    monogram = name.firstOrNull()?.toString() ?: "?",
                    photoUri = photoUri
                )
            }
            Text(name.ifBlank { "新群組" }, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, color = WColors.ink,
                modifier = Modifier.padding(top = 8.dp))
            Text("${members.size} 位成員", fontSize = 11.sp, color = WColors.inkSoft)
        }

        // 群組名稱
        FieldLabel("群組名稱")
        GlassInput(name, { name = it }, "例：早八戰隊")
        Spacer(Modifier.height(12.dp))

        // 群組留言
        FieldLabel("群組留言（選填）")
        GlassInput(message, { message = it }, "一句話描述這個群組")
        Spacer(Modifier.height(12.dp))

        // 邀請成員（被勾選者會收到通知，同意後才加入；可選 0 人）
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("邀請成員（${members.size} 已選）",
                fontSize = 11.sp, color = WColors.inkSoft)
            Text("選填", fontSize = 11.sp, color = WColors.inkSoft.copy(alpha = 0.7f))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            allFriends.forEach { f ->
                val sel = f.id in members
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) WColors.accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            members = if (sel) members - f.id else members + f.id
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Avatar(f.name, f.color, 32.dp)
                    Text(f.name, modifier = Modifier.weight(1f), fontSize = 14.sp, color = WColors.ink)
                    if (sel) WakeyIcon(WIcon.check, size = 16.dp, tint = WColors.accent)
                    else WakeyIcon(WIcon.plus, size = 14.dp, tint = WColors.inkSoft.copy(alpha = 0.5f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, color = WColors.inkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
}

@Composable
private fun GlassInput(value: String, onChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 15.sp, color = WColors.inkSoft.copy(alpha = 0.6f))
        }
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(color = WColors.ink, fontSize = 15.sp),
            cursorBrush = SolidColor(WColors.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

