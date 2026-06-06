// CameraX 預覽 View，整合 QrCodeScanner 進行即時掃描
package com.wakey.app.ui.screen.friend

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wakey.app.qr.QrCodeScanner

@Composable
fun QrScannerView(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { mutableStateOf<QrCodeScanner?>(null) }

    // 畫面離開時解除相機綁定，避免相機在背景續跑造成卡頓
    DisposableEffect(Unit) {
        onDispose { scanner.value?.stop(); scanner.value = null }
    }

    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    if (scanner.value == null) {
                        scanner.value = QrCodeScanner(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            onResult = onResult
                        ).also { it.start() }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
