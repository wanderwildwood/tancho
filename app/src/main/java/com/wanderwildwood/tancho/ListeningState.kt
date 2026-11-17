package com.wanderwildwood.tancho

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/** One species, heard once or many times in a row. */
data class Heard(
    val speciesIndex: Int,
    val name: String,
    val latinName: String,
    val firstHeard: Long,
    val lastHeard: Long,
    val bestProbability: Float,
    val times: Int,
) {
    /**
     * The probability as the screen shows it: rounded to 5%, because a row that reads 71%
     * and then 72% has told the reader nothing and cost a repaint to do it.
     */
    val shownPercent: Int get() = (bestProbability * 20f).roundToInt() * 5

    /** Whether two versions of the same row would look identical. */
    fun looksSameAs(other: Heard): Boolean =
        speciesIndex == other.speciesIndex &&
            shownPercent == other.shownPercent &&
            firstHeard / 60_000L == other.firstHeard / 60_000L
}

/**
 * What the listening screen shows, and the only thing [SoundClassifier] talks to.
 *
 * The screen is a log rather than a meter: it changes when a bird is heard, not on every
 * pass of the recogniser. An e-ink panel redraws in tenths of a second and ghosts when
 * pushed, so a value that updates four times a second is worse than useless - it is
 * unreadable and it costs battery. Everything here is therefore written only when what
 * the reader would see actually differs.
 *
 * [record] is called from the recogniser's timer thread and [flush] from the main thread,
 * so the read-modify-write pair on the log is synchronized. The flows themselves are safe
 * to read from anywhere.
 */
class ListeningState {

    private val _isListening = MutableStateFlow(true)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _heard = MutableStateFlow<List<Heard>>(emptyList())
    val heard: StateFlow<List<Heard>> = _heard.asStateFlow()

    private val _photo = MutableStateFlow<Photo?>(null)
    val photo: StateFlow<Photo?> = _photo.asStateFlow()

    /**
     * Whether the app knows where it is standing.
     *
     * False until a fix arrives or a location is typed in, and the screen says so rather
     * than offering a place setting that is not being applied to anything.
     */
    private val _placeKnown = MutableStateFlow(false)
    val placeKnown: StateFlow<Boolean> = _placeKnown.asStateFlow()

    fun setPlaceKnown(known: Boolean) {
        _placeKnown.value = known
    }

    /**
     * Whether the microphone may be used. The screen said "Listening" whether or not it could
     * hear anything, so a person who had said no to the permission was told a recogniser was
     * running that never received a sound. False shows the real state, and a way to change it.
     */
    private val _micAllowed = MutableStateFlow(true)
    val micAllowed: StateFlow<Boolean> = _micAllowed.asStateFlow()

    fun setMicAllowed(allowed: Boolean) {
        _micAllowed.value = allowed
    }

    /** How much the where-and-when model is allowed to weigh in, 0f..1f. */
    val metaInfluence = MutableStateFlow(0.6f)

    /** The bird whose picture the listening screen is showing, if it is showing one. */
    data class Photo(val speciesIndex: Int, val assetId: String)

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    @Synchronized
    fun clear() {
        _heard.value = emptyList()
        _photo.value = null
        silentTop = null
    }

    /**
     * The bird to show a picture of: the last one heard clearly enough to be worth
     * looking at, which then stays until a different bird is heard that clearly.
     *
     * Only a change of species is published. The same bird calling again would fetch and
     * paint the same photograph, and on e-ink that is a full repaint to show what is
     * already there.
     */
    @Synchronized
    fun showPhotoOf(speciesIndex: Int, assetId: String) {
        if (_photo.value?.speciesIndex == speciesIndex) return
        _photo.value = Photo(speciesIndex, assetId)
    }

