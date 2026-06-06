// 主 Activity：設定 Compose 入口，套用主題與 Navigation
package com.wakey.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.remote.PendingDeepLink
import com.wakey.app.data.remote.PendingDeepLinkManager
import com.wakey.app.ui.navigation.WakeyNavGraph
import com.wakey.app.ui.screen.auth.LoginScreen
import com.wakey.app.ui.theme.WakeyTheme
import com.wakey.app.viewmodel.AuthViewModel
import com.wakey.app.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pendingDeepLinkManager: PendingDeepLinkManager

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 使用者已回應，不做強制要求 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ 需要動態申請通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Android 12+ 需手動授權「鬧鐘與提醒」(SCHEDULE_EXACT_ALARM)
        // 未授權時鬧鐘只能降級成不精確觸發，故引導使用者到系統設定開啟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:$packageName"))
                    )
                }
            }
        }

        setContent {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val profileState by profileViewModel.uiState.collectAsState()

            val settings = profileState.settings
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()

            // 隨時間模式：每分鐘更新目前小時，白天(6:00–18:00)淺色、其餘深色
            var nowHour by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableIntStateOf(currentHour())
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                while (true) {
                    nowHour = currentHour()
                    kotlinx.coroutines.delay(60_000)
                }
            }

            val darkTheme = when (settings.themeMode) {
                com.wakey.app.data.datastore.ThemeMode.LIGHT -> false
                com.wakey.app.data.datastore.ThemeMode.DARK -> true
                com.wakey.app.data.datastore.ThemeMode.SYSTEM -> systemDark
                com.wakey.app.data.datastore.ThemeMode.TIME -> nowHour < 6 || nowHour >= 18
            }

            val authViewModel: AuthViewModel = hiltViewModel()
            val user by authViewModel.authState.collectAsState()

            WakeyTheme(darkTheme = darkTheme) {
                com.wakey.app.ui.components.ResponsiveScale {
                    if (user == null) {
                        LoginScreen(viewModel = authViewModel)
                    } else {
                        WakeyNavGraph()
                    }
                }
            }
        }

        // 處理啟動時帶進來的 deep link
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    // 解析 wakey://add-friend?uid=xxx / wakey://join-group?cloudId=xxx 寫入 manager
    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "wakey") return
        when (data.host) {
            "add-friend" -> data.getQueryParameter("uid")?.takeIf { it.isNotBlank() }?.let {
                pendingDeepLinkManager.set(PendingDeepLink.AddFriend(it))
            }
            "join-group" -> data.getQueryParameter("cloudId")?.takeIf { it.isNotBlank() }?.let {
                pendingDeepLinkManager.set(PendingDeepLink.JoinGroup(it))
            }
        }
    }

    private fun currentHour(): Int =
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
}
