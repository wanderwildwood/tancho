package com.wanderwildwood.tancho

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Opens a web address in the browser the phone actually has, and says so when there is none.
 *
 * A stock Kompakt registers the AOSP WebView test shell for web addresses; it is a developer
 * tool, not a browser, and it crashes on the first real page. It is treated as nothing at all:
 * when it is what the phone would pick, a real browser is named explicitly, and with no other
 * the reader is told there is none. The app never draws the page itself, so nothing here can
 * be taken down by a WebView.
 */
object BrowserLinks {

    /** The AOSP WebView test harness. */
    const val WEBVIEW_SHELL = "org.chromium.webview_shell"

    /**
     * Which package should open the address: null to leave it to the system (its own pick is a
     * real browser), a package name to name it, or [NONE] when only the test shell is there.
     */
    fun choose(browsers: List<String>, systemPick: String?): String? {
        val real = browsers.filter { it != WEBVIEW_SHELL }
        return when {
            real.isEmpty() -> NONE
            systemPick != null && systemPick in real -> null
            else -> real.first()
        }
    }

    const val NONE = "\u0000none"

    fun open(context: Context, address: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, address).addCategory(Intent.CATEGORY_BROWSABLE)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            val pm = context.packageManager
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
            val found = pm.queryIntentActivities(probe, PackageManager.MATCH_ALL).mapNotNull { it.activityInfo?.packageName }.distinct()
            val pick = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            when (val choice = choose(found, pick)) {
                NONE -> return failed(context)
                null -> Unit
                else -> intent.setPackage(choice)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            failed(context)
        } catch (e: SecurityException) {
            failed(context)
        }
    }

    fun open(context: Context, address: String): Boolean = open(context, Uri.parse(address))

    private fun failed(context: Context): Boolean {
        Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
        return false
    }
}
