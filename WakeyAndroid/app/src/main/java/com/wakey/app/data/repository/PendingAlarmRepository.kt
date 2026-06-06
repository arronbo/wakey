// Repository：待確認共用鬧鐘
// 寫入點：WakeyFirebaseMessagingService.handleSharedAlarm
// 接受 → 用 AlarmRepository.save 寫成正式 alarm（會自動同步上雲），刪 pending
// 拒絕 → 直接刪 pending
package com.wakey.app.data.repository

import com.wakey.app.data.local.dao.PendingAlarmDao
import com.wakey.app.data.local.entity.PendingAlarmEntity
import com.wakey.app.domain.model.Alarm
import com.wakey.app.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingAlarmRepository @Inject constructor(
    private val dao: PendingAlarmDao,
    private val alarmRepository: AlarmRepository
) {
    val pending: Flow<List<PendingAlarmEntity>> = dao.observeAll()

    suspend fun insert(p: PendingAlarmEntity): Long = dao.insert(p)

    suspend fun accept(id: Long) {
        val p = dao.getById(id) ?: return
        val alarm = Alarm(
            timeHour = p.timeHour,
            timeMinute = p.timeMinute,
            label = p.label,
            repeatMode = RepeatMode.ONCE,
            ringtone = p.ringtone,
            vibrate = p.vibrate,
            enabled = true,
            sharedFrom = p.senderName    // 標籤：是誰共用過來的
        )
        alarmRepository.save(alarm)
        dao.deleteById(id)
    }

    suspend fun reject(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearLocal() = dao.deleteAll()
}
