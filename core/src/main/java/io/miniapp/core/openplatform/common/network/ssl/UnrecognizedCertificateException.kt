

package io.miniapp.core.openplatform.common.network.ssl

import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Thrown when we are given a certificate that does match the certificate we were told to
 * expect.
 */
internal data class UnrecognizedCertificateException(
    val certificate: X509Certificate,
    val fingerprint: Fingerprint,
    override val cause: Throwable?
) : CertificateException("Unrecognized certificate with unknown fingerprint: " + certificate.subjectDN, cause)
