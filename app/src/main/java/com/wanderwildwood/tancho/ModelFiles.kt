package com.wanderwildwood.tancho

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * The two BirdNET models, carried in the APK as assets (put there by the build from
 * models/fetch.sh, which takes them from one fixed commit of the whoBIRD-TFlite repository).
 * Nothing is downloaded: the app identifies birds from the first run with no network.
 *
 * The models are parsed by native code, so each is checked against the SHA-256 it was built
 * with before it is used, once each time the app starts; a model that does not match is never
 * loaded.
 */
object ModelFiles {
    const val MODEL = "model.tflite"
    const val META_MODEL = "metaModel.tflite"

    /** BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite, 25,932,528 bytes. */
    const val MODEL_SHA256 = "5c64ba3ffab264ccbfafe784896392051a86be63b0cc507e7683db7df114546b"

    /** BirdNET_GLOBAL_6K_V2.4_MData_Model_V2_FP16.tflite, 14,770,468 bytes. */
    const val META_MODEL_SHA256 = "1226f23fc20362617deb09178f366111b70cf085c2c82893f2814e5acedce6c2"

    @Volatile private var checked = false

    /** SHA-256 of a stream as lower-case hex, or "" if it cannot be read. */
    fun sha256(open: () -> InputStream): String = try {
        open().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    } catch (e: IOException) {
        ""
    }

    /** True when both models in the APK are the ones this version was built with. */
    fun check(context: Context): Boolean {
        if (checked) return true
        val ok = sha256 { context.assets.open(MODEL) } == MODEL_SHA256 &&
            sha256 { context.assets.open(META_MODEL) } == META_MODEL_SHA256
        if (ok) {
            checked = true
            forgetDownloaded(context)
        }
        return ok
    }

    /** A model, mapped straight out of the APK (it is stored there uncompressed). */
    fun map(context: Context, name: String): MappedByteBuffer =
        context.assets.openFd(name).use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }

    /**
     * Up to 1.7.46 the models were downloaded into app_filesdir, the 32-bit one 52 MB. They are
     * not read any more, so they go, and only they.
     */
    private fun forgetDownloaded(context: Context) {
        val dir = context.getDir("filesdir", Context.MODE_PRIVATE)
        listOf(MODEL, META_MODEL).forEach { name ->
            File(dir, name).delete()
            File(dir, "$name.part").delete()
        }
    }
}
