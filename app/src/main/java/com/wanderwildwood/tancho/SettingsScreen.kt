package com.wanderwildwood.tancho

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import com.wanderwildwood.tancho.R

/**
 * Settings.
 *
 * Everything here is a tap. Upstream drew this screen with the Material preference
 * library - grey switch tracks, two sliders and a dialog - which is a different app from
 * the one the listening screen belongs to, and sliders in particular are the wrong tool on
 * a panel that repaints in tenths of a second. A setting with a handful of sensible values
 * cycles through them; a setting that is on or off is a switch; the one setting that is
 * genuinely free text is a field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    birdNames: String,
    onChooseLanguage: () -> Unit,
    onExportLog: () -> Unit,
    onSaveBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDeleteLog: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD(text = stringResource(R.string.destination_settings)) },
            actions = { BarButton(R.drawable.ic_info_24dp, R.string.about) { showAbout = true } },
        )
        // MMD's list, not a scrolling Column: it steps four rows to a swipe and stops, and it
        // brings the chevron rail at both ends. A settings screen that coasts was the one
        // screen in the app that did not behave like the phone it is on.
        LazyColumnMMD(modifier = Modifier.weight(1f)) {
            item {
                ChoiceRow(
                    label = stringResource(R.string.settings_audiosource),
                    value = stringResource(settings.audioSource.label),
                    summary = stringResource(R.string.summary_settings_audiosource),
                    onCycle = settings::cycleAudioSource,
                )
            }
            item {
                // The unit travels with the number rather than sitting in brackets after the
                // label. "Threshold [%] ... 30" was the only place on the screen that read
                // like an instrument panel.
                ChoiceRow(
                    label = stringResource(R.string.settings_threshold),
                    value = stringResource(R.string.settings_threshold_value, settings.threshold.toString()),
                    summary = stringResource(R.string.summary_settings_threshold),
                    onCycle = settings::cycleThreshold,
                )
            }
            item {
                ChoiceRow(
                    label = stringResource(R.string.settings_highpass),
                    value = stringResource(R.string.settings_highpass_value, settings.highPass.toString()),
                    summary = stringResource(R.string.summary_settings_highpass),
                    onCycle = settings::cycleHighPass,
                )
            }
            item {

                HorizontalDividerMMD()
            }
            item {

                SwitchRow(
                    label = stringResource(R.string.ignore_non_birds),
                    summary = stringResource(R.string.summary_ignore_non_birds),
                    checked = settings.ignoreNonBirds,
                    onCheckedChange = { settings.ignoreNonBirds = it },
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.list_repeats),
                    summary = stringResource(R.string.summary_list_repeats),
                    checked = settings.listRepeats,
                    onCheckedChange = { settings.listRepeats = it },
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.photo_while_listening),
                    summary = stringResource(R.string.summary_photo_while_listening),
                    checked = settings.photoWhileListening,
                    onCheckedChange = { settings.photoWhileListening = it },
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.settings_notification_sound),
                    checked = settings.notificationSound,
                    onCheckedChange = { settings.notificationSound = it },
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.save_wav),
                    summary = stringResource(R.string.summary_save_wav),
                    checked = settings.saveWav,
                    onCheckedChange = { settings.saveWav = it },
                )
            }
            item {
                // Both of these act on the recordings, so neither is offered when there are
                // none to act on. A switch that is on and does nothing is worse than no switch.
                if (settings.saveWav) {
                    SwitchRow(
                        label = stringResource(R.string.show_spectrogram),
                        summary = stringResource(R.string.summary_show_spectrogram),
                        checked = settings.showSpectrogram,
                        onCheckedChange = { settings.showSpectrogram = it },
                    )
                    SwitchRow(
                        label = stringResource(R.string.clear_recordings),
                        checked = settings.clearRecordings,
                        onCheckedChange = { settings.clearRecordings = it },
                    )
                }
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.bluetooth_connection),
                    summary = stringResource(R.string.summary_bluetooth_connection),
                    checked = settings.bluetooth,
                    onCheckedChange = { settings.bluetooth = it },
                )
            }
            item {

                HorizontalDividerMMD()
            }
            item {

                SwitchRow(
                    label = stringResource(R.string.manual_location),
                    summary = stringResource(R.string.summary_manual_location),
                    checked = settings.manualLocation,
                    onCheckedChange = { settings.manualLocation = it },
                )
            }
            item {
                if (settings.manualLocation) {
                    TextFieldMMD(
                        value = settings.manualLocationValue,
                        onValueChange = { settings.manualLocationValue = it },
                        singleLine = true,
                        isError = !settings.manualLocationIsValid,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        supportingText = {
                            if (!settings.manualLocationIsValid) {
                                TextMMD(
                                    text = stringResource(R.string.error_invalid_GPS),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            item {

                HorizontalDividerMMD()
            }
            item {

                // The names the birds are read out under. Not "Language": the app's own words
                // still follow the phone, and this is the only thing the row changes.
                ValueRow(
                    label = stringResource(R.string.bird_names),
                    value = birdNames,
                    onClick = onChooseLanguage,
                )
            }
            item {

                HorizontalDividerMMD()
            }
            item {

                // A heading groups them; the labels carry the rest. A row that needs a
                // paragraph has the wrong label.
                SectionHeading(stringResource(R.string.log_heading))
            }
            item {
                ActionRow(label = stringResource(R.string.export_log), onClick = onExportLog)
            }
            item {
                ActionRow(label = stringResource(R.string.save_backup), onClick = onSaveBackup)
            }
            item {
                // Restoring replaces the log rather than merging into it, so it asks the same
                // way deleting does: the row itself, not a dialog.
                ConfirmingRow(
                    label = R.string.restore_backup,
                    armedLabel = R.string.restore_backup_confirm,
                    onConfirmed = onRestoreBackup,
                )
            }
            item {
                DeleteRow(onConfirmed = onDeleteLog)
            }
            item {

                HorizontalDividerMMD()
            }
            item {

                // Resetting throws away every choice on this screen, so it asks the way
                // deleting the log does. A button here and a row there were two shapes for
                // one kind of action, on one screen.
                ConfirmingRow(
                    label = R.string.settings_reset,
                    armedLabel = R.string.settings_reset_confirm,
                    onConfirmed = settings::reset,
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        DestinationRowCompose(current = Destination.SETTINGS)
    }
}

/**
 * A setting with a few values, shown as the one it is on. Tapping the row moves to the
 * next and wraps, which is the whole interaction: no dialog to open, nothing to dismiss,
 * one repaint per tap.
 */
