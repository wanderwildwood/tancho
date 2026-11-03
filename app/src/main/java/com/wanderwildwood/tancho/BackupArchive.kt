package com.wanderwildwood.tancho

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * What a backup file holds, and the reading and writing of it, kept apart from Android so it
 * can be tested on its own.
 *
 * A backup is one zip: the log's `databases/` folder, as it always was, plus the recordings the
 * log names under `Recordings/` and the settings in `settings.properties`. Backups made before
 * the recordings and settings were added hold the databases folder alone, and still restore.
 *
 * The recordings matter because the app sweeps recordings that no line in the log names, on
 * every start. Before they were in the backup, restoring an older log meant the newer
 * recordings went for good at the next start, and a later restore of a newer backup brought
 * the lines back without their sound.
 *
 * [unpack] takes only the entries it knows, by exact shape, into a staging folder, and checks
 * that the log is there; the caller swaps anything in only after that.
 */
object BackupArchive {

    const val DATABASES = "databases/"
    const val RECORDINGS = "Recordings/"
    const val SETTINGS = "settings.properties"

    enum class Type { BOOLEAN, INT, FLOAT, STRING }

    /** The settings a backup carries, by their stored key and type. The keys are upstream's. */
    val SETTING_KEYS: Map<String, Type> = linkedMapOf(
        "audio_source" to Type.STRING,
        "model_threshold" to Type.INT,
        "high_pass" to Type.INT,
        "meta_model_influence" to Type.FLOAT,
        "play_sound" to Type.BOOLEAN,
        "write_wav" to Type.BOOLEAN,
        "bluetooth" to Type.BOOLEAN,
        "ignore_non_birds" to Type.BOOLEAN,
        "view_detailed" to Type.BOOLEAN,
        "show_images" to Type.BOOLEAN,
        "show_spectrogram" to Type.BOOLEAN,
        "clear_recordings" to Type.BOOLEAN,
        "share_place" to Type.BOOLEAN,
        "manual_location" to Type.BOOLEAN,
        "manual_location_value" to Type.STRING,
        "bird_names_language" to Type.STRING,
    )

    class Refused(val kind: Kind) : Exception(kind.name) {
        enum class Kind { NOT_A_BACKUP, DAMAGED }
    }

    /** A backup unpacked into staging: the databases folder, its recordings, and its settings if it had any. */
    data class Unpacked(val databases: File, val recordings: List<File>, val settings: Map<String, Any>?)

    private val RECORDING_NAME = Regex("""\d{1,19}\.wav""")
    private val PLAIN_NAME = Regex("""[A-Za-z0-9._-]+""")
    private const val MAX_SETTINGS_BYTES = 64 * 1024

    /** The settings worth carrying, from everything stored; anything else, or of the wrong type, is left out. */
    fun settingsText(stored: Map<String, *>): String {
        val p = Properties()
        for ((key, type) in SETTING_KEYS) {
            val value = stored[key] ?: continue
            val fits = when (type) {
                Type.BOOLEAN -> value is Boolean
                Type.INT -> value is Int
                Type.FLOAT -> value is Float
                Type.STRING -> value is String
            }
            if (fits) p.setProperty(key, value.toString())
        }
        return StringWriter().also { p.store(it, null) }.toString()
    }

    /** The settings in a backup, typed; unknown keys are ignored, a known key with a bad value refuses the file. */
    fun readSettings(text: String): Map<String, Any> {
        val p = Properties()
        try {
            p.load(StringReader(text))
        } catch (e: IllegalArgumentException) {
            throw Refused(Refused.Kind.DAMAGED)
        }
        val out = linkedMapOf<String, Any>()
        for ((key, type) in SETTING_KEYS) {
            val raw = p.getProperty(key) ?: continue
            out[key] = when (type) {
                Type.BOOLEAN -> when (raw) {
                    "true" -> true
                    "false" -> false
                    else -> throw Refused(Refused.Kind.DAMAGED)
                }
                Type.INT -> raw.toIntOrNull() ?: throw Refused(Refused.Kind.DAMAGED)
                Type.FLOAT -> raw.toFloatOrNull()?.takeIf { it.isFinite() } ?: throw Refused(Refused.Kind.DAMAGED)
                Type.STRING -> raw
            }
        }
        return out
    }

