// 登入畫面：品牌 + 使用 Google 登入
package com.wakey.app.ui.screen.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val c = WColors
    val ui by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onSignInResult(result.data)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 品牌標誌
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(c.accent),
                contentAlignment = Alignment.Center
            ) {
                WakeyIcon(WIcon.moon, size = 48.dp, tint = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text("夢遊 Wakey", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(
                "一起好好睡覺、好好起床",
                fontSize = 14.sp, color = c.inkSoft,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(56.dp))

            // Google 登入按鈕
            GlassCard(
                strong = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = if (ui.loading) null else {
                    { launcher.launch(viewModel.signInIntent()) }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.accent)
                    } else {
                        GoogleGlyph()
                        Spacer(Modifier.width(12.dp))
                        Text("使用 Google 登入", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = c.ink)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 訪客模式（次要按鈕）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !ui.loading) { viewModel.signInAsGuest() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "以訪客身分繼續",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.inkSoft
                )
            }

            ui.error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(
                    msg, fontSize = 13.sp, color = Color(0xFFD06A6A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "登入後你的鬧鐘、好友與設定會綁定帳號，換手機也能同步取回。\n" +
                        "訪客可隨時在「我」頁綁定 Google 帳號。",
                fontSize = 12.sp, color = c.inkSoft, textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// 簡化版 Google 四色 G 標誌
@Composable
private fun GoogleGlyph() {
    Box(
        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
    }
}
