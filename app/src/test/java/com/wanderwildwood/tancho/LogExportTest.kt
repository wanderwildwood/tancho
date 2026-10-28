package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Test

class LogExportTest {
    @Test fun linesEndWithNewline() {
        assertEquals("1,2.0,3.0,Wren,5,0.9\n7,2.0,3.0,Robin,6,0.8\n",
            LogExport.text(listOf("1,2.0,3.0,Wren,5,0.9", "7,2.0,3.0,Robin,6,0.8")))
    }

    @Test fun emptyLogIsEmptyFile() {
        assertEquals("", LogExport.text(emptyList()))
    }

    @Test fun fileNameHasNoSlashOrColon() {
        val n = LogExport.fileName("Birding", "2026-10-27_08-30")
        assertEquals("Birding_log_2026-10-27_08-30.csv", n)
    }
}
