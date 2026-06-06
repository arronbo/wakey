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
    var started by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    if (!started) {
                        started = true
                        QrCodeScanner(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            onResult = onResult
                        ).start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
