package com.wanderwildwood.tancho

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.tancho.databinding.ActivityViewBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class)
class ViewActivity : BaseActivity() {

    private lateinit var binding: ActivityViewBinding
    private lateinit var database: BirdDBHelper

    /** What the log is showing. Compose state, so a removal redraws it. */
    private val observations = mutableStateOf<List<BirdObservation>>(emptyList())

    /** The reading whose photograph is up, which the share button acts on. */
    private var selected: BirdObservation? = null

    /** The bird whose photograph is up, which the eBird and Wikipedia buttons act on. */
    private var selectedSpecies: Int? = null

    /** A hearing the listening screen asked to see, opened once the log is read in. */
    private data class Focus(val speciesId: Int, val from: Long, val to: Long)

    private var focus: Focus? = null

    /** Kept here so the log can be moved to a row from outside the list. */
    private val listState = LazyListState()

    /** The row the list is to bring to the top, or null. */
    private val scrollTarget = mutableStateOf<Int?>(null)
    private lateinit var birdObservations: ArrayList<BirdObservation>
    private lateinit var assetList: List<String>
    private lateinit var labelList: List<String>
    private lateinit var eBirdList: List<String>
    private lateinit var mContext: Context
    private var photoUrl: String? = null

    /**
     * No ActionBar. It carried the app's name and nothing else - the row along the bottom
     * already says which screen this is - and on a 480x800 panel that is a row of the log
     * given up to be told what you are already running.
     */
    override fun applyTheme() {
        setTheme(R.style.AppTheme_NoActionBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBinding.inflate(layoutInflater)
        database = BirdDBHelper.getInstance(this)
        mContext = this
        setContentView(binding.root)

        binding.destinationRow.setContent { ThemeMMD(colorScheme = monochrome) { DestinationRowCompose(Destination.HEARD) } }
        //Set aspect ratio for the photo
        val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
        val paramsPhoto: ViewGroup.LayoutParams = binding.photo.getLayoutParams() as ViewGroup.LayoutParams
        paramsPhoto.height = (width / 1.8f).toInt()
        // A third of the photograph's height. Enough to read a shape against, not so much
        // that opening a row leaves no log to go back to.
        val paramsSpectrogram: ViewGroup.LayoutParams = binding.spectrogram.getLayoutParams() as ViewGroup.LayoutParams
        paramsSpectrogram.height = (width / 5.4f).toInt()

        loadLabels(this)
        loadAssetList(this)
        loadEbirdList(this)
        wireList()
        focus = focusOf(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        focusOf(intent)?.let { focus = it }
    }

    override fun onResume() {
        super.onResume()
        reloadObservations()
        focus?.let {
            focus = null
            openFocus(it)
        }
    }

    /** Leaving the log stops a recording, so the listening screen does not come back to hear it. */
    override fun onPause() {
        super.onPause()
        WavUtils.stopPlaying()
    }

    private fun focusOf(intent: Intent?): Focus? {
        if (intent == null || !intent.hasExtra(EXTRA_SPECIES)) return null
        val focus = Focus(
            intent.getIntExtra(EXTRA_SPECIES, -1),
            intent.getLongExtra(EXTRA_FROM, 0L),
            intent.getLongExtra(EXTRA_TO, 0L),
        )
        // Read once. A later resume from the same intent must not jump the log again.
        intent.removeExtra(EXTRA_SPECIES)
        return focus
    }

    /**
     * Opens the row for a hearing from the listening screen: the reading of that bird
     * nearest the time it was heard, which is the best of the run, as the log keeps it.
     *
     * Every reading the listening screen shows was written here before it was shown, so
     * there is normally a row. When there is not - the row was removed, or the log was
     * deleted or replaced since - the bird is still put up, without a recording, and the
     * log says it has no row for it rather than opening one from another day.
     */
    private fun openFocus(focus: Focus) {
        if (focus.speciesId !in labelList.indices) return
        val rows = observations.value
        fun distance(row: BirdObservation) = when {
            row.millis < focus.from -> focus.from - row.millis
            row.millis > focus.to -> row.millis - focus.to
            else -> 0L
        }
        val match = rows.withIndex()
            .filter { it.value.speciesId == focus.speciesId && distance(it.value) <= SAME_HEARING_MS }
            .minWithOrNull(compareBy<IndexedValue<BirdObservation>> { distance(it.value) }
                .thenByDescending { it.value.probability })
        if (match != null) {
            scrollTarget.value = match.index
            openObservation(match.value)
        } else {
            scrollTarget.value = 0
            WavUtils.stopPlaying()
            showBird(focus.speciesId, null)
            Toast.makeText(this, getString(R.string.heard_not_in_log), Toast.LENGTH_SHORT).show()
        }
    }

    /** How long a row stays armed before it forgets it was asked. Six, as everywhere. */
    private val ARMED_MS = 6000L

    /** Reads the log in and puts it on screen. Run on the way in, and after a row is removed. */
    private fun reloadObservations() {
        // Asked for again rather than kept. Restoring a backup closes this helper and opens
        // a new one over a different file, and a reference taken in onCreate goes on
        // answering from the log that was replaced -- so a restore looked like it had done
        // nothing until the app was killed and started again.
        database = BirdDBHelper.getInstance(this)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDetailedFilterActive = sharedPref.getBoolean("view_detailed", false)
        birdObservations = ArrayList(database.getAllBirdObservations(isDetailedFilterActive).sortedByDescending { it.millis } )  //Conversion between Java ArrayList and Kotlin ArrayList

        //replace names with values from current language
        for (birdObservation in birdObservations){
            birdObservation.name = labelList.get(birdObservation.speciesId).split("_").last()
        }

        binding.empty.visibility = if (birdObservations.isEmpty()) View.VISIBLE else View.GONE
        observations.value = birdObservations.toList()
    }

    /**
     * The log, drawn with the listening screen's row.
     *
     * Set once. The list itself is Compose state, so a removal or a restore redraws it
     * without the screen being rebuilt around it.
     */
    private fun wireList() {
        binding.observationList.setContent {
            ThemeMMD(colorScheme = monochrome) {
                val rows by observations
                var armed by remember { mutableStateOf<Int?>(null) }

                // The question withdraws itself, like every other one these apps ask.
                LaunchedEffect(armed) {
                    if (armed == null) return@LaunchedEffect
                    delay(ARMED_MS)
                    armed = null
                }

                val target by scrollTarget
                LaunchedEffect(target) {
                    val index = target ?: return@LaunchedEffect
                    listState.scrollToItem(index)
                    scrollTarget.value = null
                }

                PagedColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        val day = dateText(row.millis)
                        if (index == 0 || dateText(rows[index - 1].millis) != day) {
                            DayHeading(day)
                        }
                        val isArmed = armed == row.id
                        BirdRow(
                            name = if (isArmed) stringResource(R.string.delete_observation_confirm)
                                else row.name,
                            latinName = if (isArmed) ""
                                else labelList[row.speciesId].split("_").first(),
                            time = timeText(row.millis),
                            percent = (row.probability * 20f).roundToInt() * 5,
                            bold = isArmed,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (isArmed) {
                                        armed = null
                                        database.removeEntries(row.coveredIds)
                                        Thread { WavUtils.sweepOrphans(this@ViewActivity, database.allTimestamps()) }.start()
                                        clearPhoto()
                                        reloadObservations()
                                    } else {
                                        armed = null
                                        openObservation(row)
                                    }
                                },
                                // A recogniser is wrong sometimes, and until there was this
                                // the only answer to a wrong line was to throw the whole log
                                // away -- so keeping a record you knew to be untrue was the
                                // cheaper option.
                                onLongClick = { armed = row.id },
                            ),
                        )
                    }
                }
            }
        }
    }

    /** Plays what was heard, and puts the bird on screen. */
    private fun openObservation(row: BirdObservation) {
        WavUtils.playWaveFile(mContext, row.millis)
        showBird(row.speciesId, row)
    }

    /**
     * Puts a bird on screen: its photograph and names, and with a [row] the sound of that
     * reading. A second reading of the bird already up changes only what is particular to
     * the reading, not the photograph.
     */
    private fun showBird(speciesId: Int, row: BirdObservation?) {
        val assetId = assetList[speciesId]
        if (assetId == "NO_ASSET") {
            clearPhoto()
            return
        }
        selected = row
        selectedSpecies = speciesId
        binding.photoShare.setVisibility(if (row != null) View.VISIBLE else View.GONE)
        if (row != null) showSpectrogram(row.millis) else {
            binding.spectrogram.setImageDrawable(null)
            binding.spectrogram.setVisibility(View.GONE)
        }
        // The library's embed page is behind a proof-of-work bot check that answers
        // "Making sure you're not a bot!" instead of a bird, so the photo comes straight
        // from its image host: one request, one jpeg.
        val url = BirdPhoto.url(assetId)
        if (url == photoUrl) return
        photoUrl = url
        val label = labelList[speciesId]
        binding.photoName.setText(label.split("_").last())
        binding.photoName.setVisibility(View.VISIBLE)
        binding.photoLatinname.setText(label.split("_").first())
        binding.photoLatinname.setVisibility(View.VISIBLE)
        binding.photoReload.setVisibility(View.VISIBLE)
        binding.photoEbird.setVisibility(View.VISIBLE)
        binding.photoWikipedia.setVisibility(View.VISIBLE)
        showPhoto(url, false)
    }


    /** Retrieve asset list from "assets" file */
    private fun loadAssetList(context: Context) {

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open("assets.txt")))  //TODO: Common definition for all classes
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it.trim())
                }
            }
            assetList = wordList.map { it }
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
        }
    }

    /** Retrieve eBird taxonomy list from "taxo_code" file */
    private fun loadEbirdList(context: Context) {

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open("taxo_code.txt")))
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it.trim())
                }
            }
            eBirdList = wordList.map { it }
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${"taxo_code.txt"}: ${e.message}")
        }
    }

    /** The names of the birds, in the reader's language. See [BirdNames]. */
    private fun loadLabels(context: Context) {
        labelList = BirdNames.load(context)
    }

    /**
     * Puts the photo on screen, and the app's own mark there if it cannot be had. The
     * panel keeps its height either way: on e-ink a layout jump repaints the whole panel,
     * and a photo that quietly never arrives is the bug this replaced.
     */
    private fun showPhoto(url: String, refresh: Boolean) {
        BirdPhoto.load(cacheDir, url, refresh) { bitmap ->
            if (bitmap == null) {
                binding.photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
                binding.photo.setImageResource(R.drawable.ic_launcher_monochrome)
                Toast.makeText(applicationContext, getString(R.string.error_download), Toast.LENGTH_SHORT).show()
            } else {
                binding.photo.setScaleType(ImageView.ScaleType.CENTER_CROP)
                binding.photo.setImageBitmap(bitmap)
            }
        }
    }

    /**
     * The sound of the detection, if it was recorded and the reader asked for it. Absent
     * rows simply have no strip, rather than an empty one: most rows in an old log were
     * heard before the setting was on, and a band of nothing on every one of them would
     * be a permanent cost for an occasional picture.
     */
    private fun showSpectrogram(timestamp: Long) {
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_spectrogram", false)) {
            binding.spectrogram.setVisibility(View.GONE)
            return
        }
        BirdSpectrogram.load(this, timestamp) { bitmap ->
            if (bitmap == null) {
                binding.spectrogram.setImageDrawable(null)
                binding.spectrogram.setVisibility(View.GONE)
            } else {
                binding.spectrogram.setImageBitmap(bitmap)
                binding.spectrogram.setVisibility(View.VISIBLE)
            }
        }
    }

    private fun clearPhoto() {
        photoUrl = null
        selected = null
        selectedSpecies = null
        binding.spectrogram.setImageDrawable(null)
        binding.spectrogram.setVisibility(View.GONE)
        binding.photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        binding.photo.setImageResource(R.drawable.ic_launcher_monochrome)
        binding.photoName.setText("")
        binding.photoName.setVisibility(View.GONE)
        binding.photoLatinname.setText("")
        binding.photoLatinname.setVisibility(View.GONE)
        binding.photoReload.setVisibility(View.GONE)
        binding.photoEbird.setVisibility(View.GONE)
        binding.photoWikipedia.setVisibility(View.GONE)
        binding.photoShare.setVisibility(View.GONE)
    }

    fun reload(view: View) {
        photoUrl?.let { showPhoto(it, true) }
    }

    fun share(view: View) {
        val row = selected ?: return
        val id = row.speciesId

        val timeString = timeText(row.millis)
        val dateString = dateText(row.millis)

        // What was heard, when and where, and nothing else. Upstream signed every shared
        // sighting "Get whoBIRD on F-Droid" -- an advertisement for a different app, in
        // English whatever the reader's language, riding out in somebody's own message.
        // Where this app came from is in About, which is where a reader looks for it.
        //
        // The place is left off when there is no place. An entry recorded without a fix
        // used to share as "..., 0.0, 0.0", which claims the equator at the prime
        // meridian rather than admitting to knowing nothing.
        val shareString = buildString {
            append(dateString).append(", ")
            append(timeString).append(", ")
            append(labelList[id].replace("_", ", "))
            // The place goes out only when the reader has asked for it in Settings: a message
            // sent to someone else should not carry where a person stood unless they chose that.
            // And it is left off when there is no place: 0/0 claims the equator at
            // the prime meridian rather than admitting to knowing nothing.
            val withPlace = PreferenceManager.getDefaultSharedPreferences(this@ViewActivity)
                .getBoolean(Settings.SHARE_PLACE, false)
            if (withPlace && (row.latitude != 0.0f || row.longitude != 0.0f)) {
                append(", ").append(row.latitude).append(", ").append(row.longitude)
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareString)
        startActivity(Intent.createChooser(shareIntent, ""))
    }

    fun ebird(view: View) {
        val id = selectedSpecies ?: return
        BrowserLinks.open(this, "https://ebird.org/species/" + eBirdList[id])
    }

    fun wikipedia(view: View) {
        val id = selectedSpecies ?: return
        val scientificName = labelList[id].split("_").first()
        BrowserLinks.open(this, BirdNames.wikipedia(this, scientificName))
    }

    companion object {
        /** A hearing to open, from the listening screen: the species and when it was heard. */
        const val EXTRA_SPECIES = "com.wanderwildwood.tancho.extra.SPECIES"
        const val EXTRA_FROM = "com.wanderwildwood.tancho.extra.FROM"
        const val EXTRA_TO = "com.wanderwildwood.tancho.extra.TO"

        /** The listening screen folds a bird's run within a minute; a reading this close is the same hearing. */
        private const val SAME_HEARING_MS = 60_000L
    }
}
