// 主頁外觀：小人（Walker）與房子（Cottage）的多種皮膚，供使用者自選
package com.wakey.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 一個外觀選項（id 存進設定，name 顯示給使用者）
data class SkinOption(val id: String, val name: String)

object Skins {
    val characters = listOf(
        SkinOption("classic", "經典"),
        SkinOption("cat", "貓耳"),
        SkinOption("bear", "小熊"),
        SkinOption("ninja", "忍者"),
        SkinOption("robot", "機器人"),
    )
    val houses = listOf(
        SkinOption("classic", "尖頂"),
        SkinOption("round", "圓頂"),
        SkinOption("tent", "帳篷"),
        SkinOption("flat", "平頂"),
    )
}

// ── 小人 ────────────────────────────────────────────────────────────────
@Composable
fun WalkerSkin(
    skinId: String,
    userColor: String,
    facing: String,
    bobbing: Boolean
) {
    val bob by animateFloatAsState(
        if (bobbing) 2f else 0f,
        animationSpec = repeatable(10, tween(120)),
        label = "bob"
    )
    val ninja = skinId == "ninja"
    val headBase = shadeColor(userColor, if (ninja) -45 else 0)
    val headLight = shadeColor(userColor, if (ninja) -25 else 18)
    val earColor = shadeColor(userColor, if (ninja) -45 else -12)
    val eyeColor = if (ninja) Color.White else WColors.ink
    val headShape = if (skinId == "robot") RoundedCornerShape(7.dp) else CircleShape

    Box(modifier = Modifier.size(28.dp, 40.dp).offset(y = bob.dp)) {
        // 陰影
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .size(20.dp, 6.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
        )
        // 身體
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter).offset(y = (-4).dp)
                .size(20.dp, 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(WColors.accent)
        )

        // 機器人天線
        if (skinId == "robot") {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-2).dp)
                    .size(2.dp, 6.dp).background(WColors.ink)
            )
            Box(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-6).dp)
                    .size(5.dp).clip(CircleShape).background(WColors.accent)
            )
        }
        // 小熊耳（圓）
        if (skinId == "bear") {
            Box(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 2.dp, y = 4.dp)
                    .size(9.dp).clip(CircleShape).background(earColor)
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 4.dp)
                    .size(9.dp).clip(CircleShape).background(earColor)
            )
        }
        // 貓耳（三角）
        if (skinId == "cat") {
            Canvas(modifier = Modifier.align(Alignment.TopCenter).size(28.dp, 12.dp)) {
                fun ear(cx: Float) {
                    val p = Path().apply {
                        moveTo(cx - size.height * 0.5f, size.height)
                        lineTo(cx + size.height * 0.5f, size.height)
                        lineTo(cx, 0f); close()
                    }
                    drawPath(p, earColor)
                }
                ear(size.width * 0.28f)
                ear(size.width * 0.72f)
            }
        }

        // 頭
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter).offset(y = 6.dp)
                .size(24.dp).clip(headShape)
                .background(Brush.linearGradient(listOf(headLight, headBase)))
        ) {
            if (facing != "up") {
                Box(
                    modifier = Modifier
                        .offset(x = if (facing == "left") 4.dp else 7.dp, y = 9.dp)
                        .size(3.dp).clip(CircleShape).background(eyeColor)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = if (facing == "right") (-4).dp else (-7).dp, y = 9.dp)
                        .size(3.dp).clip(CircleShape).background(eyeColor)
                )
            }
            // 忍者頭帶
            if (ninja) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 6.dp)
                        .size(24.dp, 4.dp).background(WColors.accent)
                )
            }
        }
    }
}

// ── 房子 ────────────────────────────────────────────────────────────────
@Composable
fun CottageSkin(
    skinId: String,
    color: String,
    canWake: Boolean,
    knocking: Boolean
) {
    val roof = shadeColor(color, -25)
    val doorColor = if (canWake) Color(0xFF3B2A4A) else Color(0xFF5A4565)
    Box(modifier = Modifier.size(100.dp, 130.dp)) {
        // 牆身
        Box(
            modifier = Modifier
                .offset(8.dp, 38.dp).size(84.dp, 88.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(parseHexColor(color), shadeColor(color, -16))))
        )

        // 屋頂（依皮膚變化）
        when (skinId) {
            "round" -> Box(
                modifier = Modifier.offset(6.dp, 14.dp).size(92.dp, 40.dp)
                    .clip(RoundedCornerShape(topStart = 46.dp, topEnd = 46.dp))
                    .background(roof)
            )
            "flat" -> Box(
                modifier = Modifier.offset(4.dp, 30.dp).size(96.dp, 12.dp)
                    .clip(RoundedCornerShape(4.dp)).background(roof)
            )
            "tent" -> Canvas(modifier = Modifier.offset(8.dp, 4.dp).size(84.dp, 46.dp)) {
                val p = Path().apply {
                    moveTo(0f, size.height); lineTo(size.width, size.height)
                    lineTo(size.width / 2f, 0f); close()
                }
                drawPath(p, roof)
            }
            else -> Canvas(modifier = Modifier.offset(6.dp, 18.dp).size(92.dp, 30.dp)) {
                val p = Path().apply {
                    moveTo(0f, size.height); lineTo(size.width, size.height)
                    lineTo(size.width / 2f, 0f); close()
                }
                drawPath(p, roof)
            }
        }

        // 煙囪（尖頂/帳篷才有）
        if (skinId == "classic" || skinId == "tent") {
            Box(
                modifier = Modifier.offset(70.dp, 8.dp).size(12.dp, 22.dp)
                    .background(shadeColor(color, -37))
            )
        }

        // 窗戶
        Box(
            modifier = Modifier
                .offset(18.dp, 56.dp).size(20.dp, 18.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (canWake) Color(0xFFFFE9A8) else Color.White.copy(alpha = 0.25f))
        )
        // 門（敲門時抖動）
        val doorOffset by animateFloatAsState(
            if (knocking) 2f else 0f,
            animationSpec = repeatable(6, tween(80)),
            label = "door"
        )
        Box(
            modifier = Modifier
                .offset((46 + doorOffset).dp, 78.dp).size(26.dp, 48.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(doorColor)
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .offset(x = (-4).dp).size(4.dp).clip(CircleShape)
                    .background(Color(0xFFFFC857))
            )
        }
        // 沉睡 Z
        if (!canWake) {
            Text("Z", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(66.dp, 0.dp))
            Text("z", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.offset(58.dp, 10.dp))
        }
    }
}

// 小型預覽（設定頁選擇用）
@Composable
fun SkinPreview(isCharacter: Boolean, skinId: String, color: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp, 80.dp)) {
        if (isCharacter) WalkerSkin(skinId, color, "down", false)
        else CottageSkin(skinId, color, canWake = true, knocking = false)
    }
}
