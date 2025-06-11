

package io.miniapp.core.openplatform.common.network.ssl

import io.miniapp.core.openplatform.miniapp.utils.LogTimber
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Various utility classes for dealing with X509Certificates.
 */
internal object CertUtil {

    // Set to false to do some test
    private const val USE_DEFAULT_HOSTNAME_VERIFIER = true

    private val hexArray = "0123456789ABCDEF".toCharArray()

    /**
     * Generates the SHA-256 fingerprint of the given certificate.
     *
     * @param cert the certificate.
     * @return the finger print
     * @throws CertificateException the certificate exception
     */
    @Throws(CertificateException::class)
    fun generateSha256Fingerprint(cert: X509Certificate): ByteArray {
        return generateFingerprint(cert, "SHA-256")
    }

    /**
     * Generates the SHA-1 fingerprint of the given certificate.
     *
     * @param cert the certificated
     * @return the SHA1 fingerprint
     * @throws CertificateException the certificate exception
     */
    @Throws(CertificateException::class)
    fun generateSha1Fingerprint(cert: X509Certificate): ByteArray {
        return generateFingerprint(cert, "SHA-1")
    }

    /**
     * Generate the fingerprint for a dedicated type.
     *
     * @param cert the certificate
     * @param type the type
     * @return the fingerprint
     * @throws CertificateException certificate exception
     */
    @Throws(CertificateException::class)
    private fun generateFingerprint(cert: X509Certificate, type: String): ByteArray {
        val fingerprint: ByteArray
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance(type)
        } catch (e: Exception) {
            // This really *really* shouldn't throw, as java should always have a SHA-256 and SHA-1 impl.
            throw CertificateException(e)
        }

        fingerprint = md.digest(cert.encoded)

        return fingerprint
    }

    /**
     * Convert the fingerprint to an hexa string.
     *
     * @param fingerprint the fingerprint
     * @param sep the separator character, default to space
     * @return the hexa string.
     */
    fun fingerprintToHexString(fingerprint: ByteArray, sep: Char = ' '): String {
        val hexChars = CharArray(fingerprint.size * 3)
        for (j in fingerprint.indices) {
            val v = (fingerprint[j].toInt() and 0xFF)
            hexChars[j * 3] = hexArray[v.ushr(4)]
            hexChars[j * 3 + 1] = hexArray[v and 0x0F]
            hexChars[j * 3 + 2] = sep
        }
        return String(hexChars, 0, hexChars.size - 1)
    }

    /**
     * Recursively checks the exception to see if it was caused by an
     * UnrecognizedCertificateException.
     *
     * @param root the throwable.
     * @return The UnrecognizedCertificateException if exists, else null.
     */
    fun getCertificateException(root: Throwable?): UnrecognizedCertificateException? {
        var e = root
        var i = 0 // Just in case there is a getCause loop
        while (e != null && i < 10) {
            if (e is UnrecognizedCertificateException) {
                return e
            }
            e = e.cause
            i++
        }

        return null
    }

    internal data class PinnedSSLSocketFactory(
            val sslSocketFactory: SSLSocketFactory,
            val x509TrustManager: X509TrustManager
    )

    /**
     * Create a SSLSocket factory for a HS config.
     *
     * @param hsConfig the HS config.
     * @return SSLSocket factory
     */
    fun newPinnedSSLSocketFactory(): PinnedSSLSocketFactory {
        try {
            var defaultTrustManager: X509TrustManager? = null

            val shouldPin = false

            // If we haven't specified that we wanted to shouldPin the certs, fallback to standard
            // X509 checks if fingerprints don't match.
            if (!shouldPin) {
                var tf: TrustManagerFactory? = null

                // get the PKIX instance
                try {
                    tf = TrustManagerFactory.getInstance("PKIX")
                } catch (e: Exception) {
                    LogTimber.e(e, "## newPinnedSSLSocketFactory() : TrustManagerFactory.getInstance failed")
                }

                // it doesn't exist, use the default one.
                if (null == tf) {
                    try {
                        tf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    } catch (e: Exception) {
                        LogTimber.e(e, "## newPinnedSSLSocketFactory() : TrustManagerFactory.getInstance of default failed")
                    }
                }

                tf!!.init(null as KeyStore?)
                val trustManagers = tf.trustManagers

                for (i in trustManagers.indices) {
                    if (trustManagers[i] is X509TrustManager) {
                        defaultTrustManager = trustManagers[i] as X509TrustManager
                        break
                    }
                }
            }

            val trustPinned = arrayOf<TrustManager>(
                PinnedTrustManagerProvider.provide(
                    emptyList(),
                    defaultTrustManager
                )
            )

            val sslSocketFactory =  {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustPinned, java.security.SecureRandom())
                sslContext.socketFactory
            }

            return PinnedSSLSocketFactory(sslSocketFactory.invoke(), defaultTrustManager!!)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
