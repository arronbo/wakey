// FCM 推播發送器：使用 FCM HTTP v1 API + Service Account JWT 認證
// V1 API 需要 OAuth2 Access Token，透過 Service Account 私鑰簽署 JWT 取得
package com.wakey.app.data.remote

import android.util.Base64
import com.wakey.app.domain.model.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmSender @Inject constructor() {

    // ── Firebase Service Account 金鑰 ──────────────────────────────────
    // 機密值集中放在 FcmSecrets.kt（已被 .gitignore 排除，不進版控）。
    // 新環境請複製 FcmSecrets.kt.template 成 FcmSecrets.kt 並填入實際值。
    private val projectId = FcmSecrets.PROJECT_ID
    private val serviceAccountEmail = FcmSecrets.SERVICE_ACCOUNT_EMAIL
    private val privateKeyPem = FcmSecrets.PRIVATE_KEY_PEM.trimIndent()

    // ─────────────────────────────────────────────────────────────────────

    private val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
    private val tokenUrl = "https://oauth2.googleapis.com/token"
    private val scope = "https://www.googleapis.com/auth/firebase.messaging"

    // 快取 Access Token，避免每次發送都重新取得
    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0L

    suspend fun sendWake(
        targetToken: String,
        senderName: String,
        senderUid: String? = null,
        wakeMessageId: String? = null
    ): Boolean = sendMessage(targetToken, JSONObject().apply {
        put("type", "wake")
        put("senderName", senderName)
        if (!senderUid.isNullOrBlank()) put("senderUid", senderUid)
        if (!wakeMessageId.isNullOrBlank()) put("wakeMessageId", wakeMessageId)
    })

    suspend fun sendGroupInvite(
        targetToken: String,
        senderName: String,
        groupName: String,
        groupCloudId: String
    ): Boolean = sendMessage(targetToken, JSONObject().apply {
        put("type", "group_invite")
        put("senderName", senderName)
        put("groupName", groupName)
        put("groupCloudId", groupCloudId)
    })

    suspend fun sendSharedAlarm(
        targetToken: String,
        alarm: Alarm,
        senderName: String
    ): Boolean = sendMessage(targetToken, JSONObject().apply {
        // FCM HTTP v1 規定 data 內所有值都必須是字串，
        // 直接放 Int/Boolean 會讓 API 回 400 INVALID_ARGUMENT，推播不會送出。
        put("type", "share_alarm")
        put("hour", alarm.timeHour.toString())
        put("minute", alarm.timeMinute.toString())
        put("label", alarm.label)
        put("ringtone", alarm.ringtone)
        put("vibrate", alarm.vibrate.toString())
        put("senderName", senderName)
    })

    private suspend fun sendMessage(token: String, data: JSONObject): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val accessToken = getAccessToken() ?: return@withContext false

                val payload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", token)
                        put("data", data)
                        // 高優先級，確保 doze 模式下仍能觸發
                        put("android", JSONObject().apply {
                            put("priority", "HIGH")
                        })
                    })
                }.toString()

                val url = URL(fcmUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; UTF-8")
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                conn.doOutput = true
                conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))

                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // 取得 OAuth2 Access Token（有效期 1 小時，過期前自動更新）
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val nowSec = System.currentTimeMillis() / 1000L
        if (cachedToken != null && nowSec < tokenExpiry - 60) return@withContext cachedToken

        return@withContext try {
            val jwt = buildJwt(nowSec)
            val token = exchangeJwtForToken(jwt)
            cachedToken = token
            tokenExpiry = nowSec + 3600L
            token
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 建立 Service Account JWT（RS256 簽名）
    private fun buildJwt(nowSec: Long): String {
        fun encode(bytes: ByteArray) = Base64.encodeToString(
            bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )

        val header = encode("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val claimsJson = JSONObject().apply {
            put("iss", serviceAccountEmail)
            put("scope", scope)
            put("aud", tokenUrl)
            put("iat", nowSec)
            put("exp", nowSec + 3600L)
        }.toString()
        val claims = encode(claimsJson.toByteArray())

        val signingInput = "$header.$claims"
        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(loadPrivateKey())
            update(signingInput.toByteArray())
        }
        val signature = encode(sig.sign())
        return "$signingInput.$signature"
    }

    // 解析 PEM 格式私鑰
    private fun loadPrivateKey(): PrivateKey {
        val pem = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val keyBytes = Base64.decode(pem, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    // 用 JWT 換取 Google OAuth2 Access Token
    private fun exchangeJwtForToken(jwt: String): String {
        val body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
        val conn = URL(tokenUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true
        conn.outputStream.write(body.toByteArray())

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return JSONObject(response).getString("access_token")
    }
}
