// 全域 UI 等比例縮放：依手機螢幕寬度修改 Compose 的 density / fontScale，
// 讓所有 dp / sp 都跟著螢幕寬度等比例放大或縮小，確保不同寬度手機上視覺一致。
//
// 設計基準：380dp（中位數 Android 手機）
//   - 螢幕寬 380dp → scale = 1.0（原樣顯示）
//   - 窄手機（320-360dp）→ scale 0.84-0.95，所有元素等比縮小，不會擠成一團
//   - 寬手機（400dp+）→ scale 1.05-1.10，元素稍大但保持比例
//   - clamp [0.85, 1.10] 避免極端機型/平板上爆掉或縮得太小
package com.wakey.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val BASELINE_WIDTH_DP = 380f
private const val MIN_SCALE = 0.85f
private const val MAX_SCALE = 1.10f

@Composable
fun ResponsiveScale(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val original = LocalDensity.current

    val widthDp = configuration.screenWidthDp.toFloat()
    val scale = (widthDp / BASELINE_WIDTH_DP).coerceIn(MIN_SCALE, MAX_SCALE)

    val newDensity = Density(
        density = original.density * scale,
        fontScale = original.fontScale * scale
    )

    CompositionLocalProvider(LocalDensity provides newDensity) {
        content()
    }
}
