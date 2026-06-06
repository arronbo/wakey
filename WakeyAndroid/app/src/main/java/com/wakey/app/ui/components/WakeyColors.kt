// 語義化色彩層：提供淺色／深色兩套色票，畫面透過 WColors 取用
// 取代各畫面散落的硬編碼顏色常量，讓深色模式能整體切換
package com.wakey.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class WakeyColors(
    val isDark: Boolean,
    val bg: Brush,                    // 全畫面漸層背景
    val ink: Color,                   // 主文字 / 深色裝飾
    val inkSoft: Color,               // 次要文字 / 說明
    val accent: Color,                // 主強調色（按鈕底、選中態）
    val accentDeep: Color,            // 強調圖示色（SettingRow 圖示）
    val onAccent: Color,              // 強調色上的文字
    // 玻璃卡片
    val glassBase: Color,
    val glassAlpha: Float,
    val glassStrongAlpha: Float,
    val glassDarkAlpha: Float,
    val glassBorder: Color,
    val glassBorderAlpha: Float,
    val glassStrongBorderAlpha: Float,
    // 實心面板
    val raised: Color,                // 浮起實心面板（輸入框/選中分段/導覽列底）
    val raisedSoft: Color,            // 淡底基色（取消鈕等，需配 copy(alpha)）
    val sheet: Color,                 // BottomSheet 容器底
    val scrim: Color
)

fun lightWakeyColors() = WakeyColors(
    isDark = false,
    bg = Brush.linearGradient(
        colors = listOf(Color(0xFFFFD3B5), Color(0xFFE8B7E0), Color(0xFFA7B7E8)),
        start = Offset(0f, 0f), end = Offset(1200f, 1800f)
    ),
    ink = Color(0xFF3B2A4A),
    inkSoft = Color(0xFF6B5A78),
    accent = Color(0xFFFF8A6B),
    accentDeep = Color(0xFFC2543A),
    onAccent = Color.White,
    glassBase = Color.White,
    glassAlpha = 0.32f,
    glassStrongAlpha = 0.55f,
    glassDarkAlpha = 0.30f,
    glassBorder = Color.White,
    glassBorderAlpha = 0.55f,
    glassStrongBorderAlpha = 0.70f,
    raised = Color.White,
    raisedSoft = Color.Black,
    sheet = Color.White,
    scrim = Color.Black
)

fun darkWakeyColors() = WakeyColors(
    isDark = true,
    bg = Brush.linearGradient(
        colors = listOf(Color(0xFF1E1430), Color(0xFF241A3A), Color(0xFF14101F)),
        start = Offset(0f, 0f), end = Offset(1200f, 1800f)
    ),
    ink = Color(0xFFECE6F4),
    inkSoft = Color(0xFFA99CB8),
    accent = Color(0xFFA78BDE),       // 柔和紫
    accentDeep = Color(0xFFC3B0EE),   // 深色下圖示用更亮紫
    onAccent = Color.White,
    glassBase = Color.White,
    glassAlpha = 0.07f,
    glassStrongAlpha = 0.12f,
    glassDarkAlpha = 0.22f,
    glassBorder = Color.White,
    glassBorderAlpha = 0.12f,
    glassStrongBorderAlpha = 0.18f,
    raised = Color(0xFF2C2142),
    raisedSoft = Color.White,
    sheet = Color(0xFF241834),
    scrim = Color.Black
)

val LocalWakeyColors = staticCompositionLocalOf { lightWakeyColors() }

// 在任何 @Composable body 內以 WColors.xxx 取用當前色票
val WColors: WakeyColors
    @ReadOnlyComposable
    @Composable
    get() = LocalWakeyColors.current
