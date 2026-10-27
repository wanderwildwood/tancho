package com.wanderwildwood.tancho

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens a web address in whatever browser the phone has, and says so when there is none.
 *
 * The Kompakt ships a WebView shell that is switched off, so a bare startActivity can find
 * nothing to open the address with. That is a plain failure to report, not a crash, and the
 * app never draws the page itself: no WebView, so nothing here can be taken down by one.
 */
object BrowserLinks {

    fun open(context: Context, address: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, address).addCategory(Intent.CATEGORY_BROWSABLE)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
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
