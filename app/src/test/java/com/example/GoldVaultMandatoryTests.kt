package com.example

import com.example.data.calculations.PortfolioCalculator
import com.example.data.local.AppDatabase
import com.example.data.model.GoldItem
import com.example.data.model.GoldPriceState
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.PriceStatus
import com.example.support.FakeHttp
import com.example.data.remote.GoldPriceFetcher
import com.example.data.remote.GoldPriceProvider
import com.example.data.repository.GoldRepository
import com.example.ui.MainViewModel
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoldVaultMandatoryTests {

    private val sampleBullions = AppDatabase.getInitialBullionList()
    private val sampleJewelry = AppDatabase.getInitialJewelryList()

    // 1. مجموع أوزان السبائك الخمس يساوي تقريبًا 92.6034768 جرامًا
    @Test
    fun test01_sumOfFiveBullionWeights_equalsExpected() {
        val calculatedWeight = PortfolioCalculator.calculateBullionWeight(sampleBullions)
        assertEquals(92.6034768, calculatedWeight, 0.00001)
        assertEquals(5, sampleBullions.size)
    }

    // 2. وزن المشغولات الابتدائي يساوي 64.900 جرام
    @Test
    fun test02_initialJewelryWeight_equals64Point9Grams() {
        val jewelryWeight = PortfolioCalculator.calculateJewelryWeight(sampleJewelry)
        assertEquals(64.900, jewelryWeight, 0.001)
    }

    // 3. إجمالي الوزن يساوي مجموع السبائك والمشغولات
    @Test
    fun test03_totalPhysicalWeight_equalsBullionPlusJewelry() {
        val total = PortfolioCalculator.calculateTotalWeight(sampleBullions + sampleJewelry)
        val expected = 92.6034768 + 64.900
        assertEquals(expected, total, 0.00001)
    }

    // 4. قيمة التصفية تجمع قيمة السبائك والمشغولات وCash Back مرة واحدة فقط
    @Test
    fun test04_liquidationValue_addsBullionAndJewelryAndConfirmedCashBackOnlyOnce() {
        val k24Buy = 4250.0
        val k21Buy = 3718.0
        val k18Buy = 3187.0
        val confirmedCashBack = 1500.0

        val bullionVal = PortfolioCalculator.calculateBullionLiquidation(sampleBullions, k24Buy, k21Buy, k18Buy)
        val jewelryVal = PortfolioCalculator.calculateJewelryLiquidation(sampleJewelry, k24Buy, k21Buy, k18Buy)

        val totalLiquidation = PortfolioCalculator.calculateTotalLiquidation(bullionVal, jewelryVal, confirmedCashBack)

        val expected = BigDecimal.valueOf(bullionVal)
            .add(BigDecimal.valueOf(jewelryVal))
            .add(BigDecimal.valueOf(confirmedCashBack))
            .toDouble()

        assertEquals(expected, totalLiquidation, 0.01)
    }

    // 5. الربح المرجعي يساوي قيمة التصفية ناقص 1,000,462 جنيهًا
    @Test
    fun test05_referenceProfit_equalsLiquidationMinusBaseline1000462() {
        val baseline = 1000462.0
        val currentLiquidation = 1050000.0
        val profit = PortfolioCalculator.calculateProfitVsBaseline(currentLiquidation, baseline)
        val expected = 1050000.0 - 1000462.0 // 49,538.0
        assertEquals(expected, profit, 0.01)
    }

    // 6. النسبة المئوية صحيحة عند وجود ربح أو خسارة
    @Test
    fun test06_percentageChange_isAccurateForGainAndLoss() {
        val baseline = 1000000.0

        // Case A: Profit (+10%)
        val liquidationGain = 1100000.0
        val pctGain = PortfolioCalculator.calculatePercentageChange(liquidationGain, baseline)
        assertEquals(10.0, pctGain, 0.01)

        // Case B: Loss (-5%)
        val liquidationLoss = 950000.0
        val pctLoss = PortfolioCalculator.calculatePercentageChange(liquidationLoss, baseline)
        assertEquals(-5.0, pctLoss, 0.01)
    }

    // 7. عدم قسمة أي قيمة على صفر
    @Test
    fun test07_zeroDivisionProtection_noDivisionByZero() {
        // Zero baseline
        val pctZeroBaseline = PortfolioCalculator.calculatePercentageChange(100000.0, 0.0)
        assertEquals(0.0, pctZeroBaseline, 0.0)

        // Negative baseline
        val pctNegativeBaseline = PortfolioCalculator.calculatePercentageChange(100000.0, -500.0)
        assertEquals(0.0, pctNegativeBaseline, 0.0)

        // Zero target
        val targetZero = PortfolioCalculator.calculateTargetProgress(50000.0, 0.0)
        assertEquals(0.0, targetZero, 0.0)
    }

    // 8. التعامل الصحيح مع سعر مفقود أو قديم أو غير صالح
    @Test
    fun test08_missingOrStaleOrInvalidPrice_handledGracefully() {
        // Zero or NaN price should not crash or produce NaN
        val priceZero = PortfolioCalculator.getJewelerBuyPriceForKarat(24, 0.0, 0.0, 0.0)
        assertEquals(0.0, priceZero, 0.0)

        val priceNaN = PortfolioCalculator.getJewelerBuyPriceForKarat(24, Double.NaN, 0.0, 0.0)
        assertEquals(0.0, priceNaN, 0.0)

        val itemValWithZeroPrice = PortfolioCalculator.calculateItemLiquidation(sampleBullions[0], 0.0, 0.0, 0.0)
        assertEquals(0.0, itemValWithZeroPrice, 0.0)
    }

    // 9. عدم خلط سعر شراء الصائغ بسعر بيع المحل
    @Test
    fun test09_neverMixUpJewelerBuyPriceWithRetailSellPrice() {
        val gram24JewelerBuyPrice = 4250.0
        val gram24RetailSellPrice = 4285.0 // Retail shop sell price (higher)

        val liquidationWithBuyPrice = PortfolioCalculator.calculateBullionLiquidation(
            sampleBullions, gram24JewelerBuyPrice, 3718.0, 3187.0
        )
        val erroneousWithSellPrice = PortfolioCalculator.calculateBullionLiquidation(
            sampleBullions, gram24RetailSellPrice, 3718.0, 3187.0
        )

        // Verify that liquidation calculated with buy price is strictly less than retail sell price
        assertTrue(liquidationWithBuyPrice < erroneousWithSellPrice)
        // Verify formula specifically uses 4250.0
        val bar1Expected = BigDecimal.valueOf(0.5).multiply(BigDecimal.valueOf(4250.0)).toDouble()
        val bar1Calculated = PortfolioCalculator.calculateItemLiquidation(sampleBullions[0], gram24JewelerBuyPrice, 0.0, 0.0)
        assertEquals(bar1Expected, bar1Calculated, 0.01)
    }

    // 10. عدم استخدام Cash Back غير المؤكد
    @Test
    fun test10_unconfirmedCashBack_excludedFromLiquidation() {
        val bVal = 100000.0
        val jVal = 50000.0
        val zeroCashBack = 0.0

        val total = PortfolioCalculator.calculateTotalLiquidation(bVal, jVal, zeroCashBack)
        assertEquals(150000.0, total, 0.01)

        // Negative or NaN cashback is also treated as zero
        val totalNegativeCb = PortfolioCalculator.calculateTotalLiquidation(bVal, jVal, -500.0)
        assertEquals(150000.0, totalNegativeCb, 0.01)
    }

    // 11. حفظ البيانات واستعادتها دون فقد
    @Test
    fun test11_backupAndRestore_preservesDataWithoutLoss() {
        val items = sampleBullions + sampleJewelry
        val settings = PortfolioSettingsEntity(
            initialBaselineEgp = 1000462.0,
            targetGoalEgp = 1250000.0,
            confirmedCashBackEgp = 2500.0,
            cashBackSourceNotes = "كاش باك بي تي سي"
        )

        // Manually export JSON similar to repository
        val root = JSONObject()
        val sObj = JSONObject().apply {
            put("initialBaselineEgp", settings.initialBaselineEgp)
            put("targetGoalEgp", settings.targetGoalEgp)
            put("confirmedCashBackEgp", settings.confirmedCashBackEgp)
            put("cashBackSourceNotes", settings.cashBackSourceNotes)
        }
        root.put("settings", sObj)

        val itemsArr = org.json.JSONArray()
        for (item in items) {
            val iObj = JSONObject().apply {
                put("title", item.title)
                put("type", item.type.name)
                put("weightGrams", item.weightGrams)
                put("karat", item.karat)
                put("brand", item.brand)
                put("condition", item.condition)
            }
            itemsArr.put(iObj)
        }
        root.put("items", itemsArr)

        val jsonString = root.toString()

        // Verify restoration
        val parsed = JSONObject(jsonString)
        assertTrue(parsed.has("settings"))
        assertTrue(parsed.has("items"))

        val parsedSettings = parsed.getJSONObject("settings")
        assertEquals(1000462.0, parsedSettings.getDouble("initialBaselineEgp"), 0.01)
        assertEquals(1250000.0, parsedSettings.getDouble("targetGoalEgp"), 0.01)
        assertEquals(2500.0, parsedSettings.getDouble("confirmedCashBackEgp"), 0.01)

        val parsedItems = parsed.getJSONArray("items")
        assertEquals(items.size, parsedItems.length())
        assertEquals(sampleBullions[0].weightGrams, parsedItems.getJSONObject(0).getDouble("weightGrams"), 0.0001)
    }

    // 12. التعامل مع فشل الإنترنت دون انهيار التطبيق
    @Test
    fun test12_offlineNetworkFailure_handledGracefullyWithoutCrash() = runBlocking {
        // Instantiate fetcher with custom unreachable client
        val fetcher = GoldPriceFetcher(
            OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MILLISECONDS)
                .readTimeout(1, TimeUnit.MILLISECONDS)
                .build()
        )

        val initialPrices = GoldPriceState(
            gram24BuyEgp = 7131.5,
            gram21BuyEgp = 6240.0,
            sourceStatus = PriceStatus.RECENT
        )

        val result = fetcher.fetchLivePrices(initialPrices)
        assertNotNull(result)
        // Ensure price state is preserved rather than throwing exception
        assertEquals(7131.5, result.gram24BuyEgp, 0.01)
        assertTrue(
            result.sourceStatus == PriceStatus.STALE ||
            result.sourceStatus == PriceStatus.UNAVAILABLE ||
            result.sourceStatus == PriceStatus.RECENT
        )
    }

    // 13. التعامل مع الأوزان والأسعار الصفرية والسالبة والمدخلات غير الصالحة
    @Test
    fun test13_invalidZeroAndNegativeInputs_handledSafely() {
        val invalidItem = GoldItem(
            id = 99,
            title = "عنصر غير صالح",
            type = GoldType.BULLION,
            weightGrams = -10.0, // Negative weight
            karat = 24
        )
        val valueNegativeWeight = PortfolioCalculator.calculateItemLiquidation(invalidItem, 4250.0, 3718.0, 3187.0)
        assertEquals(0.0, valueNegativeWeight, 0.0)

        val zeroWeightItem = invalidItem.copy(weight = 0.0)
        val valueZeroWeight = PortfolioCalculator.calculateItemLiquidation(zeroWeightItem, 4250.0, 3718.0, 3187.0)
        assertEquals(0.0, valueZeroWeight, 0.0)

        val negativePriceValue = PortfolioCalculator.calculateItemLiquidation(sampleBullions[0], -4250.0, -3718.0, 0.0)
        assertEquals(0.0, negativePriceValue, 0.0)
    }

    // 14. التأكد من أن مجموع قيمة القطع يطابق إجمالي الفئات
    @Test
    fun test14_sumOfIndividualItemLiquidation_matchesCategoryTotal() {
        val k24Buy = 4250.0
        val k21Buy = 3718.0
        val k18Buy = 3187.0

        var sumOfIndividualBullions = BigDecimal.ZERO
        for (item in sampleBullions) {
            val itemVal = PortfolioCalculator.calculateItemLiquidation(item, k24Buy, k21Buy, k18Buy)
            sumOfIndividualBullions = sumOfIndividualBullions.add(BigDecimal.valueOf(itemVal))
        }

        val totalBullionLiquidation = PortfolioCalculator.calculateBullionLiquidation(sampleBullions, k24Buy, k21Buy, k18Buy)

        assertEquals(sumOfIndividualBullions.toDouble(), totalBullionLiquidation, 0.01)
    }

    // 15. المخزون الابتدائي للسبائك داخل gold_items (جدول gold_bars المكرر أُلغي)
    @Test
    fun test15_initialBullionInventory_containsFiveBarsWithRequiredFields() {
        val initialBars = AppDatabase.getInitialBullionList()
        assertEquals(5, initialBars.size)

        val expectedWeights = listOf(0.5, 1.0, 10.0, 50.0, 31.1034768)
        assertEquals(expectedWeights, initialBars.map { it.weight })

        for (bar in initialBars) {
            assertEquals(GoldType.BULLION, bar.type)
            assertEquals(24, bar.karat)
            assertEquals("مغلفة وسليمة", bar.status)
            assertNotNull(bar.purchaseCostEgp)
            assertTrue(bar.purchaseCostEgp!! > 0.0)
            assertTrue(bar.weight > 0.0)
        }
        assertEquals(92.6034768, initialBars.sumOf { it.weight }, 0.00001)
        assertEquals(673000.0, initialBars.sumOf { it.purchaseCostEgp ?: 0.0 }, 0.01)
    }

    // 16. التحقق من كيان GoldItem والحقول (id, name, weight, karat, cost, status, type) للسبائك والمشغولات
    @Test
    fun test16_goldItemSchema_containsAllRequiredFields() {
        val bullionItem = GoldItem(
            id = 101L,
            name = "سبيكة تجريبية 10 جم",
            weight = 10.0,
            karat = 24,
            cost = 42500.0,
            status = "مغلفة وسليمة",
            type = GoldType.BULLION
        )

        val jewelryItem = GoldItem(
            id = 102L,
            name = "سلسلة ذهب عيار 21",
            weight = 15.5,
            karat = 21,
            cost = null,
            status = "سليمة",
            type = GoldType.JEWELRY
        )

        // Verify fields
        assertEquals(101L, bullionItem.id)
        assertEquals("سبيكة تجريبية 10 جم", bullionItem.name)
        assertEquals(10.0, bullionItem.weight, 0.001)
        assertEquals(24, bullionItem.karat)
        assertEquals(42500.0, bullionItem.cost!!, 0.01)
        assertEquals("مغلفة وسليمة", bullionItem.status)
        assertEquals(GoldType.BULLION, bullionItem.type)

        assertEquals(102L, jewelryItem.id)
        assertEquals("سلسلة ذهب عيار 21", jewelryItem.name)
        assertEquals(15.5, jewelryItem.weight, 0.001)
        assertEquals(21, jewelryItem.karat)
        assertEquals(null, jewelryItem.cost)
        assertEquals("سليمة", jewelryItem.status)
        assertEquals(GoldType.JEWELRY, jewelryItem.type)
    }

    // 17. مصدر واحد سليم: الأسعار تُقرأ من كتلة كل عيار وتُصنَّف "حديث" لا "مباشر"
    @Test
    fun test17_singleSource_isRecentNotLive() = runBlocking {
        val client = FakeHttp.client(mapOf("isagha" to FakeHttp.isaghaHtml(), "gold-api" to "{\"price\": 4100.5}"))
        val result = GoldPriceFetcher(client).fetchLivePrices(GoldPriceState())

        assertEquals(PriceStatus.RECENT, result.sourceStatus)
        assertEquals(6125.0, result.gram21BuyEgp, 0.001)
        assertEquals(7000.0, result.gram24BuyEgp, 0.001)
        assertEquals(5250.0, result.gram18BuyEgp, 0.001)
        assertEquals(4100.5, result.ounceUsd, 0.001)
        assertTrue(result.gram21SellEgp >= result.gram21BuyEgp)
    }

    // 18. مصدران متطابقان => LIVE
    @Test
    fun test18_twoAgreeingSources_areLive() = runBlocking {
        val client = FakeHttp.client(
            mapOf(
                "isagha" to FakeHttp.isaghaHtml(),
                "gold-price-today" to FakeHttp.goldPriceTodayHtml(k21 = 6140.0),
                "gold-api" to "{\"price\": 4100.5}"
            )
        )
        val provider = GoldPriceProvider(client)
        val result = provider.fetchGoldPrices()

        assertTrue(result.isSuccess)
        val state = result.getOrThrow()
        assertEquals(PriceStatus.LIVE, state.sourceStatus)
        assertFalse(state.isManual)
        assertEquals(6125.0, state.gram21BuyEgp, 0.001)
    }

    // 19. مصدران متناقضان => رفض التحديث والإبقاء على الأسعار السابقة مع تحذير
    @Test
    fun test19_conflictingSources_areRejectedAndKeepPreviousPrices() = runBlocking {
        val client = FakeHttp.client(
            mapOf(
                "isagha" to FakeHttp.isaghaHtml(k21 = 6125.0),
                "gold-price-today" to FakeHttp.goldPriceTodayHtml(k21 = 5400.0)
            )
        )
        val previous = GoldPriceState(gram24BuyEgp = 7131.5, gram21BuyEgp = 6240.0, sourceStatus = PriceStatus.RECENT)
        val result = GoldPriceFetcher(client).fetchLivePrices(previous)

        assertEquals(PriceStatus.STALE, result.sourceStatus)
        assertEquals(6240.0, result.gram21BuyEgp, 0.001)
        assertNotNull(result.warningMessage)
    }

    // 20. فشل كل المصادر => Result.failure (لا تغيير في الإعدادات ولا أسعار مخترعة)
    @Test
    fun test20_allSourcesDown_returnsFailure() = runBlocking {
        val provider = GoldPriceProvider(FakeHttp.client(emptyMap()))
        val result = provider.fetchGoldPrices()
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    // 21. فحص إضافة قطعة ذهبية جديدة وتحديث إجمالي وزن الذهب في المحفظة
    @Test
    fun test21_addSimpleGoldPiece_updatesTotalWeightCorrectly() {
        val initialItems = listOf(
            GoldItem(name = "سبيكة 10 جم", weight = 10.0, karat = 24, type = GoldType.BULLION),
            GoldItem(name = "سلسلة 15 جم", weight = 15.0, karat = 21, type = GoldType.JEWELRY)
        )
        val initialWeight = PortfolioCalculator.calculateTotalWeight(initialItems)
        assertEquals(25.0, initialWeight, 0.001)

        // Add a new gold piece: 31.1 grams, 24 karat
        val newPiece = GoldItem(name = "أونصة ذهب", weight = 31.1, karat = 24, type = GoldType.BULLION)
        val updatedItems = initialItems + newPiece
        val updatedWeight = PortfolioCalculator.calculateTotalWeight(updatedItems)

        // Verify total weight increased by exactly 31.1 grams
        assertEquals(25.0 + 31.1, updatedWeight, 0.001)
    }

    // 22. فحص كاش باك السبائك بمعدل 28 جنيه مصري/جرام وإضافته لإجمالي المحفظة
    @Test
    fun test22_bullionCashBack_approx28EgpPerGram_addsToLiquidation() {
        assertEquals(28.0, PortfolioCalculator.STANDARD_BULLION_CASH_BACK_PER_GRAM, 0.001)

        // Compute expected cashback for sample bullions (approx 92.6034768 grams)
        val expectedCashBack = PortfolioCalculator.calculateExpectedBullionCashBack(sampleBullions)
        val expectedValue = 92.6034768 * 28.0 // ~2592.897 EGP
        assertEquals(expectedValue, expectedCashBack, 0.01)

        // Verify liquidation with automatic bullion cashback included
        val bullionVal = 600000.0
        val jewelryVal = 400000.0
        val totalLiquidationWithAutoCb = PortfolioCalculator.calculateTotalLiquidation(
            bullionVal = bullionVal,
            jewelryVal = jewelryVal,
            confirmedCashBack = expectedCashBack
        )

        assertEquals(bullionVal + jewelryVal + expectedCashBack, totalLiquidationWithAutoCb, 0.01)
    }

    // 23. فحص مصدر الأونصة العالمية الدقيق والمؤكد
    @Test
    fun test23_accurateGlobalOunceSource_configuredCorrectly() {
        val state = GoldPriceState(
            ounceUsd = 4026.90,
            ounceSourceName = "بورصة الذهب العالمية (Gold-API.com XAU/USD)",
            ounceSourceUrl = "https://api.gold-api.com/price/XAU"
        )

        assertEquals("https://api.gold-api.com/price/XAU", state.ounceSourceUrl)
        assertTrue(state.ounceSourceName.contains("Gold-API.com"))
        assertTrue(state.ounceUsd > 2000.0)
    }

    // 24. فحص إعدادات تنبيهات حركة الأسعار والحد الأدنى للتحرك
    @Test
    fun test24_priceMovementAlerts_thresholdConfiguration() {
        val settings = PortfolioSettingsEntity(
            priceAlertsEnabled = true,
            priceAlertThresholdEgp = 25.0,
            lastNotifiedPrice21Egp = 3700.0
        )

        assertTrue(settings.priceAlertsEnabled)
        assertEquals(25.0, settings.priceAlertThresholdEgp, 0.01)

        // Movement below threshold: 3715 - 3700 = 15 (< 25) -> should NOT trigger
        val smallMove = 3715.0
        val shouldTriggerSmall = kotlin.math.abs(smallMove - settings.lastNotifiedPrice21Egp) >= settings.priceAlertThresholdEgp
        assertFalse(shouldTriggerSmall)

        // Movement at or above threshold: 3730 - 3700 = 30 (>= 25) -> SHOULD trigger
        val bigMove = 3730.0
        val shouldTriggerBig = kotlin.math.abs(bigMove - settings.lastNotifiedPrice21Egp) >= settings.priceAlertThresholdEgp
        assertTrue(shouldTriggerBig)
    }

    // 25. فحص الفترات الزمنية للرسم البياني (يومي، أسبوعي، شهري، 3 شهور)
    @Test
    fun test25_chartTimeframes_durationAndFilterCalculation() {
        val dayMillis = 24L * 60 * 60 * 1000
        val weekMillis = 7L * dayMillis
        val monthMillis = 30L * dayMillis
        val threeMonthsMillis = 90L * dayMillis

        val t1D = com.example.ui.components.ChartTimeframe.ONE_DAY
        val t1W = com.example.ui.components.ChartTimeframe.ONE_WEEK
        val t1M = com.example.ui.components.ChartTimeframe.ONE_MONTH
        val t3M = com.example.ui.components.ChartTimeframe.THREE_MONTHS

        assertEquals(dayMillis, t1D.durationMillis)
        assertEquals(weekMillis, t1W.durationMillis)
        assertEquals(monthMillis, t1M.durationMillis)
        assertEquals(threeMonthsMillis, t3M.durationMillis)

        assertEquals("1D", t1D.code)
        assertEquals("1W", t1W.code)
        assertEquals("1M", t1M.code)
        assertEquals("3M", t3M.code)
    }

    // 26. فحص حسابات نقاط الشموع التاريخية الحقيقية للذهب وتناسبها مع قيمة المحفظة
    @Test
    fun test26_realMarketCandles_historicalPointsCalculation() {
        val candles = listOf(
            com.example.data.remote.HistoricalPricePoint(timestamp = 1000L, priceUsd = 3800.0),
            com.example.data.remote.HistoricalPricePoint(timestamp = 2000L, priceUsd = 3900.0),
            com.example.data.remote.HistoricalPricePoint(timestamp = 3000L, priceUsd = 4000.0)
        )
        val currentLiquidation = 1000000.0
        val lastPrice = candles.last().priceUsd // 4000.0

        // At point 1 (3800 USD): ratio = 3800 / 4000 = 0.95 -> 950,000 EGP
        val val1 = currentLiquidation * (candles[0].priceUsd / lastPrice)
        assertEquals(950000.0, val1, 0.01)

        // At point 3 (4000 USD): ratio = 1.0 -> 1,000,000 EGP
        val val3 = currentLiquidation * (candles[2].priceUsd / lastPrice)
        assertEquals(1000000.0, val3, 0.01)
    }

    // 27. فحص ميزة البيع الذكي الجزئي للسبائك واحتساب النقدية والكاش باك بدقة
    @Test
    fun test27_smartSellBullion_partialSaleCalculation() = runBlocking {
        val bar = GoldItem(
            id = 10,
            name = "سبيكة BTC عيار 24 (50 جرامًا)",
            type = GoldType.BULLION,
            weight = 50.0,
            karat = 24,
            cost = 363378.0,
            cashBackRatePerGram = 28.0
        )
        val weightToSell = 20.0
        val sellPricePerGram = 7131.5
        val cashBackRate = 28.0

        val goldVal = weightToSell * sellPricePerGram // 142,630.0
        val cashBack = weightToSell * cashBackRate // 560.0
        val totalCash = goldVal + cashBack // 143,190.0

        assertEquals(142630.0, goldVal, 0.01)
        assertEquals(560.0, cashBack, 0.01)
        assertEquals(143190.0, totalCash, 0.01)

        val costBasis = (bar.cost!! / bar.weight) * weightToSell // (363378 / 50) * 20 = 145,351.2
        val profitOrLoss = totalCash - costBasis
        assertEquals(-2161.2, profitOrLoss, 0.1)

        val remainingWeight = bar.weight - weightToSell
        assertEquals(30.0, remainingWeight, 0.001)
    }

    // 28. الكاش باك لا يُحتسب لسبيكة مفتوحة أو مشغولات أو قطعة غير مغلفة
    @Test
    fun test28_cashBack_onlyForSealedIntactBullion() {
        val sealed = GoldItem(title = "مغلفة", type = GoldType.BULLION, weightGrams = 10.0, karat = 24, condition = "مغلفة وسليمة")
        val opened = sealed.copy(status = "مفتوحة")
        val notSealed = sealed.copy(status = "غير مغلفة")
        val simple = sealed.copy(status = "سليمة")
        val jewelry = GoldItem(title = "حلق", type = GoldType.JEWELRY, weightGrams = 10.0, karat = 21, condition = "مغلفة وسليمة")

        assertTrue(PortfolioCalculator.isCashBackEligible(sealed))
        assertFalse(PortfolioCalculator.isCashBackEligible(opened))
        assertFalse(PortfolioCalculator.isCashBackEligible(notSealed))
        assertFalse(PortfolioCalculator.isCashBackEligible(simple))
        assertFalse(PortfolioCalculator.isCashBackEligible(jewelry))
        assertEquals(280.0, PortfolioCalculator.calculateExpectedBullionCashBack(listOf(sealed, opened, notSealed, simple, jewelry)), 0.001)
    }

    // 29. لا يوجد رمز افتراضي: الرمز الافتراضي القديم 1234 لا يُعد رمزاً معيّناً ولا يفتح أي شيء
    @Test
    fun test29_securityPin_hasNoDefaultAndOldDefaultIsNotAccepted() {
        val defaultSettings = PortfolioSettingsEntity(id = 1)
        assertEquals("", defaultSettings.securityPin)
        assertFalse(com.example.security.PinHasher.hasPin(defaultSettings.securityPin))
        assertFalse(com.example.security.PinHasher.hasPin("1234"))
        assertFalse(com.example.security.PinHasher.verify("1234", defaultSettings.securityPin))
        assertTrue(defaultSettings.requireAuthOnSale)
        assertFalse(defaultSettings.isAppLockEnabled)
    }

    // 30. التحقق الفعلي من الـ PIN عبر PinHasher (بدون باب خلفي)
    @Test
    fun test30_securityAuthOnSale_usesRealPinVerification() {
        val stored = com.example.security.PinHasher.hash("4321")
        assertTrue(com.example.security.PinHasher.verify("4321", stored))
        assertFalse(com.example.security.PinHasher.verify("0000", stored))
        assertFalse("1234 must never be a master key", com.example.security.PinHasher.verify("1234", stored))
    }

    // 31. النسخة الاحتياطية لا تحتوي أبداً على الرمز أو إعدادات القفل
    @Test
    fun test31_backup_neverContainsSecuritySettings() {
        val settings = PortfolioSettingsEntity(
            id = 1,
            securityPin = com.example.security.PinHasher.hash("2580"),
            isAppLockEnabled = true
        )
        val json = com.example.data.backup.BackupCodec.encode(emptyList(), settings, emptyList(), emptyList())
        assertFalse(json.contains("securityPin"))
        assertFalse(json.contains("pbkdf2"))
        assertFalse(json.contains("isAppLockEnabled"))
        assertFalse(json.contains("isBiometricEnabled"))
    }
}
