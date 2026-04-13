package com.credenceid.sample.utils

import com.credenceid.tap2idSdk.api.models.PDF417VerificationResult
import com.credenceid.tap2idSdk.api.models.PDF417Verdict

object PDF417ReportGenerator {

    fun generateHtml(result: PDF417VerificationResult): String {
        val css = """
        <style>
            :root {
                --primary: #6200EE;
                --primary-light: #EDE7F6;
                --success: #2E7D32;
                --error: #D32F2F;
                --warning: #F57C00;
                --grey: #757575;
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
            .main-header { text-align: center; margin-bottom: 32px; padding-bottom: 16px; border-bottom: 2px solid var(--divider); }
            .report-title { margin: 0 0 16px; font-size: 24px; font-weight: 700; color: var(--text-main); }
            .verdict-badge {
                display: inline-block;
                padding: 8px 24px;
                border-radius: 24px;
                font-size: 18px;
                font-weight: 700;
                letter-spacing: 1px;
                color: #FFFFFF;
            }
            .verdict-real    { background-color: var(--success); }
            .verdict-fake    { background-color: var(--error); }
            .verdict-suspicious { background-color: var(--warning); }
            .verdict-error   { background-color: var(--grey); }
            .section { margin-bottom: 24px; border: 1px solid var(--divider); border-radius: 8px; padding: 16px; }
            .group-title {
                font-size: 12px;
                font-weight: 700;
                text-transform: uppercase;
                color: var(--text-secondary);
                letter-spacing: 1px;
                margin-bottom: 8px;
                border-bottom: 1px solid var(--divider);
                padding-bottom: 4px;
                margin-top: 16px;
            }
            .group-title:first-child { margin-top: 0; }
            .data-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #F5F5F5; }
            .data-row:last-child { border-bottom: none; }
            .key { color: var(--text-secondary); font-size: 13px; flex: 1; padding-right: 8px; }
            .value { font-weight: 500; text-align: right; color: var(--text-main); font-size: 13px; flex: 1; word-wrap: break-word; }
            .confidence-wrap { margin: 12px 0 4px; }
            .confidence-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
            .confidence-bar { background-color: #E0E0E0; border-radius: 4px; height: 8px; overflow: hidden; }
            .confidence-fill { height: 100%; border-radius: 4px; }
            .fill-real    { background-color: var(--success); }
            .fill-fake    { background-color: var(--error); }
            .fill-suspicious { background-color: var(--warning); }
            .fill-error   { background-color: var(--grey); }
            .check-item { display: flex; align-items: center; margin-bottom: 6px; font-size: 13px; }
            .check-icon { margin-right: 8px; font-weight: bold; width: 20px; text-align: center; }
            .check-success { color: var(--success); }
            .check-error { color: var(--error); }
            .check-neutral { color: var(--grey); }
            .error-container { background-color: #FFEBEE; padding: 12px; border-radius: 4px; margin-top: 16px; }
            .error-title { color: var(--error); font-weight: bold; font-size: 13px; margin-bottom: 4px; display: block; }
            .error-msg { font-size: 12px; color: #B71C1C; display: block; margin-bottom: 2px; }
            .tag-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
            .tag { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
            .tag-verified { background-color: #E8F5E9; color: var(--success); }
            .tag-unverified { background-color: #F5F5F5; color: var(--grey); }
            .expired-value { color: var(--error); font-weight: bold; }
        </style>
        """.trimIndent()

        val sb = StringBuilder()
        sb.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>$css</head><body>")
        sb.append("<div class='report-container'>")

        // Header
        val (verdictClass, verdictLabel) = when (result.verdict) {
            PDF417Verdict.REAL       -> "verdict-real" to "REAL"
            PDF417Verdict.FAKE       -> "verdict-fake" to "FAKE"
            PDF417Verdict.SUSPICIOUS -> "verdict-suspicious" to "SUSPICIOUS"
            PDF417Verdict.ERROR      -> "verdict-error" to "ERROR"
        }
        val fillClass = when (result.verdict) {
            PDF417Verdict.REAL       -> "fill-real"
            PDF417Verdict.FAKE       -> "fill-fake"
            PDF417Verdict.SUSPICIOUS -> "fill-suspicious"
            PDF417Verdict.ERROR      -> "fill-error"
        }

        sb.append("<div class='main-header'>")
        sb.append("<h1 class='report-title'>DL Barcode Verification</h1>")
        sb.append("<div><span class='verdict-badge $verdictClass'>$verdictLabel</span></div>")
        sb.append("<div class='confidence-wrap'>")
        sb.append("<div class='confidence-label'>Confidence: ${result.confidenceLevel}%</div>")
        sb.append("<div class='confidence-bar'><div class='confidence-fill $fillClass' style='width:${result.confidenceLevel}%'></div></div>")
        sb.append("</div>")
        sb.append("</div>")

        // Classification details
        sb.append("<div class='section'>")
        sb.append("<div class='group-title'>CLASSIFICATION</div>")
        sb.append(renderDataRow("State", result.stateCode ?: "Unknown"))

        sb.append("</div>")

        // Identity fields
        sb.append("<div class='section'>")
        sb.append("<div class='group-title'>IDENTITY</div>")
        val firstName = result.fields["DAC"]?.toString()
        val lastName  = result.fields["DCS"]?.toString()
        val fullName  = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { "N/A" }
        sb.append(renderDataRow("Name", fullName))
        sb.append(renderDataRow("License Number", result.fields["DAQ"]?.toString() ?: "N/A"))
        sb.append(renderDataRow("Date of Birth", result.fields["DBB"]?.toString() ?: "N/A"))

        val expiry = result.fields["DBA"]?.toString()
        val expiryHtml = run {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMddyyyy")
            val expiryDate = expiry?.let {
                runCatching { java.time.LocalDate.parse(it, formatter) }.getOrNull()
            }
            when {
                expiryDate == null -> expiry ?: "N/A"
                expiryDate.isBefore(java.time.LocalDate.now()) ->
                    "<span class='expired-value'>$expiry (Expired)</span>"
                else -> expiry
            }
        }
        sb.append(renderDataRow("Expiry Date", expiryHtml))
        sb.append(renderDataRow("Issue Date", result.fields["DBD"]?.toString() ?: "N/A"))

        val dbc = result.fields["DBC"]?.toString()
        val sexLabel = when (dbc) {
            "1" -> "Male"
            "2" -> "Female"
            else -> dbc ?: "N/A"
        }
        sb.append(renderDataRow("Sex", sexLabel))
        sb.append(renderDataRow("Height", result.fields["DAU"]?.toString() ?: "N/A"))
        sb.append(renderDataRow("Eye Color", result.fields["DAY"]?.toString() ?: "N/A"))

        sb.append("<div class='group-title'>ADDRESS</div>")
        sb.append(renderDataRow("Street", result.fields["DAG"]?.toString() ?: "N/A"))
        sb.append(renderDataRow("City", result.fields["DAI"]?.toString() ?: "N/A"))
        sb.append(renderDataRow("Postal Code", result.fields["DAK"]?.toString() ?: "N/A"))
        sb.append("</div>")

        // Errors
        if (result.errors.isNotEmpty()) {
            sb.append("<div class='error-container'>")
            sb.append("<span class='error-title'>VALIDATION ERRORS</span>")
            result.errors.forEach { error ->
                sb.append("<span class='error-msg'>• [${error.code}] ${error.message}</span>")
            }
            sb.append("</div>")
        }

        sb.append("</div></body></html>")
        return sb.toString()
    }

    private fun renderDataRow(key: String, valueHtml: String): String = """
        <div class='data-row'>
            <span class='key'>$key</span>
            <span class='value'>$valueHtml</span>
        </div>
    """.trimIndent()

    private fun renderTagCheck(label: String, isVerified: Boolean): String {
        val tagClass = if (isVerified) "tag-verified" else "tag-unverified"
        val tagText  = if (isVerified) "Verified" else "Not Present"
        val iconClass = if (isVerified) "check-success" else "check-neutral"
        val icon = if (isVerified) "&#10003;" else "&#8212;"
        return """
        <div class='check-item $iconClass'>
            <span class='check-icon'>$icon</span>
            <span>$label &nbsp;<span class='tag $tagClass'>$tagText</span></span>
        </div>
        """.trimIndent()
    }
}