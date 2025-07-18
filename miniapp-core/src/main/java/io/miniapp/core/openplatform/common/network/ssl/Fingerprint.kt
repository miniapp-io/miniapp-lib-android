

package io.miniapp.core.openplatform.common.network.ssl

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@JsonClass(generateAdapter = true)
internal data class Fingerprint(
        val bytes: ByteArray,
        val hashType: HashType
) {

    val displayableHexRepr: String by lazy {
        CertUtil.fingerprintToHexString(bytes)
    }

    @Throws(CertificateException::class)
    internal fun matchesCert(cert: X509Certificate): Boolean {
        val o: Fingerprint? = when (hashType) {
            HashType.SHA256 -> newSha256Fingerprint(cert)
            HashType.SHA1 -> newSha1Fingerprint(cert)
        }
        return equals(o)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fingerprint
        if (!bytes.contentEquals(other.bytes)) return false
        if (hashType != other.hashType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + hashType.hashCode()
        return result
    }

    internal companion object {

        @Throws(CertificateException::class)
        fun newSha256Fingerprint(cert: X509Certificate): Fingerprint {
            return Fingerprint(
                    CertUtil.generateSha256Fingerprint(cert),
                    HashType.SHA256
            )
        }

        @Throws(CertificateException::class)
        fun newSha1Fingerprint(cert: X509Certificate): Fingerprint {
            return Fingerprint(
                    CertUtil.generateSha1Fingerprint(cert),
                    HashType.SHA1
            )
        }
    }

    @JsonClass(generateAdapter = false)
    enum class HashType {
        @Json(name = "sha-1") SHA1,
        @Json(name = "sha-256") SHA256
    }
}
