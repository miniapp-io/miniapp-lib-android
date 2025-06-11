

package io.miniapp.core.openplatform.common.network.ssl

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Implements a TrustManager that checks Certificates against an explicit list of known
 * fingerprints.
 *
 * @property fingerprints Not empty array of SHA256 cert fingerprints
 * @property defaultTrustManager Optional trust manager to fall back on if cert does not match
 * any of the fingerprints. Can be null.
 */
internal class PinnedTrustManager(
    private val fingerprints: List<Fingerprint>,
    private val defaultTrustManager: X509TrustManager?
) : X509TrustManager {

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, s: String) {
        try {
            if (defaultTrustManager != null) {
                defaultTrustManager.checkClientTrusted(chain, s)
                return
            }
        } catch (e: CertificateException) {
            // If there is an exception we fall back to checking fingerprints
            if (fingerprints.isEmpty()) {
                throw UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.cause)
            }
        }

        checkTrusted(chain)
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, s: String) {
        try {
            if (defaultTrustManager != null) {
                defaultTrustManager.checkServerTrusted(chain, s)
                return
            }
        } catch (e: CertificateException) {
            // If there is an exception we fall back to checking fingerprints
            if (fingerprints.isEmpty()) {
                throw UnrecognizedCertificateException(chain[0], Fingerprint.newSha256Fingerprint(chain[0]), e.cause /* BMA: Shouldn't be `e` ? */)
            }
        }

        checkTrusted(chain)
    }

    @Throws(CertificateException::class)
    private fun checkTrusted(chain: Array<X509Certificate>) {
        val cert = chain[0]

        if (!fingerprints.any { it.matchesCert(cert) }) {
            throw UnrecognizedCertificateException(cert, Fingerprint.newSha256Fingerprint(cert), null)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager?.acceptedIssuers ?: emptyArray()
    }
}
