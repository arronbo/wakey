// BroadcastReceiver：裝置重開機後，重新排程所有已啟用的鬧鐘
package com.wakey.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakey.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val db = AppDatabase.getInstance(context)
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            db.alarmDao().getEnabled().forEach { entity ->
                scheduler.schedule(entity.toDomain())
            }
        }
    }
}
