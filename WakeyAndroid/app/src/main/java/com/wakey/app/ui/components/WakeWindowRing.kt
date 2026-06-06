// 24 小時喚醒時段環（對應 React 版 WakeWindowRing 的 conic-gradient 視覺）
package com.wakey.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun toAngle(hhmm: String): Float {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (h * 60 + m) / 1440f * 360f
}

@Composable
fun WakeWindowRing(start: String, end: String, modifier: Modifier = Modifier) {
    val a1 = toAngle(start)
    val a2 = toAngle(end)
    val span = (((a2 - a1) + 360f) % 360f)
    val hours = (span / 15f).roundToInt() * 15 / 60f

    val c = WColors
    val ringTrack = c.ink.copy(alpha = 0.10f)
    val ringAccent = c.accent
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(170.dp)) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                // 底環（淡）
                drawArc(
                    color = ringTrack,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke)
                )
                // 可喚醒區間（強調色），-90 讓 0:00 在正上方
                drawArc(
                    color = ringAccent,
                    startAngle = a1 - 90f, sweepAngle = span, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke)
                )
            }
            // 中央底（僅顯示時間範圍）
            Column(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(c.raised.copy(alpha = if (c.isDark) 0.5f else 0.7f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("$start – $end", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.ink)
            }
            // 0/6/12/18 刻度
            listOf(0, 6, 12, 18).forEach { h ->
                val angle = (h / 24f) * 360f - 90f
                val rad = Math.toRadians(angle.toDouble())
                val r = 70.dp
                Text(
                    text = "%02d".format(h),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.ink,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (cos(rad).toFloat() * r.value).dp,
                            y = (sin(rad).toFloat() * r.value).dp
                        )
                )
            }
        }
    }
}
