package com.example.data.remote

import com.example.util.NumberParsing

data class MarketRates(
    val k24Buy: Double,
    val k24Sell: Double,
    val k21Buy: Double,
    val k21Sell: Double,
    val k18Buy: Double,
    val k18Sell: Double,
    /** Spot ounce in USD when the source publishes one; never invented. */
    val ounceUsd: Double? = null
)

/** Pure (Android-free) parsing and sanity rules for scraped price pages. */
object PriceParsing {

    private val FIGURE = Regex("""data-testid="price-figure"[^>]*>\s*([0-9][0-9.,]*)""")
    private val GPT_21 = Regex("""عيار 21.{0,400}?([4-9][0-9]{3}(?:\.[0-9]+)?)""", RegexOption.DOT_MATCHES_ALL)
    private val GPT_24 = Regex("""عيار 24.{0,400}?([5-9][0-9]{3}(?:\.[0-9]+)?)""", RegexOption.DOT_MATCHES_ALL)

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    /**
     * Reads one price from an iSagha-style strip. Each match is confined to the block that starts
     * at its own `data-unit="..."` marker, so a missing value can never be borrowed from the next
     * karat's block (the old DOTALL regex could do exactly that).
     */
    fun extractISaghaUnit(html: String, unit: String, kind: String): Double? {
        val marker = "data-unit=\"$unit\""
        var from = 0
        while (true) {
            val start = html.indexOf(marker, from)
            if (start < 0) return null
            val next = html.indexOf("data-unit=\"", start + marker.length)
            val end = if (next < 0) html.length else next
            val segment = html.substring(start, end)
            val kindIdx = segment.indexOf("prices-strip__price--$kind")
            if (kindIdx >= 0) {
                val m = FIGURE.find(segment, kindIdx)
                val v = m?.let { NumberParsing.parseDouble(it.groupValues[1]) }
                if (v != null) return v
            }
            from = end
            if (from >= html.length) return null
        }
    }

    fun parseISagha(html: String): MarketRates? {
        val p21Buy = extractISaghaUnit(html, "21", "buy") ?: return null
        val p21Sell = extractISaghaUnit(html, "21", "sell") ?: (p21Buy * 1.0064)
        val p24Buy = extractISaghaUnit(html, "24", "buy") ?: round2(p21Buy * 24.0 / 21.0)
        val p24Sell = extractISaghaUnit(html, "24", "sell") ?: round2(p21Sell * 24.0 / 21.0)
        val p18Buy = extractISaghaUnit(html, "18", "buy") ?: round2(p21Buy * 18.0 / 21.0)
        val p18Sell = extractISaghaUnit(html, "18", "sell") ?: round2(p21Sell * 18.0 / 21.0)
        val ounce = extractISaghaUnit(html, "ounce", "buy")
        return MarketRates(p24Buy, p24Sell, p21Buy, p21Sell, p18Buy, p18Sell, ounce)
    }

    fun parseGoldPriceToday(html: String): MarketRates? {
        val clean = html.replace(",", "")
        val p21 = GPT_21.find(clean)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val p24 = GPT_24.find(clean)?.groupValues?.get(1)?.toDoubleOrNull() ?: round2(p21 * 24.0 / 21.0)
        val p21Sell = round2(p21 * 1.0064)
        return MarketRates(
            k24Buy = p24,
            k24Sell = round2(p24 * 1.0064),
            k21Buy = p21,
            k21Sell = p21Sell,
            k18Buy = round2(p21 * 18.0 / 21.0),
            k18Sell = round2(p21Sell * 18.0 / 21.0),
            ounceUsd = null
        )
    }

    /** Rejects values that cannot be a real Egyptian jeweler price table. */
    fun isPlausible(r: MarketRates): Boolean {
        val all = listOf(r.k24Buy, r.k24Sell, r.k21Buy, r.k21Sell, r.k18Buy, r.k18Sell)
        if (all.any { it.isNaN() || it.isInfinite() || it <= 0.0 }) return false
        if (r.k24Buy !in 500.0..200_000.0) return false
        if (!(r.k24Buy > r.k21Buy && r.k21Buy > r.k18Buy)) return false
        if (r.k21Buy / r.k24Buy !in 0.84..0.91) return false
        if (r.k18Buy / r.k21Buy !in 0.80..0.90) return false
        if (r.k24Sell < r.k24Buy * 0.999 || r.k24Sell > r.k24Buy * 1.10) return false
        if (r.k21Sell < r.k21Buy * 0.999 || r.k21Sell > r.k21Buy * 1.10) return false
        if (r.k18Sell < r.k18Buy * 0.999 || r.k18Sell > r.k18Buy * 1.10) return false
        r.ounceUsd?.let { if (it.isNaN() || it !in 500.0..100_000.0) return false }
        return true
    }

    /** Two independent sources agree when their 21k buy prices are within [tolerance]. */
    fun sourcesAgree(a: MarketRates, b: MarketRates, tolerance: Double = 0.03): Boolean {
        val base = a.k21Buy
        if (base <= 0.0) return false
        return Math.abs(a.k21Buy - b.k21Buy) / base <= tolerance
    }
}
