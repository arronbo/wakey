// SVG 線條圖示（對應網頁版 Lucide icons）+ Avatar + GroupAvatar
package com.wakey.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 圖示路徑資料（直接取自網頁版 icons.jsx，24×24 viewBox）
object WIcon {
    const val home = "M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-7h-6v7H4a1 1 0 0 1-1-1V9.5z"
    const val alarmClock = "M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 10v4l2 2M5 4 2 7M19 4l3 3M6 22l-2 2M18 22l2 2"
    const val users = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"
    const val usersRound = "M18 21a8 8 0 0 0-16 0M10 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM22 21a7 7 0 0 0-5-6.71M15 4.05a5 5 0 0 1 0 9.9"
    const val plus = "M12 5v14M5 12h14"
    const val chevronLeft = "M15 18l-6-6 6-6"
    const val chevronRight = "M9 18l6-6-6-6"
    const val bell = "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0"
    const val bellRing = "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0M4 2 2 4M22 4l-2-2"
    const val zap = "M13 2 3 14h9l-1 8 10-12h-9l1-8z"
    const val pencil = "M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"
    const val sun = "M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"
    const val moon = "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"
    const val x = "M18 6 6 18M6 6l12 12"
    const val check = "M20 6 9 17l-5-5"
    const val messageCircle = "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"
    const val logOut = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"
    const val scanLine = "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M7 12h10"
    const val music = "M9 18V5l12-2v13M9 13a3 3 0 1 1 0 6 3 3 0 0 1 0-6zM21 11a3 3 0 1 1 0 6 3 3 0 0 1 0-6z"
    const val settings = "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.36.15.67.41.89.74.22.33.34.71.34 1.11v.3z"
    const val vibrate = "M2 8 4 6M22 8l-2-2M2 16l2 2M22 16l-2 2M8 4h8a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
    const val repeat = "M17 2l4 4-4 4M3 11v-1a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v1a4 4 0 0 1-4 4H3"
    const val share = "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8M16 6l-4-4-4 4M12 2v13"
    const val trash = "M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6"
    // 圓圈內單人，用於「我」分頁，與 users（兩人）區分
    const val circleUser = "M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6M7.7 18.4A4 4 0 0 1 12 15a4 4 0 0 1 4.3 3.4"
    const val download = "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"
    const val send = "M22 2 11 13M22 2l-7 20-4-9-9-4 20-7z"
    const val play = "M6 4l14 8-14 8z"
    const val pause = "M8 5v14M16 5v14"
    const val image = "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zM8.5 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zM21 15l-5-5L5 21"
}

@Composable
fun WakeyIcon(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = WColors.ink,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path = PathParser().parsePathString(path).toPath(),
                color = tint,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// 取色彩較深/較淺版本（對應 scenes.jsx 的 shade()）
private fun Color.shade(factor: Float): Color {
    return if (factor < 0) {
        Color(red * (1 + factor), green * (1 + factor), blue * (1 + factor), alpha)
    } else {
        Color(
            red + (1 - red) * factor,
            green + (1 - green) * factor,
            blue + (1 - blue) * factor,
            alpha
        )
    }
}

fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Coral }

// 對應 scenes.jsx shade()：pct>0 變亮、pct<0 變暗
fun shadeColor(hex: String, pct: Int): Color {
    val c = parseHexColor(hex)
    val f = pct / 100f
    return if (f < 0)
        Color(c.red * (1 + f), c.green * (1 + f), c.blue * (1 + f), c.alpha)
    else
        Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, c.alpha)
}

// 圓形頭像（漸層 + 首字 / 自選照片），對應 scenes.jsx Avatar
@Composable
fun Avatar(
    name: String,
    color: String,
    size: Dp,
    ring: Boolean = false,
    photoUri: String? = null,
    modifier: Modifier = Modifier
) {
    val c = parseHexColor(color)
    val initial = name.firstOrNull()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .then(if (ring) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(c.shade(0.15f), c.shade(-0.18f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = photoUri,
                contentDescription = "頭像",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

// 照片選擇器：點頭像右下角小按鈕從相簿選圖，複製到內部儲存後回傳路徑
@Composable
fun PhotoPicker(
    photoUri: String?,
    onPicked: (String?) -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit
) {
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pendingUri = uri }

    Box(modifier = modifier.size(size)) {
        avatar()
        // 右下角編輯/新增按鈕
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((size.value * 0.30f).coerceAtLeast(24f).dp)
                .border(2.dp, Color.White, CircleShape)
                .clip(CircleShape)
                .background(WColors.accent)
                .clickable {
                    launcher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts
                                .PickVisualMedia.ImageOnly
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            WakeyIcon(
                if (photoUri.isNullOrBlank()) WIcon.plus else WIcon.pencil,
                size = (size.value * 0.15f).coerceAtLeast(12f).dp,
                tint = Color.White
            )
        }
    }

    // 選完照片 → 開啟圓形裁切
    pendingUri?.let { uri ->
        ImageCropDialog(
            sourceUri = uri,
            onCancel = { pendingUri = null },
            onCropped = { path -> onPicked(path); pendingUri = null }
        )
    }
}

// 群組頭像（單色 + 首字，或多色堆疊）對應 scenes.jsx GroupAvatar
@Composable
fun GroupAvatar(
    colors: List<String>,
    size: Dp,
    monogram: String? = null,
    photoUri: String? = null,
    modifier: Modifier = Modifier
) {
    val first = colors.firstOrNull()?.let { parseHexColor(it) } ?: WColors.accent
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(first.shade(0.15f), first.shade(-0.18f)))),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = photoUri,
                contentDescription = "群組頭像",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(
                text = monogram ?: "♥",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}
