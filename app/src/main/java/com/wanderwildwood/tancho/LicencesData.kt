package com.wanderwildwood.tancho

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/tancho"

/**
 * What the app downloads or fetches when it is used: not in the APK. The photographs are under
 * no licence at all, so their page shows [photoTerms] (the terms the app keeps to) as its text.
 */
fun licenceDownloaded(photoTerms: LicenceText): List<LicenceComponent> = listOf(
    LicenceComponent("BirdNET model and framework (downloaded once, not in the APK)", "Stefan Kahl and the Cornell Lab of Ornithology", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
    LicenceComponent("BirdNET species names in other languages (labels_xx.txt)", "The BirdNET project and its translators", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
    LicenceComponent("Bird photographs, fetched when shown, not in the APK", "Their photographers, through the Macaulay Library, Cornell Lab of Ornithology", "Copyright of the photographers", "https://www.macaulaylibrary.org", texts = listOf(photoTerms)),
)

/** Each distinct licence text once: family id and the raw resource. */
val LICENCE_RAW: List<Pair<String, Int>> = listOf(
    "gpl" to R.raw.licence_gpl3,
    "apache" to R.raw.licence_apache2,
    "ofl" to R.raw.licence_ofl,
    "cc-by" to R.raw.licence_cc,
)

@Composable
fun rememberLicencesData(): LicencesData = LicencesData(
    appName = "Birding",
    sourceUrl = SOURCE_URL,
    licenceLine = stringResource(R.string.licences_licence),
    // One row per artifact in the APK, listed by the build (config/libraries adds what Gradle cannot see).
    components = rememberGeneratedComponents(R.raw.aboutlibraries),
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "apache" -> R.string.licences_full_apache
            "ofl" -> R.string.licences_full_ofl
            "cc-by" -> R.string.licences_full_cc
            else -> R.string.licences_full_head
        }), raw)
    },
    extraGroups = listOf(ComponentGroup(stringResource(R.string.licences_fetched_head), stringResource(R.string.licences_fetched),
        licenceDownloaded(LicenceText("photos", stringResource(R.string.licences_photos_terms_title), 0, stringResource(R.string.licences_fetched))))),
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}
