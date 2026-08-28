package com.wanderwildwood.einkbirding

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
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

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
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD(stringResource(R.string.destination_settings)) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ChoiceRow(
                label = stringResource(R.string.settings_audiosource),
                value = stringResource(settings.audioSource.label),
                onCycle = settings::cycleAudioSource,
            )
            ChoiceRow(
                label = stringResource(R.string.settings_threshold),
                value = settings.threshold.toString(),
                onCycle = settings::cycleThreshold,
            )
            ChoiceRow(
                label = stringResource(R.string.settings_highpass),
                value = settings.highPass.toString(),
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
                checked = settings.notificationSound,
                onCheckedChange = { settings.notificationSound = it },
            )
            if (canSaveWav) {
                SwitchRow(
                    label = stringResource(R.string.save_wav),
                    summary = stringResource(R.string.summary_save_wav),
                    checked = settings.saveWav,
                    onCheckedChange = { settings.saveWav = it },
                )
            }
            SwitchRow(
                label = stringResource(R.string.bluetooth_connection),
                checked = settings.bluetooth,
                onCheckedChange = { settings.bluetooth = it },
            )

            HorizontalDividerMMD()

            SwitchRow(
                label = stringResource(R.string.manual_location),
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
private fun ChoiceRow(label: String, value: String, onCycle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCycle)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
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
