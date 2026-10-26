package com.example.data.calculations

import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import java.math.BigDecimal
import java.math.RoundingMode

object PortfolioCalculator {

    const val INITIAL_BASELINE_EGP = 1000462.0
    const val INITIAL_TARGET_EGP = 1250000.0
    const val INITIAL_BULLION_REFERENCE_COST_EGP = 673000.0

    // Standard 5 BTC bars default weights
    val DEFAULT_BULLION_WEIGHTS = listOf(0.5, 1.0, 10.0, 50.0, 31.1034768)
    const val EXPECTED_INITIAL_BULLION_WEIGHT = 92.6034768
    const val EXPECTED_INITIAL_JEWELRY_WEIGHT = 64.900

    // معدل الكاش باك القياسي للسبائك في مصر (BTC وغيرها): 28 جنيه / جرام تقريباً
    const val STANDARD_BULLION_CASH_BACK_PER_GRAM = 28.0

    private val NOT_SEALED_MARKERS = listOf("غير مغلفة", "مفتوح", "تالف", "مكسور", "ممزق", "مفتوحة")

    /**
     * Cash back is only refunded for bullion that is still sealed and intact.
     * Jewelry, loose gold and opened/damaged bars earn none.
     */
    fun isCashBackEligible(item: GoldItem): Boolean {
        if (item.type != GoldType.BULLION) return false
        val status = item.status
        if (!status.contains("مغلفة")) return false
        return NOT_SEALED_MARKERS.none { status.contains(it) }
    }

    private fun isFinitePositive(v: Double) = !v.isNaN() && !v.isInfinite() && v > 0.0

