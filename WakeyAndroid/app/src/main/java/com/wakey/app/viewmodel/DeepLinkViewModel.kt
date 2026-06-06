// 暴露 PendingDeepLinkManager 給 Composable 使用
package com.wakey.app.viewmodel

import androidx.lifecycle.ViewModel
import com.wakey.app.data.remote.PendingDeepLinkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    val manager: PendingDeepLinkManager
) : ViewModel()
