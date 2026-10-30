package com.wanderwildwood.tancho

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.tancho.databinding.ActivityDownloadBinding

class DownloadActivity  : BaseActivity() {

    /**
     * No ActionBar, like everywhere else. This is the first screen anyone sees, so it was
     * the most visible place left still carrying the app's name in a bar.
     */
    override fun applyTheme() {
        setTheme(R.style.AppTheme_NoActionBar)
    }

    private var binding: ActivityDownloadBinding? = null
    private var showAbout by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        // Privacy, Terms and Licences are readable here, before the first download.
        binding!!.aboutHost.setContent {
            ThemeMMD(colorScheme = monochrome) {
                CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Downloader.isRunning() && Downloader.checkModels(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun download(view: View) {
        binding?.let { Downloader.downloadModels(this, it) }
    }

    fun cancel(view: View) {
        Downloader.cancel()
    }

    fun about(view: View) {
        showAbout = true
    }
}
