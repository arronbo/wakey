// 鈴聲試聽：以 MediaPlayer 播放選定鈴聲，提供播放/停止切換與目前播放中的 uri 狀態
package com.wakey.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wakey.app.alarm.SystemRingtones

class RingtonePreviewController(private val context: Context) {
    // 目前正在試聽的鈴聲 uri（null = 沒有在播）
    var playingUri by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    // 點同一首 → 停止；點不同首 → 切換播放
    fun toggle(uriStr: String) {
        if (playingUri == uriStr) { stop(); return }
        stop()
        val uri = SystemRingtones.resolveUri(uriStr) ?: return
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                setOnErrorListener { _, _, _ -> stop(); true }
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            playingUri = uriStr
        }.onFailure { stop() }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playingUri = null
    }
}

// 在 Composable 內取得試聽控制器；離開畫面（dispose）時自動停止避免殘音
@Composable
fun rememberRingtonePreview(): RingtonePreviewController {
    val ctx = LocalContext.current
    val controller = remember { RingtonePreviewController(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { controller.stop() } }
    return controller
}
