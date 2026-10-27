package com.wanderwildwood.tancho

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * The four screens, and the one row that moves between them.
 *
 * There was a second navigation before this: the listening screen offered three words and
 * the other three screens offered a bar of four icons, so the same app moved two different
 * ways depending on where you were standing. This is the icon bar's replacement, and it
 * says what the listening screen says.
 */
enum class Destination(val label: Int, val target: Class<out Activity>) {
    LISTENING(R.string.destination_listening, MainActivity::class.java),
    HEARD(R.string.destination_observations, ViewActivity::class.java),
    SPECIES(R.string.destination_species, BirdInfoActivity::class.java),
    SETTINGS(R.string.destination_settings, SettingsActivity::class.java),
}

/**
 * The app's navigation, drawn once for every screen.
 *
 * There used to be two of these - this, and a LinearLayout of styled TextViews for the two
 * screens that are not Compose - and they drifted, as two of anything does: the XML one was
 * bold where this one is not. The other two screens host this in a ComposeView instead, so
 * there is one row and it cannot go out of step with itself again.
 */
@Composable
fun DestinationRowCompose(current: Destination) {
    val context = LocalContext.current
    HorizontalDividerMMD()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // All four on every screen, in the same places, with the one you are on in bold:
        // leaving the current one out moved the other three along each time, so the word
        // you pressed last was never where you pressed it. Pressing the bold one does nothing.
        for (destination in Destination.entries) {
            val here = destination == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(if (here) Modifier else Modifier.clickable { context.open(destination) })
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(
                    text = stringResource(destination.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (here) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun Context.open(destination: Destination) =
    startActivity(
        Intent(this, destination.target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    )
