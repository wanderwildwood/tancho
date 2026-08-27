package com.wanderwildwood.einkbirding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.setContent
import com.mudita.mmd.ThemeMMD

class SettingsActivity : BaseActivity() {

    /** Drawn in Compose with its own bar, so the ActionBar would be a second title. */
    override fun applyTheme() {
        setTheme(R.style.AppTheme_NoActionBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        setContent {
            ThemeMMD {
                SettingsScreen(
                    settings = settings,
                    onChooseLanguage = ::chooseLanguage,
                )
            }
        }
    }

    /**
     * The language is Android's to set, not the app's: the system keeps a per-app choice
     * from Android 13 on, and this hands off to the screen that owns it.
     */
    private fun chooseLanguage() {
        startActivity(
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
        )
    }
}
