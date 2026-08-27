package com.wanderwildwood.einkbirding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
enum class Destination(val viewId: Int, val label: Int, val target: Class<out Activity>) {
    LISTENING(R.id.destination_listening, R.string.destination_listening, MainActivity::class.java),
    HEARD(R.id.destination_heard, R.string.destination_observations, ViewActivity::class.java),
    SPECIES(R.id.destination_species, R.string.destination_species, BirdInfoActivity::class.java),
    SETTINGS(R.id.destination_settings, R.string.destination_settings, SettingsActivity::class.java),
}

/** See [wireDestinations]; this is the same row, for the screens that are drawn in Compose. */
@Composable
fun DestinationRowCompose(current: Destination) {
    val context = LocalContext.current
    HorizontalDividerMMD()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (destination in Destination.entries) {
            if (destination == current) continue
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { context.open(destination) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(
                    text = stringResource(destination.label),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun Context.open(destination: Destination) =
    startActivity(
        Intent(this, destination.target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    )

/**
 * Wire the row up, leaving out the screen doing the wiring.
 *
 * Destinations are reordered to the front rather than started afresh. Four screens that can
 * each reach the other three will otherwise stack a new copy per tap, and the back button
 * then walks the whole history one screen at a time instead of leaving.
 */
fun Activity.wireDestinations(current: Destination) {
    for (destination in Destination.entries) {
        val view: View = findViewById(destination.viewId) ?: continue
        if (destination == current) {
            view.visibility = View.GONE
            continue
        }
        view.setOnClickListener {
            startActivity(
                Intent(this, destination.target)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }
}
