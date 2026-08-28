package com.wanderwildwood.einkbirding

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
import com.wanderwildwood.einkbirding.databinding.ActivityViewBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ViewActivity : BaseActivity() {

    private lateinit var binding: ActivityViewBinding
    private lateinit var database: BirdDBHelper
    private lateinit var adapter: RecyclerOverviewListAdapterObservations
    private lateinit var birdObservations: ArrayList<BirdObservation>
    private lateinit var assetList: List<String>
    private lateinit var labelList: List<String>
    private lateinit var eBirdList: List<String>
    private lateinit var mContext: Context
    private var rowTapsWired = false
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

        wireDestinations(Destination.HEARD)
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

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerObservations.setLayoutManager(linearLayoutManager)

        loadLabels(this)
        loadAssetList(this)
        loadEbirdList(this)
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDetailedFilterActive = sharedPref.getBoolean("view_detailed", false)
        birdObservations = ArrayList(database.getAllBirdObservations(isDetailedFilterActive).sortedByDescending { it.millis } )  //Conversion between Java ArrayList and Kotlin ArrayList

        //replace names with values from current language
        for (birdObservation in birdObservations){
            birdObservation.name = labelList.get(birdObservation.speciesId).split("_").last()
        }

        adapter = RecyclerOverviewListAdapterObservations(applicationContext, birdObservations)
        binding.recyclerObservations.setAdapter(adapter)
        binding.recyclerObservations.setFocusable(false)
        // Once only. Added on every resume, this stacks another listener on the same
        // list each time the screen is opened, and one tap then runs the handler as many
        // times as the screen has been visited.
        if (!rowTapsWired) {
            rowTapsWired = true
            binding.recyclerObservations.addOnItemTouchListener(
            RecyclerItemClickListener(baseContext, binding.recyclerObservations, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {
                    WavUtils.playWaveFile(mContext, adapter.getMillis(position))
                    val assetId = assetList[adapter.getSpeciesID(position)]
                    if (assetId == "NO_ASSET") {
                        clearPhoto()
                        return
                    }
                    // The library's embed page is behind a proof-of-work bot check that
                    // answers "Making sure you're not a bot!" instead of a bird, so the
                    // photo comes straight from its image host: one request, one jpeg.
                    val url = BirdPhoto.url(assetId)
                    if (url == photoUrl) return
                    photoUrl = url
                    val label = labelList[adapter.getSpeciesID(position)]
                    binding.photoName.setText(label.split("_").last())
                    binding.photoName.setVisibility(View.VISIBLE)
                    binding.photoLatinname.setText(label.split("_").first())
                    binding.photoLatinname.setVisibility(View.VISIBLE)
                    binding.photoReload.setVisibility(View.VISIBLE)
                    binding.photoEbird.setVisibility(View.VISIBLE)
                    binding.photoEbird.setTag(position)
                    binding.photoShare.setVisibility(View.VISIBLE)
                    binding.photoShare.setTag(position)
                    showPhoto(url, false)
                    showSpectrogram(adapter.getMillis(position))
                }

                override fun onLongItemClick(view: View?, position: Int) {}
            })
            )
        }
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

    /** Retrieve labels from "labels.txt" file */
    private fun loadLabels(context: Context) { //TODO: Refactor
        val localeList = context.resources.configuration.locales
        var language = localeList.get(0).language

        if (language == "en") {
            val country = localeList.get(0).country
            language = when (country) {
                "GB" -> "en_uk"
                else -> "en"
            }
        } else if (language == "pt") {
            val country = localeList.get(0).country
            language = when (country) {
                "BR" -> "pt_BR"
                else -> "pt_PT"
            }
        }

        var filename = "labels"+"_${language}.txt"    // TODO: Common definition for all classes

        //Check if file exists
        val assetManager = context.assets // Replace 'assets' with actual AssetManager instance
        try {
            val mapList = assetManager.list("")?.toMutableList()

            if (mapList != null) {
                if (!mapList.contains(filename)) {
                    filename = "labels"+"_en.txt"
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            filename = "labels"+"_en.txt"
        }

        Log.i("ViewActivity", filename)

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open(filename)))
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it)
                }
            }
            labelList = wordList.map { it.toTitleCase() }
            Log.i("ViewActivity", "Label list entries: ${labelList.size}")
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${filename}: ${e.message}")
        }
    }

    private fun String.toTitleCase() =
        splitToSequence("_")
            .map { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
            .joinToString("_")
            .trim()

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
        val position = binding.photoShare.tag as Int

        val id = adapter.getSpeciesID(position)

        val sdf: SimpleDateFormat
        val date = Date(adapter.getMillis(position))
        sdf = if (DateFormat.is24HourFormat(this)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("hh:mm aa", Locale.getDefault())
        }
        val timeString = sdf.format(date)

        val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
        val dateString = df.format(adapter.getMillis(position))

        val locationString = adapter.getLocation(position)

        val shareString = dateString + ", " + timeString + ", " + labelList[id].replace("_",", ") + ", " + locationString +"\n\nGet whoBIRD on F-Droid"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareString)
        startActivity(Intent.createChooser(shareIntent, ""))
    }

    fun ebird(view: View) {
        val position = binding.photoEbird.tag as Int
        val id = adapter.getSpeciesID(position)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ebird.org/species/"+eBirdList[id])))
    }
}
