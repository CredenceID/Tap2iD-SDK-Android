package com.credenceid.sample.utils

import com.credenceid.tap2idSdk.core.model.PDF417Verdict
import com.credenceid.tap2idSdk.core.model.Pdf417Attestations
import com.credenceid.tap2idSdk.core.model.Pdf417CryptoVerification
import com.credenceid.tap2idSdk.core.model.Pdf417VerificationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the "Document Expiry" row of [VerificationReportGenerator] (CIE-6915).
 *
 * Since SDK 2.2.1, `documentExpired` is `null` when the VwC profile does not apply the expiry
 * check. The report used to show "Not expired" for every document, which claimed a check that
 * never ran: the bug QA reported. Each of the three states must render its own label.
 */
class VerificationReportDocumentExpiryTest {

    private fun reportFor(documentExpired: Boolean?): String =
        VerificationReportGenerator.generateHtml(
            Pdf417VerificationResult(
                verdict = PDF417Verdict.REAL,
                confidenceLevel = 92,
                stateCode = "CA",
                fields = emptyMap(),
                errors = emptyList(),
                crypto = Pdf417CryptoVerification.NONE,
                warnings = emptyList(),
                attestations = Pdf417Attestations(documentExpired = documentExpired),
            ),
        )

    /**
     * The "Document Expiry" row alone. Other rows share the same labels ("Crypto Verification" also
     * renders "Not checked"), so assertions on the whole page would pass or fail for the wrong row.
     */
    private fun expiryRow(html: String): String {
        val start = html.indexOf("Document Expiry")
        check(start >= 0) { "the report has no Document Expiry row" }
        return html.substring(start, html.indexOf("</div>", start))
    }

    @Test
    fun `an expiry check that was not applied is reported as not checked`() {
        // Arrange
        val documentExpired: Boolean? = null

        // Act
        val row = expiryRow(reportFor(documentExpired))

        // Assert
        assertTrue(row.contains(">Not checked</span>"))
        assertFalse(row.contains(">Not expired</span>"))
        assertFalse(row.contains(">Expired</span>"))
    }

    @Test
    fun `an expired document is reported as expired`() {
        // Act
        val row = expiryRow(reportFor(documentExpired = true))

        // Assert
        assertTrue(row.contains(">Expired</span>"))
        assertFalse(row.contains(">Not checked</span>"))
    }

    @Test
    fun `a valid document is reported as not expired`() {
        // Act
        val row = expiryRow(reportFor(documentExpired = false))

        // Assert
        assertTrue(row.contains(">Not expired</span>"))
        assertFalse(row.contains(">Not checked</span>"))
    }
}
