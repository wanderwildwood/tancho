package com.wanderwildwood.tancho

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.mudita.mmd.ThemeMMD
import net.lingala.zip4j.ZipFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Settings, and the three things you can do to the log.
 *
 * Exporting, backing up and clearing the log used to be icons on the Heard screen's action
 * bar, declared `showAsAction="always"` with an icon and no title. Nothing drew them: a
 * dump of the running screen's views found the bar holding its title and no menu items at
 * all, so in every released version those three have been unreachable. They are here now
 * because this is where someone goes looking for them, and because the bar they were
 * nailed to has been taken down.
 */
class SettingsActivity : BaseActivity() {

    /** Drawn in Compose, and with no title bar anywhere in the app. */
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
                    onExportLog = ::exportLog,
                    onSaveBackup = ::saveBackup,
                    onDeleteLog = ::deleteLog,
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

    /** Every observation as text, handed to whatever the reader wants to send it with. */
    private fun exportLog() {
        val database = BirdDBHelper.getInstance(this)
        val intent = Intent(Intent.ACTION_SEND)
        intent.setType("text/plain")
        intent.putExtra(Intent.EXTRA_TEXT, database.exportAllEntriesAsCSV().joinToString("\n"))
        startActivity(Intent.createChooser(intent, ""))
    }

    /** The database itself, zipped, wherever the reader chooses to put it. */
    private fun saveBackup() {
        // A plain sortable stamp, not the reader's locale date. Upstream offered the name
        // through ofLocalizedDate(SHORT), which in most of the world contains slashes or
        // dots - "Birding_8/27/26_8:30 PM" is not a filename, and a colon is no
        // better. This also puts a folder of backups in the order they were made.
        val stamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.ROOT))
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        intent.putExtra(
            Intent.EXTRA_TITLE,
            resources.getString(R.string.app_name) + "_" + stamp + ".zip"
        )
        resultLauncher.launch(intent)
    }

    /** Confirmed on the row itself before it gets here. There is no undo. */
    private fun deleteLog() {
        BirdDBHelper.getInstance(this).clearAllEntries()
        Toast.makeText(this, getString(R.string.clear_db), Toast.LENGTH_SHORT).show()
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                result.data?.data?.let { performBackup(it) }
            }
        }

    private fun performBackup(uri: Uri) {
        val intData = File(
            Environment.getDataDirectory().toString() + "//data//" + this.packageName + "//databases//"
        )
        try {
            val tmpFile = File(cacheDir, "backup.zip")
            if (tmpFile.exists()) tmpFile.delete()
            ZipFile(tmpFile).addFolder(intData)
            val srcStream = tmpFile.inputStream()
            val dstStream = contentResolver.openOutputStream(uri)!!
            val buffer = ByteArray(1024)
            var read: Int
            while ((srcStream.read(buffer).also { read = it }) != -1) {
                dstStream.write(buffer, 0, read)
            }
            srcStream.close()
            dstStream.close()
            tmpFile.delete()
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
