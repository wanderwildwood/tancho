package com.wanderwildwood.tancho

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicencesTest {
    private val data = LicencesData(
        "tancho", SOURCE_URL, "GPL-3.0-only", LICENCE_COMPONENTS,
        LICENCE_RAW.map { LicenceText(it.first, it.first, it.second) }, listOf(ComponentGroup("x", null, LICENCE_DOWNLOADED)),
    )

    @Test fun theDataHasNoProblems() {
        assertEquals(emptyList<String>(), licencesProblems(data))
        assertTrue(LICENCE_COMPONENTS.isNotEmpty())
    }

    @Test fun theSourceLinkIsTheRepo() {
        assertEquals("https://github.com/wanderwildwood/tancho", SOURCE_URL)
    }

    @Test fun gplIsFirstAndEveryTextIsBundled() {
        assertEquals("gpl", orderedTexts(data.texts).first().id)
        val raw = File("src/main/res/raw")
        assertTrue(raw.listFiles().orEmpty().count { it.extension == "txt" } >= LICENCE_RAW.size)
        assertTrue(licenceParagraphs(File(raw, raw.list().orEmpty().first { it.contains("gpl") }).readText()).size > 10)
    }
}
