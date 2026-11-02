package com.wanderwildwood.tancho

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.tancho.BuildConfig
import com.wanderwildwood.tancho.R

/**
 * This app's About: only the words and the address. The dialog, its one Close button, the
 * Licences row, the links and the llama are the shared HouseAbout.kt, the same in every app.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        sections = listOf(
            AboutSection(stringResource(R.string.about_privacy_title), stringResource(R.string.about_privacy)),
            AboutSection(null, stringResource(R.string.about_privacy_more)),
            AboutSection(stringResource(R.string.about_terms_title), stringResource(R.string.about_terms)),
            AboutSection(stringResource(R.string.about_credits_title), stringResource(R.string.about_credits)),
        ),
        sourceUrl = SOURCE_URL,
        links = listOf(
            AboutLinkData(stringResource(R.string.about_model_licence_link), "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
        ),
    )
    val labels = AboutLabels(
        close = stringResource(R.string.close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss) { modifier, back ->
        LicencesScreen(rememberLicencesData(), rememberLicencesLabels(), onBack = back, modifier = modifier)
    }
}
