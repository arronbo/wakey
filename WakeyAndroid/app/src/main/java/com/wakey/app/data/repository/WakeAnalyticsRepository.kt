// Repository：早起／喚醒事件統計
// 寫入：鬧鐘關閉（WakeRecord）、FCM 喚醒（WakeEvent）
// 讀取：原始 Flow + 統計計算函式（純函式，由 ViewModel 隨流呼叫）
package com.wakey.app.data.repository

import com.wakey.app.data.datastore.SettingsDataStore
import com.wakey.app.data.local.dao.WakeEventDao
import com.wakey.app.data.local.dao.WakeRecordDao
import com.wakey.app.data.local.entity.WakeEventEntity
import com.wakey.app.data.local.entity.WakeRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WakeAnalyticsRepository @Inject constructor(
    private val recordDao: WakeRecordDao,
    private val eventDao: WakeEventDao,
    private val settingsDataStore: SettingsDataStore
) {
    val records: Flow<List<WakeRecordEntity>> = recordDao.observeAll()
    val events: Flow<List<WakeEventEntity>> = eventDao.observeAll()

    // ── 寫入 ────────────────────────────────────────────────────────
    /** alarmStart 鬧鐘響起時間；source: "scheduled" 或 "remote" */
    suspend fun logAlarmDismissed(alarmStart: Long, dismissed: Long, source: String) {
        val earlyWakeTime = runCatching {
            settingsDataStore.settings.first().earlyWakeTime
        }.getOrNull()
        val wasEarly = isWithinEarlyWindow(dismissed, earlyWakeTime)
        recordDao.insert(
            WakeRecordEntity(
                alarmStartMillis = alarmStart,
                dismissedMillis = dismissed,
                snoozeMs = (dismissed - alarmStart).coerceAtLeast(0),
                source = source,
                wasEarly = wasEarly
            )
        )
    }

    suspend fun logOutgoing(peerUid: String) {
        if (peerUid.isBlank()) return
        eventDao.insert(
            WakeEventEntity(
                timestampMillis = System.currentTimeMillis(),
                direction = "out", peerUid = peerUid
            )
        )
    }

    suspend fun logIncoming(peerUid: String) {
        if (peerUid.isBlank()) return
        eventDao.insert(
            WakeEventEntity(
                timestampMillis = System.currentTimeMillis(),
                direction = "in", peerUid = peerUid
            )
        )
    }

    suspend fun clearLocal() {
        recordDao.deleteAll()
        eventDao.deleteAll()
    }

    // ── 統計（純函式） ───────────────────────────────────────────────
    /** 連續早起天數：從今天往回算，每一天「至少一次 wasEarly」即算成功 */
    fun consecutiveEarlyDays(records: List<WakeRecordEntity>): Int {
        if (records.isEmpty()) return 0
        val daysEarly = records
            .groupBy { dayKey(it.dismissedMillis) }
            .mapValues { (_, recs) -> recs.any { it.wasEarly } }

        var count = 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (daysEarly[dayKey(cal.timeInMillis)] == true) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    /** 最近 N 天每天的「最早一次起床時間」 */
    fun recentWakeups(records: List<WakeRecordEntity>, days: Int = 7): List<DailyWake> {
        val byDay = records.groupBy { dayKey(it.dismissedMillis) }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val result = mutableListOf<DailyWake>()
        for (i in 0 until days) {
            val key = dayKey(today.timeInMillis)
            val first = byDay[key]?.minByOrNull { it.dismissedMillis }
            result += DailyWake(key, first?.dismissedMillis, first?.wasEarly == true)
            today.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    /** 平均賴床時長（毫秒），無資料回傳 0 */
    fun averageSnoozeMs(records: List<WakeRecordEntity>): Long {
        if (records.isEmpty()) return 0
        val valid = records.filter { it.snoozeMs in 1..(60 * 60 * 1000L) }
        if (valid.isEmpty()) return 0
        return valid.sumOf { it.snoozeMs } / valid.size
    }

    /** 出現次數最多的對方 uid（取前 N 個）  */
    fun topPeers(events: List<WakeEventEntity>, direction: String, n: Int = 3): List<Pair<String, Int>> =
        events.asSequence()
            .filter { it.direction == direction && it.peerUid.isNotBlank() }
            .groupingBy { it.peerUid }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(n)

    // ── 私用 ────────────────────────────────────────────────────────
    private fun dayKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun isWithinEarlyWindow(dismissedMs: Long, earlyWakeTime: String?): Boolean {
        if (earlyWakeTime.isNullOrBlank()) return false
        val parts = earlyWakeTime.split(":")
        if (parts.size != 2) return false
        val targetH = parts[0].toIntOrNull() ?: return false
        val targetM = parts[1].toIntOrNull() ?: return false
        val target = Calendar.getInstance().apply {
            timeInMillis = dismissedMs
            set(Calendar.HOUR_OF_DAY, targetH)
            set(Calendar.MINUTE, targetM)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return abs(dismissedMs - target.timeInMillis) <= 2 * 60 * 1000L
    }
}

data class DailyWake(
    val dayKey: String,           // "YYYY-MM-DD"
    val wakeMillis: Long?,        // null = 那天沒紀錄
    val wasEarly: Boolean
)
