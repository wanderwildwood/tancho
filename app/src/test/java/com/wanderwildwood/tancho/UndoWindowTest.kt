package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UndoWindowTest {
    @Test fun `nothing is held to begin with`() {
        assertNull(UndoWindow<String>().held)
        assertNull(UndoWindow<String>().release())
    }

    @Test fun `undo takes back what was held and only once`() {
        val w = UndoWindow<String>()
        assertNull(w.hold("first"))
        assertEquals("first", w.release())
        assertNull(w.release())
    }

    @Test fun `a second delete makes the first final`() {
        val w = UndoWindow<String>()
        w.hold("first")
        assertEquals("first", w.hold("second"))
        assertEquals("second", w.held)
    }
}
