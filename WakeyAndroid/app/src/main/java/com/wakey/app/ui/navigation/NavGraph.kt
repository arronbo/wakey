// Navigation：定義所有畫面路徑與底部導航列
package com.wakey.app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import com.wakey.app.ui.components.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.wakey.app.ui.screen.alarm.AlarmEditScreen
import com.wakey.app.ui.screen.alarm.AlarmScreen
import com.wakey.app.ui.screen.friend.FriendProfileScreen
import com.wakey.app.ui.screen.friend.FriendScreen
import com.wakey.app.ui.screen.friend.UserProfileScreen
import com.wakey.app.ui.screen.chat.ChatListScreen
import com.wakey.app.ui.screen.chat.ChatScreen
import com.wakey.app.domain.model.ChatType
import com.wakey.app.ui.screen.group.GroupDetailScreen
import com.wakey.app.ui.screen.group.GroupScreen
import com.wakey.app.ui.screen.home.HomeScreen
import com.wakey.app.ui.screen.profile.ProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.wakey.app.data.remote.PendingDeepLink
import com.wakey.app.ui.screen.notifications.NotificationsScreen
import com.wakey.app.ui.screen.settings.SettingsScreen
import com.wakey.app.viewmodel.DeepLinkViewModel
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Home : Screen("home", "主頁", WIcon.home)
    object Alarm : Screen("alarm", "鬧鐘", WIcon.alarmClock)
    object AlarmEdit : Screen("alarm_edit?id={id}", "編輯鬧鐘", "") {
        fun route(id: Long? = null) = if (id != null) "alarm_edit?id=$id" else "alarm_edit"
    }
    object Friends : Screen("friends", "好友", WIcon.users)
    object FriendProfile : Screen("friend/{friendId}", "好友資料", "") {
        fun route(id: Long) = "friend/$id"
    }
    object UserProfile : Screen("user/{uid}", "使用者", "") {
        fun route(uid: String) = "user/$uid"
    }
    object Groups : Screen("groups", "群組", WIcon.usersRound)
    object GroupDetail : Screen("group/{groupId}", "群組", "") {
        fun route(id: Long) = "group/$id"
    }
    object Chats : Screen("chats", "聊天", WIcon.messageCircle)
    object Chat : Screen("chat?type={type}&id={id}", "對話", "") {
        fun route(type: String, id: Long) = "chat?type=$type&id=$id"
    }
    object Profile : Screen("profile", "我", WIcon.circleUser)
    object Settings : Screen("settings", "設定", "")
    object Notifications : Screen("notifications", "通知", "")
}

val bottomNavItems =
    listOf(Screen.Home, Screen.Alarm, Screen.Friends, Screen.Groups, Screen.Chats, Screen.Profile)

// 把目前路由（含詳情頁）對應回所屬的底部分頁，讓詳情頁仍高亮母分頁
private fun activeTabRoute(route: String?): String? = when {
    route == null -> null
    route.startsWith("alarm") -> Screen.Alarm.route          // alarm / alarm_edit
    route == "friends" || route.startsWith("friend/") || route.startsWith("user/") -> Screen.Friends.route
    route == "groups" || route.startsWith("group/") -> Screen.Groups.route
    route.startsWith("chat") -> Screen.Chats.route           // chats / chat
    route == "profile" || route == "settings" || route == "notifications" -> Screen.Profile.route
    route == "home" -> Screen.Home.route
    else -> null
}