    /**
     * Record a detection. A bird that goes on singing folds into the row it already has
     * rather than filling the log with itself; a row is only republished when it would
     * look different, which is what keeps the panel still.
     */
    @Synchronized
    fun record(
        speciesIndex: Int,
        name: String,
        latinName: String,
        probability: Float,
        timeMillis: Long,
    ) {
        val existing = _heard.value
        val top = existing.firstOrNull()
        // The row as it really stands: the published top row with anything held back
        // folded in, so a run of silent merges keeps its count and its last time.
        val current = silentTop?.takeIf { top != null && it.isSameRowAs(top) } ?: top

        if (top != null && current != null && current.speciesIndex == speciesIndex &&
            timeMillis - current.lastHeard <= SAME_BIRD_WINDOW_MS
        ) {
            val merged = current.copy(
                lastHeard = timeMillis,
                bestProbability = max(current.bestProbability, probability),
                times = current.times + 1,
            )
            if (merged.looksSameAs(top)) {
                // Same bird, same confidence, same minute: nothing to redraw. The count
                // is still kept, so the row is right the next time something does change.
                silentTop = merged
                return
            }
            _heard.value = listOf(merged) + existing.drop(1)
            silentTop = null
        } else {
            val entry = Heard(
                speciesIndex = speciesIndex,
                name = name,
                latinName = latinName,
                firstHeard = timeMillis,
                lastHeard = timeMillis,
                bestProbability = probability,
                times = 1,
            )
            // The row this one pushes down gets what was held back for it, since the
            // list is redrawn now anyway.
            val settled = if (current != null) listOf(current) + existing.drop(1) else existing
            _heard.value = (listOf(entry) + settled).take(MAX_ROWS)
            silentTop = null
        }
    }

    /**
     * The top row as it would be if every unpublished change were published. Held back
     * from the flow so the screen does not redraw for it, and folded in the moment
     * something else makes a redraw necessary anyway.
     */
    private var silentTop: Heard? = null

    /**
     * Publish anything held back. Called when the screen is about to redraw for another
     * reason - stopping, leaving - so the log is not left showing a stale count.
     *
     * Only ever over the row it was held back from. A newer run of the same bird starts a
     * row of its own; writing the old run's copy over it would put two rows with one start
     * time in the list, and an older count over a newer one.
     */
    @Synchronized
    fun flush() {
        val pending = silentTop ?: return
        silentTop = null
        val existing = _heard.value
        val top = existing.firstOrNull() ?: return
        if (pending.isSameRowAs(top)) {
            _heard.value = listOf(pending) + existing.drop(1)
        }
    }

    /** Two versions of one row: the same bird, from the same first hearing. */
    private fun Heard.isSameRowAs(other: Heard): Boolean =
        speciesIndex == other.speciesIndex && firstHeard == other.firstHeard

    private companion object {
        /** Longer than the gap between two songs from one bird, shorter than a walk. */
        const val SAME_BIRD_WINDOW_MS = 60_000L

        /** The log on screen. The database keeps everything; this is just what is shown. */
        const val MAX_ROWS = 200
    }
}

/**
 * How much the where-and-when model is allowed to weigh in.
 *
 * The three are named for what the reader gets, not for how much the model counts:
 * "Weighed" and "Decisive" described the arithmetic, which tells you nothing about
 * whether the bird you are looking for can still appear in the list. A filter that
 * quietly rules out a rarity should say so in the words on the screen.
 *
 * Three named settings rather than a continuous slider: the underlying number was never
 * precise, and a tap is a single repaint where a drag is dozens.
 */
enum class PlaceAndDate(val influence: Float, val label: Int) {
    IGNORED(0f, R.string.place_and_date_off),
    WEIGHED(0.6f, R.string.place_and_date_some),
    DECISIVE(1f, R.string.place_and_date_full);

    fun next(): PlaceAndDate = entries[(ordinal + 1) % entries.size]

    companion object {
        /** The closest setting to a stored slider value, so an old preference still means something. */
        fun nearest(influence: Float): PlaceAndDate =
            entries.minBy { kotlin.math.abs(it.influence - influence) }
    }
}
