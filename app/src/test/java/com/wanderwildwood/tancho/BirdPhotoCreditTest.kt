package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BirdPhotoCreditTest {
    private val by = "Photo: %1\$s, Macaulay Library, Cornell Lab of Ornithology"
    private val plain = "Macaulay Library, Cornell Lab of Ornithology"

    @Test fun `no photographer gives the Library line alone`() {
        assertEquals(plain, BirdPhoto.creditLine(null, by, plain))
        assertEquals(plain, BirdPhoto.creditLine("  ", by, plain))
    }

    @Test fun `a photographer is named first`() {
        assertEquals("Photo: Ann Lee, Macaulay Library, Cornell Lab of Ornithology", BirdPhoto.creditLine(" Ann Lee ", by, plain))
    }

    @Test fun `the strings carry the same wording and the credit is shown in all three places`() {
        val strings = File("src/main/res/values/strings.xml").readText()
        assertTrue(strings.contains(">$plain<"))
        assertTrue(strings.contains(">Photo: %1\$s, $plain<"))
        listOf("activity_bird_info.xml", "activity_view.xml").forEach {
            assertTrue(File("src/main/res/layout/$it").readText().contains("@+id/photo_credit"))
        }
        assertTrue(File("src/main/java/com/wanderwildwood/tancho/ListeningScreen.kt").readText().contains("R.string.photo_credit"))
    }
}
