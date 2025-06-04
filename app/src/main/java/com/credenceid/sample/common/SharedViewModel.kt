package com.credenceid.sample.common

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credenceid.sample.BuildConfig
import com.credenceid.sample.utils.Jpeg2kConverter
import com.credenceid.sample.utils.TAG
import com.credenceid.sample.utils.Utils
import com.credenceid.sample.utils.toBitmap
import com.credenceid.tap2idSdk.api.InitSdkResultListener
import com.credenceid.tap2idSdk.api.MdocVerificationListener
import com.credenceid.tap2idSdk.api.Tap2iDSdk
import com.credenceid.tap2idSdk.api.models.EngagementConfig
import com.credenceid.tap2idSdk.api.models.NfcConfig
import com.credenceid.tap2idSdk.api.models.QrConfig
import com.credenceid.tap2idSdk.api.models.SdkConfigBuilder
import com.credenceid.tap2idSdk.api.models.SdkInitializationResult
import com.credenceid.tap2idSdk.api.models.VerificationStage
import com.credenceid.tap2idSdk.core.model.DrivingPrivilege
import com.credenceid.tap2idSdk.core.model.MdocAttributes
import com.credenceid.tap2idSdk.core.model.ValidationResult
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.time.LocalDate

class SharedViewModel : ViewModel() {

    var userPortraitLiveData: Bitmap? = null

    fun getTitle(screen: Screen): String {
        return when (screen) {
            Screen.HOME -> "Tap2iD-SDK\nSample\nApp Version : ${BuildConfig.VERSION_NAME}\nSDK Version : ${Tap2iDSdk.getSdkVersion()}"
            Screen.NFC -> "NFC Engagement"
            Screen.QR -> "QR Engagement"
            Screen.RESULT -> "mDL Data"
            Screen.LICENSE_KEY_VERIFICATION -> "Please enter License Key\nto verify with VwC"
        }
    }

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
                        resultCallback(Result.failure(Error("License is not valid. Please use a valid license key")))
                    }
                }
            })
        }
    }

    fun verifyWithQr(context: Context, qrCodeString: String) = callbackFlow<VerificationResult> {
        viewModelScope.launch {
            Tap2iDSdk.verifyMdoc(
                engagementConfig = EngagementConfig(qrConfig = QrConfig(qrCodeString)), object : MdocVerificationListener {
                    override fun onVerificationCompleted(mdocAttributes: MdocAttributes, verificationResult: ValidationResult) {
                        val attributesResult: String = prettyPrintJson(mdocAttributes)
                        val validationResult: String = prettyPrintJson(verificationResult)
                        val result = attributesResult.plus("\n").plus(validationResult)

                        mdocAttributes.portrait?.let {
                            userPortraitLiveData = Jpeg2kConverter.decodeByteArray(context, it)
                        }

                        trySend(
                            VerificationResult.VerificationCompleted(
                                message = "Success",
                                resultJsonString = result,
                                userPortrait = mdocAttributes.portrait?.toBitmap(),
                                hasValidationErrors = !verificationResult.validationErrors.isNullOrEmpty()
                            )
                        )
                    }

                    override fun onVerificationStageCompleted(stage: VerificationStage) {
                        trySend(
                            VerificationResult.StageCompleted(
                                message = "Completed :".plus(stage.toString())
                            )
                        )
                    }

                    override fun onVerificationStageError(stage: VerificationStage, error: Throwable) {
                        val errorMessage = buildString {
                            append("Error Stage :".plus(stage.name).plus("\n"))
                            append("Message :".plus(error.message ?: "Unknown error").plus("\n"))
                            append("Verification Failed".plus("\n"))
                        }
                        trySend(
                            VerificationResult.StageError(
                                message = errorMessage
                            )
                        )
                    }

                    override fun onVerificationStageStarted(stage: VerificationStage) {
                        trySend(
                            VerificationResult.StageStarted(
                                message = "Started :".plus(stage.toString())
                            )
                        )
                    }
                })
        }

        try {
            send(VerificationResult.VerificationProcessStarted)
            awaitClose {
                Log.d(TAG, "callbackFlow for mdocVerificationResult closed")
            }
        } finally {
            Log.d(TAG, "callbackFlow block finished")
        }
    }

    fun verifyWitNfc(activity: Activity) = callbackFlow<VerificationResult> {
        viewModelScope.launch {
            Tap2iDSdk.verifyMdoc(
                engagementConfig = EngagementConfig(nfcConfig = NfcConfig(activity)), object : MdocVerificationListener {
                    override fun onVerificationCompleted(mdocAttributes: MdocAttributes, verificationResult: ValidationResult) {
                        val attributesResult: String = prettyPrintJson(mdocAttributes)
                        val validationResult: String = prettyPrintJson(verificationResult)
                        val result = attributesResult.plus("\n").plus(validationResult)
                        userPortraitLiveData = mdocAttributes.portrait?.toBitmap()

                        trySend(
                            VerificationResult.VerificationCompleted(
                                message = "Success",
                                resultJsonString = result,
                                userPortrait = mdocAttributes.portrait?.toBitmap(),
                                hasValidationErrors = !verificationResult.validationErrors.isNullOrEmpty()
                            )
                        )
                    }

                    override fun onVerificationStageCompleted(stage: VerificationStage) {
                        trySend(
                            VerificationResult.StageCompleted(
                                message = "Completed :".plus(stage.toString())
                            )
                        )
                    }

                    override fun onVerificationStageError(stage: VerificationStage, error: Throwable) {
                        val errorMessage = buildString {
                            append("Error Stage :".plus(stage.name).plus("\n"))
                            append("Message :".plus(error.message ?: "Unknown error").plus("\n"))
                            append("Verification Failed".plus("\n"))
                        }
                        trySend(
                            VerificationResult.StageError(
                                message = errorMessage
                            )
                        )
                    }

                    override fun onVerificationStageStarted(stage: VerificationStage) {
                        trySend(
                            VerificationResult.StageStarted(
                                message = "Started :".plus(stage.toString())
                            )
                        )
                    }
                })
        }

        try {
            send(VerificationResult.VerificationProcessStarted)
            awaitClose {
                Log.d(TAG, "callbackFlow for mdocVerificationResult closed")
            }
        } finally {
            Log.d(TAG, "callbackFlow block finished")
        }
    }
}

