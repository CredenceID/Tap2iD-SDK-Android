package com.credenceid.sample.common

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credenceid.sample.BuildConfig
import com.credenceid.sample.utils.TAG
import com.credenceid.sample.utils.Utils
import com.credenceid.sample.utils.VerificationReportGenerator
import com.credenceid.tap2idSdk.api.InitSdkResultListener
import com.credenceid.tap2idSdk.api.MdocVerificationListener
import com.credenceid.tap2idSdk.api.Tap2iDSdk
import com.credenceid.tap2idSdk.api.models.EngagementConfig
import com.credenceid.tap2idSdk.api.models.NfcConfig
import com.credenceid.tap2idSdk.api.models.QrConfig
import com.credenceid.tap2idSdk.api.models.SdkConfigBuilder
import com.credenceid.tap2idSdk.api.models.SdkInitializationResult
import com.credenceid.tap2idSdk.api.models.VerificationStage
// Updated Import: TrustResult -> TrustStatus
import com.credenceid.tap2idSdk.core.model.TrustStatus
import com.credenceid.tap2idSdk.core.model.VerificationResult
import com.credenceid.tap2idSdk.core.model.VerificationStatus
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SharedViewModel : ViewModel() {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    var storedVerificationHtml: String? = null
        private set

    fun initializeSdk(licenseKey: String, applicationContext: Context, resultCallback: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val sdkConfig = SdkConfigBuilder()
                .setApplicationContext(applicationContext)
                .setApiKey(licenseKey)
                .build()

            Tap2iDSdk.initSdk(sdkConfig, object : InitSdkResultListener {
                override fun onInitializationFailure(error: Error) {
                    Log.e(TAG, "[Error] $error")
                    resultCallback(Result.failure(error))
                }

                override fun onInitializationSuccess(result: SdkInitializationResult) {
                    if (result.licenseVerificationResult.isValid) {
                        Log.d(TAG, "SDK initialized")
                        val isValid = if (result.licenseVerificationResult.isValid) "VALID" else "EXPIRED"
                        val resultData = buildString {
                            append("Licence is $isValid\n")
                            append("Expiry Date : ${Utils.millisToDateString(result.licenseVerificationResult.expiryDate)}\n")
                            append("Profile : ${result.licenseVerificationResult.profileName}")
                        }
                        resultCallback(Result.success(resultData))
                    } else {
                        Log.e(TAG, "[Error] ${result.licenseVerificationResult}")
                        resultCallback(Result.failure(Error("License is not valid")))
                    }
                }
            })
        }
    }

    fun verifyWithQr(qrCodeString: String) = callbackFlow {
        viewModelScope.launch {
            Tap2iDSdk.verifyMdoc(
                engagementConfig = EngagementConfig(qrConfig = QrConfig(qrCodeString)),
                mdocVerificationListener = createVerificationListener(this@callbackFlow)
            )
        }
        try {
            send(VerificationResultCallback.VerificationProcessStarted)
            awaitClose { Log.d(TAG, "callbackFlow for QR closed") }
        } finally {
            Log.d(TAG, "callbackFlow block finished")
        }
    }

    fun verifyWitNfc(activity: Activity) = callbackFlow {
        viewModelScope.launch {
            Tap2iDSdk.verifyMdoc(
                engagementConfig = EngagementConfig(nfcConfig = NfcConfig(activity)),
                mdocVerificationListener = createVerificationListener(this@callbackFlow)
            )
        }
        try {
            send(VerificationResultCallback.VerificationProcessStarted)
            awaitClose { Log.d(TAG, "callbackFlow for NFC closed") }
        } finally {
            Log.d(TAG, "callbackFlow block finished")
        }
    }

    fun clearVerificationData() {
        storedVerificationHtml = null
        Log.d(TAG, "Verification data cleared from ViewModel")
    }

    fun getTitle(screen: Screen, context: Context): String {
        return when (screen) {
            Screen.HOME -> "Tap2iD-SDK\nSample\nApp Version : ${BuildConfig.VERSION_NAME}\nSDK Version : ${Tap2iDSdk.getSdkVersion()}\nDeviceID : ${Utils.getAndroidId(context)}\nPackage Name : ${context.packageName}"
            Screen.NFC -> "NFC Engagement"
            Screen.QR -> "QR Engagement"
            Screen.RESULT -> "mDL Data"
            Screen.LICENSE_KEY_VERIFICATION -> "Please enter License Key\nto verify with VwC\n---\nApp Version : ${BuildConfig.VERSION_NAME}\nSDK Version : ${Tap2iDSdk.getSdkVersion()}\nDeviceID : ${Utils.getAndroidId(context)}\nPackage Name : ${context.packageName}"
        }
    }

    private fun createVerificationListener(producer: kotlinx.coroutines.channels.ProducerScope<VerificationResultCallback>) =
        object : MdocVerificationListener {
            override fun onVerificationStageCompleted(stage: VerificationStage) {
                producer.trySend(VerificationResultCallback.StageCompleted("Completed: $stage"))
            }

            override fun onVerificationCompleted(verificationResult: VerificationResult) {
                logResultAsJson(verificationResult)

                viewModelScope.launch(Dispatchers.Default) {
                    storedVerificationHtml = VerificationReportGenerator.generateHtml(verificationResult)
                    producer.trySend(
                        VerificationResultCallback.VerificationCompleted(
                            message = "Success",
                            hasValidationErrors = verificationResult.status == VerificationStatus.FAILURE
                        )
                    )
                }
            }

            override fun onVerificationStageError(stage: VerificationStage, error: Throwable) {
                val errorMessage = "Error Stage: ${stage.name}\nMessage: ${error.message ?: "Unknown"}"
                producer.trySend(VerificationResultCallback.StageError(errorMessage))
            }

            override fun onVerificationStageStarted(stage: VerificationStage) {
                producer.trySend(VerificationResultCallback.StageStarted("Started: $stage"))
            }
        }

    private fun logResultAsJson(result: VerificationResult) {
        try {
            val jsonString = gson.toJson(result)
            if (jsonString.length > 4000) {
                Log.d(TAG, "VerificationResult JSON (Part 1):")
                jsonString.chunked(4000).forEach { Log.d(TAG, it) }
            } else {
                Log.d(TAG, "VerificationResult JSON:\n$jsonString")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize JSON: ${e.message}")
        }
    }
}

enum class Screen {
    HOME, NFC, QR, RESULT, LICENSE_KEY_VERIFICATION
}

sealed class VerificationResultCallback {
    object VerificationProcessStarted : VerificationResultCallback()
    data class StageStarted(val message: String) : VerificationResultCallback()
    data class StageCompleted(val message: String) : VerificationResultCallback()
    data class StageError(val message: String) : VerificationResultCallback()
    data class VerificationCompleted(val message: String, val hasValidationErrors: Boolean) : VerificationResultCallback()
}


