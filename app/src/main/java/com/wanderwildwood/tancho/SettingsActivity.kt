package com.wanderwildwood.tancho

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import com.mudita.mmd.ThemeMMD
import androidx.preference.PreferenceManager
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
            ThemeMMD(colorScheme = monochrome, typography = houseTypography) {
                ArmedScope {
                  // A blinking cursor repaints its field twice a second on a panel that cannot afford it.
                  CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
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
                            onExportLog = { settleLog(); exportLog() },
                            onSaveBackup = { settleLog(); saveBackup() },
                            onRestoreBackup = { settleLog(); restoreBackup() },
                            onDeleteLog = ::deleteLog,
                            logGone = logGone.value,
                            onUndoLog = ::undoLog,
                        )
                    }
                  }
                }
            }
        }
    }

    /** Every observation as a CSV file, saved wherever the reader chooses with the file picker. */
    private fun exportLog() {
        val stamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.ROOT))
        exportLauncher.launch(
            LogExport.fileName(resources.getString(R.string.app_name), stamp)
        )
    }

    private val exportLauncher =
        registerForActivityResult(FilesCreateDocument("text/csv")) { uri ->
            if (uri != null) performExport(uri)
        }

    private fun performExport(uri: Uri) {
        try {
            val lines = BirdDBHelper.getInstance(this).exportAllEntriesAsCSV()
            contentResolver.openOutputStream(uri)!!.use {
                it.write(LogExport.text(lines).toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_LONG).show()
        }
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
        resultLauncher.launch(FilesPicker.aim(this, intent, writable = true))
    }

    /** The log just deleted, held (rows, and the recordings left alone) until the reader moves on. */
    private val undoLogWindow = UndoWindow<List<BirdObservation>>()
    private val logGone = androidx.compose.runtime.mutableStateOf(false)

    /** Confirmed on the row itself before it gets here. Undo is on offer until the reader moves on. */
    private fun deleteLog() {
        settleLog()
        val db = BirdDBHelper.getInstance(this)
        undoLogWindow.hold(db.allRows())
        db.clearAllEntries()
        logGone.value = true
    }

    private fun undoLog() {
        val rows = undoLogWindow.release() ?: return
        logGone.value = false
        BirdDBHelper.getInstance(this).restoreRows(rows)
    }

    /** Makes a deleted log final: the recordings go now. */
    private fun settleLog() {
        logGone.value = false
        if (undoLogWindow.release() == null) return
        WavUtils.sweepOrphans(this, BirdDBHelper.getInstance(this).allTimestamps())
    }

    override fun onStop() {
        super.onStop()
        settleLog()
    }

    /**
     * Reads a backup back in.
     *
     * The zip holds the databases folder, the recordings the log named and the settings, so
     * this replaces the log and the settings rather than merging - there is no sensible way to
     * reconcile two logs of the same mornings. Recordings join the ones already kept; a backup
     * from before they were included has none, and a row whose recording is gone still reads,
     * it just has nothing to play.
     *
     * The whole file is unpacked and the log opened before anything changes, and the work is
     * done off the main thread: a backup with a season of recordings is hundreds of megabytes.
     */
    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        restoreLauncher.launch(FilesPicker.aim(this, intent))
    }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { uri ->
                Toast.makeText(this, R.string.restore_working, Toast.LENGTH_SHORT).show()
                val app = applicationContext
                Thread {
                    val (message, restored) = performRestore(app, uri)
                    runOnUiThread {
                        Toast.makeText(app, message, Toast.LENGTH_LONG).show()
                        // The settings on screen were read before the restore; read them again.
                        if (restored && !isFinishing && !isDestroyed) recreate()
                    }
                }.start()
            }
        }

    /** Does the restore and says how it went: the sentence to show, and whether anything changed. */
    private fun performRestore(context: android.content.Context, uri: Uri): Pair<String, Boolean> {
        val databases = context.getDatabasePath(BirdDBHelper.DB_NAME).parentFile!!
        val staging = File(context.cacheDir, "restore-staging")
        try {
            val unpacked = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BackupArchive.unpack(input, staging, BirdDBHelper.DB_NAME)
                } ?: return context.getString(R.string.restore_unreadable) to false
            } catch (e: BackupArchive.Refused) {
                return context.getString(
                    if (e.kind == BackupArchive.Refused.Kind.NOT_A_BACKUP) R.string.restore_failed else R.string.restore_damaged
                ) to false
            } catch (e: java.io.IOException) {
                return context.getString(R.string.restore_unreadable) to false
            }

            val stagedDb = File(unpacked.databases, BirdDBHelper.DB_NAME)
            val opens = runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    stagedDb.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                ).use { db -> db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() } }
            }.getOrDefault(false)
            if (!opens) return context.getString(R.string.restore_damaged) to false

            // Close the open handle first: moving a database out from under SQLite leaves the
            // process holding a file that no longer exists.
            // ⚠ Unpacked and checked beside the log, then swapped in -- never deleted first,
            // so a damaged backup or a full disk part way through leaves the log as it was.
            BirdDBHelper.getInstance(context).close()
            val previous = File(databases.parentFile, "databases-before-restore").apply { deleteRecursively() }
            val setAside = !databases.exists() || databases.renameTo(previous)
            val placed = setAside && unpacked.databases.renameTo(databases)
            if (!placed) {
                // Whatever happened, the log that was there goes back where it was.
                if (!databases.exists() && previous.exists()) previous.renameTo(databases)
                BirdDBHelper.reopen(context)
                return context.getString(R.string.restore_unreadable) to false
            }
            previous.deleteRecursively()
            BirdDBHelper.reopen(context)

            val recordingsDir = WavUtils.recordingsDir(context)
            val sounds = if (recordingsDir == null) 0 else BackupArchive.placeRecordings(unpacked.recordings, recordingsDir)
            unpacked.settings?.let { applySettings(context, it) }
            val lines = BirdDBHelper.getInstance(context).allTimestamps().size
            return context.getString(R.string.restore_done_counts, lines, sounds) to true
        } catch (e: Exception) {
            e.printStackTrace()
            return context.getString(R.string.restore_unreadable) to false
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun applySettings(context: android.content.Context, values: Map<String, Any>) {
        val edit = PreferenceManager.getDefaultSharedPreferences(context).edit()
        for ((key, value) in values) when (value) {
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is Float -> edit.putFloat(key, value)
            is String -> edit.putString(key, value)
        }
        edit.commit()
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                result.data?.data?.let { uri ->
                    Toast.makeText(this, R.string.backup_working, Toast.LENGTH_SHORT).show()
                    val app = applicationContext
                    Thread {
                        val ok = performBackup(app, uri)
                        runOnUiThread {
                            Toast.makeText(app, if (ok) R.string.backup_saved else R.string.backup_failed,
                                if (ok) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                        }
                    }.start()
                }
            }
        }

    /**
     * The log's database folder, the recordings its lines name and the settings, zipped straight
     * into the file the reader chose (no copy in the cache first: the recordings can be large).
     */
    private fun performBackup(context: android.content.Context, uri: Uri): Boolean {
        val databases = context.getDatabasePath(BirdDBHelper.DB_NAME).parentFile!!
        return try {
            val logged = BirdDBHelper.getInstance(context).allTimestamps()
            val recordings = BackupArchive.recordingsInLog(WavUtils.recordings(context).toList(), logged)
            val settings = BackupArchive.settingsText(PreferenceManager.getDefaultSharedPreferences(context).all)
            context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                BackupArchive.write(out.buffered(), databases, recordings, settings)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
            false
        }
    }
}