@Composable
private fun ChoiceRow(label: String, value: String, summary: String? = null, onCycle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                TextMMD(text = summary, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextMMD(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * A setting shown as the value it is on, which opens a screen to change it. It looks like
 * [ChoiceRow] and means something else: there a tap *is* the change, here it is the way to
 * the choice. Worth the second shape only where the values are too many to tap through.
 */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
        TextMMD(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                TextMMD(text = summary, style = MaterialTheme.typography.bodySmall)
            }
        }
        SwitchMMD(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Clearing the log cannot be undone, so it asks first - but on the row itself rather than
 * in a dialog. A dialog is two full-panel repaints to ask one question; this is one row
 * changing what it says. The question withdraws itself after a few seconds, so a row armed
 * by a stray tap is not left armed for the next person to walk into.
 */
@Composable
private fun DeleteRow(onConfirmed: () -> Unit) =
    ConfirmingRow(R.string.delete_log, R.string.delete_log_confirm, onConfirmed)

/** A row that asks once. See the note above DeleteRow for why this is not a dialog. */
@Composable
private fun ConfirmingRow(label: Int, armedLabel: Int, onConfirmed: () -> Unit) {
    var armed by remember { mutableStateOf(false) }

    LaunchedEffect(armed) {
        if (!armed) return@LaunchedEffect
        delay(ARMED_MS)
        armed = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (armed) {
                    armed = false
                    onConfirmed()
                } else {
                    armed = true
                }
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        TextMMD(
            text = stringResource(if (armed) armedLabel else label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (armed) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** How long a tap on the delete row stays armed before it forgets it was asked. */
private const val ARMED_MS = 4000L

/**
 * The only heading this app draws. The listening screen wants every pixel and says what it
 * is by what is on it; a settings list does not, and without a name at the top the "i" in
 * the corner has nothing to sit beside.
 */
@Composable
private fun BarButton(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    // A 22dp mark in a 48dp target, the same three numbers as every other app of this shop.
    // What stood here was a 24dp Image with clickable hung off it -- half the target.
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * What this is, what it was built from, and who the parts belong to. Read once, by someone
 * deciding whether to trust it, so it opens over the list rather than sending them to a
 * screen they then have to leave.
 *
 * No dim behind it: the panel would repaint every pixel to draw a grey it cannot show.
 */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextMMD(
            text = stringResource(R.string.about_body),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Llama()

        Spacer(modifier = Modifier.height(18.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextMMD(stringResource(R.string.close))
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit, summary: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
        if (summary != null) {
            TextMMD(text = summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A heading over a group of rows, so a list of verbs reads as being about one thing. */
@Composable
private fun SectionHeading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 * It shares one line with the site's name, which is plain text; only the llama and its
 * words are pressed.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(text = "wanderthe.dev", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    // Straight to the checkout. The Donate button on the site only leads
                    // here anyway, so the page in between is a press the reader does not need.
                    // The short square.link form, not the long checkout.square.site address it
                    // redirects to -- the short one is what the site itself links to, so a
                    // regenerated checkout follows it and a published app does not break.
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://square.link/u/AGu8oT10")),
                        )
                    }.onFailure {
                        Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(vertical = 4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.bodySmall)
        }
    }
}
