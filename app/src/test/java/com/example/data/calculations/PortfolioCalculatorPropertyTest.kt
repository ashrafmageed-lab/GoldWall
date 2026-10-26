package com.example.data.calculations

import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PortfolioCalculatorPropertyTest {

    private val p24 = 7000.0
    private val p21 = 6125.0
    private val p18 = 5250.0

    private fun randomItems(rnd: Random, n: Int): List<GoldItem> = List(n) {
        val bullion = rnd.nextBoolean()
        GoldItem(
            title = "item$it",
            type = if (bullion) GoldType.BULLION else GoldType.JEWELRY,
            weightGrams = (rnd.nextInt(1, 20000) / 100.0),
            karat = if (bullion) 24 else listOf(18, 21, 24).random(rnd),
            condition = if (rnd.nextInt(4) == 0) "مفتوحة" else "مغلفة وسليمة"
        )
    }

    private fun total(items: List<GoldItem>): Double {
        val bullion = items.filter { it.type == GoldType.BULLION }
        val jewelry = items.filter { it.type == GoldType.JEWELRY }
        val b = PortfolioCalculator.calculateBullionLiquidation(bullion, p24, p21, p18)
        val j = PortfolioCalculator.calculateJewelryLiquidation(jewelry, p24, p21, p18)
        val cb = PortfolioCalculator.calculateExpectedBullionCashBack(bullion)
        return PortfolioCalculator.calculateTotalLiquidation(b, j, cb)
    }

    @Test fun totalEqualsSumOfItemLiquidationsPlusCashBack() {
        val rnd = Random(42)
        repeat(200) {
            val items = randomItems(rnd, rnd.nextInt(1, 15))
            val perItem = items.sumOf { PortfolioCalculator.calculateItemLiquidation(it, p24, p21, p18) }
            val cb = PortfolioCalculator.calculateExpectedBullionCashBack(items.filter { it.type == GoldType.BULLION })
            assertEquals(perItem + cb, total(items), 0.05)
        }
    }

    @Test fun addingThenRemovingAnItemRestoresTheTotal() {
        val rnd = Random(7)
        repeat(100) {
            val items = randomItems(rnd, 6)
            val extra = randomItems(rnd, 1)
            assertEquals(total(items), total((items + extra).dropLast(1)), 0.0001)
            assertTrue(total(items + extra) >= total(items))
        }
    }

    @Test fun nonFinitePricesAndWeightsNeverThrow() {
        val item = GoldItem(title = "x", type = GoldType.BULLION, weightGrams = 10.0, karat = 24)
        val inf = Double.POSITIVE_INFINITY
        assertEquals(0.0, PortfolioCalculator.calculateItemLiquidation(item, inf, inf, inf), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculateTotalLiquidation(inf, Double.NaN, inf), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculateProfitVsBaseline(inf, 1000.0), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculatePercentageChange(inf, 1000.0), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculateTargetProgress(inf, 1000.0), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculateValuationDifference(inf, 1000.0), 0.0)
        val hugeWeight = item.copy(weight = inf)
        assertEquals(0.0, PortfolioCalculator.calculateExpectedBullionCashBack(listOf(hugeWeight)), 0.0)
        assertEquals(0.0, PortfolioCalculator.calculateBullionWeight(listOf(hugeWeight)), 0.0)
    }

    @Test fun cashBackIsAddedOnceAndOnlyForSealedBars() {
        val sealed = GoldItem(title = "s", type = GoldType.BULLION, weightGrams = 100.0, karat = 24)
        val opened = sealed.copy(status = "مفتوحة")
        assertEquals(2800.0, PortfolioCalculator.calculateExpectedBullionCashBack(listOf(sealed, opened)), 0.001)
        assertFalse(PortfolioCalculator.isCashBackEligible(opened))
    }

    @Test fun partialSaleKeepsRemainingValueProportional() {
        val bar = GoldItem(title = "bar", type = GoldType.BULLION, weightGrams = 50.0, karat = 24)
        val remaining = bar.copy(weight = 30.0)
        val full = PortfolioCalculator.calculateItemLiquidation(bar, p24, p21, p18)
        val part = PortfolioCalculator.calculateItemLiquidation(remaining, p24, p21, p18)
        assertEquals(full * 30.0 / 50.0, part, 0.01)
    }
}
