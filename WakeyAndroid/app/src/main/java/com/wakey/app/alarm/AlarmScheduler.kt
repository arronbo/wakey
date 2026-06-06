// AlarmScheduler：封裝 AlarmManager 的設定與取消，支援 Android 12+ 精確鬧鐘
package com.wakey.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakey.app.domain.model.Alarm
import com.wakey.app.domain.model.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) return
        val triggerTime = nextTriggerMillis(alarm) ?: return
        val intent = buildIntent(alarm)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

        if (canExact) {
            // 精確鬧鐘（最可靠，會在鎖定畫面顯示鬧鐘圖示）
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, buildOpenPendingIntent()),
                intent
            )
        } else {
            // 沒有精確權限時降級：仍會在 Doze 模式下觸發（可能延遲幾分鐘）
            // 同時應引導使用者到設定開啟「鬧鐘與提醒」權限（見 MainActivity）
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intent
            )
        }
    }

    // 是否可排程精確鬧鐘（供 UI 判斷是否要請使用者授權）
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun cancel(alarm: Alarm) {
        alarmManager.cancel(buildIntent(alarm))
    }

    // 計算下次觸發時間（毫秒），考慮重複模式
    fun nextTriggerMillis(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.timeHour)
            set(Calendar.MINUTE, alarm.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 若今天時間已過，移到明天
        if (candidate <= now) candidate.add(Calendar.DAY_OF_YEAR, 1)

        return when (alarm.repeatMode) {
            RepeatMode.ONCE -> candidate.timeInMillis

            RepeatMode.DAILY -> candidate.timeInMillis

            RepeatMode.WEEKDAYS -> {
                // 找下一個週一到週五
                while (candidate.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }

            RepeatMode.CUSTOM -> {
                if (alarm.customDays.isEmpty()) return null
                // 找下一個符合自訂星期的日子
                for (i in 0..6) {
                    val dow = candidate.get(Calendar.DAY_OF_WEEK)
                    if (dow in alarm.customDays) break
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }
        }
    }

    private fun buildIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_RINGTONE, alarm.ringtone)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildOpenPendingIntent(): PendingIntent {
        val intent = Intent(context, Class.forName("com.wakey.app.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
