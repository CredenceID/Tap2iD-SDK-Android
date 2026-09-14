package com.credenceid.sample.utils

import com.credenceid.tap2idSdk.core.model.PDF417ClassifierCode
import com.credenceid.tap2idSdk.core.model.PDF417Verdict
import com.credenceid.tap2idSdk.core.model.Pdf417CryptoVerification
import com.credenceid.tap2idSdk.core.model.Pdf417VerificationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [VerificationReportGenerator.generateHtml] on PDF417 results.
 *
 * These run on the local JVM (no device) because the PDF417 report path touches no Android
 * framework classes as long as the `fields` map carries no `portrait` entry (portrait rendering
 * is the only branch that uses Bitmap/Base64).
 */
class Pdf417ReportGeneratorTest {

    @Test
    fun realResult_rendersVerdictConfidenceAndFields() {
        val result = Pdf417VerificationResult(
            verdict = PDF417Verdict.REAL,
            confidenceLevel = 92,
            stateCode = "CA",
            fields = mapOf(
                "given_name" to "ALEX",
                "family_name" to "SAMPLE",
                "document_number" to "D1234567",
            ),
            errors = emptyList(),
            crypto = Pdf417CryptoVerification(authority = "CA_DMV", checked = true, verified = true, detail = ""),
            warnings = emptyList(),
        )

        val html = VerificationReportGenerator.generateHtml(result)

        assertTrue("title present", html.contains("PDF417 Verification Report"))
        assertTrue("verdict shown", html.contains("REAL"))
        assertTrue("success styling", html.contains("status-success"))
        assertTrue("confidence shown", html.contains("92 / 100"))
        assertTrue("state shown", html.contains("CA"))
        assertTrue("identity field value", html.contains("ALEX"))
        assertTrue("identity field value", html.contains("D1234567"))
        assertFalse("no validation-errors block", html.contains("VALIDATION ERRORS"))
        assertFalse("no warnings block", html.contains("WARNINGS"))
    }

    @Test
    fun errorResult_rendersErrorVerdictAndValidationErrors() {
        val result = Pdf417VerificationResult(
            verdict = PDF417Verdict.ERROR,
            confidenceLevel = 0,
            stateCode = null,
            fields = emptyMap(),
            errors = listOf(
                PDF417ClassifierCode(1002, "No VwC profile is configured."),
            ),
            crypto = Pdf417CryptoVerification(authority = "", checked = false, verified = false, detail = ""),
            warnings = emptyList(),
        )

        val html = VerificationReportGenerator.generateHtml(result)

        assertTrue("error verdict shown", html.contains("ERROR"))
        assertTrue("failure styling", html.contains("status-failure"))
        assertTrue("no-fields message", html.contains("No fields returned."))
        assertTrue("validation-errors block", html.contains("VALIDATION ERRORS"))
        assertTrue("error code shown", html.contains("1002"))
        assertTrue("error message shown", html.contains("No VwC profile is configured."))
        assertFalse("no warnings block", html.contains("WARNINGS"))
    }

    @Test
    fun suspiciousResult_usesWarningStyling() {
        val result = Pdf417VerificationResult(
            verdict = PDF417Verdict.SUSPICIOUS,
            confidenceLevel = 55,
            stateCode = "NY",
            fields = mapOf("birth_date" to "1990-01-01"),
            errors = emptyList(),
            crypto = Pdf417CryptoVerification(authority = "NY_DMV", checked = true, verified = true, detail = ""),
            warnings = emptyList(),
        )

        val html = VerificationReportGenerator.generateHtml(result)

        assertTrue("suspicious verdict shown", html.contains("SUSPICIOUS"))
        assertTrue("warning styling", html.contains("status-warning"))
    }

    @Test
    fun warningsRenderedInOrangeBlock() {
        val result = Pdf417VerificationResult(
            verdict = PDF417Verdict.REAL,
            confidenceLevel = 80,
            stateCode = "TX",
            fields = emptyMap(),
            errors = emptyList(),
            crypto = Pdf417CryptoVerification(authority = "", checked = false, verified = false, detail = ""),
            warnings = listOf(
                PDF417ClassifierCode(3001, "Address format non-standard."),
            ),
        )

        val html = VerificationReportGenerator.generateHtml(result)

        assertTrue("warnings block present", html.contains("WARNINGS"))
        assertTrue("warning code shown", html.contains("3001"))
        assertTrue("warning message shown", html.contains("Address format non-standard."))
        assertTrue("warning styling", html.contains("warning-container"))
        assertFalse("no errors block", html.contains("VALIDATION ERRORS"))
    }
}
