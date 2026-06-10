// 主頁：可操控 3D 小人在村莊中走動，走到「可被喚醒」好友家門口可敲門
// 對應 React 版 home-world.jsx（搖桿移動、碰撞、門口偵測、傳送門換群組）
package com.wakey.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.wakey.app.data.remote.FcmSender
import com.wakey.app.domain.model.Friend
import com.wakey.app.ui.components.*
import com.wakey.app.viewmodel.FriendViewModel
import com.wakey.app.viewmodel.GroupViewModel
import com.wakey.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

private const val WORLD_W = 402f
private const val WORLD_H = 720f
private const val PORTAL_X = 372f
private const val PORTAL_Y = 540f
private const val PORTAL_R = 38f
private val SLOTS = listOf(
    30f to 195f, 240f to 210f, 25f to 340f, 245f to 358f, 130f to 485f
)

private data class House(val friend: Friend, val x: Float, val y: Float) {
    val doorX get() = x + 50f
    val doorY get() = y + 130f
}

@Composable
fun HomeScreen(
    navController: NavController,
    friendViewModel: FriendViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel()
) {
    val friendState by friendViewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val groupState by groupViewModel.uiState.collectAsState()

    val userColor = profileState.profile?.color ?: "#FF8A6B"
    val use24h = profileState.settings.use24hFormat
    val characterSkin = profileState.settings.characterSkin
    val houseSkin = profileState.settings.houseSkin

    var groupMode by remember { mutableStateOf<Long?>(null) }
    val currentGroup = remember(groupState.groups, groupMode) {
        groupMode?.let { gm -> groupState.groups.firstOrNull { it.id == gm } }
    }
    val visibleFriends = remember(friendState.friends, currentGroup, groupMode) {
        if (groupMode == null) friendState.friends
        else currentGroup
            // 用雲端權威的 memberUids 對好友（memberIds 本機映射常為空，會導致房子不見）
            ?.memberUids?.mapNotNull { uid -> friendState.friends.firstOrNull { it.userId == uid } }
            ?: friendState.friends
    }
    val houses = remember(visibleFriends) {
        visibleFriends.take(SLOTS.size).mapIndexed { i, f ->
            House(f, SLOTS[i].first, SLOTS[i].second)
        }
    }

    // 角色位置/速度
    var posX by remember { mutableFloatStateOf(WORLD_W / 2f) }
    var posY by remember { mutableFloatStateOf(380f) }
    var vel by remember { mutableStateOf(Offset.Zero) }
    var facing by remember { mutableStateOf("down") }
    var nearbyId by remember { mutableStateOf<Long?>(null) }
    var knockingId by remember { mutableStateOf<Long?>(null) }
    var toastName by remember { mutableStateOf<String?>(null) }
    var toastOk by remember { mutableStateOf(true) }
    var portalActive by remember { mutableStateOf(false) }
    var showPortal by remember { mutableStateOf(false) }
    var portalCooldown by remember { mutableStateOf(false) }

    val housesState = rememberUpdatedState(houses)
    val showPortalState = rememberUpdatedState(showPortal)

    // 60fps 迴圈
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis { t ->
                if (last == 0L) last = t
                val dt = (t - last).coerceAtMost(50L).toFloat()
                last = t
                val v = if (showPortalState.value) Offset.Zero else vel
                val mag = hypot(v.x, v.y)
                if (mag > 0.001f) {
                    val speed = 0.24f
                    var nx = (posX + v.x * speed * dt).coerceIn(20f, WORLD_W - 20f)
                    var ny = (posY + v.y * speed * dt).coerceIn(40f, WORLD_H - 100f)
                    fun collides(x: Float, y: Float): Boolean =
                        housesState.value.any { h ->
                            x > h.x + 4 && x < h.x + 96 && y > h.y + 38 && y < h.y + 116
                        }
                    when {
                        !collides(nx, ny) -> { posX = nx; posY = ny }
                        !collides(posX, ny) -> { posY = ny }
                        !collides(nx, posY) -> { posX = nx }
                    }
                    facing = if (kotlin.math.abs(v.x) > kotlin.math.abs(v.y))
                        if (v.x > 0) "right" else "left"
                    else if (v.y > 0) "down" else "up"
                }
                // 門口偵測
                var best: Long? = null; var bestD = 70f
                housesState.value.forEach { h ->
                    val d = hypot(posX - h.doorX, posY - h.doorY)
                    if (d < bestD) { bestD = d; best = h.friend.id }
                }
                if (best != nearbyId) nearbyId = best
                // 傳送門偵測
                val pd = hypot(posX - PORTAL_X, posY - PORTAL_Y)
                portalActive = pd < PORTAL_R + 30
                if (pd < PORTAL_R && !portalCooldown && !showPortalState.value) {
                    portalCooldown = true; showPortal = true
                }
                if (pd > PORTAL_R + 20) portalCooldown = false
            }
        }
    }

    // 敲門流程：播放敲門動畫的同時，發送 FCM 喚醒推播給好友
    val scope = rememberCoroutineScope()
    fun knock(h: House) {
        if (!h.friend.canWake || knockingId != null) return
        knockingId = h.friend.id
        val myName = profileState.profile?.name ?: "朋友"
        scope.launch {
            val token = h.friend.fcmToken
            // 動畫與發送並行：門至少抖 1.3 秒，再依發送結果顯示 toast
            val sendDeferred = async {
                if (token != null) {
                    val r = FcmSender().sendWake(token, myName, profileState.profile?.uuid)
                    if (r) profileViewModel.logOutgoingWake(h.friend.userId)
                    r
                } else false
            }
            delay(1300)
            val ok = sendDeferred.await()
            knockingId = null
            toastName = h.friend.name
            toastOk = ok
            delay(1800)
            toastName = null
        }
    }

    val nearbyHouse = houses.firstOrNull { it.friend.id == nearbyId }
    val night = WColors.isDark

    // 世界天空/草地漸層色階（世界與背景共用，確保地平線位置一致）
    val skyStops = if (night) arrayOf(
        0.0f to Color(0xFF241B3D),
        0.30f to Color(0xFF34264E),
        0.30f to Color(0xFF24382A),
        1.0f to Color(0xFF1C2A22)
    ) else arrayOf(
        0.0f to Color(0xFFFFC4A8),
        0.30f to Color(0xFFF4A8C4),
        0.30f to Color(0xFFC7E1A8),
        1.0f to Color(0xFFA8D4A0)
    )
    val worldBrush = Brush.verticalGradient(colorStops = skyStops)

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 多覆蓋 1px，避免浮點縮放在右緣留下接縫、露出後方底色
            val scale = with(LocalDensity.current) { (maxWidth.toPx() + 1f) } /
                    with(LocalDensity.current) { WORLD_W.dp.toPx() }

            // 全螢幕背景漸層：用「世界縮放後的高度」做對應，
            // 讓地平線位置與世界完全一致 → 右緣/底部的接縫顏色與位置都吻合、看不出邊。
            val worldVisualHeightPx = with(LocalDensity.current) { WORLD_H.dp.toPx() } * scale
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(colorStops = skyStops, startY = 0f, endY = worldVisualHeightPx))
            )

            // ── 世界（依寬度縮放）─────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(WORLD_W.dp, WORLD_H.dp)
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .background(worldBrush)
            ) {
                // 太陽 / 月亮
                Box(
                    modifier = Modifier
                        .offset(x = (WORLD_W - 52).dp, y = 130.dp)
                        .size(30.dp).clip(CircleShape)
                        .background(if (night) Color(0xFFF3ECCB) else Color(0xFFFFC857))
                )
                // 星星（僅夜晚）
                if (night) {
                    listOf(60 to 90, 120 to 60, 200 to 100, 280 to 70, 330 to 120, 90 to 150)
                        .forEach { (sx, sy) ->
                            Box(
                                modifier = Modifier.offset(sx.dp, sy.dp).size(3.dp)
                                    .clip(CircleShape).background(Color.White.copy(alpha = 0.85f))
                            )
                        }
                }
                // 樹
                val trunkColor = if (night) Color(0xFF3A2820) else Color(0xFF7A4F3C)
                val leafColor = if (night) Color(0xFF355A32) else Color(0xFF6FB84F)
                listOf(40 to 250, 350 to 230, 40 to 430, 355 to 450, 40 to 620).forEach { (tx, ty) ->
                    Box(modifier = Modifier.offset(tx.dp, ty.dp).size(16.dp, 32.dp)) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .size(4.dp, 14.dp).background(trunkColor)
                        )
                        Box(
                            modifier = Modifier.align(Alignment.TopCenter)
                                .size(16.dp).clip(CircleShape).background(leafColor)
                        )
                    }
                }

                // 傳送門（底下顯示目前所在群組）
                PortalVisual(
                    active = portalActive,
                    scope = if (groupMode == null) "整個村莊" else (currentGroup?.name ?: "群組")
                )

                // 房子（依 y 排序，後面的蓋前面）
                houses.sortedBy { it.y }.forEach { h ->
                    Box(modifier = Modifier.offset(h.x.dp, h.y.dp)) {
                        CottageSkin(houseSkin, h.friend.color, h.friend.canWake, knockingId == h.friend.id)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-8).dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1A1025).copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                h.friend.name + if (h.friend.canWake) "" else " · 勿擾",
                                color = Color.White, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 門口虛線圈
                nearbyHouse?.let { h ->
                    if (knockingId == null) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .offset((h.doorX - 30).dp, (h.doorY - 30).dp)
                                .size(60.dp)
                        ) {
                            drawCircle(
                                color = if (h.friend.canWake) Color(0xFFFFC857)
                                else Color.White.copy(alpha = 0.5f),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }

                // 小人
                Box(
                    modifier = Modifier.offset((posX - 14).dp, (posY - 30).dp)
                ) {
                    WalkerSkin(characterSkin, userColor, facing, bobbing = knockingId != null)
                    if (knockingId != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-30).dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("扣扣扣！", color = WColors.ink, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 上方時鐘 ─────────────────────────────────────────────────
        var timeStr by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf("") }
        LaunchedEffect(use24h) {
            while (true) {
                val n = Date()
                timeStr = SimpleDateFormat(if (use24h) "HH:mm" else "hh:mm", Locale.TAIWAN).format(n)
                dateStr = SimpleDateFormat("MM月dd日 EEEE", Locale.TAIWAN).format(n)
                delay(10000)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A1025).copy(alpha = 0.45f))
                .padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WakeyIcon(WIcon.moon, size = 22.dp, tint = Color.White)
                Text(timeStr, color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold)
            }
            Text(dateStr, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }

        // ── 門口提示卡 ────────────────────────────────────────────────
        nearbyHouse?.let { h ->
            if (knockingId == null && toastName == null) {
                DoorPrompt(h.friend) { knock(h) }
            }
        }

        // ── 喚醒 toast ────────────────────────────────────────────────
        toastName?.let { name ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WakeyIcon(WIcon.bellRing, size = 20.dp,
                        tint = if (toastOk) WColors.accent else Color(0xFFA04040))
                    Column {
                        Text(
                            if (toastOk) "已喚醒 $name" else "喚醒 $name 失敗",
                            color = WColors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (toastOk) "叮叮叮～她起床了！" else "無法取得對方推播，請稍後再試",
                            color = WColors.inkSoft, fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ── 搖桿 ──────────────────────────────────────────────────────
        if (toastName == null && !showPortal) {
            Joystick(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp),
                onVelocity = { vel = it }
            )
        }
    }

    // ── 傳送門群組選擇 ────────────────────────────────────────────────
    if (showPortal) {
        PortalSheet(
            groups = groupState.groups,
            currentGroupId = groupMode,
            onPickAll = {
                groupMode = null; showPortal = false
                posX = WORLD_W / 2f; posY = 380f; vel = Offset.Zero; portalCooldown = true
            },
            onPick = { gid ->
                groupMode = gid; showPortal = false
                posX = WORLD_W / 2f; posY = 380f; vel = Offset.Zero; portalCooldown = true
            },
            onClose = {
                showPortal = false; vel = Offset.Zero
                posX = PORTAL_X - 80f; posY = PORTAL_Y; portalCooldown = true
            }
        )
    }
}

// ── 傳送門 ────────────────────────────────────────────────────────────
@Composable
private fun PortalVisual(active: Boolean, scope: String) {
    Box(
        modifier = Modifier
            .offset((PORTAL_X - 35).dp, (PORTAL_Y - 45).dp)
            .size(70.dp, 110.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp).clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFA7B7E8).copy(alpha = if (active) 0.95f else 0.75f),
                            Color(0xFFD8B5FF).copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1A1025).copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("傳送門", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            // 目前所在群組
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WColors.accent.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    scope, color = Color.White, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1
                )
            }
        }
    }
}

