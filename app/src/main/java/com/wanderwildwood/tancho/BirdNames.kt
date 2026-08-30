package com.wanderwildwood.tancho

import android.content.Context
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.Collator
import java.util.Locale

/**
 * Which language the birds are named in, and the one place that decides it.
 *
 * BirdNET ships its 6,522 species in thirty-eight languages and upstream picked between
 * them by reading the phone's own language, which is the right default and was the only
 * way to say it. That is fine on a phone whose owner reads what the phone is set to. It
 * is not fine here: the Kompakt runs Android 12, which has no per-app language screen at
 * all, so a Dutch birder carrying an English phone had no way to be shown *Roodwang-
 * boszanger* short of putting the whole device into Dutch. The names were always in the
 * app; there was no door to them.
 *
 * So the language is a setting now, defaulting to the phone as it always did. It governs
 * the bird names and nothing else - the app's own words still follow the phone, because
 * they always did and because these are two different questions. Someone reading an
 * English phone in a Dutch wood wants the birds in Dutch and the buttons where they were.
 *
 * Three copies of this used to sit in [SoundClassifier], [ViewActivity] and
 * [BirdInfoActivity], two of them carrying a TODO asking for exactly this.
 */
object BirdNames {

    /** Stored when nobody has chosen: the names follow the phone, as they used to. */
    const val FOLLOW_PHONE = ""

    /** The language every reading falls back to, and the only one certain to be there. */
    private const val FALLBACK = "en"

    private const val KEY = "bird_names_language"
    private const val BASE = "labels"

    /**
     * Every language there are names in, each by the code its asset file is named with
     * and under the name it calls itself. A reader looking for their own language scans
     * for the word they would write, not for the English for it.
     *
     * The codes are BirdNET's and are not locale tags: `in` is Indonesian by the old ISO
     * code, and `en_uk`, `pt_BR` and `pt_PT` are files rather than languages. This list
     * is what the picker offers and what a stored choice is checked against, so a code
     * here must have a `labels_<code>.txt` beside it in assets.
     */
    val languages: List<BirdLanguage> = listOf(
        BirdLanguage("af", "Afrikaans"),
        BirdLanguage("ar", "العربية"),
        BirdLanguage("bg", "Български"),
        BirdLanguage("ca", "Català"),
        BirdLanguage("cs", "Čeština"),
        BirdLanguage("da", "Dansk"),
        BirdLanguage("de", "Deutsch"),
        BirdLanguage("el", "Ελληνικά"),
        BirdLanguage("en", "English"),
        BirdLanguage("en_uk", "English (UK)"),
        BirdLanguage("es", "Español"),
        BirdLanguage("fi", "Suomi"),
        BirdLanguage("fr", "Français"),
        BirdLanguage("he", "עברית"),
        BirdLanguage("hr", "Hrvatski"),
        BirdLanguage("hu", "Magyar"),
        BirdLanguage("in", "Bahasa Indonesia"),
        BirdLanguage("is", "Íslenska"),
        BirdLanguage("it", "Italiano"),
        BirdLanguage("ja", "日本語"),
        BirdLanguage("ko", "한국어"),
        BirdLanguage("lt", "Lietuvių"),
        BirdLanguage("ml", "മലയാളം"),
        BirdLanguage("nl", "Nederlands"),
        BirdLanguage("no", "Norsk"),
        BirdLanguage("pl", "Polski"),
        BirdLanguage("pt_BR", "Português (Brasil)"),
        BirdLanguage("pt_PT", "Português (Portugal)"),
        BirdLanguage("ro", "Română"),
        BirdLanguage("ru", "Русский"),
        BirdLanguage("sk", "Slovenčina"),
        BirdLanguage("sl", "Slovenščina"),
        BirdLanguage("sr", "Српски"),
        BirdLanguage("sv", "Svenska"),
        BirdLanguage("th", "ไทย"),
        BirdLanguage("tr", "Türkçe"),
        BirdLanguage("uk", "Українська"),
        BirdLanguage("zh", "中文"),
    ).sortedWith(compareBy(Collator.getInstance(Locale.ROOT)) { it.name })

    /** What is stored, which is [FOLLOW_PHONE] until somebody picks. */
    fun chosen(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY, FOLLOW_PHONE) ?: FOLLOW_PHONE

    fun choose(context: Context, code: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(KEY, code)
            .apply()
    }

    /**
     * The code the names are actually read in: what was chosen, or what the phone is set
     * to, or English. A stored code that is not in [languages] is treated as no choice,
     * so a language dropped from a future BirdNET does not leave the app reading a file
     * that is not there.
     */
    fun inUse(context: Context): String {
        val chosen = chosen(context)
        if (chosen != FOLLOW_PHONE && languages.any { it.code == chosen }) return chosen
        return fromPhone(context)
    }

    /** The name of the language in use, for the settings row to show as its value. */
    fun nameInUse(context: Context): String {
        val code = inUse(context)
        return languages.firstOrNull { it.code == code }?.name ?: code
    }

    /**
     * What the phone's own language comes to. Upstream's reading, kept: English and
     * Portuguese are the two that split by country, because BirdNET names a wren
     * differently either side of the Atlantic.
     */
    private fun fromPhone(context: Context): String {
        val locale = context.resources.configuration.locales.get(0) ?: return FALLBACK
        val code = when (locale.language) {
            "en" -> when (locale.country) {
                "GB", "AU", "NZ", "IE", "ZA" -> "en_uk"
                else -> "en"
            }
            "pt" -> when (locale.country) {
                "BR" -> "pt_BR"
                else -> "pt_PT"
            }
            else -> locale.language
        }
        return if (languages.any { it.code == code }) code else FALLBACK
    }

    /** The asset the names are read from, which is what [load] opens. */
    fun file(context: Context): String = "${BASE}_${inUse(context)}.txt"

    /**
     * The names themselves, one per line as `Scientific_Common`, title-cased on both
     * halves because the file is not. A language whose file will not open falls back to
     * English rather than leaving the app with no names at all: every screen indexes this
     * list by the model's output, and a short list would be read past its end.
     */
    fun load(context: Context): List<String> {
        val wanted = file(context)
        return read(context, wanted)
            ?: read(context, "${BASE}_$FALLBACK.txt")
            ?: emptyList()
    }

    private fun read(context: Context, filename: String): List<String>? = try {
        BufferedReader(InputStreamReader(context.assets.open(filename))).use { reader ->
            reader.lineSequence().map { it.toTitleCase() }.toList()
        }
    } catch (e: IOException) {
        null
    }

    private fun String.toTitleCase() = splitToSequence("_")
        .map { part ->
            part.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
        .joinToString("_")
        .trim()
}

/** A language there are bird names in, under the name it calls itself. */
data class BirdLanguage(val code: String, val name: String)
