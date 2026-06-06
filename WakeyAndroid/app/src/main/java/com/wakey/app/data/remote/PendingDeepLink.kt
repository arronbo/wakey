// 等待處理的 Deep Link：使用者點 wakey:// 連結進來，先暫存讓 NavGraph 收到後處理
package com.wakey.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class PendingDeepLink {
    data class AddFriend(val uid: String) : PendingDeepLink()
    data class JoinGroup(val cloudId: String) : PendingDeepLink()
}

@Singleton
class PendingDeepLinkManager @Inject constructor() {
    private val _pending = MutableStateFlow<PendingDeepLink?>(null)
    val pending: StateFlow<PendingDeepLink?> = _pending.asStateFlow()

    fun set(link: PendingDeepLink) { _pending.value = link }
    fun consume() { _pending.value = null }
}
