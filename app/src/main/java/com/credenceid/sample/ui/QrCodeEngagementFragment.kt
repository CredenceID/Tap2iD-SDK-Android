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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.databinding.FragmentQrCodeEngagementBinding
import com.credenceid.sample.utils.BarcodeScannerCallback
import com.credenceid.sample.utils.QRCodeScanner
import com.credenceid.sample.utils.TAG
import com.credenceid.tap2idSdk.api.MdocVerificationListener
import com.credenceid.tap2idSdk.api.models.VerificationStage
import com.credenceid.tap2idSdk.core.model.MdocAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class QrCodeEngagementFragment : Fragment() {

    private var _binding: FragmentQrCodeEngagementBinding? = null
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
        _binding = FragmentQrCodeEngagementBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupQrCodeScanner()
        startCamera()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel.getTitle(Screen.QR)
        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_qrCodeEngagementFragment_to_homeFragment)
        }
    }

    private fun setupQrCodeScanner() {
        barcodeScannerHelper = QRCodeScanner(
            applicationContext = requireContext().applicationContext,
            screenAspectRatio = screenAspectRatio,
            previewView = binding.previewView,
            lifecycleOwner = this,
            barcodeScannerCallback = onBarcodeScannerCallback
        )
    }

    private fun startCamera() {
        if (!isCameraStarted) {
            barcodeScannerHelper.startCamera()
            isCameraStarted = true
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
        }

        override fun onBarcodeDetected(rawValue: String?) {
            rawValue?.let { capturedQr ->
                if (!isBarcodeCaptured) {
                    isBarcodeCaptured = true
                    Log.d(TAG, "Barcode detected: ${capturedQr.length}")
                    verifyWithQr(capturedQr)
                }
            }
        }

        override fun onBarcodeDetectionFailed(errorMessage: String) {
            Log.e(TAG, "Barcode Detection Failed: $errorMessage")
        }
    }

    private fun verifyWithQr(qrCode: String) {
        sharedViewModel.verifyWithQr(qrCode, mdocVerificationListener = object : MdocVerificationListener {
            override fun onVerificationCompleted(result: MdocAttributes) {
                setStatusOnUi("Verification Success")
                lifecycleScope.launch {
                    delay(2000)
                    val identityResult: String = sharedViewModel.prettyPrintJson(result)
                    val directions = QrCodeEngagementFragmentDirections.actionQrCodeEngagementFragmentToResultFragment(identityResult)
                    findNavController().navigate(directions)
                }
            }

            override fun onVerificationStageCompleted(stage: VerificationStage) {
                Log.d(TAG, stage.name)
                setStatusOnUi("Completed :".plus(stage.toString()))
            }

            override fun onVerificationStageError(stage: VerificationStage, error: Throwable) {
                Log.e(TAG, "Stage :${stage.name}\nError Message :${error.message}")
                setStatusOnUi("Error Stage :".plus(stage.name))
                setStatusOnUi("Message :".plus(error.message ?: "Unknown error"))
                setStatusOnUi("Verification Failed")
            }

            override fun onVerificationStageStarted(stage: VerificationStage) {
                Log.d(TAG, stage.name)
                setStatusOnUi("Started:".plus(stage.toString()))
            }
        })
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
    }
}
