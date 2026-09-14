package com.credenceid.sample.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.common.VerificationResultCallback
import com.credenceid.sample.databinding.FragmentPdf417EngagementBinding
import com.credenceid.sample.utils.BarcodeScannerCallback
import com.credenceid.sample.utils.QRCodeScanner
import com.credenceid.sample.utils.TAG
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Scans an AAMVA driver's-license PDF417 barcode with the camera (ML Kit, PDF417 format) and
 * verifies it through [SharedViewModel.verifyPdf417]. The result is rendered as an HTML report on
 * [ResultFragment] (same path the QR/NFC flows use).
 *
 * Mirrors [QrCodeEngagementFragment]; the only scanning difference is the barcode format passed to
 * [QRCodeScanner].
 */
class Pdf417EngagementFragment : Fragment() {

    private var _binding: FragmentPdf417EngagementBinding? = null
    private val binding
        get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var barcodeScannerHelper: QRCodeScanner

    private val statusQueue = StringBuilder()

    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { binding.previewView.display?.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    private var isCameraStarted: Boolean = false
    private var isBarcodeCaptured: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPdf417EngagementBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupPdf417Scanner()
        startCamera()
        startScanTimeout()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel.getTitle(Screen.PDF417, requireContext())
        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_pdf417EngagementFragment_to_homeFragment)
        }
    }

    private fun setupPdf417Scanner() {
        barcodeScannerHelper = QRCodeScanner(
            applicationContext = requireContext().applicationContext,
            screenAspectRatio = screenAspectRatio,
            previewView = binding.previewView,
            lifecycleOwner = this,
            barcodeScannerCallback = onBarcodeScannerCallback,
            barcodeFormat = Barcode.FORMAT_PDF417
        )
    }

    private fun startCamera() {
        if (!isCameraStarted) {
            barcodeScannerHelper.startCamera()
            isCameraStarted = true
        }
    }

    private fun startScanTimeout() {
        lifecycleScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (!isBarcodeCaptured && isAdded) {
                isBarcodeCaptured = true
                setStatusOnUi("No PDF417 barcode detected. Press Cancel to go back and try again.")
            }
        }
    }

    private val onBarcodeScannerCallback = object : BarcodeScannerCallback {
        override fun onCameraStarted() {
            Log.d(TAG, "Camera Started")
        }

        override fun onCameraStopped() {
            Log.d(TAG, "Camera Stopped")
        }

        override fun onCameraError(errorMessage: String) {
            Log.e(TAG, "Camera Error: $errorMessage")
            if (isAdded) setStatusOnUi("Camera Error: $errorMessage")
        }

        override fun onBarcodeDetected(rawValue: String?) {
            rawValue?.let { capturedPdf417 ->
                if (!isBarcodeCaptured) {
                    isBarcodeCaptured = true
                    Log.d(TAG, "PDF417 detected: ${capturedPdf417.length} chars")
                    verifyPdf417(capturedPdf417)
                }
            }
        }

        override fun onBarcodeDetectionFailed(errorMessage: String) {
            Log.e(TAG, "Barcode Detection Failed: $errorMessage")
            if (isAdded) setStatusOnUi("Unable to read barcode: $errorMessage")
        }
    }

    private fun verifyPdf417(pdf417Value: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.verifyPdf417(pdf417Value).collect { result ->
                    when (result) {
                        is VerificationResultCallback.StageStarted -> {
                            if (isAdded) setStatusOnUi(result.message)
                        }

                        is VerificationResultCallback.StageCompleted -> {
                            if (isAdded) setStatusOnUi(result.message)
                        }

                        is VerificationResultCallback.StageError -> {
                            // Unexpected throwable (e.g. native classifier missing on a non-arm64 device).
                            if (isAdded) setStatusOnUi(result.message)
                        }

                        is VerificationResultCallback.VerificationCompleted -> {
                            if (isAdded) {
                                if (result.hasValidationErrors) {
                                    setStatusOnUi("Verdict: ${result.message} (see report)")
                                    delay(2000)
                                } else {
                                    setStatusOnUi("Verdict: ${result.message}")
                                }
                                findNavController().navigate(R.id.action_pdf417EngagementFragment_to_resultFragment)
                            }
                        }

                        VerificationResultCallback.VerificationProcessStarted -> {
                            if (isAdded) setStatusOnUi("Verifying PDF417...")
                        }
                    }
                }
            }
        }
    }

    private fun setStatusOnUi(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            statusQueue.append(message.plus("\n"))
            binding.statusTv.text = statusQueue.toString()
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val SCAN_TIMEOUT_MS = 20_000L
    }
}
