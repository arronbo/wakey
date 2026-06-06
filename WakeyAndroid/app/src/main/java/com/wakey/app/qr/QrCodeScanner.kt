// QR Code 掃描器：使用 CameraX + ML Kit Barcode Scanning 解析 QR 內容
package com.wakey.app.qr

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrCodeScanner(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onResult: (String) -> Unit,
    private val onError: (Exception) -> Unit = {}
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient()
    private var alreadyFound = false
    private var cameraProvider: ProcessCameraProvider? = null

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 停止掃描：解除相機綁定並釋放資源（畫面關閉時務必呼叫，否則相機會在背景續跑）
    fun stop() {
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        runCatching { scanner.close() }
        runCatching { executor.shutdown() }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {
        if (alreadyFound) { imageProxy.close(); return }

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                    ?.rawValue
                    ?.let { value ->
                        alreadyFound = true
                        onResult(value)
                    }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
