package com.example.support

/** HTML fixtures for price-parsing tests (no HTTP dependency). */
object PriceHtml {
    fun isaghaHtml(k24: Double? = 7000.0, k21: Double = 6125.0, k18: Double = 5250.0): String {
        fun block(unit: String, buy: Double, sell: Double) =
            """<div class="prices-strip" data-unit="$unit">
                 <span class="prices-strip__price--buy"><b data-testid="price-figure">$buy</b></span>
                 <span class="prices-strip__price--sell"><b data-testid="price-figure">$sell</b></span>
               </div>"""
        return buildString {
            append("<html><body>")
            if (k24 != null) append(block("24", k24, k24 + 45.0))
            append(block("21", k21, k21 + 40.0))
            append(block("18", k18, k18 + 34.0))
            append("</body></html>")
        }
    }

    fun goldPriceTodayHtml(k21: Double, k24: Double = k21 * 24.0 / 21.0): String =
        "<html><body><td>عيار 24</td><td>${String.format(java.util.Locale.US, "%.2f", k24)}</td><td>عيار 21</td><td>${String.format(java.util.Locale.US, "%.2f", k21)}</td></body></html>"
}
