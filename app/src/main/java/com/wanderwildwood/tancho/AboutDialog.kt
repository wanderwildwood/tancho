package com.wanderwildwood.tancho

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * What this is, what it does with what it hears and where it goes online, and who the parts
 * belong to. Read once, by someone deciding whether to trust it, so it opens over the list
 * rather than sending them to a screen they then have to leave.
 *
 * The text pages on whole rows, because it is longer than a held-across panel; Close stays
 * outside the pages, always in reach. No dim behind it: the panel would repaint every pixel
 * to draw a grey it cannot show.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        val room = (LocalConfiguration.current.screenHeightDp - 165).coerceIn(120, 460).dp
        PagedColumn(Modifier.fillMaxWidth().height(room)) {
            // A heading shares its row with the paragraph under it: a page shares spare room
            // out between rows, and a heading in a row of its own is left floating above its text.
            item {
                Column {
                    Heading(R.string.about_privacy_title, first = true)
                    Body(R.string.about_privacy)
                }
            }
            item { Body(R.string.about_privacy_more) }
            item {
                Column {
                    Heading(R.string.about_terms_title)
                    Body(R.string.about_terms)
                }
            }
            item {
                Column {
                    Heading(R.string.about_credits_title)
                    Body(R.string.about_credits)
                }
            }
            item { AboutLinks() }
        }
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
private fun Heading(text: Int, first: Boolean = false) {
    TextMMD(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = if (first) 0.dp else 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Body(text: Int) {
    TextMMD(
        text = stringResource(text),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/**
 * The foot of the About: the words that open the text of the licence the model is under, and
 * the site's name, which opens the site. Each opens its page in a browser, or says there is
 * none; a press is 48dp tall whatever the words are.
 */
@Composable
private fun AboutLinks() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        LinkRow(stringResource(R.string.about_model_licence_link)) {
            BrowserLinks.open(context, "https://creativecommons.org/licenses/by-nc-sa/4.0/")
        }
        LinkRow("wanderthe.dev") { BrowserLinks.open(context, "https://wanderthe.dev") }
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
