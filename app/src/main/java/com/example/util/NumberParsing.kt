package com.example.util

/**
 * Locale-independent number parsing for user input and scraped text.
 * Accepts Arabic-Indic (٠١٢٣) and Persian (۰۱۲۳) digits, Arabic decimal/thousands separators,
 * "1,234.50" and "1234,5". Never returns NaN/Infinity.
 */
object NumberParsing {

    private val PLAIN_NUMBER = Regex("[+-]?(\\d+\\.?\\d*|\\.\\d+)")
    private val THOUSANDS_COMMA = Regex("[+-]?\\d{1,3}(,\\d{3})+(\\.\\d+)?")

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text.trim()) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append('0' + (ch - '\u0660'))
                in '\u06F0'..'\u06F9' -> sb.append('0' + (ch - '\u06F0'))
                '\u066B' -> sb.append('.')          // Arabic decimal separator ٫
                '\u066C' -> Unit                    // Arabic thousands separator ٬
                '\u060C' -> sb.append(',')          // Arabic comma ،
                ' ', '\u00A0', '\u200E', '\u200F', '\u202F' -> Unit
                else -> sb.append(ch)
            }
        }
        var s = sb.toString()
        if (s.contains(',')) {
            s = if (s.contains('.')) {
                s.replace(",", "")                  // 1,234.50
            } else if (THOUSANDS_COMMA.matches(s)) {
                s.replace(",", "")                  // 1,234,567
            } else {
                s.replace(',', '.')                 // 1234,5 (decimal comma)
            }
        }
        return s
    }

    fun parseDouble(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val s = normalize(text)
        if (!PLAIN_NUMBER.matches(s)) return null
        val v = s.toDoubleOrNull() ?: return null
        return if (v.isNaN() || v.isInfinite()) null else v
    }
}

fun String.toDoubleOrNullLocalized(): Double? = NumberParsing.parseDouble(this)
