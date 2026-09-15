package com.credenceid.sample.utils

import com.credenceid.tap2idSdk.core.model.PDF417ClassifierCode
import com.credenceid.tap2idSdk.core.model.PDF417Verdict
import com.credenceid.tap2idSdk.core.model.Pdf417Attestations
import com.credenceid.tap2idSdk.core.model.Pdf417CryptoVerification
import com.credenceid.tap2idSdk.core.model.Pdf417VerificationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the HTML escaping applied to document-derived text in [VerificationReportGenerator].
 *
 * Every value under test originates in the scanned document, so it is untrusted input rendered
 * into a WebView. Exercised through the PDF417 path because it is constructible on the JVM; the
 * mDoc path escapes the same values through the same helper.
 */
class VerificationReportEscapingTest {

    private fun result(
        stateCode: String? = "CA",
        fields: Map<String, Any?> = emptyMap(),
        errors: List<PDF417ClassifierCode> = emptyList(),
        warnings: List<PDF417ClassifierCode> = emptyList(),
        crypto: Pdf417CryptoVerification = Pdf417CryptoVerification.NONE,
        attestations: Pdf417Attestations = Pdf417Attestations(),
    ) = Pdf417VerificationResult(
        verdict = PDF417Verdict.REAL,
        confidenceLevel = 92,
        stateCode = stateCode,
        fields = fields,
        errors = errors,
        crypto = crypto,
        warnings = warnings,
        attestations = attestations,
    )

    @Test
    fun `markup in a field value cannot inject script into the report`() {
        // Arrange - a crafted barcode carrying markup in a name field.
        val payload = "<script>alert('pwned')</script>"

        // Act
        val html = VerificationReportGenerator.generateHtml(result(fields = mapOf("family_name" to payload)))

        // Assert
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&#39;"))
    }

    @Test
    fun `markup in a field name is escaped too`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(fields = mapOf("<img src=x onerror=alert(1)>" to "VALUE"))
        )

        // Assert
        assertFalse(html.contains("<img src=x"))
        assertTrue(html.contains("&lt;img src=x"))
    }

    @Test
    fun `an ampersand is escaped once, not doubly`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(fields = mapOf("resident_address" to "1 A & B Road"))
        )

        // Assert
        assertTrue(html.contains("1 A &amp; B Road"))
        assertFalse(html.contains("&amp;amp;"))
    }

    @Test
    fun `markup in a classifier error message is escaped`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(errors = listOf(PDF417ClassifierCode(1001, "missing <header>")))
        )

        // Assert
        assertTrue(html.contains("missing &lt;header&gt;"))
    }

    @Test
    fun `markup in a classifier warning message is escaped`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(warnings = listOf(PDF417ClassifierCode(5001, "version <unseen>")))
        )

        // Assert
        assertTrue(html.contains("version &lt;unseen&gt;"))
    }

    @Test
    fun `markup in the state code is escaped`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(result(stateCode = "<b>CA</b>"))

        // Assert
        assertFalse(html.contains("<b>CA</b>"))
        assertTrue(html.contains("&lt;b&gt;CA&lt;/b&gt;"))
    }

    @Test
    fun `markup in the crypto authority is escaped`() {
        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(
                crypto = Pdf417CryptoVerification(
                    authority = "<i>CA_DMV</i>",
                    checked = true,
                    verified = true,
                    detail = "",
                )
            )
        )

        // Assert
        assertFalse(html.contains("<i>CA_DMV</i>"))
        assertTrue(html.contains("&lt;i&gt;CA_DMV&lt;/i&gt;"))
    }

    @Test
    fun `the report's own markup is still rendered, not escaped along with the data`() {
        // Arrange - the expiry row is deliberately given real markup by the generator.
        // Escaping in the wrong place would turn it into visible angle brackets.

        // Act
        val html = VerificationReportGenerator.generateHtml(
            result(attestations = Pdf417Attestations(documentExpired = true))
        )

        // Assert
        assertTrue(html.contains("<span style='color:#D32F2F; font-weight:bold;'>Expired</span>"))
        assertFalse(html.contains("&lt;span style="))
    }
}
