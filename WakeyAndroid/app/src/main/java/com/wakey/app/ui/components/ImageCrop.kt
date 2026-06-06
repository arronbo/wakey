// Instagram 風格圓形頭像裁切：雙指縮放 + 拖曳，圓形預覽範圍
// 確認後依縮放/位移數學計算原圖裁切範圍輸出方形圖（不依賴 graphicsLayer 擷取 API）
package com.wakey.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ImageCropDialog(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onCropped: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val cropDp = 300.dp
    val cropPx = with(density) { cropDp.toPx() }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "雙指縮放、拖曳調整位置",
                    color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                    modifier = Modifier.size(cropDp),
                    contentAlignment = Alignment.Center
                ) {
                    // 預覽圖（縮放/位移）
                    Box(
                        modifier = Modifier
                            .size(cropDp)
                            .clip(RoundedCornerShape(0.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    val maxOff = cropPx * (scale - 1f) / 2f
                                    offset = Offset(
                                        (offset.x + pan.x).coerceIn(-maxOff, maxOff),
                                        (offset.y + pan.y).coerceIn(-maxOff, maxOff)
                                    )
                                }
                            }
                    ) {
                        AsyncImage(
                            model = sourceUri,
                            contentDescription = "裁切預覽",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    }

                    // 圓形遮罩：圓外暗化 + 白框
                    Box(
                        modifier = Modifier
                            .size(cropDp)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val r = size.minDimension / 2f
                                drawRect(Color.Black.copy(alpha = 0.55f))
                                drawCircle(Color.Black, radius = r, blendMode = BlendMode.Clear)
                                drawCircle(
                                    Color.White, radius = r - 1f,
                                    style = Stroke(width = 3f)
                                )
                            }
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .pointerInput(Unit) { detectTapGestures { onCancel() } }
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) { Text("取消", color = Color.White, fontSize = 15.sp) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(WColors.accent)
                            .pointerInput(scale, offset) {
                                detectTapGestures {
                                    scope.launch {
                                        val path = withContext(Dispatchers.IO) {
                                            cropToFile(context, sourceUri, scale, offset.x, offset.y, cropPx)
                                        }
                                        if (path != null) onCropped(path)
                                    }
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text("完成", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 依顯示時的 scale / offset，反推原圖要裁切的方形範圍並輸出
private fun cropToFile(
    context: Context,
    uri: Uri,
    userScale: Float,
    offsetXpx: Float,
    offsetYpx: Float,
    cropPx: Float
): String? {
    return try {
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val sw = src.width.toFloat()
        val sh = src.height.toFloat()
        // ContentScale.Crop 基礎縮放：填滿正方形
        val baseScale = max(cropPx / sw, cropPx / sh)
        val f = baseScale * userScale  // 原圖像素 → 顯示像素

        // 顯示區中心對應原圖中心 + 位移
        // view 點 (vx,vy) → 原圖點：src = sCenter + (v - (cropPx/2 + offset)) / f
        fun mapX(vx: Float) = sw / 2f + (vx - (cropPx / 2f + offsetXpx)) / f
        fun mapY(vy: Float) = sh / 2f + (vy - (cropPx / 2f + offsetYpx)) / f

        var left = mapX(0f)
        var top = mapY(0f)
        var right = mapX(cropPx)
        var bottom = mapY(cropPx)

        left = left.coerceIn(0f, sw)
        top = top.coerceIn(0f, sh)
        right = right.coerceIn(0f, sw)
        bottom = bottom.coerceIn(0f, sh)

        val w = (right - left).roundToInt().coerceAtLeast(1)
        val h = (bottom - top).roundToInt().coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(
            src, left.roundToInt().coerceIn(0, src.width - 1),
            top.roundToInt().coerceIn(0, src.height - 1),
            w.coerceAtMost(src.width - left.roundToInt()),
            h.coerceAtMost(src.height - top.roundToInt())
        )
        // 輸出統一 512 方圖
        val out = Bitmap.createScaledBitmap(cropped, 512, 512, true)

        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
