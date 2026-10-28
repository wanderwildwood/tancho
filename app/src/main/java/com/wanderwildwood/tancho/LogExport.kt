package com.wanderwildwood.tancho

/** What the log looks like as a file the reader saves with the system file picker. */
object LogExport {
    /** One line per observation, ending in a newline so the file is a proper text file. */
    fun text(lines: List<String>): String =
        if (lines.isEmpty()) "" else lines.joinToString("\n", postfix = "\n")

    /** A plain sortable stamp, no slashes or colons, so it is a valid file name anywhere. */
    fun fileName(appName: String, stamp: String): String = appName + "_log_" + stamp + ".csv"
}
