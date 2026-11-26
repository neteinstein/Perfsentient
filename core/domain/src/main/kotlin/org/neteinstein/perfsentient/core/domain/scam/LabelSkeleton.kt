package org.neteinstein.perfsentient.core.domain.scam

import java.text.Normalizer

/**
 * Reduces an app label to plain lowercase ASCII words so that look-alikes compare equal:
 * "WhatsАpp" (Cyrillic А), "Wh4tsApp", "W h a t s A p p" and "WhatsApp" all give the same result.
 * Ported from the Scam-Detector browser extension (src/lib/lookalike.js).
 */
object LabelSkeleton {
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val SEPARATORS = Regex("[^a-z0-9]+")

    private val UNICODE_CONFUSABLES: Map<Char, Char> = buildMap {
        fun map(target: Char, sources: String) = sources.forEach { put(it, target) }
        map('a', "аɑα")
        map('b', "ьƅ")
        map('c', "сϲƈ")
        map('d', "ԁɗđ")
        map('e', "еёҽε")
        map('g', "ցɡ")
        map('h', "һհħ")
        map('i', "іɩιı")
        map('j', "јϳ")
        map('k', "κк")
        map('l', "ӏł")
        map('m', "м")
        map('n', "пո")
        map('o', "оοօø")
        map('p', "рρ")
        map('r', "г")
        map('s', "ѕ")
        map('t', "т")
        map('u', "υս")
        map('v', "νѵ")
        map('w', "ԝω")
        map('x', "хχ")
        map('y', "уүγ")
    }

    /** Digits swapped for letters inside a word ("Wh4tsApp", "Netfl1x"). */
    private val DIGIT_CONFUSABLES = mapOf('0' to 'o', '1' to 'l', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '8' to 'b')

    fun tokens(label: String): List<String> {
        val lower = Normalizer.normalize(label.lowercase(), Normalizer.Form.NFKD).replace(COMBINING_MARKS, "")
        val mapped = lower.map { UNICODE_CONFUSABLES[it] ?: it }.joinToString("")
        val words = mapped.split(SEPARATORS).filter { it.isNotEmpty() }
        return glueSpacedLetters(words).map(::undigit)
    }

    /** A word mixing letters and digits gets its digits read as letters; plain numbers ("26") stay. */
    private fun undigit(word: String): String =
        if (word.any { it.isLetter() } && word.any { it.isDigit() } && word.length > 3) {
            word.map { DIGIT_CONFUSABLES[it] ?: it }.joinToString("")
        } else {
            word
        }

    /** "W h a t s A p p" → "whatsapp": runs of three or more single letters become one word. */
    private fun glueSpacedLetters(words: List<String>): List<String> {
        val result = mutableListOf<String>()
        val run = StringBuilder()
        var runLength = 0
        fun flush() {
            if (runLength >= MIN_SPACED_RUN) {
                result += run.toString()
            } else {
                run.forEach { result += it.toString() }
            }
            run.clear()
            runLength = 0
        }
        words.forEach { word ->
            if (word.length == 1) {
                run.append(word)
                runLength++
            } else {
                flush()
                result += word
            }
        }
        flush()
        return result
    }

    private const val MIN_SPACED_RUN = 3
}
