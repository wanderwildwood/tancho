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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * The language the birds are named in.
 *
 * BirdNET has always carried its 6,522 species in thirty-eight languages and this app has
 * always read the one the phone was set to. On a Kompakt that was the end of it: Android
 * 12 has no per-app language screen, so a Dutch birder carrying an English phone could
 * only have *Roodwangboszanger* by putting the whole device into Dutch. This is the door
 * that was missing.
 *
 * Thirty-nine rows is too many to cycle through on a settings row and too many for a
 * dialog, so it is the app's one sub-screen. The hardware back button is the way out:
 * nothing here needs confirming, and a tap is the whole interaction.
 *
 * Each language is written the way it writes itself. Somebody looking for Dutch is
 * looking for "Nederlands"; a list of English names for other people's languages would be
 * the wrong list to hand them.
 */
@Composable
fun LanguageScreen(chosen: String, inUse: String, onChoose: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeading(stringResource(R.string.bird_names))
        LazyColumn(modifier = Modifier.weight(1f)) {
            // Following the phone is a choice like any other, and the one everybody
            // starts on, so it is the first row rather than a switch above the list. It
            // carries what it currently comes to, because "same as the phone" does not
            // answer the question the reader came here with.
            item {
                LanguageRow(
                    name = stringResource(R.string.bird_names_phone),
                    value = BirdNames.languages.firstOrNull { it.code == inUse }?.name,
                    chosen = chosen == BirdNames.FOLLOW_PHONE,
                    onClick = { onChoose(BirdNames.FOLLOW_PHONE) },
                )
                HorizontalDividerMMD()
            }
            items(BirdNames.languages) { language ->
                LanguageRow(
                    name = language.name,
                    chosen = chosen == language.code,
                    onClick = { onChoose(language.code) },
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * One language. Bold is the chosen one — no tick and no radio button: a column of empty
 * circles is thirty-eight glyphs the panel has to draw to say "not this one".
 */
@Composable
private fun LanguageRow(
    name: String,
    chosen: Boolean,
    onClick: () -> Unit,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
        )
        if (value != null) {
            TextMMD(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** The same name-at-the-top the settings list has, without the "i" it has no use for. */
@Composable
private fun ScreenHeading(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextMMD(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