@Composable
fun WakeyNavGraph() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Deep link 處理：登入後監聽 pending，自動執行加好友 / 加群組
    val deepLinkVm: DeepLinkViewModel = hiltViewModel()
    val friendViewModel: FriendViewModel = hiltViewModel()
    val groupViewModel: GroupViewModel = hiltViewModel()
    val pending by deepLinkVm.manager.pending.collectAsState()
    LaunchedEffect(pending) {
        val link = pending ?: return@LaunchedEffect
        when (link) {
            is PendingDeepLink.AddFriend -> {
                friendViewModel.addFriendFromQr(link.uid)
                // 加好友會 navigate 到好友頁讓使用者看到結果
                runCatching {
                    navController.navigate(Screen.Friends.route) { launchSingleTop = true }
                }
            }
            is PendingDeepLink.JoinGroup -> {
                scope.launch {
                    runCatching { groupViewModel.joinByCloudId(link.cloudId) }
                    runCatching {
                        navController.navigate(Screen.Groups.route) { launchSingleTop = true }
                    }
                }
            }
            is PendingDeepLink.OpenChats -> {
                runCatching {
                    navController.navigate(Screen.Chats.route) { launchSingleTop = true }
                }
            }
            is PendingDeepLink.OpenNotifications -> {
                runCatching {
                    navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                }
            }
        }
        deepLinkVm.manager.consume()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 全螢幕 NavHost：漸層延伸到狀態列與導覽列後方
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            // 純淡入淡出，無方向、無縮放，維持 300ms 的節奏。
            enterTransition = { fadeIn(tween(300, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(300, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(300, easing = FastOutSlowInEasing)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }
            composable(Screen.Alarm.route) {
                AlarmScreen(
                    onAdd = { navController.navigate(Screen.AlarmEdit.route()) },
                    onEdit = { id -> navController.navigate(Screen.AlarmEdit.route(id)) }
                )
            }
            composable(
                Screen.AlarmEdit.route,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { back ->
                AlarmEditScreen(
                    alarmId = back.arguments?.getLong("id")?.takeIf { it != -1L },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Friends.route) {
                FriendScreen(
                    onFriendClick = { id -> navController.navigate(Screen.FriendProfile.route(id)) }
                )
            }
            composable(
                Screen.FriendProfile.route,
                arguments = listOf(navArgument("friendId") { type = NavType.LongType })
            ) { back ->
                val fid = back.arguments!!.getLong("friendId")
                FriendProfileScreen(
                    friendId = fid,
                    onBack = { navController.popBackStack() },
                    onMessage = { navController.navigate(Screen.Chat.route("direct", fid)) }
                )
            }
            composable(
                Screen.UserProfile.route,
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) {
                UserProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Groups.route) {
                GroupScreen(
                    onGroupClick = { id -> navController.navigate(Screen.GroupDetail.route(id)) }
                )
            }
            composable(
                Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { back ->
                val gid = back.arguments!!.getLong("groupId")
                GroupDetailScreen(
                    groupId = gid,
                    onBack = { navController.popBackStack() },
                    onMemberClick = { id -> navController.navigate(Screen.FriendProfile.route(id)) },
                    onUserClick = { uid -> navController.navigate(Screen.UserProfile.route(uid)) },
                    onOpenChat = { navController.navigate(Screen.Chat.route("group", gid)) }
                )
            }
            composable(Screen.Chats.route) {
                ChatListScreen(
                    onOpenChat = { type, localId ->
                        val t = if (type == ChatType.GROUP) "group" else "direct"
                        navController.navigate(Screen.Chat.route(t, localId))
                    }
                )
            }
            composable(
                Screen.Chat.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "direct" },
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { back ->
                val typeArg = back.arguments?.getString("type") ?: "direct"
                ChatScreen(
                    chatType = if (typeArg == "group") ChatType.GROUP else ChatType.DIRECT,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenNotifications = { navController.navigate(Screen.Notifications.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
        }

        // 底部浮動導覽列（避開系統導覽列）。對話頁（聊天輸入列釘在底部）不顯示，避免重疊。
        run {
            val c = WColors
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDest = navBackStackEntry?.destination
            val currentRoute = currentDest?.route
            val activeTab = activeTabRoute(currentRoute)
            val hideBottomNav = currentDest?.hierarchy?.any { it.route == Screen.Chat.route } == true
            if (!hideBottomNav) Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(c.raised.copy(alpha = if (c.isDark) 0.92f else 0.78f))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = activeTab == screen.route
                        val tint = if (selected) c.accent else c.inkSoft
                        Column(
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 圖示框（選中時珊瑚色底）
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) c.accent.copy(alpha = 0.18f) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                WakeyIcon(
                                    path = screen.icon,
                                    size = 20.dp,
                                    tint = tint,
                                    strokeWidth = if (selected) 2.4f else 1.9f
                                )
                            }
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tint
                            )
                        }
                    }
                }
            }
        }
    }
}
