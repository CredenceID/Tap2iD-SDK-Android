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
import com.credenceid.sample.databinding.FragmentPdf417ScanBinding
import com.credenceid.sample.utils.BarcodeScannerCallback
import com.credenceid.sample.utils.QRCodeScanner
import com.credenceid.sample.utils.TAG
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PDF417ScanFragment : Fragment() {

    private var _binding: FragmentPdf417ScanBinding? = null
    private val binding
        get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var barcodeScannerHelper: QRCodeScanner

    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { binding.previewView.display?.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    private var isCameraStarted = false
    private var isBarcodeProcessing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPdf417ScanBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupScanner()
        startCamera()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel.getTitle(Screen.PDF417, requireContext())
        binding.statusTv.text = getString(R.string.pdf417_scanning)
        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_pdf417ScanFragment_to_homeFragment)
        }
    }

    private fun setupScanner() {
        barcodeScannerHelper = QRCodeScanner(
            applicationContext = requireContext().applicationContext,
            screenAspectRatio = screenAspectRatio,
            previewView = binding.previewView,
            lifecycleOwner = this,
            barcodeScannerCallback = onBarcodeScannerCallback,
            barcodeFormats = Barcode.FORMAT_PDF417,
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
            rawValue?.let { barcode ->
                if (!isBarcodeProcessing) {
                    isBarcodeProcessing = true
                    Log.d(TAG, "PDF417 barcode detected (length=${barcode.length})")
                    verifyBarcode(barcode)
                }
            }
        }

        override fun onBarcodeDetectionFailed(errorMessage: String) {
            Log.e(TAG, "Barcode Detection Failed: $errorMessage")
        }
    }

    private fun verifyBarcode(barcode: String) {
        lifecycleScope.launch {
            setStatus("Verifying...")
            sharedViewModel.verifyPDF417FromDL(barcode)
            if (isAdded) {
                findNavController().navigate(R.id.action_pdf417ScanFragment_to_resultFragment)
            }
        }
    }

    private fun setStatus(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.statusTv.text = message
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