    /**
     * Calculates expected bullion cash back (استرداد جزء من المصنعية عند بيع السبائك مغلفة)
     * Standard BTC bullion cashback rate is ~28 EGP / gram, only for sealed, intact bars.
     */
    fun calculateExpectedBullionCashBack(
        items: List<GoldItem>,
        defaultRatePerGram: Double = STANDARD_BULLION_CASH_BACK_PER_GRAM
    ): Double {
        var total = BigDecimal.ZERO
        for (item in items) {
            if (isCashBackEligible(item) && isFinitePositive(item.weightGrams)) {
                val rate = item.cashBackRatePerGram ?: defaultRatePerGram
                if (rate.isNaN() || rate.isInfinite() || rate < 0.0) continue
                val itemCb = BigDecimal.valueOf(item.weightGrams).multiply(BigDecimal.valueOf(rate))
                total = total.add(itemCb)
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates total bullion weight.
     * Guards against negative or NaN values.
     */
    fun calculateBullionWeight(items: List<GoldItem>): Double {
        var total = BigDecimal.ZERO
        for (item in items) {
            if (item.type == GoldType.BULLION && item.weightGrams > 0 && !item.weightGrams.isNaN() && !item.weightGrams.isInfinite()) {
                total = total.add(BigDecimal.valueOf(item.weightGrams))
            }
        }
        return total.toDouble()
    }

    /**
     * Calculates total jewelry weight.
     */
    fun calculateJewelryWeight(items: List<GoldItem>): Double {
        var total = BigDecimal.ZERO
        for (item in items) {
            if (item.type == GoldType.JEWELRY && item.weightGrams > 0 && !item.weightGrams.isNaN() && !item.weightGrams.isInfinite()) {
                total = total.add(BigDecimal.valueOf(item.weightGrams))
            }
        }
        return total.toDouble()
    }

    /**
     * Calculates combined physical weight.
     */
    fun calculateTotalWeight(items: List<GoldItem>): Double {
        val b = BigDecimal.valueOf(calculateBullionWeight(items))
        val j = BigDecimal.valueOf(calculateJewelryWeight(items))
        return b.add(j).toDouble()
    }

    /**
     * Obtains the jeweler buy price (سعر شراء الصائغ من العميل) for a given karat.
     * Never mixes up buy price with retail sell price.
     */
    fun getJewelerBuyPriceForKarat(
        karat: Int,
        gram24BuyPrice: Double,
        gram21BuyPrice: Double,
        gram18BuyPrice: Double
    ): Double {
        if (gram24BuyPrice <= 0 || gram24BuyPrice.isNaN()) return 0.0
        return when (karat) {
            24 -> gram24BuyPrice
            21 -> if (gram21BuyPrice > 0 && !gram21BuyPrice.isNaN()) gram21BuyPrice else (gram24BuyPrice * 21.0 / 24.0)
            18 -> if (gram18BuyPrice > 0 && !gram18BuyPrice.isNaN()) gram18BuyPrice else (gram24BuyPrice * 18.0 / 24.0)
            else -> {
                if (karat in 1..24) {
                    (gram24BuyPrice * karat.toDouble() / 24.0)
                } else 0.0
            }
        }
    }

    /**
     * Calculates estimated liquidation value of a single item when sold to a jeweler.
     */
    fun calculateItemLiquidation(
        item: GoldItem,
        gram24BuyPrice: Double,
        gram21BuyPrice: Double,
        gram18BuyPrice: Double
    ): Double {
        if (item.weightGrams <= 0 || item.weightGrams.isNaN() || item.weightGrams.isInfinite()) return 0.0
        val pricePerGram = getJewelerBuyPriceForKarat(item.karat, gram24BuyPrice, gram21BuyPrice, gram18BuyPrice)
        if (!pricePerGram.isFinite() || pricePerGram <= 0) return 0.0

        val weightBd = BigDecimal.valueOf(item.weightGrams)
        val priceBd = BigDecimal.valueOf(pricePerGram)
        val itemVal = weightBd.multiply(priceBd)

        return itemVal.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates estimated liquidation value for all bullion items.
     */
    fun calculateBullionLiquidation(
        items: List<GoldItem>,
        gram24BuyPrice: Double,
        gram21BuyPrice: Double,
        gram18BuyPrice: Double
    ): Double {
        var total = BigDecimal.ZERO
        for (item in items) {
            if (item.type == GoldType.BULLION) {
                val itemVal = calculateItemLiquidation(item, gram24BuyPrice, gram21BuyPrice, gram18BuyPrice)
                total = total.add(BigDecimal.valueOf(itemVal))
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates estimated liquidation value for all jewelry items.
     */
    fun calculateJewelryLiquidation(
        items: List<GoldItem>,
        gram24BuyPrice: Double,
        gram21BuyPrice: Double,
        gram18BuyPrice: Double
    ): Double {
        var total = BigDecimal.ZERO
        for (item in items) {
            if (item.type == GoldType.JEWELRY) {
                val itemVal = calculateItemLiquidation(item, gram24BuyPrice, gram21BuyPrice, gram18BuyPrice)
                total = total.add(BigDecimal.valueOf(itemVal))
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Total liquidation value = Bullion liquidation + Jewelry liquidation + Confirmed Cash Back.
     * Confirmed Cash Back is added strictly ONCE.
     */
    fun calculateTotalLiquidation(
        bullionVal: Double,
        jewelryVal: Double,
        confirmedCashBack: Double
    ): Double {
        val b = BigDecimal.valueOf(if (isFinitePositive(bullionVal)) bullionVal else 0.0)
        val j = BigDecimal.valueOf(if (isFinitePositive(jewelryVal)) jewelryVal else 0.0)
        val cb = BigDecimal.valueOf(if (isFinitePositive(confirmedCashBack)) confirmedCashBack else 0.0)
        return b.add(j).add(cb).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Reference profit or loss: Total liquidation - Baseline.
     */
    fun calculateProfitVsBaseline(totalLiquidation: Double, baseline: Double): Double {
        if (!totalLiquidation.isFinite() || !baseline.isFinite()) return 0.0
        val t = BigDecimal.valueOf(totalLiquidation)
        val b = BigDecimal.valueOf(baseline)
        return t.subtract(b).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Reference percentage change: (Total liquidation - Baseline) / Baseline * 100.
     * Protected against divide by zero (baseline == 0).
     */
    fun calculatePercentageChange(totalLiquidation: Double, baseline: Double): Double {
        if (baseline <= 0.0 || !baseline.isFinite() || !totalLiquidation.isFinite()) {
            return 0.0
        }
        val diff = totalLiquidation - baseline
        val pct = (diff / baseline) * 100.0
        return BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Progress towards financial target: (Total liquidation / Target) * 100.
     * Protected against divide by zero (target <= 0).
     */
    fun calculateTargetProgress(totalLiquidation: Double, target: Double): Double {
        if (target <= 0.0 || !target.isFinite() || !totalLiquidation.isFinite() || totalLiquidation <= 0.0) {
            return 0.0
        }
        val pct = (totalLiquidation / target) * 100.0
        return BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Difference compared to last saved valuation.
     */
    fun calculateValuationDifference(currentVal: Double, lastVal: Double): Double {
        if (!currentVal.isFinite() || !lastVal.isFinite() || lastVal <= 0.0) return 0.0
        return BigDecimal.valueOf(currentVal).subtract(BigDecimal.valueOf(lastVal))
            .setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
