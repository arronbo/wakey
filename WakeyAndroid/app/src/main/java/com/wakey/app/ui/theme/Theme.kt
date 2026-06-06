// App 主題：支援淺色/深色模式，採用 Material 3 + 自訂 WakeyColors 語義色彩層
package com.wakey.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.wakey.app.ui.components.LocalWakeyColors
import com.wakey.app.ui.components.darkWakeyColors
import com.wakey.app.ui.components.lightWakeyColors

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = DeepPlum,
    tertiary = Sunny,
    background = Peach,
    surface = Color.White,
    onBackground = DeepPlum,
    onSurface = DeepPlum,
    error = Color(0xFFE53935)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BDE),          // 柔和紫，與 WakeyColors.accent 一致
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = DeepPlum,
    tertiary = Color(0xFFC3B0EE),
    background = Color(0xFF14101F),
    surface = Color(0xFF2C2142),
    onBackground = Color(0xFFECE6F4),
    onSurface = Color(0xFFECE6F4),
    error = Color(0xFFE57373)
)

@Composable
fun WakeyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val wakeyColors = if (darkTheme) darkWakeyColors() else lightWakeyColors()

    CompositionLocalProvider(LocalWakeyColors provides wakeyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WakeyTypography,
            content = content
        )
    }
}
