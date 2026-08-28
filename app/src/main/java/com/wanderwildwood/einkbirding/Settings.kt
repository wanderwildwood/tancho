package com.wanderwildwood.einkbirding

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import kotlin.math.abs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * What the settings screen shows, and the only thing that writes it.
 *
 * Every key and every type here is upstream's, unchanged: [SoundClassifier] still reads
 * `audio_source` as a string of digits and the rest as it always did, so a copy that has
 * been installed since before this rewrite keeps the settings it had.
 *
 * Each setting is a property that writes itself through to storage when assigned, so there
 * is no save step to forget and no second copy of the value to keep in step.
 */
class Settings(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    /** Re-read hooks, one per setting, for [reset] to call once the keys are gone. */
    private val reload = mutableListOf<() -> Unit>()

    var audioSource by pref(
        read = { AudioSource.of(prefs.getString(AUDIO_SOURCE, null)?.toIntOrNull()) },
        write = { prefs.edit().putString(AUDIO_SOURCE, it.value.toString()).apply() },
    )

    /** Percent. Below this, a detection is not the bird and is not written down. */
    var threshold by pref(
        read = { THRESHOLD_STEPS.nearestTo(prefs.getInt(THRESHOLD, 30)) },
        write = { prefs.edit().putInt(THRESHOLD, it).apply() },
    )

    /** Hz. Everything below is thrown away before the recogniser hears it. */
    var highPass by pref(
        read = { HIGH_PASS_STEPS.nearestTo(prefs.getInt(HIGH_PASS, 0)) },
        write = { prefs.edit().putInt(HIGH_PASS, it).apply() },
    )

    var notificationSound by boolPref(PLAY_SOUND)
    var saveWav by boolPref(WRITE_WAV)
    var bluetooth by boolPref(BLUETOOTH)
    var manualLocation by boolPref(MANUAL_LOCATION)

    /**
     * Whether BirdNET's non-bird classes are thrown away rather than written down. On by
     * default: the model will happily report a passing car or someone talking, and a log
     * of those is not a log of birds.
     */
    var ignoreNonBirds by boolPref(IGNORE_NON_BIRDS, default = true)

    /**
     * Whether a bird that calls twenty times in a row is twenty lines in the log or one.
     * Off, a run of the same species collapses to its best reading; on, every hearing is
     * kept with its own time. Upstream put this on the log screen itself, where it was a
     * box across the top of a list it was only read once to set.
     */
    var listRepeats by boolPref(VIEW_DETAILED)

    /**
     * Whether the listening screen carries the picture of the bird it last heard clearly.
     * Off by default: on a 480x800 panel the band leaves about one row of the log, and the
     * log is what that screen is for. The photo is for the phone propped on a windowsill,
     * not for reading a list.
     */
    var photoWhileListening by boolPref(SHOW_IMAGES)

    /**
     * Typing is allowed to be wrong on the way to being right, so what is typed is always
     * kept on screen and only a well-formed pair is written down. Upstream cleared the
     * field back to zeroes on a bad character, which loses the digits you had got right.
     */
    var manualLocationValue by pref(
        read = { prefs.getString(MANUAL_LOCATION_VALUE, DEFAULT_LOCATION) ?: DEFAULT_LOCATION },
        write = { if (isValidLocation(it)) prefs.edit().putString(MANUAL_LOCATION_VALUE, it).apply() },
    )

    val manualLocationIsValid: Boolean get() = isValidLocation(manualLocationValue)

    fun cycleAudioSource() {
        audioSource = audioSource.next()
    }

    fun cycleThreshold() {
        threshold = THRESHOLD_STEPS.cycleFrom(threshold)
    }

    fun cycleHighPass() {
        highPass = HIGH_PASS_STEPS.cycleFrom(highPass)
    }

    /** Back to how it arrived. Location is left alone: it is where you are, not a setting. */
    fun reset() {
        prefs.edit()
            .remove(AUDIO_SOURCE)
            .remove(HIGH_PASS)
            .remove(THRESHOLD)
            .remove(PLAY_SOUND)
            .remove(WRITE_WAV)
            .remove(BLUETOOTH)
            .apply()
        reload.forEach { it() }
    }

    private fun boolPref(key: String, default: Boolean = false) = pref(
        read = { prefs.getBoolean(key, default) },
        write = { prefs.edit().putBoolean(key, it).apply() },
    )

    /**
     * A setting held as Compose state and stored the moment it changes. The read is kept
     * so [reset] can run it again after clearing the keys, rather than each setting having
     * to know its own default twice.
     */
    private fun <T> pref(read: () -> T, write: (T) -> Unit): ReadWriteProperty<Any?, T> =
        object : ReadWriteProperty<Any?, T> {
            private var state by mutableStateOf(read())

            init {
                reload += { state = read() }
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = state

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                state = value
                write(value)
            }
        }

    private companion object {
        const val AUDIO_SOURCE = "audio_source"
        const val THRESHOLD = "model_threshold"
        const val HIGH_PASS = "high_pass"
        const val PLAY_SOUND = "play_sound"
        const val WRITE_WAV = "write_wav"
        const val BLUETOOTH = "bluetooth"
        const val IGNORE_NON_BIRDS = "ignore_non_birds"
        const val VIEW_DETAILED = "view_detailed"
        const val SHOW_IMAGES = "show_images"
        const val MANUAL_LOCATION = "manual_location"
        const val MANUAL_LOCATION_VALUE = "manual_location_value"
        const val DEFAULT_LOCATION = "0.000/0.000"
    }
}

