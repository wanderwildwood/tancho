package com.wanderwildwood.tancho

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/tancho"

/** What the APK carries that is not this app's own code, and the licence each project declares. */
val LICENCE_COMPONENTS: List<LicenceComponent> = listOf(
    LicenceComponent("whoBIRD", "woheller69", "GPL-3.0", "https://github.com/woheller69/whoBIRD"),
    LicenceComponent("TensorFlow Lite examples", "The TensorFlow Authors", "Apache-2.0", "https://www.tensorflow.org/lite/examples"),
    LicenceComponent("LiteRT", "Google", "Apache-2.0", "https://ai.google.dev/edge/litert"),
    LicenceComponent("AndroidX AppCompat, ConstraintLayout, Preference; Material Components", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenceComponent("Zip4j", "Srikanth Reddy Lingala", "Apache-2.0", "https://github.com/srikanth-lingala/zip4j"),
    LicenceComponent("iirj, IIR filters", "Bernd Porr", "Apache-2.0", "https://github.com/berndporr/iirj"),
    LicenceComponent("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
    LicenceComponent("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
    LicenceComponent("Jetpack Compose (UI, Foundation, Material 3), Activity, Lifecycle, Core", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenceComponent("Kotlin standard library and coroutines", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
    LicenceComponent("Material Symbols icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
)

/** What the app downloads or fetches when it is used: not in the APK. */
val LICENCE_DOWNLOADED: List<LicenceComponent> = listOf(
    LicenceComponent("BirdNET model and framework (downloaded once, not in the APK)", "Stefan Kahl and the Cornell Lab of Ornithology", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
    LicenceComponent("BirdNET species names in other languages (labels_xx.txt)", "The BirdNET project and its translators", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
    LicenceComponent("Bird photographs, fetched when shown, not in the APK", "Their photographers, through the Macaulay Library, Cornell Lab of Ornithology", "Copyright of the photographers", "https://www.macaulaylibrary.org"),
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
    components = LICENCE_COMPONENTS,
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "apache" -> R.string.licences_full_apache
            "ofl" -> R.string.licences_full_ofl
            "cc-by" -> R.string.licences_full_cc
            else -> R.string.licences_full_head
        }), raw)
    },
    extraGroups = listOf(ComponentGroup(stringResource(R.string.licences_fetched_head), stringResource(R.string.licences_fetched), LICENCE_DOWNLOADED)),
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        licenceHeading = stringResource(R.string.licences_licence_head),
        builtWithHeading = stringResource(R.string.licences_built_head),
        fullTextsHeading = stringResource(R.string.licences_full_head),
        noBrowser = { address -> none.replace("@@", address) },
    )
}
