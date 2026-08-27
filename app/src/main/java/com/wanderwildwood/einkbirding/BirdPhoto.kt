package com.wanderwildwood.einkbirding

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * One bird photo, fetched from the Macaulay Library's image host and decoded here.
 *
 * This was a WebView until 2026-08-27. A WebView paints in a separate sandboxed
 * process, and the Kompakt's MediaTek DuraSpeed suppressor will not let this app start
 * one: no renderer ever appeared, so no WebViewClient callback ever fired, so nothing
 * ever made the panel visible again - blank photo, no error, nothing in the log to say
 * why, and it survived reinstalling and rebooting because the suppress list is not ours.
 * Every Kompakt ships DuraSpeed, so this was not a fault on one phone.
 *
 * What arrives is a plain jpeg. There was nothing a browser was doing for us that a
 * decoder cannot, and this way there is no second process to be denied.
 */
object BirdPhoto {

    /** The image host offers several widths; this is the panel's own, so none is wasted. */
    private const val WIDTH = 480

    /** Photos are worth keeping between sessions, but not without end. */
    private const val KEEP = 200

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /**
     * Only the newest request is allowed to paint. Tapping down a list starts a fetch per
     * row, and without this the slowest one wins and the panel settles on a bird the
     * reader has already gone past.
     */
    private val newest = AtomicLong(0)

    fun url(assetId: String) =
        "https://cdn.download.ams.birds.cornell.edu/api/v2/asset/$assetId/$WIDTH"

    /**
     * Hands [onResult] a bitmap, or null if the photo could not be had. Always on the
     * main thread, and never for a request a later one has overtaken.
     */
    fun load(cacheDir: File, url: String, refresh: Boolean, onResult: (Bitmap?) -> Unit) {
        val ticket = newest.incrementAndGet()
        io.execute {
            val bitmap = try {
                fetch(cacheDir, url, refresh)
            } catch (e: Exception) {
                Log.i("BirdPhoto", "Could not load $url: ${e.message}")
                null
            }
            main.post { if (ticket == newest.get()) onResult(bitmap) }
        }
    }

    private fun fetch(cacheDir: File, url: String, refresh: Boolean): Bitmap? {
        val dir = File(cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, name(url))
        if (refresh) file.delete()
        if (!file.exists()) {
            download(url, file)
            trim(dir)
        }
        val bitmap = BitmapFactory.decodeFile(file.path)
        // A file that will not decode is not a photo. Drop it, or it is served from the
        // cache from now on and the bird never recovers without clearing the app's data.
        if (bitmap == null) file.delete()
        return bitmap
    }

    /**
     * Downloads beside the real name and renames on success, so an interrupted fetch
     * cannot leave half a jpeg in the cache under the name of a whole one.
     */
    private fun download(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            val partial = File(file.path + ".part")
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
            if (!partial.renameTo(file)) partial.delete()
        } finally {
            connection.disconnect()
        }
    }

    /** Asset id and width, which is all that distinguishes one of these urls from another. */
    private fun name(url: String) =
        url.substringAfter("/asset/", url).filter { it.isLetterOrDigit() }

    private fun trim(dir: File) {
        val files = dir.listFiles() ?: return
        if (files.size <= KEEP) return
        files.sortedBy { it.lastModified() }.take(files.size - KEEP).forEach { it.delete() }
    }
}
