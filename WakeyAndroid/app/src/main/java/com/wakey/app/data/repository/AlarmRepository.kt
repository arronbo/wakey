// Repository：鬧鐘，橋接 DAO 與 AlarmManager，並與 Firestore 雙向同步
package com.wakey.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.wakey.app.alarm.AlarmScheduler
import com.wakey.app.data.local.dao.AlarmDao
import com.wakey.app.data.local.entity.AlarmEntity
import com.wakey.app.data.remote.FirestoreUserDirectory
import com.wakey.app.data.remote.RemoteAlarm
import com.wakey.app.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
    private val userProfileRepository: UserProfileRepository,
    private val directory: FirestoreUserDirectory,
    private val auth: FirebaseAuth
) {
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun save(alarm: Alarm): Long {
        val existingCloudId = if (alarm.id != 0L) dao.getById(alarm.id)?.cloudId else null
        val cloudId = existingCloudId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val entity = AlarmEntity.fromDomain(alarm).copy(cloudId = cloudId)
        val newId = dao.upsert(entity)
        val saved = entity.copy(id = newId)
        if (saved.enabled) scheduler.schedule(saved.toDomain())
        refreshNextAlarmStatus()
        pushAlarm(saved)
        return newId
    }

    suspend fun toggle(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        val entity = dao.getById(id) ?: return
        if (enabled) scheduler.schedule(entity.toDomain()) else scheduler.cancel(entity.toDomain())
        refreshNextAlarmStatus()
        pushAlarm(entity)
    }

    suspend fun delete(id: Long) {
        val entity = dao.getById(id)
        dao.deleteById(id)
        entity?.let { scheduler.cancel(it.toDomain()) }
        refreshNextAlarmStatus()
        val uid = auth.currentUser?.uid
        if (uid != null && entity != null && entity.cloudId.isNotBlank()) {
            runCatching { directory.deleteAlarm(uid, entity.cloudId) }
        }
    }

    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabled().map { it.toDomain() }

    // ── 雲端同步 ───────────────────────────────────────────────────────
    private suspend fun pushAlarm(e: AlarmEntity) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            directory.pushAlarm(
                uid,
                RemoteAlarm(
                    cloudId = e.cloudId, timeHour = e.timeHour, timeMinute = e.timeMinute,
                    label = e.label, repeatMode = e.repeatMode, customDays = e.customDays,
                    ringtone = e.ringtone, vibrate = e.vibrate, enabled = e.enabled,
                    sharedFrom = e.sharedFrom
                )
            )
        }
    }

    // 以雲端鬧鐘覆蓋本機：取消舊排程 → 清空 → 寫入 → 重排
    suspend fun applyRemote(remotes: List<RemoteAlarm>) {
        dao.getAll().forEach { runCatching { scheduler.cancel(it.toDomain()) } }
        dao.deleteAll()
        remotes.forEach { r ->
            val entity = AlarmEntity(
                cloudId = r.cloudId, timeHour = r.timeHour, timeMinute = r.timeMinute,
                label = r.label, repeatMode = r.repeatMode, customDays = r.customDays,
                ringtone = r.ringtone, vibrate = r.vibrate, enabled = r.enabled,
                sharedFrom = r.sharedFrom
            )
            val newId = dao.upsert(entity)
            val saved = entity.copy(id = newId).toDomain()
            if (saved.enabled) scheduler.schedule(saved)
        }
        refreshNextAlarmStatus()
    }

    // 登出時清本機（取消所有排程 + 清空）
    suspend fun clearLocal() {
        dao.getAll().forEach { runCatching { scheduler.cancel(it.toDomain()) } }
        dao.deleteAll()
    }

    // 首次登入時把本機鬧鐘上傳雲端
    suspend fun pushAllToCloud() {
        auth.currentUser?.uid ?: return
        dao.getAll().forEach { e ->
            val cloudId = e.cloudId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val withCloud = if (cloudId != e.cloudId) e.copy(cloudId = cloudId).also { dao.upsert(it) } else e
            pushAlarm(withCloud)
        }
    }

    // 計算所有啟用鬧鐘的「最近一次觸發時間」並更新本機 profile（會自動 push 至 Firestore）
    suspend fun refreshNextAlarmStatus() {
        val time = computeNextAlarmTime()
        val current = userProfileRepository.getOrCreate()
        if (current.nextAlarmTime != time) {
            userProfileRepository.save(current.copy(nextAlarmTime = time))
        }
    }

    private suspend fun computeNextAlarmTime(): String? {
        val enabled = dao.getEnabled().map { it.toDomain() }
        if (enabled.isEmpty()) return null
        val nextMillis = enabled.mapNotNull { scheduler.nextTriggerMillis(it) }
            .minOrNull() ?: return null
        val cal = Calendar.getInstance().apply { timeInMillis = nextMillis }
        return "%02d:%02d".format(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }
}
