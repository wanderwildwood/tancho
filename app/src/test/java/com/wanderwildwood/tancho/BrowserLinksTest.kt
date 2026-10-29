package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserLinksTest {
    private val shell = BrowserLinks.WEBVIEW_SHELL

    @Test fun `only the test shell means no browser`() {
        assertEquals(BrowserLinks.NONE, BrowserLinks.choose(listOf(shell), shell))
        assertEquals(BrowserLinks.NONE, BrowserLinks.choose(emptyList(), null))
    }

    @Test fun `a real browser the system picks is left to it`() {
        assertNull(BrowserLinks.choose(listOf(shell, "org.example.browser"), "org.example.browser"))
    }

    @Test fun `when the system would pick the test shell a real browser is named`() {
        assertEquals("org.example.browser", BrowserLinks.choose(listOf(shell, "org.example.browser"), shell))
        assertEquals("org.example.browser", BrowserLinks.choose(listOf("org.example.browser"), null))
    }
}
