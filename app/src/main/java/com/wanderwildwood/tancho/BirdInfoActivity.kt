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
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.tancho.databinding.ActivityBirdInfoBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier


class BirdInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityBirdInfoBinding
    private lateinit var database: BirdDBHelper
    private lateinit var assetList: List<String>
    private lateinit var labelList: List<String>
    private lateinit var eBirdList: List<String>
    private lateinit var mContext: Context
    private var photoUrl: String? = null
    private lateinit var allBirdsList: ArrayList<Pair<Int, String>>

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
        binding = ActivityBirdInfoBinding.inflate(layoutInflater)
        database = BirdDBHelper.getInstance(this)
        mContext = this
        setContentView(binding.root)

        binding.destinationRow.setContent { ThemeMMD { DestinationRowCompose(Destination.SPECIES) } }
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

        loadLabels(this)
        loadAssetList(this)
        loadEbirdList(this)
        allBirdsList = labelList.mapIndexed { index, element ->
            Pair(index, element)
        }.sortedBy { it.second.split("_")[1] }.toCollection(ArrayList())

    }

    /** What the list is showing, which the search field narrows. */
    private val shown = mutableStateOf<List<Pair<Int, String>>>(emptyList())

    override fun onResume() {
        super.onResume()

        shown.value = allBirdsList
        binding.speciesList.setContent {
            ThemeMMD {
                val birds by shown
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(birds, key = { it.first }) { (speciesId, label) ->
                        BirdRow(
                            name = label.split("_").last(),
                            latinName = label.split("_").first(),
                            modifier = Modifier.clickable { showSpecies(speciesId) },
                        )
                    }
                }
            }
        }
        binding.searchEdit.doOnTextChanged { text, _, _, _ ->
            shown.value = labelList.mapIndexed { index, element -> Pair(index, element) }
                .filter { it.second.contains(text.toString(), ignoreCase = true) }
                .sortedBy { it.second.split("_")[1] }
        }
    }

    /** Puts a bird's photograph up. Was the body of the RecyclerView's tap listener. */
    private fun showSpecies(speciesId: Int) {
        val assetId = assetList[speciesId]
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
        val label = labelList[speciesId]
        binding.photoName.setText(label.split("_").last())
        binding.photoName.setVisibility(View.VISIBLE)
        binding.photoLatinname.setText(label.split("_").first())
        binding.photoLatinname.setVisibility(View.VISIBLE)
        binding.photoReload.setVisibility(View.VISIBLE)
        binding.photoEbird.setVisibility(View.VISIBLE)
        binding.photoEbird.setTag(speciesId)
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
            Log.e("BirdInfoActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
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
            Log.e("BirdInfoActivity", "Failed to read labels ${"taxo_code.txt"}: ${e.message}")
        }
    }

    /** The names of the birds, in the reader's language. See [BirdNames]. */
    private fun loadLabels(context: Context) {
        labelList = BirdNames.load(context)
    }

    companion object {

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

    private fun clearPhoto() {
        photoUrl = null
        binding.photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        binding.photo.setImageResource(R.drawable.ic_launcher_monochrome)
        binding.photoName.setText("")
        binding.photoName.setVisibility(View.GONE)
        binding.photoLatinname.setText("")
        binding.photoLatinname.setVisibility(View.GONE)
        binding.photoReload.setVisibility(View.GONE)
        binding.photoEbird.setVisibility(View.GONE)
    }

    fun reload(view: View) {
        photoUrl?.let { showPhoto(it, true) }
    }


    fun ebird(view: View) {
        // The tag holds the species, not a row: with the list in Compose there are no row
        // positions to look one up from, and the species was what this ever wanted.
        val id = binding.photoEbird.tag as Int
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ebird.org/species/"+eBirdList[id])))
    }

}
