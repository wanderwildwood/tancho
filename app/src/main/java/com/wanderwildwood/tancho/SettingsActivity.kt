package com.wanderwildwood.tancho

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        val activity = this
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                // The one place in the app with a screen behind a screen, so it is held
                // here rather than by a second activity: one window, and back is back.
                var showLanguages by remember { mutableStateOf(false) }
                // Held out here so that choosing a language comes back to the row you
                // chose it from, near the bottom, rather than to the top of the list.
                var chosen by remember { mutableStateOf(BirdNames.chosen(activity)) }

                if (showLanguages) {
                    BackHandler { showLanguages = false }
                    LanguageScreen(
                        chosen = chosen,
                        inUse = remember(chosen) { BirdNames.inUse(activity) },
                        onChoose = { code ->
                            BirdNames.choose(activity, code)
                            chosen = code
                            showLanguages = false
                        },
                    )
                } else {
                    SettingsScreen(
                        settings = settings,
                        birdNames = remember(chosen) { BirdNames.nameInUse(activity) },
                        onChooseLanguage = { showLanguages = true },
                        onExportLog = ::exportLog,
                        onSaveBackup = ::saveBackup,
                        onRestoreBackup = ::restoreBackup,
                        onDeleteLog = ::deleteLog,
                    )
                }
            }
        }
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
        WavUtils.sweepOrphans(this, emptySet())
        Toast.makeText(this, getString(R.string.clear_db), Toast.LENGTH_SHORT).show()
    }

    /**
     * Reads a backup back in.
     *
     * The zip holds the databases folder and nothing else, so this replaces the log rather
     * than merging into it - there is no sensible way to reconcile two logs of the same
     * mornings. Recordings are not in a backup and are not restored: a row whose recording
     * is gone still reads, it just has nothing to play.
     */
    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        restoreLauncher.launch(intent)
    }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { performRestore(it) }
        }

    private fun performRestore(uri: Uri) {
        val databases = File(Environment.getDataDirectory(), "//data//$packageName//databases//")
        val staged = File(cacheDir, "restore.zip")
        try {
            contentResolver.openInputStream(uri).use { input ->
                staged.outputStream().use { output -> input!!.copyTo(output) }
            }

            // Refuse anything that is not one of ours before touching the live database.
            val zip = ZipFile(staged)
            val looksRight = zip.fileHeaders.any { it.fileName.endsWith(BirdDBHelper.DB_NAME) }
            if (!looksRight) {
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                return
            }

            // ⚠ Unpacked and checked beside the log, then swapped in -- never deleted first,
            // which is what this did: every file in the databases folder went, then the backup
            // was extracted, so a damaged backup or a full disk part way through left no log
            // at all and only part of the new one.
            val staging = File(cacheDir, "restore-staging").apply { deleteRecursively(); mkdirs() }
            zip.extractAll(staging.path)
            val stagedDb = staging.walkTopDown().firstOrNull { it.name == BirdDBHelper.DB_NAME }
            val opens = stagedDb != null && runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    stagedDb.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                ).use { db -> db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() } }
            }.getOrDefault(false)
            if (!opens) {
                staging.deleteRecursively()
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                return
            }

            // Close the open handle first: moving a database out from under SQLite leaves the
            // process holding a file that no longer exists.
            BirdDBHelper.getInstance(this).close()
            val previous = File(databases.parentFile, "databases-before-restore").apply { deleteRecursively() }
            val setAside = !databases.exists() || databases.renameTo(previous)
            val placed = setAside && stagedDb!!.parentFile!!.renameTo(databases)
            if (!placed) {
                // Whatever happened, the log that was there goes back where it was.
                if (!databases.exists() && previous.exists()) previous.renameTo(databases)
                staging.deleteRecursively()
                BirdDBHelper.reopen(this)
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                return
            }
            previous.deleteRecursively()
            staging.deleteRecursively()
            BirdDBHelper.reopen(this)
            // Recordings belong to lines; those the restored log has no line for go.
            WavUtils.sweepOrphans(this, BirdDBHelper.getInstance(this).allTimestamps())
            Toast.makeText(this, getString(R.string.restore_done), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            staged.delete()
        }
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
