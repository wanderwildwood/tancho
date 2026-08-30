package com.wanderwildwood.tancho

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD

/**
 * One bird, on any of the three screens that list birds.
 *
 * There used to be three of these and they agreed about nothing. The listening screen drew a
 * name, a latin name and a line of detail underneath in MMD's type; the log drew a time and a
 * name side by side inside a rounded box whose border weight carried the confidence; the
 * species list drew two plain TextViews at whatever size Android picks by default. Only the
 * first was ever really Mudita's type scale — the other two were RecyclerViews inherited from
 * upstream and never brought over, which is why one screen looked like the app and two looked
 * like something else.
 *
 * One composable now, so they cannot drift again. What varies between the screens is what
 * there is to say: the species list has no time and no confidence, because browsing a list of
 * every bird in the world is not a record of hearing one.
 */
@Composable
fun BirdRow(
    name: String,
    latinName: String,
    modifier: Modifier = Modifier,
    time: String? = null,
    percent: Int? = null,
    times: Int = 1,
    bold: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        TextMMD(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        if (latinName.isNotBlank()) {
            TextMMD(
                text = latinName,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (time != null || percent != null || times > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (time != null) {
                    TextMMD(text = time, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                // Blocks, and no number beside them. What BirdNET reports is a softmax
                // output, not a calibrated probability: "85%" reads as "wrong about one time
                // in seven", which is a promise nothing here can keep. Five squares say how
                // sure it sounds without saying how often it is right.
                if (percent != null) ConfidenceBlocks(percent = percent)
                if (times > 1) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TextMMD(text = "×$times", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * A date, over the readings taken under it.
 *
 * The log is the only one of the three that needs this: a session is one morning and a species
 * list is not in time at all.
 */
@Composable
fun DayHeading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
    )
}

/** Confidence as filled squares. Five of them, one per twenty points. */
@Composable
fun ConfidenceBlocks(percent: Int) {
    val filled = (percent + 19) / 20
    Row {
        repeat(5) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .padding(end = 3.dp)
                    .size(10.dp)
                    .then(
                        if (isFilled) {
                            Modifier.background(MaterialTheme.colorScheme.onSurface)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface)
                        }
                    ),
            )
        }
    }
}
