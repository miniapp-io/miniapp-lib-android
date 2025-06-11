package io.miniapp.core.openplatform.miniapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import io.miniapp.core.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

internal object Utilities {
    var pattern = Pattern.compile("[\\-0-9]+")
    var random = SecureRandom()

    @Volatile
    var stageQueue: DispatchQueue = DispatchQueue("stageQueue")

    @Volatile
    var globalQueue: DispatchQueue = DispatchQueue("globalQueue")

    @Volatile
    var cacheClearQueue: DispatchQueue = DispatchQueue("cacheClearQueue")

    @Volatile
    var searchQueue: DispatchQueue = DispatchQueue("searchQueue")

    @Volatile
    var phoneBookQueue: DispatchQueue = DispatchQueue("phoneBookQueue")

    @Volatile
    var themeQueue: DispatchQueue = DispatchQueue("themeQueue")

    @Volatile
    var externalNetworkQueue: DispatchQueue = DispatchQueue("externalNetworkQueue")

    @Volatile
    var videoPlayerQueue: DispatchQueue? = null
    private const val RANDOM_STRING_CHARS =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    internal val hexArray = "0123456789ABCDEF".toCharArray()

    init {
        try {
            val URANDOM_FILE = File("/dev/urandom")
            val sUrandomIn = FileInputStream(URANDOM_FILE)
            val buffer = ByteArray(1024)
            sUrandomIn.read(buffer)
            sUrandomIn.close()
            random.setSeed(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    external fun pinBitmap(bitmap: Bitmap?): Int
    external fun unpinBitmap(bitmap: Bitmap?)
    external fun blurBitmap(
        bitmap: Any?,
        radius: Int,
        unpin: Int,
        width: Int,
        height: Int,
        stride: Int
    )

    external fun needInvert(bitmap: Any?, unpin: Int, width: Int, height: Int, stride: Int): Int
    external fun calcCDT(
        hsvBuffer: ByteBuffer?,
        width: Int,
        height: Int,
        buffer: ByteBuffer?,
        calcBuffer: ByteBuffer?
    )

    external fun loadWebpImage(
        bitmap: Bitmap?,
        buffer: ByteBuffer?,
        len: Int,
        options: BitmapFactory.Options?,
        unpin: Boolean
    ): Boolean

    external fun convertVideoFrame(
        src: ByteBuffer?,
        dest: ByteBuffer?,
        destFormat: Int,
        width: Int,
        height: Int,
        padding: Int,
        swap: Int
    ): Int

    private external fun aesIgeEncryption(
        buffer: ByteBuffer,
        key: ByteArray,
        iv: ByteArray,
        encrypt: Boolean,
        offset: Int,
        length: Int
    )

    private external fun aesIgeEncryptionByteArray(
        buffer: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        encrypt: Boolean,
        offset: Int,
        length: Int
    )

    external fun aesCtrDecryption(
        buffer: ByteBuffer?,
        key: ByteArray?,
        iv: ByteArray?,
        offset: Int,
        length: Int
    )

    external fun aesCtrDecryptionByteArray(
        buffer: ByteArray?,
        key: ByteArray?,
        iv: ByteArray?,
        offset: Int,
        length: Long,
        n: Int
    )

    private external fun aesCbcEncryptionByteArray(
        buffer: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        offset: Int,
        length: Int,
        n: Int,
        encrypt: Int
    )

    external fun aesCbcEncryption(
        buffer: ByteBuffer?,
        key: ByteArray?,
        iv: ByteArray?,
        offset: Int,
        length: Int,
        encrypt: Int
    )

    external fun readlink(path: String?): String?
    external fun readlinkFd(fd: Int): String?
    external fun getDirSize(path: String?, docType: Int, subdirs: Boolean): Long
    external fun getLastUsageFileTime(path: String?): Long
    external fun clearDir(path: String?, docType: Int, time: Long, subdirs: Boolean)
    private external fun pbkdf2(
        password: ByteArray,
        salt: ByteArray,
        dst: ByteArray,
        iterations: Int
    ): Int

    external fun stackBlurBitmap(bitmap: Bitmap?, radius: Int)
    external fun drawDitheredGradient(
        bitmap: Bitmap?,
        colors: IntArray?,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int
    )

    external fun saveProgressiveJpeg(
        bitmap: Bitmap?,
        width: Int,
        height: Int,
        stride: Int,
        quality: Int,
        path: String?
    ): Int

    external fun generateGradient(
        bitmap: Bitmap?,
        unpin: Boolean,
        phase: Int,
        progress: Float,
        width: Int,
        height: Int,
        stride: Int,
        colors: IntArray?
    )

    external fun setupNativeCrashesListener(path: String?)
    @JvmOverloads
    fun stackBlurBitmapMax(bitmap: Bitmap, round: Boolean = false): Bitmap {
        val w: Int = AndroidUtils.dp(20)
        val h = (AndroidUtils.dp(20) * bitmap.getHeight().toFloat() / bitmap.getWidth()).toInt()
        val scaledBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        canvas.save()
        canvas.scale(
            scaledBitmap.getWidth().toFloat() / bitmap.getWidth(),
            scaledBitmap.getHeight().toFloat() / bitmap.getHeight()
        )
        if (round) {
            val path = Path()
            path.addCircle(
                bitmap.getWidth() / 2f,
                bitmap.getHeight() / 2f,
                min(bitmap.getWidth(), bitmap.getHeight()) / 2f - 1,
                Path.Direction.CW
            )
            canvas.clipPath(path)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.restore()
        stackBlurBitmap(scaledBitmap, max(10, max(w, h) / 150))
        return scaledBitmap
    }

    fun stackBlurBitmapWithScaleFactor(bitmap: Bitmap, scaleFactor: Float): Bitmap {
        val w =
            Math.max(AndroidUtils.dp(20).toFloat(), bitmap.getWidth() / scaleFactor).toInt()
        val h = Math.max(
            AndroidUtils.dp(20) * bitmap.getHeight().toFloat() / bitmap.getWidth(),
            bitmap.getHeight() / scaleFactor
        )
            .toInt()
        val scaledBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        canvas.save()
        canvas.scale(
            scaledBitmap.getWidth().toFloat() / bitmap.getWidth(),
            scaledBitmap.getHeight().toFloat() / bitmap.getHeight()
        )
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.restore()
        stackBlurBitmap(scaledBitmap, max(10, max(w, h) / 150))
        return scaledBitmap
    }

    fun blurWallpaper(src: Bitmap?): Bitmap? {
        if (src == null) {
            return null
        }
        val b: Bitmap
        b = if (src.getHeight() > src.getWidth()) {
            Bitmap.createBitmap(
                Math.round(450f * src.getWidth() / src.getHeight()),
                450,
                Bitmap.Config.ARGB_8888
            )
        } else {
            Bitmap.createBitmap(
                450,
                Math.round(450f * src.getHeight() / src.getWidth()),
                Bitmap.Config.ARGB_8888
            )
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val rect = Rect(0, 0, b.getWidth(), b.getHeight())
        Canvas(b).drawBitmap(src, null, rect, paint)
        stackBlurBitmap(b, 12)
        return b
    }

    fun aesIgeEncryption(
        buffer: ByteBuffer,
        key: ByteArray,
        iv: ByteArray,
        encrypt: Boolean,
        changeIv: Boolean,
        offset: Int,
        length: Int
    ) {
        aesIgeEncryption(
            buffer,
            key,
            if (changeIv) iv else iv.clone(),
            encrypt,
            offset,
            length
        )
    }

    fun aesIgeEncryptionByteArray(
        buffer: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        encrypt: Boolean,
        changeIv: Boolean,
        offset: Int,
        length: Int
    ) {
        aesIgeEncryptionByteArray(
            buffer,
            key,
            if (changeIv) iv else iv.clone(),
            encrypt,
            offset,
            length
        )
    }

    fun aesCbcEncryptionByteArraySafe(
        buffer: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        offset: Int,
        length: Int,
        n: Int,
        encrypt: Int
    ) {
        aesCbcEncryptionByteArray(
            buffer,
            key,
            iv.clone(),
            offset,
            length,
            n,
            encrypt
        )
    }

    fun parseInt(value: CharSequence?): Int {
        if (value == null) {
            return 0
        }
        if (BuildConfig.BUILD_HOST_IS_WINDOWS) {
            val matcher: Matcher = pattern.matcher(value)
            if (matcher.find()) {
                return matcher.group().toInt()
            }
        } else {
            var `val` = 0
            try {
                var start = -1
                var end: Int
                end = 0
                while (end < value.length) {
                    val character = value[end]
                    val allowedChar = character == '-' || character >= '0' && character <= '9'
                    if (allowedChar && start < 0) {
                        start = end
                    } else if (!allowedChar && start >= 0) {
                        end++
                        break
                    }
                    ++end
                }
                if (start >= 0) {
                    val str = value.subSequence(start, end).toString()
                    //                val = parseInt(str);
                    `val` = str.toInt()
                }
            } catch (ignore: Exception) {
            }
            return `val`
        }
        return 0
    }

    private fun parseInt(s: String): Int {
        var num = 0
        var negative = true
        val len = s.length
        val ch = s[0]
        if (ch == '-') {
            negative = false
        } else {
            num = '0'.code - ch.code
        }
        var i = 1
        while (i < len) {
            num = num * 10 + '0'.code - s[i++].code
        }
        return if (negative) -num else num
    }

    fun parseLong(value: String?): Long {
        if (value == null) {
            return 0L
        }
        var `val` = 0L
        try {
            val matcher: Matcher = pattern.matcher(value)
            if (matcher.find()) {
                val num = matcher.group(0)
                if (num != null) {
                    `val` = num.toLong()
                }
            }
        } catch (ignore: Exception) {
        }
        return `val`
    }

    fun parseIntToString(value: String?): String? {
        val matcher: Matcher? = value?.let { pattern.matcher(it) }
        if (matcher != null) {
            return if (matcher.find()) {
                matcher.group(0)
            } else null
        } else {
            return null
        }
    }

    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null) {
            return ""
        }
        val hexChars = CharArray(bytes.size * 2)
        var v: Int
        for (j in bytes.indices) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray.get(v ushr 4)
            hexChars[j * 2 + 1] = hexArray.get(v and 0x0F)
        }
        return String(hexChars)
    }

    fun hexToBytes(hex: String?): ByteArray? {
        if (hex == null) {
            return null
        }
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                ((hex[i].digitToIntOrNull(16) ?: (-1 shl 4)) + hex[i + 1].digitToIntOrNull(16)!!).toByte()
            i += 2
        }
        return data
    }

    fun isGoodPrime(prime: ByteArray, g: Int): Boolean {
        if (!(g >= 2 && g <= 7)) {
            return false
        }
        if (prime.size != 256 || prime[0] >= 0) {
            return false
        }
        val dhBI = BigInteger(1, prime)
        if (g == 2) { // p mod 8 = 7 for g = 2;
            val res = dhBI.mod(BigInteger.valueOf(8))
            if (res.toInt() != 7) {
                return false
            }
        } else if (g == 3) { // p mod 3 = 2 for g = 3;
            val res = dhBI.mod(BigInteger.valueOf(3))
            if (res.toInt() != 2) {
                return false
            }
        } else if (g == 5) { // p mod 5 = 1 or 4 for g = 5;
            val res = dhBI.mod(BigInteger.valueOf(5))
            val `val` = res.toInt()
            if (`val` != 1 && `val` != 4) {
                return false
            }
        } else if (g == 6) { // p mod 24 = 19 or 23 for g = 6;
            val res = dhBI.mod(BigInteger.valueOf(24))
            val `val` = res.toInt()
            if (`val` != 19 && `val` != 23) {
                return false
            }
        } else if (g == 7) { // p mod 7 = 3, 5 or 6 for g = 7.
            val res = dhBI.mod(BigInteger.valueOf(7))
            val `val` = res.toInt()
            if (`val` != 3 && `val` != 5 && `val` != 6) {
                return false
            }
        }
        val hex: String = bytesToHex(prime)
        if (hex == "C71CAEB9C6B1C9048E6C522F70F13F73980D40238E3E21C14934D037563D930F48198A0AA7C14058229493D22530F4DBFA336F6E0AC925139543AED44CCE7C3720FD51F69458705AC68CD4FE6B6B13ABDC9746512969328454F18FAF8C595F642477FE96BB2A941D5BCD1D4AC8CC49880708FA9B378E3C4F3A9060BEE67CF9A4A4A695811051907E162753B56B0F6B410DBA74D8A84B2A14B3144E0EF1284754FD17ED950D5965B4B9DD46582DB1178D169C6BC465B0D6FF9CA3928FEF5B9AE4E418FC15E83EBEA0F87FA9FF5EED70050DED2849F47BF959D956850CE929851F0D8115F635B105EE2E4E15D04B2454BF6F4FADF034B10403119CD8E3B92FCC5B") {
            return true
        }
        val dhBI2 = dhBI.subtract(BigInteger.valueOf(1)).divide(BigInteger.valueOf(2))
        return !(!dhBI.isProbablePrime(30) || !dhBI2.isProbablePrime(30))
    }

    fun isGoodGaAndGb(g_a: BigInteger, p: BigInteger): Boolean {
        return !(g_a.compareTo(BigInteger.valueOf(1)) <= 0 || g_a.compareTo(
            p.subtract(
                BigInteger.valueOf(
                    1
                )
            )
        ) >= 0)
    }

    fun arraysEquals(arr1: ByteArray?, offset1: Int, arr2: ByteArray?, offset2: Int): Boolean {
        if (arr1 == null || arr2 == null || offset1 < 0 || offset2 < 0 || arr1.size - offset1 > arr2.size - offset2 || arr1.size - offset1 < 0 || arr2.size - offset2 < 0) {
            return false
        }
        var result = true
        for (a in offset1 until arr1.size) {
            if (arr1[a + offset1] != arr2[a + offset2]) {
                result = false
            }
        }
        return result
    }

    @JvmOverloads
    fun computeSHA1(convertme: ByteArray, offset: Int = 0, len: Int = convertme.size): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-1")
            md.update(convertme, offset, len)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(20)
    }

