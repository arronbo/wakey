// BroadcastReceiver：接收 AlarmManager 觸發事件，啟動前景服務，
// 並針對重複鬧鐘自動排程下一次
package com.wakey.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakey.app.data.local.AppDatabase
import com.wakey.app.domain.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.wakey.app.ACTION_ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_RINGTONE = "ringtone"
        const val EXTRA_VIBRATE = "vibrate"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)

        // 1) 啟動前景服務響鈴
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "")
            putExtra(EXTRA_RINGTONE, intent.getStringExtra(EXTRA_RINGTONE) ?: "default")
            putExtra(EXTRA_VIBRATE, intent.getBooleanExtra(EXTRA_VIBRATE, true))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2) 重複鬧鐘 → 排程下一次；單次鬧鐘 → 自動關閉
        if (alarmId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).alarmDao()
                val entity = dao.getById(alarmId) ?: return@launch
                val alarm = entity.toDomain()
                val scheduler = AlarmScheduler(context)
                if (alarm.repeatMode == RepeatMode.ONCE) {
                    dao.setEnabled(alarmId, false)   // 單次響完即停用
                } else {
                    scheduler.schedule(alarm)        // 重複 → 排下一次觸發
                }
            } finally {
                pending.finish()
            }
        }
    }
}
