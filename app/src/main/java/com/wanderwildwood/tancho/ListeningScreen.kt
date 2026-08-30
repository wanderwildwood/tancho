package com.wanderwildwood.tancho

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

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
    placeAndDate: PlaceAndDate,
    placeKnown: Boolean,
    settings: Settings,
    onPlaceChanged: () -> Unit,
    showPhoto: Boolean,
    photoAssetId: String?,
    onToggleListening: () -> Unit,
    onCyclePlaceAndDate: () -> Unit,
) {
    var askingPlace by remember { mutableStateOf(false) }

    if (askingPlace) {
        PlaceDialog(
            settings = settings,
            onChanged = onPlaceChanged,
            onDismiss = { askingPlace = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // The two controls share one row under the picture. They used to have a row each -
        // 118px of a 800px panel to say a word that never changes, print a location that
        // never changes, and hold a setting touched once a session - which with the
        // photograph on left the log a row and a half.
        //
        // They sat over the photograph for a while, which cost nothing but was a mistake:
        // anything opaque enough to read over a picture is opaque enough to hide it, so the
        // strip took the bottom of every bird with it and clipped the mark that stands in
        // when there is no bird. Below it they cover nothing, and one row instead of two is
        // most of the space back anyway.
        if (showPhoto) PhotoBand(assetId = photoAssetId)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListeningChip(isListening = isListening, onToggle = onToggleListening)
            // Until there is a fix, or a location typed in, this setting is not doing
            // anything: the meta model is left neutral and every answer is weighed the
            // same. Saying "Expected first" over that is the app claiming to know where
            // it is standing. It says what it actually knows instead, and does not offer
            // a choice that would have no effect -- the same rule as the spectrogram
            // switch, which is not shown when there are no recordings to draw.
            val labels = PlaceAndDate.entries.map { stringResource(it.label) } +
                stringResource(R.string.place_and_date_unknown)
            // Known: the tap cycles how much the place counts for, which is one repaint
            // and no screen to leave. Not known: the tap opens the way to fixing that,
            // because the chip is where the reader finds out, and the answer belongs where
            // the question was raised rather than three taps away in settings.
            ControlChip(
                text = if (placeKnown) stringResource(placeAndDate.label)
                    else stringResource(R.string.place_and_date_unknown),
                widest = labels.maxBy { it.length },
                anchor = Alignment.CenterEnd,
                onClick = if (placeKnown) onCyclePlaceAndDate else { { askingPlace = true } },
            )
        }
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
                val listState = rememberLazyListState()

                // A list keyed by row keeps whichever row was at the top of the window
                // where it was, so a new bird arrives above the window and the screen
                // goes on showing an older one - which reads as the app having stopped
                // hearing anything. Follow it down only for a reader who was already at
                // the top; one who has scrolled back through the log is reading it, and
                // yanking them to the newest row would lose their place.
                LaunchedEffect(heard.firstOrNull()?.firstHeard) {
                    if (listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
                }

                LazyColumnMMD(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(heard, key = { it.firstHeard }) { entry ->
                        HeardRow(entry)
                    }
                }
            }
        }

        DestinationRowCompose(current = Destination.LISTENING)
    }
}

/**
 * The picture of the last bird heard clearly, for the phone left on a windowsill rather
 * than read as a list. Off unless asked for: on this panel it leaves about one row of the
 * log showing.
 *
 * Once the setting is on the band is always there, carrying the app's own mark until a
 * bird has been heard clearly enough to be worth a picture. It does not appear with the
 * first bird and vanish with a clearing of the log: that would move everything below it
 * twice, and on e-ink moving anything repaints all of it.
 *
 * A photograph that has arrived also stays until another replaces it, rather than
 * blanking back to the mark while the next one loads.
 */
@Composable
private fun PhotoBand(assetId: String?) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(assetId) {
        if (assetId == null) return@LaunchedEffect
        BirdPhoto.load(context.cacheDir, BirdPhoto.url(assetId), refresh = false) { loaded ->
            if (loaded != null) bitmap = loaded.asImageBitmap()
        }
    }

    val modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.8f)

    val current = bitmap
    if (current == null) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Image(
            bitmap = current,
            contentDescription = stringResource(R.string.photo_description),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/**
 * The one thing on the screen that moves, and the switch that stops it.
 *
 * While the recorder is running the word is followed by one dot, then two, then three, and
 * round again. That is the whole of why it is there: a log that has said nothing for ten
 * minutes looks exactly the same whether the microphone is open or the app died quietly an
 * hour ago. The dots are appended here rather than written into the string, so no
 * translation has to know about them.
 *
 * Tapping it stops and starts the recorder, which is what the button across the bottom of
 * the screen used to do. Putting it here costs no height at all and gives the log the
 * button's, and the state and the control saying the same thing in the same place is
 * honest: the word tells you whether it is listening, and it is the thing you press to
 * change that.
 */
private const val ELLIPSIS_PERIOD_MS = 700L

@Composable
private fun ListeningChip(isListening: Boolean, onToggle: () -> Unit) {
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

    ControlChip(
        text = if (isListening) {
            stringResource(R.string.listening) + ".".repeat(dots)
        } else {
            stringResource(R.string.stopped)
        },
        // Sized to the widest thing it will ever say, so the dots do not push the box
        // wider and let it fall back three times a second.
        widest = stringResource(R.string.listening) + "...",
        anchor = Alignment.CenterStart,
        onClick = onToggle,
    )
}

/**
 * A word you can press. Nothing behind it and no rule around it: it sits on the surface
 * like everything else on this screen, and the only thing marking it out is that it is the
 * one line of it that answers to a tap.
 *
 * [widest] is the longest thing this will ever say, laid out unseen behind the real text
 * to fix the width, and [anchor] is the edge that stays put inside it: the listening word
 * grows dots to the right of itself, so it is held to the left; the filter is read against
 * the right margin, so it is held to the right. Without that they slide sideways as they
 * change, which is worse than the box was. Both of these change what they say - one cycles through
 * three filters, the other gains and loses dots every 700ms - and a box that resizes
 * redraws its border every time. On e-ink that is a twitch, three times a second, on the
 * one part of the screen whose whole job is to sit still and be glanced at.
 *
 * Three taps rather than a dragged slider for the filter: a drag on e-ink repaints the
 * whole track for every pixel of movement, and the value was never that precise.
 */
/**
 * Where you are, asked at the point the app admits it does not know.
 *
 * The same two settings live in Settings, and this is deliberately the same pair rather than
 * a second way of storing them: it writes the identical preferences, so whichever screen you
 * reach them from, there is one answer. What it saves is the walk — the chip is where the
 * reader learns the app has no location, and a fix offered there is worth two full-panel
 * repaints in a way a settings screen three taps away is not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceDialog(settings: Settings, onChanged: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                TextMMD(
                    text = stringResource(R.string.place_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            settings.manualLocation = !settings.manualLocation
                            onChanged()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextMMD(
                        text = stringResource(R.string.place_use_gps),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                    )
                    SwitchMMD(
                        checked = !settings.manualLocation,
                        onCheckedChange = {
                            settings.manualLocation = !it
                            onChanged()
                        },
                    )
                }
                // Only where there is something to type. A field that is on screen and not
                // being read is the same lie as a switch that does nothing.
                if (settings.manualLocation) {
                    TextFieldMMD(
                        value = settings.manualLocationValue,
                        onValueChange = {
                            settings.manualLocationValue = it
                            onChanged()
                        },
                        singleLine = true,
                        isError = !settings.manualLocationIsValid,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    TextMMD(
                        text = stringResource(R.string.place_manual_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedButtonMMD(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    TextMMD(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun ControlChip(
    text: String,
    widest: String,
    anchor: Alignment,
    /** Null where the chip is stating a fact rather than offering a choice. */
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        contentAlignment = anchor,
    ) {
        TextMMD(
            text = widest,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(0f),
        )
        TextMMD(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
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
            // Blocks, and no number beside them. What BirdNET reports is a softmax output,
            // not a calibrated probability: "85%" reads as "wrong about one time in seven",
            // which is a promise nothing here can keep. Five squares say how sure it sounds
            // without saying how often it is right. The blocks are coarser than the number
            // was -- twenty points a square against the number's five -- and that is the
            // trade: the lost resolution was never resolution, only decimal places.
            ConfidenceBlocks(percent = entry.shownPercent)
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