    @JvmOverloads
    fun computeSHA1(
        convertme: ByteBuffer,
        offset: Int = 0,
        len: Int = convertme.limit()
    ): ByteArray {
        val oldp = convertme.position()
        val oldl = convertme.limit()
        try {
            val md = MessageDigest.getInstance("SHA-1")
            convertme.position(offset)
            convertme.limit(len)
            md.update(convertme)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            convertme.limit(oldl)
            convertme.position(oldp)
        }
        return ByteArray(20)
    }

    @JvmOverloads
    fun computeSHA256(
        convertme: ByteArray,
        offset: Int = 0,
        len: Long = convertme.size.toLong()
    ): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(convertme, offset, len.toInt())
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(32)
    }

    fun computeSHA256(vararg args: ByteArray): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            for (a in args.indices) {
                md.update(args[a], 0, args[a].size)
            }
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(32)
    }

    fun computeSHA512(convertme: ByteArray): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(convertme, 0, convertme.size)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(64)
    }

    fun computeSHA512(convertme: ByteArray, convertme2: ByteArray): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(convertme, 0, convertme.size)
            md.update(convertme2, 0, convertme2.size)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(64)
    }

    fun computePBKDF2(password: ByteArray, salt: ByteArray): ByteArray {
        val dst = ByteArray(64)
        pbkdf2(password, salt, dst, 100000)
        return dst
    }

    fun computeSHA512(
        convertme: ByteArray,
        convertme2: ByteArray,
        convertme3: ByteArray
    ): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(convertme, 0, convertme.size)
            md.update(convertme2, 0, convertme2.size)
            md.update(convertme3, 0, convertme3.size)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ByteArray(64)
    }

    fun computeSHA256(
        b1: ByteArray?,
        o1: Int,
        l1: Int,
        b2: ByteBuffer,
        o2: Int,
        l2: Int
    ): ByteArray {
        val oldp = b2.position()
        val oldl = b2.limit()
        try {
            val md = MessageDigest.getInstance("SHA-256")
            if (b1 != null) {
                md.update(b1, o1, l1)
            }
            b2.position(o2)
            b2.limit(l2)
            md.update(b2)
            return md.digest()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            b2.limit(oldl)
            b2.position(oldp)
        }
        return ByteArray(32)
    }

    fun bytesToLong(bytes: ByteArray): Long {
        return ((bytes[7].toLong() shl 56) + (bytes[6].toLong() and 0xFFL shl 48) + (bytes[5].toLong() and 0xFFL shl 40) + (bytes[4].toLong() and 0xFFL shl 32)
                + (bytes[3].toLong() and 0xFFL shl 24) + (bytes[2].toLong() and 0xFFL shl 16) + (bytes[1].toLong() and 0xFFL shl 8) + (bytes[0].toLong() and 0xFFL))
    }

    fun bytesToInt(bytes: ByteArray): Int {
        return (bytes[3].toInt() and 0xFF shl 24) + (bytes[2].toInt() and 0xFF shl 16) + (bytes[1].toInt() and 0xFF shl 8) + (bytes[0].toInt() and 0xFF)
    }

    fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte()
        )
    }

    fun MD5(md5: String?): String? {
        if (md5 == null) {
            return null
        }
        try {
            val md = MessageDigest.getInstance("MD5")
            val array = md.digest(AndroidUtils.getStringBytes(md5))
            val sb = StringBuilder()
            for (a in array.indices) {
                sb.append(Integer.toHexString(array[a].toInt() and 0xFF or 0x100).substring(1, 3))
            }
            return sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return null
    }

    fun clamp(value: Int, maxValue: Int, minValue: Int): Int {
        return max(min(value, maxValue), minValue)
    }

    fun clamp(value: Long, maxValue: Long, minValue: Long): Long {
        return max(min(value, maxValue), minValue)
    }

    fun clamp(value: Float, maxValue: Float, minValue: Float): Float {
        if (java.lang.Float.isNaN(value)) {
            return minValue
        }
        return if (java.lang.Float.isInfinite(value)) {
            maxValue
        } else max(min(value, maxValue), minValue)
    }

    fun clamp(value: Double, maxValue: Double, minValue: Double): Double {
        if (java.lang.Double.isNaN(value)) {
            return minValue
        }
        return if (java.lang.Double.isInfinite(value)) {
            maxValue
        } else max(min(value, maxValue), minValue)
    }

    fun getExtension(fileName: String): String? {
        val idx = fileName.lastIndexOf('.')
        var ext: String? = null
        if (idx != -1) {
            ext = fileName.substring(idx + 1)
        }
        if (ext == null) {
            return null
        }
        ext = ext.uppercase(Locale.getDefault())
        return ext
    }

    fun <Key, Value> getOrDefault(
        map: HashMap<Key, Value>,
        key: Key,
        defaultValue: Value
    ): Value {
        return map[key] ?: return defaultValue
    }

    private fun doCallbacks(
        i: Int,
        vararg actions: Callback<Runnable>
    ) {
        if (actions.size > i) {
            actions[i].run(Runnable {
                doCallbacks(
                    i + 1,
                    *actions
                )
            })
        }
    }

    fun raceCallbacks(
        onFinish: Runnable?,
        vararg actions: Callback<Runnable?>
    ) {
        if (actions.isEmpty()) {
            onFinish?.run()
            return
        }
        val finished = intArrayOf(0)
        val checkFinish = Runnable {
            finished[0]++
            if (finished[0] == actions.size) {
                onFinish?.run()
            }
        }
        for (i in actions.indices) {
            actions[i].run(checkFinish)
        }
    }

    val orCreatePlayerQueue: DispatchQueue?
        get() {
            if (videoPlayerQueue == null) {
                videoPlayerQueue = DispatchQueue("playerQueue")
            }
            return videoPlayerQueue
        }

    fun isNullOrEmpty(list: Collection<*>?): Boolean {
        return list == null || list.isEmpty()
    }

    interface Callback<T> {
        fun run(arg: T)
    }

    interface CallbackVoidReturn<ReturnType> {
        fun run(): ReturnType
    }

    interface Callback0Return<ReturnType> {
        fun run(): ReturnType
    }

    interface CallbackReturn<Arg, ReturnType> {
        fun run(arg: Arg): ReturnType
    }

    interface Callback2Return<T1, T2, ReturnType> {
        fun run(arg: T1, arg2: T2): ReturnType
    }

    interface Callback3Return<T1, T2, T3, ReturnType> {
        fun run(arg: T1, arg2: T2, arg3: T3): ReturnType
    }

    interface Callback2<T, T2> {
        fun run(arg: T, arg2: T2)
    }

    interface Callback3<T, T2, T3> {
        fun run(arg: T, arg2: T2, arg3: T3)
    }

    interface Callback4<T, T2, T3, T4> {
        fun run(arg: T, arg2: T2, arg3: T3, arg4: T4)
    }

    interface Callback4Return<T, T2, T3, T4, ReturnType> {
        fun run(arg: T, arg2: T2, arg3: T3, arg4: T4): ReturnType
    }

    interface Callback5<T, T2, T3, T4, T5> {
        fun run(arg: T, arg2: T2, arg3: T3, arg4: T4, arg5: T5)
    }

    interface Callback5Return<T, T2, T3, T4, T5, ReturnType> {
        fun run(arg: T, arg2: T2, arg3: T3, arg4: T4, arg5: T5): ReturnType
    }

    fun lerp(a: Float, b: Int, f: Float): Int {
        return (a + f * (b - a)).toInt()
    }
}