/** Whether the app can offer the .wav setting: the Music directory needs Android 12. */
val canSaveWav: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Whether the system keeps a per-app language for this device. Android 13 on. */
val canChooseLanguage: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * The recorder to take audio from. The numbers are Android's `MediaRecorder.AudioSource`
 * constants and are what gets stored, so they cannot be reordered or renumbered.
 */
enum class AudioSource(val value: Int, val label: Int) {
    UNPROCESSED(9, R.string.source_unprocessed),
    MICROPHONE(1, R.string.source_microphone),
    VOICE_RECOGNITION(6, R.string.source_voicerecognition);

    fun next(): AudioSource = entries[(ordinal + 1) % entries.size]

    companion object {
        fun of(value: Int?): AudioSource = entries.firstOrNull { it.value == value } ?: UNPROCESSED
    }
}

/**
 * The values the threshold and the filter can take.
 *
 * Upstream made both of them sliders over every whole number in their range. A drag on
 * e-ink repaints the track for every pixel of travel, and neither number was ever that
 * precise - the difference between a 34% threshold and a 35% one is not audible. A value
 * stored by the old slider is rounded to the nearest of these on the way in, so a setting
 * made before this still means what it meant.
 */
private val THRESHOLD_STEPS = listOf(10, 20, 30, 40, 50, 60, 70, 80)
private val HIGH_PASS_STEPS = listOf(0, 100, 200, 300, 500, 1000)

private fun List<Int>.nearestTo(value: Int): Int = minBy { abs(it - value) }

private fun List<Int>.cycleFrom(value: Int): Int {
    val index = withIndex().minBy { abs(it.value - value) }.index
    return this[(index + 1) % size]
}

private val GPS_PAIR = Regex("""^-?\d+(\.\d+)?/-?\d+(\.\d+)?$""")

/** A "lat/lon" pair that is both well-formed and actually on the planet. */
fun isValidLocation(value: String): Boolean {
    if (!GPS_PAIR.matches(value)) return false
    val parts = value.split("/")
    val lat = parts[0].toDoubleOrNull() ?: return false
    val lon = parts[1].toDoubleOrNull() ?: return false
    return lat in -90.0..90.0 && lon in -180.0..180.0
}
