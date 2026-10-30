package com.wanderwildwood.tancho

/** One piece of other people's work that ships inside the app (or is fetched by it), and its licence. */
data class Library(
    val name: String,
    val by: String,
    val licence: String,
    val link: String,
)

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/tancho"

/** Everything the APK carries, or fetches, that this project did not write. */
val LIBRARIES: List<Library> =
    listOf(
        Library("whoBIRD", "woheller69", "GPL-3.0", "https://github.com/woheller69/whoBIRD"),
        Library("BirdNET model and framework", "Stefan Kahl and the Cornell Lab of Ornithology (downloaded once, not in the APK)", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
        Library("BirdNET species names in other languages (labels_xx.txt)", "The BirdNET project and its translators", "CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
        Library("Bird photographs, fetched when shown, not in the APK", "Their photographers, through the Macaulay Library, Cornell Lab of Ornithology", "Copyright of the photographers", "https://www.macaulaylibrary.org"),
        Library("TensorFlow Lite examples", "The TensorFlow Authors", "Apache-2.0", "https://www.tensorflow.org/lite/examples"),
        Library("LiteRT", "Google", "Apache-2.0", "https://ai.google.dev/edge/litert"),
        Library("AndroidX AppCompat, ConstraintLayout, Preference; Material Components", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
        Library("Zip4j", "Srikanth Reddy Lingala", "Apache-2.0", "https://github.com/srikanth-lingala/zip4j"),
        Library("iirj, IIR filters", "Bernd Porr", "Apache-2.0", "https://github.com/berndporr/iirj"),
        Library("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        Library("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        Library("Jetpack Compose (UI, Foundation, Material 3), Activity, Lifecycle, Core", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
        Library("Kotlin standard library and coroutines", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
        Library("Material Symbols icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
    )

/**
 * A licence text as the paragraphs a reader turns pages by: the texts are wrapped at about
 * seventy columns with a blank line between paragraphs; each paragraph is joined into one row.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }
