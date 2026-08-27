package com.wanderwildwood.einkbirding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The listening screen.
 *
 * It is a log, not a meter. While nothing is heard, nothing on it moves; a bird appends a
 * row, and a bird that goes on singing thickens the row it already has rather than adding
 * more. Confidence is a count of filled blocks and a number, never a colour, because the
 * panel is grey and the difference between orange and yellow does not survive it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(
    isListening: Boolean,
    heard: List<Heard>,
    location: ListeningState.Location?,
    placeAndDate: PlaceAndDate,
    onToggleListening: () -> Unit,
    onCyclePlaceAndDate: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD(stringResource(R.string.app_name)) },
        )

        StatusLine(isListening = isListening, location = location)
        PlaceAndDateRow(placeAndDate = placeAndDate, onCycle = onCyclePlaceAndDate)
        HorizontalDividerMMD()

        Box(modifier = Modifier.weight(1f)) {
            if (heard.isEmpty()) {
                TextMMD(
                    text = stringResource(
                        if (isListening) R.string.nothing_heard_yet else R.string.stopped_explainer
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                )
            } else {
                LazyColumnMMD(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(heard, key = { it.firstHeard }) { entry ->
                        HeardRow(entry)
                    }
                }
            }
        }

        HorizontalDividerMMD()
        ButtonMMD(
            onClick = onToggleListening,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextMMD(
                stringResource(if (isListening) R.string.stop_listening else R.string.start_listening)
            )
        }
        DestinationRowCompose(current = Destination.LISTENING)
    }
}

/**
 * While the recorder is running, the word is followed by one dot, then two, then three,
 * and round again. It is the only thing on the screen that moves, which is the whole of
 * why it is there: a log that has said nothing for ten minutes looks exactly the same
 * whether the microphone is open or the app died quietly an hour ago.
 *
 * The dots are appended here rather than written into the string, so no translation has
 * to know about them.
 */
private const val ELLIPSIS_PERIOD_MS = 700L

@Composable
private fun StatusLine(isListening: Boolean, location: ListeningState.Location?) {
    var dots by remember { mutableIntStateOf(1) }

    // Keyed on isListening so that stopping cancels it rather than leaving a loop running
    // against a screen that is no longer saying anything.
    LaunchedEffect(isListening) {
        if (!isListening) return@LaunchedEffect
        dots = 1
        while (true) {
            delay(ELLIPSIS_PERIOD_MS)
            dots = dots % 3 + 1
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(
            text = if (isListening) {
                stringResource(R.string.listening) + ".".repeat(dots)
            } else {
                stringResource(R.string.stopped)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        if (location != null) {
            TextMMD(
                text = "%.2f, %.2f".format(location.latitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * The where-and-when model's say in the answer, as three taps rather than a dragged
 * slider: a drag on e-ink repaints the whole track for every pixel of movement, and the
 * value was never that precise to begin with.
 *
 * The value is boxed. Everything else on this screen is a label or a log line, so a bare
 * word here reads as a readout of something rather than a control you can change - and
 * this is the one control on the screen that decides which birds are allowed to appear.
 */
@Composable
private fun PlaceAndDateRow(placeAndDate: PlaceAndDate, onCycle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(
            text = stringResource(R.string.place_and_date),
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.onSurface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            TextMMD(
                text = stringResource(placeAndDate.label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HeardRow(entry: Heard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        TextMMD(
            text = entry.name,
            style = MaterialTheme.typography.titleMedium,
        )
        TextMMD(
            text = entry.latinName,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = clockTime(entry.firstHeard),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.width(12.dp))
            ConfidenceBlocks(percent = entry.shownPercent)
            Spacer(modifier = Modifier.width(8.dp))
            TextMMD(
                text = "${entry.shownPercent}%",
                style = MaterialTheme.typography.bodySmall,
            )
            if (entry.times > 1) {
                Spacer(modifier = Modifier.width(12.dp))
                TextMMD(
                    text = "×${entry.times}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Confidence as filled squares. Five of them, one per twenty points. */
@Composable
private fun ConfidenceBlocks(percent: Int) {
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
                    )
            )
        }
    }
}

private fun clockTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
