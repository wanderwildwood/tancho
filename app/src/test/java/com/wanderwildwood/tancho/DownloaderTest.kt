package com.wanderwildwood.tancho

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderTest {
    @Test fun `sha256 of a known file`() {
        val f = File.createTempFile("sha", ".bin")
        f.writeText("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Downloader.sha256(f))
        f.delete()
    }

    @Test fun `an unreadable file hashes to nothing and is never accepted`() {
        val sha = Downloader.sha256(File("/no/such/file"))
        assertEquals("", sha)
        assertFalse(Downloader.modelHashOk(sha))
        assertFalse(Downloader.metaHashOk(sha))
    }

    @Test fun `the pinned hashes are sha-256 and the urls are one fixed commit`() {
        listOf(Downloader.model16SHA256, Downloader.model32SHA256, Downloader.metaModelSHA256)
            .forEach { assertTrue(Regex("[0-9a-f]{64}").matches(it)) }
        listOf(Downloader.model16URL, Downloader.model32URL, Downloader.metaModelURL)
            .forEach { assertFalse(it.contains("/master/")); assertTrue(Regex("/[0-9a-f]{40}/").containsMatchIn(it)) }
    }
}
