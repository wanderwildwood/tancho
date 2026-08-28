package com.wanderwildwood.einkbirding

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * The picture of one detection's sound, made once from its saved recording.
 *
 * Upstream draws a spectrogram of the live microphone buffer, rebuilt on every pass of the
 * recogniser - every 800ms, indefinitely. That is unreadable here: the panel is display
 * bound and would spend the whole session smearing, and a mel spectrogram says what it has
 * to say in intensity, which sixteen greys mostly throw away when they are also being
 * repainted faster than they settle.
 *
 * Made once, for a row the reader has asked about, it costs a single paint - the same as
 * the photograph beside it - and it can be looked at for as long as it takes to read.
 */
object BirdSpectrogram {

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Only the newest request paints, as with the photograph. */
    private val newest = AtomicLong(0)

    /**
     * Hands [onResult] a picture of what was heard at [timestamp], or null if there is no
     * recording for it - which is the ordinary answer for anything heard while the
     * recordings setting was off. Always called on the main thread.
     */
    fun load(context: Context, timestamp: Long, onResult: (Bitmap?) -> Unit) {
        val ticket = newest.incrementAndGet()
        val appContext = context.applicationContext
        io.execute {
            val bitmap = try {
                val samples = WavUtils.readWaveFile(appContext, timestamp)
                // Hi-res: 80 mel bands rather than 40. Upstream chose between them for the
                // cost of doing it every 800ms; done once there is no reason to take the
                // coarser one.
                if (samples == null) null
                else MelSpectrogram.getMelBitmap(samples, SAMPLE_RATE, true, HOP)
            } catch (e: Exception) {
                Log.i("BirdSpectrogram", "Could not draw $timestamp: ${e.message}")
                null
            }
            main.post { if (ticket == newest.get()) onResult(bitmap) }
        }
    }

    /** What the recogniser records at, and so what the recordings hold. */
    private const val SAMPLE_RATE = 48000

    /**
     * A third of upstream's step, so a three-second detection is 225 columns rather than
     * 75 and does not have to be stretched six times over to fill the panel. Three times
     * the work, once, on a clip that takes a fraction of a second either way.
     */
    private const val HOP = 320
}
