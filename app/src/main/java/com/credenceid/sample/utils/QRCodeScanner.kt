package com.credenceid.sample.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QRCodeScanner(
    private val applicationContext: Context,
    private val screenAspectRatio: Int,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val barcodeScannerCallback: BarcodeScannerCallback,
    private val barcodeFormat: Int = Barcode.FORMAT_QR_CODE,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var isBarcodeDetected = false
    private val cameraProviderLiveData: MutableLiveData<ProcessCameraProvider> by lazy {
        MutableLiveData<ProcessCameraProvider>().also { fetchCameraProvider() }
    }

    fun startCamera() {
        cameraProviderLiveData.observe(lifecycleOwner) { provider ->
            cameraProvider = provider
            if (isCameraPermissionGranted()) {
                bindCameraUseCases()
            } else {
                barcodeScannerCallback.onCameraError("Camera permission not provided. Please request CAMERA permission.")
                Log.e(TAG, "Camera permission not provided. Please request CAMERA permission.")
            }
        }
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalysisUseCase()
    }

    private fun bindPreviewUseCase() {
        cameraProvider?.let { provider ->
            previewUseCase?.let { provider.unbind(it) }

            previewUseCase = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(previewView.display.rotation)
                .build().also { useCase ->
                    useCase.setSurfaceProvider(previewView.surfaceProvider)
                    bindUseCase(provider, useCase)
                }
        }
    }

    private fun bindAnalysisUseCase() {
        cameraProvider?.let { provider ->
            analysisUseCase?.let { provider.unbind(it) }

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(barcodeFormat).build()
            )

            analysisUseCase = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(previewView.display.rotation)
                .build().also { useCase ->
                    useCase.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy)
                    }
                    bindUseCase(provider, useCase)
                }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        if (isBarcodeDetected) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    isBarcodeDetected = true
                    barcodes.forEach { barcode ->
                        barcodeScannerCallback.onBarcodeDetected(barcode.payload())
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                barcodeScannerCallback.onBarcodeDetectionFailed(it.message ?: "Barcode scanning failed.")
                Log.e(TAG, it.message ?: "Barcode scanning failed.")
                imageProxy.close()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * The barcode's payload, falling back to a byte-for-byte decode of [Barcode.rawBytes].
     *
     * ML Kit populates [Barcode.rawValue] only when the payload decodes as UTF-8. An AAMVA
     * PDF417 carries a binary subfile on some jurisdictions (the CA DMV signature among them),
     * so relying on `rawValue` alone drops those scans silently - the camera reads the barcode
     * and the app reports nothing found. ISO-8859-1 is the decode that round-trips every byte
     * 0x00-0xFF to the same code point, so the payload reaches the classifier unaltered.
     */
    private fun Barcode.payload(): String? =
        rawValue ?: rawBytes?.toString(Charsets.ISO_8859_1)

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            try {
                cameraProviderLiveData.value = cameraProviderFuture.get()
            } catch (e: Exception) {
                barcodeScannerCallback.onCameraError("Error fetching camera provider: ${e.message}")
                Log.e(TAG, "Error fetching camera provider.", e)
            }
        }, ContextCompat.getMainExecutor(applicationContext))
    }

    private fun bindUseCase(provider: ProcessCameraProvider, useCase: UseCase) {
        try {
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            barcodeScannerCallback.onCameraStarted()
        } catch (e: IllegalStateException) {
            barcodeScannerCallback.onCameraError("Failed to bind use case: ${e.message}")
            Log.e(TAG, e.message ?: "Failed to bind use case.")
        } catch (e: IllegalArgumentException) {
            barcodeScannerCallback.onCameraError("Failed to bind use case: ${e.message}")
            Log.e(TAG, e.message ?: "Failed to bind use case.")
        }
    }
}

interface BarcodeScannerCallback {
    fun onCameraStarted()
    fun onCameraStopped()
    fun onCameraError(errorMessage: String)
    fun onBarcodeDetected(rawValue: String?)
    fun onBarcodeDetectionFailed(errorMessage: String)
}
