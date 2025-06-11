package io.openweb3.sample

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.openweb3.sample", appContext.packageName)
    }


    fun decodeUrlFormat(encodedUrl: String): String {
        // 替换自定义编码分隔符
        var decoded = encodedUrl
            .replace("__", "=")
            .replace("--", "%")

        // URL解码
        return URLDecoder.decode(decoded, "UTF-8")
    }

    fun encodeToCustomFormat(standardUrl: String): String {
        // URL编码
        val encoded = URLEncoder.encode(standardUrl, "UTF-8")

        // 替换标准分隔符为自定义分隔符
        return encoded
            .replace("=", "__")
            .replace("%", "--")
    }

    @Test
    fun decodeUrl() {
        val encoded = "tonconnect-v__2-id__2bb340e1ef6bec99d50e5c934fd016276838c14d7620a798248a7341fd826e13-r__--7B--22manifestUrl--22--3A--22https--3A--2F--2Fapp--2Eston--2Efi--2Ftonconnect--2Dmanifest--2Ejson--22--2C--22items--22--3A--5B--7B--22name--22--3A--22ton--5Faddr--22--7D--5D--7D-ret__back"
        val decoded = decodeUrlFormat(encoded)
        println(decoded)
    }
}