fun prettyPrintJson(model: MdocAttributes): String {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        .registerTypeAdapter(ByteArray::class.java, ByteArraySerializer())
        .registerTypeAdapter(
            object : TypeToken<List<DrivingPrivilege>>() {}.type,
            DrivingPrivilegesSerializer()
        )
        .create()
    return gson.toJson(model)
}

fun prettyPrintJson(model: ValidationResult): String {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        .registerTypeAdapter(ByteArray::class.java, ByteArraySerializer())
        .registerTypeAdapter(
            object : TypeToken<List<DrivingPrivilege>>() {}.type,
            DrivingPrivilegesSerializer()
        )
        .create()
    return gson.toJson(model)
}

class LocalDateSerializer : JsonSerializer<LocalDate> {
    override fun serialize(
        src: LocalDate,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return context.serialize(src.toString())
    }
}

class ByteArraySerializer : JsonSerializer<ByteArray> {
    override fun serialize(
        src: ByteArray,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("image(bytes)", src.size)
        return jsonObject
    }
}

class DrivingPrivilegesSerializer : JsonSerializer<List<DrivingPrivilege>> {
    override fun serialize(
        src: List<DrivingPrivilege>,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val jsonObject = JsonObject()
        val drivingPrivilegeStringBuilder = buildString {
            src.forEach { drivingPrivileges ->
                val codes = buildString {
                    drivingPrivileges.codes?.forEach {
                        append(it)
                    }
                }
                append(drivingPrivileges.vehicleCategory)
                append(drivingPrivileges.issueDate)
                append(drivingPrivileges.expiryDate)
                append(codes)
            }
        }
        jsonObject.addProperty("drivingPrivileges", drivingPrivilegeStringBuilder)
        return jsonObject
    }
}

enum class Screen {
    HOME,
    NFC,
    QR,
    RESULT,
    LICENSE_KEY_VERIFICATION
}

sealed class VerificationResult {
    object VerificationProcessStarted : VerificationResult()
    data class StageStarted(val message: String) : VerificationResult()
    data class StageCompleted(val message: String) : VerificationResult()
    data class StageError(val message: String) : VerificationResult()
    data class VerificationCompleted(
        val message: String,
        val resultJsonString: String,
        val userPortrait: Bitmap?,
        val hasValidationErrors: Boolean
    ) : VerificationResult()
}
