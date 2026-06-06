// ViewModel：登入狀態、Google 登入流程、訪客模式與綁定、登出
package com.wakey.app.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.wakey.app.data.remote.AuthRepository
import com.wakey.app.data.remote.LinkResult
import com.wakey.app.data.remote.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val linkSuccessMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<FirebaseUser?> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    val isAnonymous: StateFlow<Boolean> = authState
        .map { it?.isAnonymous == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isAnonymous)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInIntent(): Intent = authRepository.signInIntent()

    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val r = authRepository.handleSignInResult(data)) {
                is SignInResult.Success -> _uiState.update { it.copy(loading = false) }
                is SignInResult.Error ->
                    _uiState.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    // 訪客模式
    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val r = authRepository.signInAnonymously()) {
                is SignInResult.Success -> _uiState.update { it.copy(loading = false) }
                is SignInResult.Error ->
                    _uiState.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    // 訪客升級綁定 Google 帳號
    fun linkGoogleAccount(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, linkSuccessMessage = null) }
            when (val r = authRepository.linkWithGoogle(data)) {
                is LinkResult.Success -> _uiState.update {
                    it.copy(loading = false, linkSuccessMessage = "已成功綁定 ${r.user.email ?: "Google 帳號"}")
                }
                LinkResult.AlreadyInUse -> _uiState.update {
                    it.copy(
                        loading = false,
                        error = "此 Google 帳號已被其他使用者綁定。請改用其他帳號，或登出後直接登入該帳號。"
                    )
                }
                is LinkResult.Error -> _uiState.update {
                    it.copy(loading = false, error = r.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, linkSuccessMessage = null) }
}
