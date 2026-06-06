// 無限循環滾輪：用大量虛擬索引取模對應真實值，可往兩個方向無限滾動
package com.wakey.app.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ITEM_HEIGHT = 52.dp
private const val VIRTUAL_COUNT = 100_000  // 虛擬項目總數（足夠長等同無限）

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { ITEM_HEIGHT.toPx() }
    val n = items.size
    val inkColor = WColors.ink

    // 起始虛擬索引：取一個 n 的整數倍中段，再加上要選的值
    // 這樣 (虛擬索引 % n) == selectedIndex，且上下都有大量空間可滾
    val startIndex = remember(n) {
        val mid = (VIRTUAL_COUNT / 2)
        mid - (mid % n) + selectedIndex
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    // 即時計算正中央對應的真實索引（供文字高亮）
    val centeredReal by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val virtual = first + if (offset > itemHeightPx / 2f) 1 else 0
            ((virtual % n) + n) % n
        }
    }

    // 外部值變動且未滑動時，捲到對應位置（保持在中段附近）
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) {
            val cur = ((listState.firstVisibleItemIndex % n) + n) % n
            if (cur != selectedIndex) {
                val base = listState.firstVisibleItemIndex
                listState.scrollToItem(base - cur + selectedIndex)
            }
        }
    }

    // 捲動停止才回報，避免迴圈
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centeredReal != selectedIndex) {
            onIndexSelected(centeredReal)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * 2),
        modifier = modifier
            .width(96.dp)
            .height(ITEM_HEIGHT * 5)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.28f to Color.Black,
                        0.72f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        items(VIRTUAL_COUNT) { virtualIndex ->
            val real = ((virtualIndex % n) + n) % n
            Box(
                modifier = Modifier
                    .height(ITEM_HEIGHT)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[real],
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (real == centeredReal) inkColor else inkColor.copy(alpha = 0.35f)
                )
            }
        }
    }
}
