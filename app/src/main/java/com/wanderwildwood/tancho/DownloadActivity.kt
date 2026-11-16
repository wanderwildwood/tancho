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

/**
 * The launcher's entry, still under its old name so a home-screen slot pointing at it keeps
 * working. The model used to be downloaded here; it is in the APK now, so all this screen does
 * is check both models against their SHA-256 and go on to listening. Only if a model is not the
 * file this version was built with does it stay, and say so.
 */
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
        // Privacy, Terms and Licences are readable here even when the model is refused.
        binding!!.aboutHost.setContent {
            ThemeMMD(colorScheme = monochrome, typography = houseTypography) { DarkSelection {
                // No ArmedScope here: this ComposeView lies over the whole screen, and a scope's
                // touch watcher would take the taps meant for the button under it. The About
                // dialog is its own window and has no armed row.
                CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
                }
            } }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ModelFiles.check(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            binding?.modelDamaged?.visibility = View.VISIBLE
        }
    }

    fun about(view: View) {
        showAbout = true
    }
}
