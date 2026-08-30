package com.wanderwildwood.tancho

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

    /** The reading whose photograph is up, which the share and eBird buttons act on. */
    private var selected: BirdObservation? = null
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

        binding.destinationRow.setContent { ThemeMMD { DestinationRowCompose(Destination.HEARD) } }
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
    }

    override fun onResume() {
        super.onResume()
        reloadObservations()
    }

    /** How long a row stays armed before it forgets it was asked. Four, as everywhere. */
    private val ARMED_MS = 4000L

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
            ThemeMMD {
                val rows by observations
                var armed by remember { mutableStateOf<Int?>(null) }

                // The question withdraws itself, like every other one these apps ask.
                LaunchedEffect(armed) {
                    if (armed == null) return@LaunchedEffect
                    delay(ARMED_MS)
                    armed = null
                }

                val dayFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        val day = dayFormat.format(Date(row.millis))
                        if (index == 0 || dayFormat.format(Date(rows[index - 1].millis)) != day) {
                            DayHeading(day)
                        }
                        val isArmed = armed == row.id
                        BirdRow(
                            name = if (isArmed) stringResource(R.string.delete_observation_confirm)
                                else row.name,
                            latinName = if (isArmed) ""
                                else labelList[row.speciesId].split("_").first(),
                            time = clockTime(row.millis),
                            percent = (row.probability * 20f).roundToInt() * 5,
                            bold = isArmed,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (isArmed) {
                                        armed = null
                                        database.removeEntries(row.coveredIds)
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
        val assetId = assetList[row.speciesId]
        if (assetId == "NO_ASSET") {
            clearPhoto()
            return
        }
        // The library's embed page is behind a proof-of-work bot check that answers
        // "Making sure you're not a bot!" instead of a bird, so the photo comes straight
        // from its image host: one request, one jpeg.
        val url = BirdPhoto.url(assetId)
        if (url == photoUrl) return
        photoUrl = url
        selected = row
        val label = labelList[row.speciesId]
        binding.photoName.setText(label.split("_").last())
        binding.photoName.setVisibility(View.VISIBLE)
        binding.photoLatinname.setText(label.split("_").first())
        binding.photoLatinname.setVisibility(View.VISIBLE)
        binding.photoReload.setVisibility(View.VISIBLE)
        binding.photoEbird.setVisibility(View.VISIBLE)
        binding.photoShare.setVisibility(View.VISIBLE)
        showPhoto(url, false)
        showSpectrogram(row.millis)
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
        binding.photoShare.setVisibility(View.GONE)
    }

    fun reload(view: View) {
        photoUrl?.let { showPhoto(it, true) }
    }

    fun share(view: View) {
        val row = selected ?: return
        val id = row.speciesId

        val sdf: SimpleDateFormat
        val date = Date(row.millis)
        sdf = if (DateFormat.is24HourFormat(this)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("hh:mm aa", Locale.getDefault())
        }
        val timeString = sdf.format(date)

        val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
        val dateString = df.format(row.millis)

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
            // The place is left off when there is no place: 0/0 claims the equator at
            // the prime meridian rather than admitting to knowing nothing.
            if (row.latitude != 0.0f || row.longitude != 0.0f) {
                append(", ").append(row.latitude).append(", ").append(row.longitude)
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareString)
        startActivity(Intent.createChooser(shareIntent, ""))
    }

    fun ebird(view: View) {
        val id = selected?.speciesId ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ebird.org/species/"+eBirdList[id])))
    }
}
