package com.wanderwildwood.tancho

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import kotlinx.coroutines.delay

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
    onChooseLanguage: () -> Unit,
    onExportLog: () -> Unit,
    onSaveBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDeleteLog: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ChoiceRow(
                label = stringResource(R.string.settings_audiosource),
                value = stringResource(settings.audioSource.label),
                summary = stringResource(R.string.summary_settings_audiosource),
                onCycle = settings::cycleAudioSource,
            )
            // The unit travels with the number rather than sitting in brackets after the
            // label. "Threshold [%] ... 30" was the only place on the screen that read
            // like an instrument panel.
            ChoiceRow(
                label = stringResource(R.string.settings_threshold),
                value = "${settings.threshold}%",
                summary = stringResource(R.string.summary_settings_threshold),
                onCycle = settings::cycleThreshold,
            )
            ChoiceRow(
                label = stringResource(R.string.settings_highpass),
                value = "${settings.highPass} Hz",
                summary = stringResource(R.string.summary_settings_highpass),
                onCycle = settings::cycleHighPass,
            )

            HorizontalDividerMMD()

            SwitchRow(
                label = stringResource(R.string.ignore_non_birds),
                summary = stringResource(R.string.summary_ignore_non_birds),
                checked = settings.ignoreNonBirds,
                onCheckedChange = { settings.ignoreNonBirds = it },
            )
            SwitchRow(
                label = stringResource(R.string.list_repeats),
                summary = stringResource(R.string.summary_list_repeats),
                checked = settings.listRepeats,
                onCheckedChange = { settings.listRepeats = it },
            )
            SwitchRow(
                label = stringResource(R.string.photo_while_listening),
                summary = stringResource(R.string.summary_photo_while_listening),
                checked = settings.photoWhileListening,
                onCheckedChange = { settings.photoWhileListening = it },
            )
            SwitchRow(
                label = stringResource(R.string.settings_notification_sound),
                summary = stringResource(R.string.summary_settings_notification_sound),
                checked = settings.notificationSound,
                onCheckedChange = { settings.notificationSound = it },
            )
            SwitchRow(
                label = stringResource(R.string.save_wav),
                summary = stringResource(R.string.summary_save_wav),
                checked = settings.saveWav,
                onCheckedChange = { settings.saveWav = it },
            )
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
                    summary = stringResource(R.string.summary_clear_recordings),
                    checked = settings.clearRecordings,
                    onCheckedChange = { settings.clearRecordings = it },
                )
            }
            SwitchRow(
                label = stringResource(R.string.bluetooth_connection),
                summary = stringResource(R.string.summary_bluetooth_connection),
                checked = settings.bluetooth,
                onCheckedChange = { settings.bluetooth = it },
            )

            HorizontalDividerMMD()

            SwitchRow(
                label = stringResource(R.string.manual_location),
                summary = stringResource(R.string.summary_manual_location),
                checked = settings.manualLocation,
                onCheckedChange = { settings.manualLocation = it },
            )
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

            HorizontalDividerMMD()

            if (canChooseLanguage) {
                ActionRow(label = stringResource(R.string.language), onClick = onChooseLanguage)
            }

            HorizontalDividerMMD()

            ActionRow(label = stringResource(R.string.export_log), onClick = onExportLog)
            ActionRow(label = stringResource(R.string.save_backup), onClick = onSaveBackup)
            // Restoring replaces the log rather than merging into it, so it asks the same
            // way deleting does: the row itself, not a dialog.
            ConfirmingRow(
                label = R.string.restore_backup,
                armedLabel = R.string.restore_backup_confirm,
                onConfirmed = onRestoreBackup,
            )
            DeleteRow(onConfirmed = onDeleteLog)

            HorizontalDividerMMD()

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButtonMMD(
                onClick = settings::reset,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                TextMMD(stringResource(R.string.settings_reset))
            }
            Spacer(modifier = Modifier.height(24.dp))
            AboutSection()
            Spacer(modifier = Modifier.height(24.dp))
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
private const val ARMED_MS = 5000L

/**
 * What this is, what it was built from, and who the parts belong to. Folded away because
 * it is read once; a row that opens in place rather than a screen that has to be left.
 */
@Composable
private fun AboutSection() {
    var open by remember { mutableStateOf(false) }

    ActionRow(label = stringResource(R.string.about), onClick = { open = !open })
    if (open) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            TextMMD(
                text = stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            TextMMD(
                text = BuildConfig.APPLICATION_ID,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextMMD(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
