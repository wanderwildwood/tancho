package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val db = "BirdDatabase.db"

    private fun databases(): File = tmp.newFolder("live-databases").apply {
        File(this, db).writeText("the log")
        File(this, "$db-journal").writeText("")
    }

    private fun recording(dir: File, at: Long, body: String = "sound $at"): File =
        File(dir, "$at.wav").apply { writeText(body) }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, body) in entries) {
                z.putNextEntry(ZipEntry(name)); z.write(body.toByteArray()); z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun refusedAs(kind: BackupArchive.Refused.Kind, block: () -> Unit) {
        try {
            block()
            fail("expected $kind")
        } catch (e: BackupArchive.Refused) {
            assertEquals(kind, e.kind)
        }
    }

    @Test
    fun aBackupCarriesTheLogItsRecordingsAndTheSettings() {
        val recDir = tmp.newFolder("rec")
        val kept = listOf(recording(recDir, 1_700_000_000_000), recording(recDir, 1_700_000_000_800))
        val settings = BackupArchive.settingsText(
            mapOf("write_wav" to true, "model_threshold" to 45, "meta_model_influence" to 60f, "bird_names_language" to "de"),
        )
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, databases(), kept, settings)

        val back = BackupArchive.unpack(ByteArrayInputStream(out.toByteArray()), tmp.newFolder("staging"), db)
        assertEquals("the log", File(back.databases, db).readText())
        assertEquals(listOf("1700000000000.wav", "1700000000800.wav"), back.recordings.map { it.name })
        assertEquals("sound 1700000000800", back.recordings[1].readText())
        assertEquals(
            mapOf("model_threshold" to 45, "meta_model_influence" to 60f, "write_wav" to true, "bird_names_language" to "de"),
            back.settings,
        )
    }

    @Test
    fun onlyRecordingsTheLogNamesGoIntoTheBackup() {
        val recDir = tmp.newFolder("rec")
        val a = recording(recDir, 100)
        recording(recDir, 200)
        File(recDir, "notes.txt").writeText("not ours")
        assertEquals(listOf(a), BackupArchive.recordingsInLog(recDir.listFiles()!!.toList(), setOf(100L, 300L)))
    }

    @Test
    fun anOlderBackupWithTheLogAloneStillRestores() {
        val back = BackupArchive.unpack(
            ByteArrayInputStream(zipOf("databases/$db" to "old log", "databases/$db-journal" to "")),
            tmp.newFolder("staging"), db,
        )
        assertEquals("old log", File(back.databases, db).readText())
        assertTrue(back.recordings.isEmpty())
        assertNull(back.settings)
    }

    @Test
    fun aFileWithoutTheLogIsNotABackup() {
        refusedAs(BackupArchive.Refused.Kind.NOT_A_BACKUP) {
            BackupArchive.unpack(ByteArrayInputStream(zipOf("photo.jpg" to "x")), tmp.newFolder("s1"), db)
        }
        refusedAs(BackupArchive.Refused.Kind.NOT_A_BACKUP) {
            BackupArchive.unpack(ByteArrayInputStream("plain text, not a zip".toByteArray()), tmp.newFolder("s2"), db)
        }
    }

    @Test
    fun aBrokenOffBackupIsRefused() {
        val noise = java.util.Random(7).let { r -> String(CharArray(20_000) { 'a' + r.nextInt(26) }) }
        val whole = zipOf("databases/$db" to noise, SETTINGS_ENTRY to "write_wav=true")
        refusedAs(BackupArchive.Refused.Kind.DAMAGED) {
            BackupArchive.unpack(ByteArrayInputStream(whole.copyOf(whole.size / 3)), tmp.newFolder("s"), db)
        }
    }

    @Test
    fun namesThatClimbOutOfTheFolderAreSkipped() {
        val staging = tmp.newFolder("deep", "staging")
        val back = BackupArchive.unpack(
            ByteArrayInputStream(
                zipOf(
                    "databases/$db" to "the log",
                    "databases/../../escaped" to "no",
                    "Recordings/../escaped.wav" to "no",
                    "Recordings/123.wav/../../x.wav" to "no",
                    "../escaped" to "no",
                ),
            ),
            staging, db,
        )
        assertFalse(File(staging.parentFile, "escaped").exists())
        assertFalse(File(staging.parentFile.parentFile, "escaped").exists())
        assertTrue(back.recordings.isEmpty())
    }

    @Test
    fun aSettingOfTheWrongKindRefusesTheFileAndUnknownOnesAreIgnored() {
        refusedAs(BackupArchive.Refused.Kind.DAMAGED) {
            BackupArchive.unpack(
                ByteArrayInputStream(zipOf("databases/$db" to "log", SETTINGS_ENTRY to "model_threshold=lots")),
                tmp.newFolder("s"), db,
            )
        }
        assertEquals(mapOf("write_wav" to false), BackupArchive.readSettings("write_wav=false\nsomething_else=1\n"))
        // Not a setting worth carrying, or not the type it is stored as: left out of the file.
        val text = BackupArchive.settingsText(mapOf("write_wav" to "yes", "last_seen" to 5L, "bluetooth" to true))
        assertEquals(mapOf("bluetooth" to true), BackupArchive.readSettings(text))
    }

    @Test
    fun restoredRecordingsJoinTheOnesThereWithoutReplacingAny() {
        val live = tmp.newFolder("live")
        recording(live, 100, "kept as it was")
        recording(live, 900, "newer than the backup")
        val staged = tmp.newFolder("staged")
        val placed = BackupArchive.placeRecordings(listOf(recording(staged, 100, "from backup"), recording(staged, 200)), live)
        assertEquals(1, placed)
        assertEquals("kept as it was", File(live, "100.wav").readText())
        assertEquals("sound 200", File(live, "200.wav").readText())
        assertEquals("newer than the backup", File(live, "900.wav").readText())
    }

    private companion object {
        const val SETTINGS_ENTRY = BackupArchive.SETTINGS
    }
}
