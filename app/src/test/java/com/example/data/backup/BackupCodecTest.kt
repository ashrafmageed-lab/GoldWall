package com.example.data.backup

import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.TransactionRecord
import com.example.data.model.ValuationRecord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCodecTest {

    private val items = listOf(
        GoldItem(title = "سبيكة 10 جم", type = GoldType.BULLION, weightGrams = 10.0, karat = 24, purchaseCostEgp = 72000.0),
        GoldItem(title = "خاتم", type = GoldType.JEWELRY, weightGrams = 5.5, karat = 21, condition = "سليمة")
    )
    private val settings = PortfolioSettingsEntity(id = 1, initialBaselineEgp = 900000.0, confirmedCashBackEgp = 1200.0, useManualPrices = true)
    private val valuations = listOf(ValuationRecord(timestamp = 1L, bullionValueEgp = 1.0, jewelryValueEgp = 2.0, cashBackEgp = 3.0, totalLiquidationEgp = 6.0, gram24PriceEgp = 7.0, gram21PriceEgp = 6.0, ounceUsdPrice = 4000.0, profitVsBaselineEgp = 0.0, percentChange = 0.0))
    private val transactions = listOf(TransactionRecord(timestamp = 2L, actionType = "a", title = "t", details = "d"))

    private fun success(json: String) =
        (BackupCodec.decode(json) as BackupCodec.DecodeResult.Success).backup

    private fun failure(json: String) =
        (BackupCodec.decode(json) as BackupCodec.DecodeResult.Failure).errors

    @Test fun roundTripPreservesEverything() {
        val json = BackupCodec.encode(items, settings, valuations, transactions)
        val b = success(json)
        assertEquals(2, b.items.size)
        assertEquals(10.0, b.items[0].weight, 0.0)
        assertEquals(72000.0, b.items[0].purchaseCostEgp!!, 0.0)
        assertEquals("سليمة", b.items[1].status)
        assertEquals(1, b.valuations!!.size)
        assertEquals(1, b.transactions!!.size)
        val restored = b.settings!!.applyTo(PortfolioSettingsEntity())
        assertEquals(900000.0, restored.initialBaselineEgp, 0.0)
        assertEquals(1200.0, restored.confirmedCashBackEgp, 0.0)
        assertTrue(restored.useManualPrices)
    }

    @Test fun v1FilesStillImport() {
        val json = """{"app":"x","version":1,"items":[{"title":"سبيكة","type":"BULLION","weightGrams":10,"karat":24}]}"""
        val b = success(json)
        assertEquals(1, b.items.size)
        assertEquals(null, b.valuations)
    }

    @Test fun rejectsInvalidItemsWithoutPartialResult() {
        val bad = """{"version":2,"items":[
            {"title":"ok","type":"BULLION","weightGrams":10,"karat":24},
            {"title":"","type":"BULLION","weightGrams":10,"karat":24},
            {"title":"neg","type":"BULLION","weightGrams":-1,"karat":24},
            {"title":"karat","type":"JEWELRY","weightGrams":3,"karat":30},
            {"title":"type","type":"???","weightGrams":3,"karat":21},
            {"title":"cost","type":"BULLION","weightGrams":3,"karat":24,"purchaseCostEgp":-5}
        ]}"""
        val errors = failure(bad)
        assertEquals(5, errors.size)
    }

    @Test fun rejectsStructuralProblems() {
        assertTrue(failure("not json").first().contains("JSON"))
        assertTrue(failure("""{"version":9,"items":[{}]}""").first().contains("غير مدعوم"))
        assertTrue(failure("""{"version":2}""").first().contains("عناصر"))
        assertTrue(failure("""{"version":2,"items":[]}""").first().contains("لن تتم الاستعادة"))
    }

    @Test fun exportNeverContainsSecurityFields() {
        val json = BackupCodec.encode(items, settings.copy(securityPin = "pbkdf2:1:aa:bb", isAppLockEnabled = true), emptyList(), emptyList())
        val s = JSONObject(json).getJSONObject("settings")
        assertTrue(!s.has("securityPin") && !s.has("isAppLockEnabled") && !s.has("isBiometricEnabled"))
    }
}
