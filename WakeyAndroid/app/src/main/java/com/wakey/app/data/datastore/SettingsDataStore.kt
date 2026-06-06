// DataStore：儲存輕量級使用者設定（時間制式、深色模式、預設鈴聲、震動）
package com.wakey.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.wakey.app.data.remote.FirestoreUserDirectory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wakey_settings")

// 主題模式：淺色／深色／跟隨系統／隨時間（白天淺、夜晚深）
enum class ThemeMode { LIGHT, DARK, SYSTEM, TIME }

data class AppSettings(
    val use24hFormat: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val defaultRingtone: String = "default",
    val defaultVibrate: Boolean = true,
    val earlyWakeTime: String? = null    // "HH:mm"，使用者設定的「早起時間」（功能待定）
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directory: FirestoreUserDirectory,
    private val auth: FirebaseAuth
) {
    private object Keys {
        val USE_24H = booleanPreferencesKey("use_24h")
        val DARK_MODE = booleanPreferencesKey("dark_mode")              // 舊設定，僅供遷移
        val FOLLOW_SYSTEM_DARK = booleanPreferencesKey("follow_system_dark") // 舊設定，僅供遷移
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_RINGTONE = stringPreferencesKey("default_ringtone")
        val DEFAULT_VIBRATE = booleanPreferencesKey("default_vibrate")
        val EARLY_WAKE_TIME = stringPreferencesKey("early_wake_time")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            // 優先讀新 key；若無則由舊 darkMode/followSystemDark 推斷（向後相容）
            val themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: when {
                    prefs[Keys.FOLLOW_SYSTEM_DARK] == true -> ThemeMode.SYSTEM
                    prefs[Keys.DARK_MODE] == true -> ThemeMode.DARK
                    else -> ThemeMode.LIGHT
                }
            AppSettings(
                use24hFormat = prefs[Keys.USE_24H] ?: true,
                themeMode = themeMode,
                defaultRingtone = prefs[Keys.DEFAULT_RINGTONE] ?: "default",
                defaultVibrate = prefs[Keys.DEFAULT_VIBRATE] ?: true,
                earlyWakeTime = prefs[Keys.EARLY_WAKE_TIME]
            )
        }

    suspend fun setUse24hFormat(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = value }
        pushSettings()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
        pushSettings()
    }

    suspend fun setDefaultRingtone(value: String) {
        context.dataStore.edit { it[Keys.DEFAULT_RINGTONE] = value }
        pushSettings()
    }

    suspend fun setDefaultVibrate(value: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_VIBRATE] = value }
        pushSettings()
    }

    suspend fun setEarlyWakeTime(value: String?) {
        context.dataStore.edit {
            if (value.isNullOrBlank()) it.remove(Keys.EARLY_WAKE_TIME)
            else it[Keys.EARLY_WAKE_TIME] = value
        }
        pushSettings()
    }

    // ── 雲端同步 ───────────────────────────────────────────────────────
    suspend fun pushSettings() {
        val uid = auth.currentUser?.uid ?: return
        val s = settings.first()
        runCatching {
            directory.pushSettings(
                uid,
                mapOf(
                    "themeMode" to s.themeMode.name,
                    "use24h" to s.use24hFormat,
                    "defaultRingtone" to s.defaultRingtone,
                    "defaultVibrate" to s.defaultVibrate,
                    "earlyWakeTime" to s.earlyWakeTime
                )
            )
        }
    }

    // 登出時清本機設定
    suspend fun clearLocal() {
        context.dataStore.edit { it.clear() }
    }

    // 以雲端設定覆蓋本機
    suspend fun applyRemote(remote: Map<String, Any?>) {
        context.dataStore.edit { prefs ->
            (remote["themeMode"] as? String)?.let { prefs[Keys.THEME_MODE] = it }
            (remote["use24h"] as? Boolean)?.let { prefs[Keys.USE_24H] = it }
            (remote["defaultRingtone"] as? String)?.let { prefs[Keys.DEFAULT_RINGTONE] = it }
            (remote["defaultVibrate"] as? Boolean)?.let { prefs[Keys.DEFAULT_VIBRATE] = it }
            (remote["earlyWakeTime"] as? String)?.let { prefs[Keys.EARLY_WAKE_TIME] = it }
        }
    }
}
