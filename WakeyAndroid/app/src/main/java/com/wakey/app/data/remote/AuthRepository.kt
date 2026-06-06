// 認證：Google 登入 → Firebase Auth，暴露目前登入使用者狀態
package com.wakey.app.data.remote

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class SignInResult {
    data class Success(val user: FirebaseUser) : SignInResult()
    data class Error(val message: String) : SignInResult()
}

sealed class LinkResult {
    data class Success(val user: FirebaseUser) : LinkResult()
    object AlreadyInUse : LinkResult()    // 該 Google 帳號已綁定其他使用者
    data class Error(val message: String) : LinkResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isAnonymous: Boolean get() = auth.currentUser?.isAnonymous == true

    // 登入狀態 Flow（供 gate 監聽）
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // 訪客模式：建立 Anonymous 帳號（仍有 uid，資料正常上雲）
    suspend fun signInAnonymously(): SignInResult = try {
        val r = auth.signInAnonymously().await()
        val user = r.user ?: return SignInResult.Error("訪客建立失敗")
        SignInResult.Success(user)
    } catch (e: Exception) {
        SignInResult.Error(e.message ?: "訪客建立失敗")
    }

    // 訪客升級綁定：把目前的 anonymous user 與 Google 憑證合併
    // 該 Google 帳號若已被其他使用者使用，回傳 AlreadyInUse（保留訪客身分）
    suspend fun linkWithGoogle(data: Intent?): LinkResult {
        val user = auth.currentUser
            ?: return LinkResult.Error("尚未建立訪客身分")
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val idToken = account.idToken
                ?: return LinkResult.Error("無法取得 Google idToken")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = user.linkWithCredential(credential).await()
            val upgraded = result.user ?: return LinkResult.Error("綁定失敗")
            LinkResult.Success(upgraded)
        } catch (e: FirebaseAuthUserCollisionException) {
            // credential 已被其他帳號使用 → 不要 sign out 訪客
            LinkResult.AlreadyInUse
        } catch (e: ApiException) {
            LinkResult.Error("Google 登入失敗（code ${e.statusCode}）")
        } catch (e: Exception) {
            LinkResult.Error(e.message ?: "綁定發生未知錯誤")
        }
    }

    // 取得 Google 登入 Intent（交給 Activity launcher 啟動）
    fun signInIntent(): Intent = googleSignInClient.signInIntent

    // 處理登入回傳 Intent：取 Google idToken → 換 Firebase 憑證
    suspend fun handleSignInResult(data: Intent?): SignInResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val idToken = account.idToken
                ?: return SignInResult.Error("無法取得 Google idToken（請確認 SHA-1 與 OAuth 設定）")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
                ?: return SignInResult.Error("登入失敗：使用者為空")
            SignInResult.Success(user)
        } catch (e: ApiException) {
            SignInResult.Error("Google 登入失敗（code ${e.statusCode}）")
        } catch (e: Exception) {
            SignInResult.Error(e.message ?: "登入發生未知錯誤")
        }
    }

    // 登出：Firebase + Google（讓下次能重新選帳號）
    suspend fun signOut() {
        auth.signOut()
        runCatching { googleSignInClient.signOut().await() }
    }
}
