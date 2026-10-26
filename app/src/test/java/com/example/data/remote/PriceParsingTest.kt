package com.example.data.remote

import com.example.support.PriceHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceParsingTest {

    @Test fun readsEachKaratFromItsOwnBlock() {
        val html = PriceHtml.isaghaHtml(k24 = 7000.0, k21 = 6125.0, k18 = 5250.0)
        assertEquals(6125.0, PriceParsing.extractISaghaUnit(html, "21", "buy")!!, 0.0)
        assertEquals(6165.0, PriceParsing.extractISaghaUnit(html, "21", "sell")!!, 0.0)
        assertEquals(5250.0, PriceParsing.extractISaghaUnit(html, "18", "buy")!!, 0.0)
    }

    @Test fun neverBorrowsAValueFromTheNextKaratBlock() {
        // The 21k block has no "buy" price. The old DOTALL regex would have grabbed the 18k figure.
        val html = """
            <div data-unit="21"><span class="prices-strip__price--sell"><b data-testid="price-figure">6165.0</b></span></div>
            <div data-unit="18"><span class="prices-strip__price--buy"><b data-testid="price-figure">5250.0</b></span></div>
        """.trimIndent()
        assertNull(PriceParsing.extractISaghaUnit(html, "21", "buy"))
        assertNull(PriceParsing.parseISagha(html))
    }

    @Test fun derives24kOnlyWhenMissing() {
        val rates = PriceParsing.parseISagha(PriceHtml.isaghaHtml(k24 = null, k21 = 6125.0, k18 = 5250.0))!!
        assertEquals(7000.0, rates.k24Buy, 0.01)
    }

    @Test fun plausibilityRejectsImpossibleTables() {
        val ok = PriceParsing.parseISagha(PriceHtml.isaghaHtml())!!
        assertTrue(PriceParsing.isPlausible(ok))
        assertFalse(PriceParsing.isPlausible(ok.copy(k21Buy = 7100.0)))            // 21k above 24k
        assertFalse(PriceParsing.isPlausible(ok.copy(k18Buy = 3000.0)))            // wrong ratio
        assertFalse(PriceParsing.isPlausible(ok.copy(k24Sell = ok.k24Buy * 0.5)))  // sell below buy
        assertFalse(PriceParsing.isPlausible(ok.copy(k21Buy = Double.NaN)))
        assertFalse(PriceParsing.isPlausible(ok.copy(ounceUsd = 12.0)))
    }

    @Test fun sourcesAgreeWithinThreePercent() {
        val a = PriceParsing.parseISagha(PriceHtml.isaghaHtml(k21 = 6125.0, k24 = null, k18 = 5250.0))!!
        val near = a.copy(k21Buy = 6200.0)
        val far = a.copy(k21Buy = 5500.0)
        assertTrue(PriceParsing.sourcesAgree(a, near))
        assertFalse(PriceParsing.sourcesAgree(a, far))
    }

    @Test fun goldPriceTodayParsing() {
        val rates = PriceParsing.parseGoldPriceToday(PriceHtml.goldPriceTodayHtml(k21 = 6140.0))
        assertNotNull(rates)
        assertEquals(6140.0, rates!!.k21Buy, 0.01)
        assertTrue(PriceParsing.isPlausible(rates))
        assertNull(PriceParsing.parseGoldPriceToday("<html>no prices here</html>"))
    }
}
