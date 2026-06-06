// 錄音叫醒朋友：BottomSheet 內錄 ≤10 秒語音，送出時上傳 Firestore 並透過 FCM 喚醒
package com.wakey.app.ui.screen.friend

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wakey.app.data.remote.AudioStore
import com.wakey.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_RECORD_MS = 10_000L

/**
 * @param onSend 錄好後呼叫，傳 Base64 字串給呼叫端去做上傳 + FCM
 */
@Composable
fun WakeRecordSheet(
    friendName: String,
    sending: Boolean,
    onSend: (audioBase64: String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = WColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var recordedPath by remember { mutableStateOf<String?>(null) }
    var previewing by remember { mutableStateOf(false) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    val requestPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒絕就維持 idle，不顯示錯誤 */ }

    DisposableEffect(Unit) {
        onDispose {
            recorder.value?.runCatching { stop(); release() }
            recorder.value = null
            player.value?.runCatching { stop(); release() }
            player.value = null
        }
    }

    WakeyBottomSheet(onDismiss = onDismiss) {
        Text("錄音叫醒 $friendName", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = c.ink,
            modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "錄一段最長 10 秒的語音，對方鬧鐘會播這段聲音。",
            fontSize = 12.sp, color = c.inkSoft,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when {
                    sending -> "傳送中…"
                    recording -> "錄製中…"
                    recordedPath != null -> "預覽錄音"
                    else -> "按下開始錄音"
                },
                fontSize = 14.sp, color = c.inkSoft,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                "%.1f / 10.0 秒".format(elapsed / 1000f),
                fontSize = 32.sp, fontWeight = FontWeight.Bold, color = c.ink
            )

            Spacer(Modifier.height(20.dp))

            // 中央大按鈕：錄音 / 停止
            Box(
                modifier = Modifier
                    .size(72.dp).clip(CircleShape)
                    .background(if (recording) Color(0xFFD06A6A) else c.accent)
                    .clickable(enabled = !sending) {
                        if (recording) {
                            recorder.value?.runCatching { stop() }
                            recorder.value?.release()
                            recorder.value = null
                            recording = false
                        } else {
                            // 檢查麥克風權限
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                requestPerm.launch(Manifest.permission.RECORD_AUDIO)
                                return@clickable
                            }
                            val path = startRecording(context, recorder)
                            if (path != null) {
                                recordedPath = path
                                elapsed = 0L
                                recording = true
                                scope.launch {
                                    val tick = 100L
                                    while (recording && elapsed < MAX_RECORD_MS) {
                                        delay(tick); elapsed += tick
                                    }
                                    if (recording) {
                                        recorder.value?.runCatching { stop() }
                                        recorder.value?.release()
                                        recorder.value = null
                                        recording = false
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(28.dp))
                } else {
                    WakeyIcon(
                        if (recording) WIcon.x else WIcon.bellRing,
                        size = 28.dp, tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (!recording && recordedPath != null && !sending) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 預覽 / 停止預覽
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.raisedSoft.copy(alpha = if (c.isDark) 0.10f else 0.08f))
                            .clickable {
                                if (previewing) {
                                    player.value?.runCatching { stop(); release() }
                                    player.value = null
                                    previewing = false
                                } else {
                                    runCatching {
                                        player.value = MediaPlayer().apply {
                                            setDataSource(recordedPath)
                                            setOnCompletionListener {
                                                runCatching { it.release() }
                                                player.value = null
                                                previewing = false
                                            }
                                            prepare(); start()
                                        }
                                        previewing = true
                                    }
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(if (previewing) "停止" else "預覽",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                    }

                    // 送出
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp)).background(c.accent)
                            .clickable {
                                player.value?.runCatching { stop(); release() }
                                player.value = null
                                scope.launch {
                                    val b64 = withContext(Dispatchers.IO) {
                                        encodeToBase64(recordedPath!!)
                                    }
                                    if (b64 != null) onSend(b64)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text("送出叫醒", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "取消", fontSize = 13.sp, color = c.inkSoft,
                modifier = Modifier.clickable(enabled = !sending) { onDismiss() }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// 把音檔讀進來編碼成 Base64；過大就放棄
private fun encodeToBase64(path: String): String? = try {
    val f = File(path)
    if (!f.exists() || f.length() == 0L) null
    else {
        val bytes = f.readBytes()
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        if (b64.length > 700_000) null else b64
    }
} catch (e: Exception) {
    e.printStackTrace(); null
}

private fun startRecording(
    context: Context,
    recorder: MutableState<MediaRecorder?>
): String? {
    val cache = File(context.cacheDir, "wake_msgs_out").apply { mkdirs() }
    val target = File(cache, "out_${System.currentTimeMillis()}.m4a")
    return try {
        val rec = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(64_000)
        rec.setAudioSamplingRate(44_100)
        rec.setMaxDuration(MAX_RECORD_MS.toInt())
        rec.setOutputFile(target.absolutePath)
        rec.prepare(); rec.start()
        recorder.value = rec
        target.absolutePath
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}
