// 全螢幕關鬧鐘畫面：風格與鬧鐘頁一致（漸層背景 + 玻璃卡）
// 可在鎖定畫面顯示並點亮螢幕；按「關閉鬧鐘」後停止響鈴並回到 App（不跳出）
package com.wakey.app.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakey.app.MainActivity
import com.wakey.app.ui.components.AppGradient
import com.wakey.app.ui.components.Coral
import com.wakey.app.ui.components.DeepPlum
import com.wakey.app.ui.components.GlassCard
import com.wakey.app.ui.components.InkSoft
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 鎖定畫面也顯示 + 點亮螢幕
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val label = intent.getStringExtra(AlarmForegroundService.EXTRA_LABEL).orEmpty()

        setContent {
            RingScreen(
                label = label,
                onDismiss = { dismissAndReturnToApp() }
            )
        }
    }

    // 防止使用者用返回鍵略過（必須按關閉鬧鐘）
    override fun onBackPressed() { /* 不處理 */ }

    private fun dismissAndReturnToApp() {
        // 停止響鈴服務
        startService(AlarmForegroundService.dismissIntent(this))
        // 回到 App 主畫面（而非跳出 App）
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
        finish()
    }
}

@Composable
private fun RingScreen(label: String, onDismiss: () -> Unit) {
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            timeStr = SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 鈴鐺圖示
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Coral.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⏰", fontSize = 48.sp)
            }

            Spacer(Modifier.height(28.dp))

            GlassCard(strong = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPlum
                    )
                    Text(
                        text = label.ifBlank { "鬧鐘時間到了" },
                        fontSize = 16.sp,
                        color = InkSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // 關閉鬧鐘按鈕
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Coral)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "關閉鬧鐘",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
