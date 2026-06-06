// Firebase Cloud Messaging 服務：處理收到的推播（喚醒、共用鬧鐘）
package com.wakey.app.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wakey.app.R
import com.wakey.app.alarm.AlarmForegroundService
import com.wakey.app.alarm.AlarmReceiver
import com.wakey.app.data.local.AppDatabase
import com.wakey.app.data.local.entity.AlarmEntity
import com.wakey.app.data.repository.FriendRepository
import com.wakey.app.data.local.entity.PendingAlarmEntity
import com.wakey.app.data.repository.PendingAlarmRepository
import com.wakey.app.data.repository.UserProfileRepository
import com.wakey.app.data.repository.WakeAnalyticsRepository
import kotlinx.coroutines.runBlocking
import com.wakey.app.domain.model.RepeatMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WakeyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userProfileRepository: UserProfileRepository
    @Inject lateinit var friendRepository: FriendRepository
    @Inject lateinit var directory: FirestoreUserDirectory
    @Inject lateinit var audioStore: AudioStore
    @Inject lateinit var wakeAnalytics: WakeAnalyticsRepository
    @Inject lateinit var pendingAlarmRepository: PendingAlarmRepository

    companion object {
        const val CHANNEL_ID = "wakey_fcm_channel"
        const val TYPE_WAKE = "wake"          // 群組起床喚醒
        const val TYPE_SHARE_ALARM = "share_alarm" // 共用鬧鐘
        const val TYPE_GROUP_INVITE = "group_invite"
    }

    override fun onNewToken(token: String) {
        // 更新本機 + Firestore 上的 FCM token
        CoroutineScope(Dispatchers.IO).launch {
            userProfileRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            TYPE_WAKE -> handleWake(data)
            TYPE_SHARE_ALARM -> handleSharedAlarm(data)
            TYPE_GROUP_INVITE -> handleGroupInvite(data)
        }
    }

    // 群組邀請通知：彈出系統通知，點擊開 App（進通知頁）
    private fun handleGroupInvite(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "朋友"
        val groupName = data["groupName"] ?: "群組"
        createFcmChannel()
        val openIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("群組邀請")
            .setContentText("$senderName 邀請你加入「$groupName」")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$senderName 邀請你加入「$groupName」，到「我」頁查看"))
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    // 收到喚醒通知：直接觸發本機鬧鐘服務。
    // 鈴聲優先序：
    //   1. FCM 帶 wakeMessageId → 抓 wake_messages 文件取臨時錄音（用完刪 doc）
    //   2. 寄件人 profile 預設喚醒鈴聲（已透過 FriendSyncService 同步到本機）
    //   3. 系統預設鬧鈴
    // FCM 即時性需求走 runBlocking 同步取資料。
    private fun handleWake(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "朋友"
        val senderUid = data["senderUid"]
        // 紀錄「被誰喚醒」（統計用）
        if (!senderUid.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { wakeAnalytics.logIncoming(senderUid) }
            }
        }
        val wakeMessageId = data["wakeMessageId"]

        // 1. 臨時錄音
        var ringtonePath: String? = null
        if (!wakeMessageId.isNullOrBlank()) {
            runCatching {
                runBlocking {
                    val b64 = directory.fetchWakeMessage(wakeMessageId)
                    if (!b64.isNullOrBlank()) {
                        ringtonePath = audioStore.saveTransientWakeMessage(b64)
                    }
                    directory.deleteWakeMessage(wakeMessageId) // 用完即刪
                }
            }
        }

        // 2. 寄件人預設喚醒鈴聲
        if (ringtonePath == null && !senderUid.isNullOrBlank()) {
            ringtonePath = runCatching {
                runBlocking { friendRepository.getByUserId(senderUid) }
            }.getOrNull()?.wakeAudioPath?.takeIf { java.io.File(it).exists() }
        }

        val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, -2L) // 特殊 ID 標記為遠端喚醒
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "🌅 $senderName 叫你起床！")
            putExtra(AlarmReceiver.EXTRA_RINGTONE, ringtonePath ?: "default")
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // 收到共用鬧鐘：儲存至本機鬧鐘列表
    // 共用鬧鐘改成「邀請制」：先寫進 pending_alarms，使用者到通知頁同意才會轉成正式鬧鐘
    private fun handleSharedAlarm(data: Map<String, String>) {
        android.util.Log.d("WakeyFCM", "handleSharedAlarm data=$data")
        val hour = data["hour"]?.toIntOrNull()
        val minute = data["minute"]?.toIntOrNull()
        if (hour == null || minute == null) {
            android.util.Log.w("WakeyFCM", "missing hour/minute, abort")
            return
        }
        val label = data["label"] ?: ""
        val senderName = data["senderName"] ?: "朋友"
        val ringtone = data["ringtone"] ?: "default"
        val vibrate = data["vibrate"]?.toBooleanStrictOrNull() ?: true

        CoroutineScope(Dispatchers.IO).launch {
            val id = runCatching {
                pendingAlarmRepository.insert(
                    PendingAlarmEntity(
                        timeHour = hour, timeMinute = minute, label = label,
                        ringtone = ringtone, vibrate = vibrate,
                        senderName = senderName,
                        receivedMillis = System.currentTimeMillis()
                    )
                )
            }.onFailure { android.util.Log.e("WakeyFCM", "pendingAlarm insert failed", it) }
                .getOrNull()
            android.util.Log.d("WakeyFCM", "pending alarm inserted id=$id")
            showSharedAlarmNotification(senderName, hour, minute, label)
        }
    }

    private fun showSharedAlarmNotification(sender: String, hour: Int, minute: Int, label: String) {
        createFcmChannel()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = "$sender 想共用 %02d:%02d 鬧鐘給你%s".format(
            hour, minute, if (label.isNotBlank()) "（$label）" else ""
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("共用鬧鐘邀請")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$body，到「我」頁 → 通知查看與接受"))
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createFcmChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fcm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.fcm_channel_desc) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
