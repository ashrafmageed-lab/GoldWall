package com.example.ui

import com.example.data.local.AppDatabase
import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioDistributionTest {

    @Test
    fun testPortfolioDistributionMetrics_karatAndWeightAccuracy() {
        val bullions = AppDatabase.getInitialBullionList()
        val jewelry = AppDatabase.getInitialJewelryList()

        val state = PortfolioUiState(
            bullionList = bullions,
            jewelryList = jewelry,
            settings = PortfolioSettingsEntity()
        )

        val totalWeight = state.totalPhysicalWeight
        // Bullions: 92.6034768 + Jewelry: 64.9 = 157.5034768
        assertEquals(157.5034768, totalWeight, 0.001)

        // Karat 24 should only be from the bullions (all 5 are 24k)
        val k24Weight = bullions.filter { it.karat == 24 }.sumOf { it.weight }
        assertEquals(92.6034768, k24Weight, 0.001)

        // Karat 21 items in jewelry (64.900g)
        val k21Weight = jewelry.filter { it.karat == 21 }.sumOf { it.weight }
        assertEquals(64.900, k21Weight, 0.001)

        // Verify total weight equals sum of karats
        assertEquals(totalWeight, k24Weight + k21Weight, 0.001)

        // Pure gold (24k normalized equivalent): 92.6034768 + 64.900 * 21 / 24 = 92.6034768 + 56.7875 = 149.3909768
        val pureGold24k = (k24Weight * 24.0 / 24.0) + (k21Weight * 21.0 / 24.0)
        assertEquals(149.3909768, pureGold24k, 0.001)
        assertTrue("Pure gold must be less than or equal to total physical weight", pureGold24k <= totalWeight)
        assertEquals(94.849, (pureGold24k / totalWeight) * 100.0, 0.1)

        // Market values
        assertTrue("Total liquidation must be positive", state.totalLiquidation > 0.0)
        assertTrue("Bullion liquidation must be positive", state.bullionLiquidation > 0.0)
        assertTrue("Jewelry liquidation must be positive", state.jewelryLiquidation > 0.0)
    }

    @Test
    fun testPortfolioDistributionWith18kItem() {
        val bullions = AppDatabase.getInitialBullionList()
        val customJewelry = listOf(
            GoldItem(id = 10, title = "خاتم ذهب عيار 18", type = GoldType.JEWELRY, weightGrams = 12.0, karat = 18),
            GoldItem(id = 11, title = "سلسلة ذهب عيار 21", type = GoldType.JEWELRY, weightGrams = 20.0, karat = 21)
        )

        val state = PortfolioUiState(
            bullionList = bullions,
            jewelryList = customJewelry,
            settings = PortfolioSettingsEntity()
        )

        val k18Weight = customJewelry.filter { it.karat == 18 }.sumOf { it.weight }
        assertEquals(12.0, k18Weight, 0.001)

        val pure18 = 12.0 * 18.0 / 24.0
        assertEquals(9.0, pure18, 0.001)

        val totalPhysical = state.totalPhysicalWeight
        assertEquals(92.6034768 + 32.0, totalPhysical, 0.001)
    }
}
