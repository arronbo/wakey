// 玻璃擬態共用元件：對應網頁版 GlassCard、IconButton、WakeToggle、SettingRow、BottomSheet
package com.wakey.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── 色彩常數（對應 CSS 設計系統）────────────────────────────────────────
val Coral     = Color(0xFFFF8A6B)
val DeepPlum  = Color(0xFF3B2A4A)
val InkSoft   = Color(0xFF6B5A78)
val DarkCoral = Color(0xFFC2543A)
val Peach     = Color(0xFFFFD3B5)
val Lilac     = Color(0xFFE8B7E0)
val Periwinkle= Color(0xFFA7B7E8)

// ── 漸層背景（對應 app-bg）──────────────────────────────────────────────
val AppGradient = Brush.linearGradient(
    colors = listOf(Peach, Lilac, Periwinkle),
    start = Offset(0f, 0f),
    end = Offset(1200f, 1800f)
)

// ── GlassCard ────────────────────────────────────────────────────────────
// strong=true → 更不透明（對應 .glass-strong）
// dark=true   → 深色玻璃（對應 .glass-dark）
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    dark: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val c = WColors
    val bgColor = if (dark && !c.isDark) Color.Black else c.glassBase
    val bgAlpha = when {
        dark -> if (c.isDark) c.glassStrongAlpha else c.glassDarkAlpha
        strong -> c.glassStrongAlpha
        else -> c.glassAlpha
    }
    val borderAlpha = if (strong) c.glassStrongBorderAlpha else c.glassBorderAlpha
    val shape = RoundedCornerShape(24.dp)

    val base = Modifier
        .clip(shape)
        .background(bgColor.copy(alpha = bgAlpha))
        .border(1.dp, c.glassBorder.copy(alpha = borderAlpha), shape)

    Box(
        modifier = modifier.then(base).then(
            if (onClick != null) Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ) else Modifier
        ),
        content = content
    )
}

// ── 圓形玻璃按鈕（對應 IconButton 元件）──────────────────────────────────
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c = WColors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.glassBase.copy(alpha = c.glassAlpha))
            .border(1.dp, c.glassBorder.copy(alpha = c.glassBorderAlpha), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ── 自訂 Toggle 開關（對應 alarm.enabled 切換，非 Material Switch）────────
@Composable
fun WakeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = tween(150),
        label = "knob"
    )
    val c = WColors
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (checked) c.accent else c.ink.copy(alpha = 0.15f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ── Setting Row（鬧鐘編輯列，含珊瑚色圖示框、標籤、詳情、箭頭或 Toggle）──
@Composable
fun SettingRow(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    toggle: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val c = WColors
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (toggle == null) onClick else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 強調色圖示框
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { icon() }

            // 標籤
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.ink
            )

            // 右側：詳情文字 / Toggle / 箭頭
            when {
                toggle != null -> WakeToggle(
                    checked = toggle,
                    onCheckedChange = { onToggle?.invoke() }
                )
                detail != null -> Text(
                    text = detail,
                    fontSize = 14.sp,
                    color = c.inkSoft
                )
            }
            if (toggle == null) {
                Text("›", fontSize = 20.sp, color = c.ink.copy(alpha = 0.4f))
            }
        }
    }
}

// ── Bottom Sheet（對應 BottomSheet 元件）────────────────────────────────
// 採用 Material3 ModalBottomSheet：原生處理鍵盤遮擋(imePadding)、系統列 inset、
// 拖曳關閉，外觀套用玻璃白底以符合設計
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeyBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = WColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.sheet.copy(alpha = if (c.isDark) 1f else 0.97f),
        scrimColor = Color.Black.copy(alpha = 0.30f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(c.ink.copy(alpha = 0.18f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())  // 內容過長可整體捲動
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding()   // 鍵盤彈出時自動上推，輸入框不被遮住
        ) {
            content()
        }
    }
}