// ── 門口提示卡 ────────────────────────────────────────────────────────
@Composable
private fun BoxScope.DoorPrompt(friend: Friend, onKnock: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
            .widthIn(min = 240.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(friend.name, friend.color, 36.dp, ring = true, photoUri = friend.photoUri)
            Column(modifier = Modifier.weight(1f)) {
                Text("${friend.name} 的家", color = WColors.ink, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (friend.canWake) "燈還亮著，可以敲門" else "已熟睡，請勿打擾",
                    color = WColors.inkSoft, fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (friend.canWake) WColors.accent else WColors.ink.copy(alpha = 0.15f))
                    .then(
                        if (friend.canWake)
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { onKnock() }
                            } else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WakeyIcon(
                        WIcon.bellRing, size = 14.dp,
                        tint = if (friend.canWake) Color.White else WColors.inkSoft
                    )
                    Text("敲門", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (friend.canWake) Color.White else WColors.inkSoft)
                }
            }
        }
    }
}

// ── 搖桿 ──────────────────────────────────────────────────────────────
@Composable
private fun Joystick(modifier: Modifier = Modifier, onVelocity: (Offset) -> Unit) {
    val base = 116f
    val stick = 52f
    val maxR = (base - stick) / 2f
    var knob by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(base.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knob = Offset.Zero; onVelocity(Offset.Zero) },
                    onDragCancel = { knob = Offset.Zero; onVelocity(Offset.Zero) }
                ) { change, _ ->
                    change.consume()
                    val centerPx = with(density) { (base / 2f).dp.toPx() }
                    val dx = change.position.x - centerPx
                    val dy = change.position.y - centerPx
                    val maxPx = with(density) { maxR.dp.toPx() }
                    val d = hypot(dx, dy)
                    val cx = if (d > maxPx) dx / d * maxPx else dx
                    val cy = if (d > maxPx) dy / d * maxPx else dy
                    knob = with(density) { Offset(cx.toDp().toPx(), cy.toDp().toPx()) }
                    // 正規化 + 死區 + smoothstep
                    val nx = cx / maxPx
                    val ny = cy / maxPx
                    val m = hypot(nx, ny)
                    val dz = 0.16f
                    if (m < dz) onVelocity(Offset.Zero)
                    else {
                        val tt = ((m - dz) / (1 - dz)).coerceIn(0f, 1f)
                        val eased = tt * tt * (3 - 2 * tt)
                        onVelocity(Offset((nx / m) * eased, (ny / m) * eased))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(knob.x.toInt(), knob.y.toInt()) }
                .size(stick.dp).clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFFB199), Color(0xFFE66948)))
                )
        )
    }
}