    /** The recordings worth keeping: those named for a moment the log still has a line for. */
    fun recordingsInLog(files: List<File>, logged: Set<Long>): List<File> =
        files.filter { f -> RECORDING_NAME.matches(f.name) && f.name.dropLast(4).toLongOrNull() in logged }
            .sortedBy { it.name }

    /** Writes the whole backup to [out]: the databases folder's files, then [recordings], then [settings]. */
    fun write(out: OutputStream, databases: File, recordings: List<File>, settings: String) {
        val zip = ZipOutputStream(out)
        val dbFiles = databases.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        for (f in dbFiles) put(zip, DATABASES + f.name, f)
        for (f in recordings) put(zip, RECORDINGS + f.name, f)
        zip.putNextEntry(ZipEntry(SETTINGS))
        zip.write(settings.toByteArray(Charsets.ISO_8859_1))
        zip.closeEntry()
        zip.finish()
        out.flush()
    }

    private fun put(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /**
     * Unpacks [input] into [staging], which is emptied first. Only `…databases/<file>`,
     * `Recordings/<moment>.wav` and the settings are taken; any other entry, and any name that
     * could climb out of the folder, is skipped. Throws [Refused] for a file that is not a
     * backup (no log in it) or that breaks off part way.
     */
    fun unpack(input: InputStream, staging: File, dbName: String): Unpacked {
        staging.deleteRecursively()
        val dbDir = File(staging, "databases").apply { mkdirs() }
        val recDir = File(staging, "Recordings").apply { mkdirs() }
        var settings: Map<String, Any>? = null
        val recordings = mutableListOf<File>()
        try {
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val parts = entry.name.replace('\\', '/').split('/')
                val leaf = parts.last()
                when {
                    parts.size >= 2 && parts[parts.size - 2] == "databases" && PLAIN_NAME.matches(leaf) &&
                        leaf != "." && leaf != ".." ->
                        File(dbDir, leaf).outputStream().use { zip.copyTo(it) }
                    parts.size == 2 && parts[0] == "Recordings" && RECORDING_NAME.matches(leaf) ->
                        recordings += File(recDir, leaf).also { f -> f.outputStream().use { zip.copyTo(it) } }
                    parts.size == 1 && leaf == SETTINGS -> {
                        val bytes = readCapped(zip)
                        settings = readSettings(String(bytes, Charsets.ISO_8859_1))
                    }
                }
            }
        } catch (e: Refused) {
            throw e
        } catch (e: ZipException) {
            throw Refused(Refused.Kind.DAMAGED)
        } catch (e: java.io.EOFException) {
            throw Refused(Refused.Kind.DAMAGED)
        }
        if (!File(dbDir, dbName).isFile) throw Refused(Refused.Kind.NOT_A_BACKUP)
        return Unpacked(dbDir, recordings.sortedBy { it.name }, settings)
    }

    private fun readCapped(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > MAX_SETTINGS_BYTES) throw Refused(Refused.Kind.DAMAGED)
        }
        return out.toByteArray()
    }

    /**
     * Moves restored recordings into [into]. One already there under the same name is the same
     * moment's sound and is kept as it is. Returns how many were placed.
     */
    fun placeRecordings(recordings: List<File>, into: File): Int {
        into.mkdirs()
        var placed = 0
        for (f in recordings) {
            val target = File(into, f.name)
            if (target.exists()) continue
            val moved = f.renameTo(target) || try {
                f.copyTo(target)
                f.delete()
                true
            } catch (e: IOException) {
                target.delete()
                false
            }
            if (moved) placed++
        }
        return placed
    }
}
