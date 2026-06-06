// 我的喚醒鈴聲設定：選擇來源（系統預設 / 從手機選音檔 / 錄製語音 ≤10s）
package com.wakey.app.ui.screen.profile

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_RECORD_MS = 10_000L
private const val MAX_PICK_FILE_BYTES = 700_000L  // Base64 後約 933KB，留安全空間

@Composable
fun WakeAudioSheet(
    currentPath: String?,
    onPicked: (String) -> Unit,
    onCleared: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = WColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioStore = remember { AudioStore(context.applicationContext) }

    var mode by remember { mutableStateOf<String?>(null) }   // null | "record" | "preview"
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // 檢查大小（粗略估計：太大就拒絕）
            val size = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.available().toLong() }
            }.getOrNull() ?: 0L
            if (size > MAX_PICK_FILE_BYTES) {
                statusMsg = "檔案太大（${size / 1024}KB），請選 ${MAX_PICK_FILE_BYTES / 1024}KB 以下的短音檔"
                return@launch
            }
            val path = audioStore.importFromUri(uri)
            if (path != null) {
                onPicked(path)
            } else {
                statusMsg = "無法讀取此檔案"
            }
        }
    }

    WakeyBottomSheet(onDismiss = onDismiss) {
        Text("我的喚醒鈴聲", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink,
            modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "好友叫醒你時，對方裝置會播這個音檔。\n沒設定就使用系統預設鬧鈴。",
            fontSize = 12.sp, color = c.inkSoft,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (mode == "record") {
            RecordPanel(
                onCancel = { mode = null },
                onDone = { path -> onPicked(path) }
            )
        } else {
            // 三個來源選項
            OptionRow(
                icon = WIcon.music, title = "從手機選音檔",
                desc = "AAC/MP3/M4A 等，≤約 700KB"
            ) { pickLauncher.launch("audio/*") }

            OptionRow(
                icon = WIcon.bell, title = "錄製語音",
                desc = "最長 10 秒"
            ) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) mode = "record"
                else statusMsg = "請先到系統設定授予錄音權限"
            }

            if (!currentPath.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.raisedSoft.copy(alpha = if (c.isDark) 0.06f else 0.04f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("目前已設定", modifier = Modifier.weight(1f),
                        fontSize = 13.sp, color = c.inkSoft)
                    PreviewButton(currentPath)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "移除", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA04040),
                        modifier = Modifier.clickable { onCleared() }
                    )
                }
            }

            statusMsg?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, fontSize = 12.sp, color = Color(0xFFA04040))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OptionRow(icon: String, title: String, desc: String, onClick: () -> Unit) {
    val c = WColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.raisedSoft.copy(alpha = if (c.isDark) 0.06f else 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(c.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { WakeyIcon(icon, size = 18.dp, tint = c.accentDeep) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text(desc, fontSize = 11.sp, color = c.inkSoft)
        }
        Text("›", fontSize = 20.sp, color = c.ink.copy(alpha = 0.4f))
    }
}

@Composable
private fun PreviewButton(path: String) {
    val c = WColors
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(path) {
        onDispose {
            player.value?.runCatching { stop(); release() }
            player.value = null
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f))
            .clickable {
                if (playing) {
                    player.value?.runCatching { stop(); release() }
                    player.value = null
                    playing = false
                } else {
                    runCatching {
                        player.value = MediaPlayer().apply {
                            setDataSource(path)
                            setOnCompletionListener {
                                runCatching { it.release() }
                                player.value = null
                                playing = false
                            }
                            prepare(); start()
                        }
                        playing = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        WakeyIcon(
            if (playing) WIcon.x else WIcon.bellRing,
            size = 16.dp, tint = c.accentDeep
        )
    }
}

@Composable
private fun RecordPanel(onCancel: () -> Unit, onDone: (String) -> Unit) {
    val c = WColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var recordedPath by remember { mutableStateOf<String?>(null) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    var previewing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.value?.runCatching { stop(); release() }
            recorder.value = null
            player.value?.runCatching { stop(); release() }
            player.value = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (recording) "錄製中…" else if (recordedPath != null) "預覽錄音" else "按下開始錄音",
            fontSize = 14.sp, color = c.inkSoft, modifier = Modifier.padding(bottom = 12.dp)
        )

        // 倒數
        Text(
            "%.1f / 10.0 秒".format(elapsed / 1000f),
            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = c.ink
        )

        Spacer(Modifier.height(20.dp))

        // 中央按鈕
        Box(
            modifier = Modifier
                .size(72.dp).clip(CircleShape)
                .background(if (recording) Color(0xFFD06A6A) else c.accent)
                .clickable {
                    if (recording) {
                        recorder.value?.runCatching { stop() }
                        recorder.value?.release()
                        recorder.value = null
                        recording = false
                    } else {
                        val path = startRecording(context, recorder)
                        if (path != null) {
                            recordedPath = path
                            elapsed = 0L
                            recording = true
                            scope.launch {
                                val tick = 100L
                                while (recording && elapsed < MAX_RECORD_MS) {
                                    delay(tick)
                                    elapsed += tick
                                }
                                if (recording) {
                                    // 到上限自動停止
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
            WakeyIcon(
                if (recording) WIcon.x else WIcon.bellRing,
                size = 28.dp, tint = Color.White
            )
        }

        Spacer(Modifier.height(24.dp))

        if (!recording && recordedPath != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 預覽
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
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(if (previewing) "停止" else "預覽", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = c.ink)
                }
                // 確定使用
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)).background(c.accent)
                        .clickable {
                            player.value?.runCatching { stop(); release() }
                            recordedPath?.let { onDone(it) }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text("使用這段錄音", fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "取消", fontSize = 13.sp, color = c.inkSoft,
                modifier = Modifier.clickable {
                    recorder.value?.runCatching { stop(); release() }
                    recorder.value = null
                    player.value?.runCatching { stop(); release() }
                    player.value = null
                    onCancel()
                }
            )
        }
    }
}

// 啟動 MediaRecorder 寫到 my_audio/wake_audio.m4a，回傳檔案路徑或 null
private fun startRecording(
    context: Context,
    recorder: MutableState<MediaRecorder?>
): String? {
    val target = File(context.filesDir, "my_audio").apply { mkdirs() }
        .let { File(it, "wake_audio.m4a") }
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
        rec.prepare()
        rec.start()
        recorder.value = rec
        target.absolutePath
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}