// ── 傳送門群組選單 ────────────────────────────────────────────────────
@Composable
private fun PortalSheet(
    groups: List<com.wakey.app.domain.model.Group>,
    currentGroupId: Long?,
    onPickAll: () -> Unit,
    onPick: (Long) -> Unit,
    onClose: () -> Unit
) {
    WakeyBottomSheet(onDismiss = onClose) {
        Text("傳送到群組", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
        Text("選擇群組，地圖上的房子會變成那個群組成員的家",
            fontSize = 12.sp, color = WColors.inkSoft, modifier = Modifier.padding(vertical = 8.dp))

        SheetRow(
            selected = currentGroupId == null, onClick = onPickAll
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WColors.accent, Color(0xFFFFC857)))),
                contentAlignment = Alignment.Center
            ) { WakeyIcon(WIcon.users, size = 18.dp, tint = Color.White) }
            Column(modifier = Modifier.weight(1f)) {
                Text("整個村莊", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                Text("顯示全部好友", fontSize = 11.sp, color = WColors.inkSoft)
            }
            if (currentGroupId == null) WakeyIcon(WIcon.check, size = 16.dp, tint = WColors.accent)
        }

        groups.forEach { g ->
            val sel = currentGroupId == g.id
            SheetRow(selected = sel, onClick = { onPick(g.id) }) {
                GroupAvatar(
                    colors = listOf(g.color ?: "#FF8A6B"), size = 42.dp,
                    monogram = g.name.firstOrNull()?.toString(), photoUri = g.photoUri
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(g.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.ink)
                    Text("${g.memberUids.size} 位成員 · 「${g.message}」",
                        fontSize = 11.sp, color = WColors.inkSoft, maxLines = 1)
                }
                if (sel) WakeyIcon(WIcon.check, size = 16.dp, tint = WColors.accent)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SheetRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WColors.accent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.03f))
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
