// 前景服務：鬧鐘觸發時播放鈴聲、震動，並開啟全螢幕關鬧鐘畫面 AlarmRingActivity
package com.wakey.app.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.wakey.app.R
import com.wakey.app.data.repository.WakeAnalyticsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var wakeAnalytics: WakeAnalyticsRepository

    companion object {
        const val ACTION_DISMISS = "com.wakey.app.ACTION_ALARM_DISMISS"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "wakey_alarm_channel"

        const val EXTRA_LABEL = AlarmReceiver.EXTRA_ALARM_LABEL

        fun dismissIntent(context: Context) =
            Intent(context, AlarmForegroundService::class.java).apply { action = ACTION_DISMISS }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var alarmStartMillis: Long = 0L
    private var alarmSource: String = "scheduled"   // "scheduled" 或 "remote"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL).orEmpty()
        val vibrate = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        val ringtone = intent?.getStringExtra(AlarmReceiver.EXTRA_RINGTONE)

        alarmStartMillis = System.currentTimeMillis()
        // alarmId == -2 為遠端喚醒（WakeyFirebaseMessagingService.handleWake 設定）
        alarmSource = if (alarmId == -2L) "remote" else "scheduled"

        startForeground(NOTIFICATION_ID, buildNotification(label))
        playRingtone(ringtone)
        if (vibrate) startVibration()
        launchRingScreen(label)

        return START_STICKY
    }

    // 使用使用者選的鈴聲 URI（無效則回退系統預設），全程 try-catch 避免崩潰
    private fun playRingtone(ringtone: String?) {
        val uri: Uri = SystemRingtones.resolveUri(ringtone)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmForegroundService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setOnErrorListener { _, _, _ -> true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 備援：用 RingtoneManager 直接播放
            runCatching {
                RingtoneManager.getRingtone(this, uri)?.play()
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 600)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    // 開啟全螢幕關鬧鐘畫面
    private fun launchRingScreen(label: String) {
        val i = Intent(this, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_LABEL, label)
        }
        runCatching { startActivity(i) }
    }

    private fun stopAlarm() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        vibrator?.cancel()
        // 寫鬧鐘關閉紀錄（給「連續早起」與「平均賴床」用）
        val start = alarmStartMillis
        val dismissed = System.currentTimeMillis()
        val source = alarmSource
        if (start > 0) {
            ioScope.launch {
                runCatching { wakeAnalytics.logAlarmDismissed(start, dismissed, source) }
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(label: String): Notification {
        // 全螢幕 Intent：螢幕關閉/鎖定時直接彈出關鬧鐘畫面
        val fullScreenIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_alarm_title))
            .setContentText(label.ifBlank { getString(R.string.notification_alarm_text) })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "關閉", dismissPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_channel_desc)
                setSound(null, null)          // 聲音由 MediaPlayer 控制
                enableVibration(false)
                setBypassDnd(true)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        runCatching { mediaPlayer?.release() }
        vibrator?.cancel()
        super.onDestroy()
    }
}
