package com.credenceid.sample.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.credenceid.tap2idSdk.core.model.TrustStatus
import com.credenceid.tap2idSdk.core.model.VerificationResult
import com.credenceid.tap2idSdk.core.model.VerificationStatus
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object VerificationReportGenerator {

    fun generateHtml(result: VerificationResult): String {
        return result.toHtmlString()
    }

    private fun VerificationResult.toHtmlString(): String {
        val css = """
        <style>
            :root {
                --primary: #6200EE;
                --primary-light: #EDE7F6;
                --success: #2E7D32;
                --error: #D32F2F;
                --warning: #F57C00;
                --text-main: #212121;
                --text-secondary: #757575;
                --bg-main: #FFFFFF;
                --divider: #E0E0E0;
            }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; 
                background-color: var(--bg-main); 
                margin: 0; 
                padding: 24px; 
                color: var(--text-main); 
                line-height: 1.5;
            }
            .report-container { max-width: 600px; margin: 0 auto; }
            
            /* Header */
            .main-header { text-align: center; margin-bottom: 32px; padding-bottom: 16px; border-bottom: 2px solid var(--divider); }
            .report-title { margin: 0; font-size: 24px; font-weight: 700; color: var(--text-main); }
            .report-status { margin-top: 8px; font-size: 16px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
            
            .status-success { color: var(--success); }
            .status-warning { color: var(--warning); }
            .status-failure { color: var(--error); }
            
            /* Document Section */
            .doc-section { margin-bottom: 40px; border: 1px solid var(--divider); border-radius: 8px; padding: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
            .doc-header { display: flex; align-items: center; margin-bottom: 16px; border-bottom: 2px solid var(--primary); padding-bottom: 8px; }
            .doc-type { font-size: 18px; font-weight: 700; color: var(--primary); margin: 0; flex-grow: 1; word-break: break-word; }
            
            /* Portrait */
            .portrait-wrapper { text-align: center; margin-bottom: 24px; background-color: #FAFAFA; padding: 16px; border-radius: 8px; }
            .portrait-img { height: 180px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); object-fit: contain; }
            
            /* Data Groups */
            .group-title { 
                font-size: 12px; 
                font-weight: 700; 
                text-transform: uppercase; 
                color: var(--text-secondary); 
                letter-spacing: 1px; 
                margin-bottom: 8px; 
                border-bottom: 1px solid var(--divider); 
                padding-bottom: 4px; 
                margin-top: 24px;
            }
            
            /* Namespace Header */
            .namespace-header {
                background-color: var(--primary-light);
                color: var(--primary);
                font-size: 13px;
                font-weight: 600;
                padding: 6px 12px;
                border-radius: 4px;
                margin-top: 16px;
                margin-bottom: 8px;
                font-family: monospace; /* Monospace usually looks better for technical namespaces */
            }
            
            /* Key-Value List */
            .data-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #F5F5F5; }
            .data-row:last-child { border-bottom: none; }
            .key { color: var(--text-secondary); font-size: 13px; flex: 1; padding-right: 8px; }
            .value { font-weight: 500; text-align: right; color: var(--text-main); font-size: 13px; flex: 1; word-wrap: break-word; }
            
            /* Status Indicators */
            .check-item { display: flex; align-items: center; margin-bottom: 6px; font-size: 13px; }
            .check-icon { margin-right: 8px; font-weight: bold; width: 20px; text-align: center; }
            .check-success { color: var(--success); }
            .check-error { color: var(--error); }
            
            /* Error Box */
            .error-container { background-color: #FFEBEE; padding: 12px; border-radius: 4px; margin-top: 16px; }
            .error-title { color: var(--error); font-weight: bold; font-size: 13px; margin-bottom: 4px; display: block; }
            .error-msg { font-size: 12px; color: #B71C1C; display: block; margin-bottom: 2px; }
        </style>
    """.trimIndent()

        val sb = StringBuilder()
        sb.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>$css</head><body>")
        sb.append("<div class='report-container'>")

        sb.append("<div class='main-header'>")
        sb.append("<h1 class='report-title'>Verification Report</h1>")

        val (statusClass, statusText) = when (this.status) {
            VerificationStatus.SUCCESS -> "status-success" to "PASSED"
            VerificationStatus.PARTIAL_SUCCESS -> "status-warning" to "PARTIAL SUCCESS"
            VerificationStatus.FAILURE -> "status-failure" to "FAILED"
        }
        sb.append("<div class='report-status $statusClass'>$statusText</div>")
        sb.append("</div>")

        if (this.documents.isEmpty()) {
            sb.append("<p style='text-align:center; color:#757575;'>No documents processed.</p>")
        }

        this.documents.forEach { doc ->
            sb.append("<div class='doc-section'>")
            sb.append("<div class='doc-header'>")
            val displayDocType = doc.docType.replace("org.iso.18013.5.1.", "")
            sb.append("<h2 class='doc-type'>$displayDocType</h2>")
            sb.append("</div>")

            val portraitObj = doc.nameSpaces.firstNotNullOfOrNull { it.attributes["portrait"] }
            val portraitBitmap = when (portraitObj) {
                is ByteArray -> BitmapFactory.decodeByteArray(portraitObj, 0, portraitObj.size)
                is Bitmap -> portraitObj
                else -> null
            }?.scaleToFitHeight(300)

            if (portraitBitmap != null) {
                val base64 = portraitBitmap.toBase64String()
                sb.append("<div class='portrait-wrapper'><img src='$base64' class='portrait-img'/></div>")
            }

            // 3. Security & Trust Section
            val auth = doc.authentication
            sb.append("<div class='group-title'>SECURITY & TRUST</div>")

            sb.append(renderSimpleCheck("Issuer Signature", auth.securityChecks.isIssuerSignedValid))
            sb.append(renderSimpleCheck("Device Signature", auth.securityChecks.isDeviceSignedValid))
            sb.append(renderSimpleCheck("Data Integrity", auth.securityChecks.areDigestsValid))

            val isTrusted = auth.trustAttributes.chainStatus == TrustStatus.VERIFIED
            sb.append(renderSimpleCheck("Root of Trust (${auth.trustAttributes.chainStatus})", isTrusted))

            if (auth.trustAttributes.issuerDistinguishedName != null) {
                sb.append("<div class='data-row' style='margin-top:8px;'>")
                sb.append("<span class='key'>Issuer DN</span>")
                sb.append("<span class='value' style='font-size:11px;'>${auth.trustAttributes.issuerDistinguishedName}</span>")
                sb.append("</div>")
            }

            // 4. Validity Section
            sb.append("<div class='group-title'>VALIDITY PERIOD</div>")
            sb.append(renderDataRow("Valid From", auth.msoValidity.validFrom.toHtmlDate()))

            val now = System.currentTimeMillis()
            val isExpired = (auth.msoValidity.validUntil != null && auth.msoValidity.validUntil!! < now)
            val expiryVal = auth.msoValidity.validUntil.toHtmlDate()
            val expiryHtml = if (isExpired) "<span style='color:#D32F2F; font-weight:bold;'>$expiryVal (Expired)</span>" else expiryVal

            sb.append(renderDataRow("Valid Until", expiryHtml))
            sb.append(renderDataRow("MSO Status", auth.msoValidity.status.name))

            // 5. Identity Data (Per Namespace)
            sb.append("<div class='group-title'>IDENTITY DATA</div>")

            if (doc.nameSpaces.isEmpty()) {
                sb.append("<p style='font-style:italic; color:#757575; font-size:13px;'>No namespaces found.</p>")
            } else {
                doc.nameSpaces.forEach { ns ->
                    sb.append("<div class='namespace-header'>${ns.name}</div>")

                    if (ns.attributes.isEmpty()) {
                        sb.append("<p style='font-style:italic; color:#9E9E9E; font-size:12px; margin:4px 0;'>Empty Namespace</p>")
                    } else {
                        ns.attributes.toSortedMap().forEach { (key, value) ->
                            if (key != "portrait") {
                                val prettyKey = key.replace("_", " ")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                                val displayValue = when(value) {
                                    is ByteArray -> "[Binary Data (${value.size} bytes)]"
                                    is Bitmap -> "[Bitmap Image]"
                                    else -> value.toString()
                                }
                                sb.append(renderDataRow(prettyKey, displayValue))
                            }
                        }
                    }
                }
            }

            // 6. Validation Errors
            if (auth.validationErrors.isNotEmpty()) {
                sb.append("<div class='error-container'>")
                sb.append("<span class='error-title'>VALIDATION ERRORS</span>")
                auth.validationErrors.forEach { error ->
                    sb.append("<span class='error-msg'>• $error</span>")
                }
                sb.append("</div>")
            }

            sb.append("</div>")
        }

        sb.append("</div></body></html>")
        return sb.toString()
    }

    private fun renderSimpleCheck(label: String, isValid: Boolean): String {
        val colorClass = if (isValid) "check-success" else "check-error"
        val icon = if (isValid) "&#10003;" else "&#10007;" // Checkmark or X
        return """
        <div class='check-item $colorClass'>
            <span class='check-icon'>$icon</span>
            <span>$label</span>
        </div>
    """.trimIndent()
    }

    private fun renderDataRow(key: String, valueHtml: String): String {
        return """
        <div class='data-row'>
            <span class='key'>$key</span>
            <span class='value'>$valueHtml</span>
        </div>
    """.trimIndent()
    }

    private fun Bitmap.scaleToFitHeight(targetHeight: Int = 300): Bitmap {
        if (this.height <= targetHeight) return this
        val aspectRatio = this.width.toDouble() / this.height.toDouble()
        val targetWidth = (targetHeight * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun Bitmap.toBase64String(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun Long?.toHtmlDate(): String {
        if (this == null) return "N/A"
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply { timeZone = TimeZone.getDefault() }
            sdf.format(Date(this))
        } catch (e: Exception) {
            this.toString()
        }
    }
}
