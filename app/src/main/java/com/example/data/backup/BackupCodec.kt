package com.example.data.backup

import com.example.data.model.GoldItem
import com.example.data.model.GoldType
import com.example.data.model.PortfolioSettingsEntity
import com.example.data.model.TransactionRecord
import com.example.data.model.ValuationRecord
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Backup file format (v2) with full validation. Decoding never touches the database: the caller
 * only writes after [decode] returned [DecodeResult.Success], inside one transaction.
 *
 * v1 files (items + 7 settings only) are still accepted.
 * Security settings (PIN, lock, biometrics) are intentionally never exported or imported.
 */
object BackupCodec {

    const val APP_NAME = "Gold Vault Tracker"
    const val CURRENT_VERSION = 2
    const val MAX_ITEMS = 10_000
    const val MAX_HISTORY_ROWS = 50_000
    const val MAX_FILE_CHARS = 5_000_000
    private const val MAX_ERRORS = 10
    private const val MAX_WEIGHT_GRAMS = 100_000.0
    private const val MAX_MONEY = 1.0E12

    data class SettingsPatch(
        val initialBaselineEgp: Double?,
        val targetGoalEgp: Double?,
        val confirmedCashBackEgp: Double?,
        val cashBackSourceNotes: String?,
        val manual24BuyPrice: Double?,
        val manual21BuyPrice: Double?,
        val useManualPrices: Boolean?,
        val autoAddBullionCashBack: Boolean?,
        val priceAlertsEnabled: Boolean?,
        val priceAlertThresholdEgp: Double?
    ) {
        fun applyTo(current: PortfolioSettingsEntity): PortfolioSettingsEntity = current.copy(
            initialBaselineEgp = initialBaselineEgp ?: current.initialBaselineEgp,
            targetGoalEgp = targetGoalEgp ?: current.targetGoalEgp,
            confirmedCashBackEgp = confirmedCashBackEgp ?: current.confirmedCashBackEgp,
            cashBackSourceNotes = cashBackSourceNotes ?: current.cashBackSourceNotes,
            manual24BuyPrice = manual24BuyPrice ?: current.manual24BuyPrice,
            manual21BuyPrice = manual21BuyPrice ?: current.manual21BuyPrice,
            useManualPrices = useManualPrices ?: current.useManualPrices,
            autoAddBullionCashBack = autoAddBullionCashBack ?: current.autoAddBullionCashBack,
            priceAlertsEnabled = priceAlertsEnabled ?: current.priceAlertsEnabled,
            priceAlertThresholdEgp = priceAlertThresholdEgp ?: current.priceAlertThresholdEgp,
            isPortfolioInitialized = true
        )
    }

    data class DecodedBackup(
        val version: Int,
        val settings: SettingsPatch?,
        val items: List<GoldItem>,
        val valuations: List<ValuationRecord>?,
        val transactions: List<TransactionRecord>?
    )

    sealed interface DecodeResult {
        data class Success(val backup: DecodedBackup) : DecodeResult
        data class Failure(val errors: List<String>) : DecodeResult
    }

    fun encode(
        items: List<GoldItem>,
        settings: PortfolioSettingsEntity,
        valuations: List<ValuationRecord>,
        transactions: List<TransactionRecord>,
        now: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", APP_NAME)
        root.put("version", CURRENT_VERSION)
        root.put("exportTime", now)

        root.put("settings", JSONObject().apply {
            put("initialBaselineEgp", settings.initialBaselineEgp)
            put("targetGoalEgp", settings.targetGoalEgp)
            put("confirmedCashBackEgp", settings.confirmedCashBackEgp)
            put("cashBackSourceNotes", settings.cashBackSourceNotes)
            put("manual24BuyPrice", settings.manual24BuyPrice)
            put("manual21BuyPrice", settings.manual21BuyPrice)
            put("useManualPrices", settings.useManualPrices)
            put("autoAddBullionCashBack", settings.autoAddBullionCashBack)
            put("priceAlertsEnabled", settings.priceAlertsEnabled)
            put("priceAlertThresholdEgp", settings.priceAlertThresholdEgp)
        })

        val itemsArray = JSONArray()
        for (item in items) {
            itemsArray.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("type", item.type.name)
                put("weightGrams", item.weight)
                put("karat", item.karat)
                item.purchaseCostEgp?.let { put("purchaseCostEgp", it) }
                put("brand", item.brand)
                put("condition", item.status)
                put("notes", item.notes)
                item.cashBackRatePerGram?.let { put("cashBackRatePerGram", it) }
                item.barcode?.let { put("barcode", it) }
                item.imageUri?.let { put("imageUri", it) }
                item.invoiceUri?.let { put("invoiceUri", it) }
                put("createdAt", item.createdAt)
            })
        }
        root.put("items", itemsArray)

        val valuationsArray = JSONArray()
        for (v in valuations) {
            valuationsArray.put(JSONObject().apply {
                put("timestamp", v.timestamp)
                put("bullionValueEgp", v.bullionValueEgp)
                put("jewelryValueEgp", v.jewelryValueEgp)
                put("cashBackEgp", v.cashBackEgp)
                put("totalLiquidationEgp", v.totalLiquidationEgp)
                put("gram24PriceEgp", v.gram24PriceEgp)
                put("gram21PriceEgp", v.gram21PriceEgp)
                put("ounceUsdPrice", v.ounceUsdPrice)
                put("profitVsBaselineEgp", v.profitVsBaselineEgp)
                put("percentChange", v.percentChange)
                put("notes", v.notes)
            })
        }
        root.put("valuations", valuationsArray)

        val transactionsArray = JSONArray()
        for (t in transactions) {
            transactionsArray.put(JSONObject().apply {
                put("timestamp", t.timestamp)
                put("actionType", t.actionType)
                put("title", t.title)
                put("details", t.details)
                put("amountOrWeight", t.amountOrWeight)
            })
        }
        root.put("transactions", transactionsArray)

        return root.toString(2)
    }

    fun decode(jsonString: String): DecodeResult {
        if (jsonString.length > MAX_FILE_CHARS) return fail("الملف كبير جداً (الحد الأقصى ${MAX_FILE_CHARS / 1_000_000} ميجابايت)")
        val root = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            return fail("الملف ليس بصيغة JSON صالحة")
        }

        val errors = ArrayList<String>()
        fun err(message: String) {
            if (errors.size < MAX_ERRORS) errors.add(message)
        }

        val version = root.optInt("version", 1)
        if (version < 1 || version > CURRENT_VERSION) {
            return fail("إصدار النسخة الاحتياطية ($version) غير مدعوم. حدّث التطبيق أولاً")
        }
        val itemsArray = root.optJSONArray("items") ?: return fail("ملف النسخة الاحتياطية غير صالح: لا يحتوي على عناصر المحفظة")
        if (itemsArray.length() == 0) return fail("النسخة الاحتياطية لا تحتوي على أي عناصر؛ لن تتم الاستعادة حتى لا تُحذف محفظتك الحالية")
        if (itemsArray.length() > MAX_ITEMS) return fail("عدد العناصر أكبر من الحد المسموح ($MAX_ITEMS)")

        val settings = root.optJSONObject("settings")?.let { parseSettings(it, ::err) }

        val items = ArrayList<GoldItem>(itemsArray.length())
        for (i in 0 until itemsArray.length()) {
            val o = itemsArray.optJSONObject(i)
            if (o == null) {
                err("عنصر رقم ${i + 1}: ليس كائناً صالحاً")
                continue
            }
            parseItem(o, i + 1, ::err)?.let { items.add(it) }
        }

        val valuations = root.optJSONArray("valuations")?.let { arr ->
            if (arr.length() > MAX_HISTORY_ROWS) {
                err("عدد سجلات التقييم أكبر من الحد المسموح")
                return@let null
            }
            val out = ArrayList<ValuationRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o == null) { err("سجل تقييم رقم ${i + 1}: غير صالح"); continue }
                parseValuation(o, i + 1, ::err)?.let { out.add(it) }
            }
            out
        }

        val transactions = root.optJSONArray("transactions")?.let { arr ->
            if (arr.length() > MAX_HISTORY_ROWS) {
                err("عدد سجلات العمليات أكبر من الحد المسموح")
                return@let null
            }
            val out = ArrayList<TransactionRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o == null) { err("سجل عملية رقم ${i + 1}: غير صالح"); continue }
                out.add(
                    TransactionRecord(
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        actionType = o.optString("actionType", "").take(200),
                        title = o.optString("title", "").take(500),
                        details = o.optString("details", "").take(4000),
                        amountOrWeight = o.optString("amountOrWeight", "").take(200)
                    )
                )
            }
            out
        }

        if (errors.isNotEmpty()) return DecodeResult.Failure(errors)
        return DecodeResult.Success(DecodedBackup(version, settings, items, valuations, transactions))
    }

    private fun fail(message: String) = DecodeResult.Failure(listOf(message))

    private fun finite(o: JSONObject, key: String): Double? {
        if (!o.has(key) || o.isNull(key)) return null
        val d = o.optDouble(key, Double.NaN)
        return if (d.isNaN() || d.isInfinite()) Double.NaN else d
    }

    private fun parseSettings(o: JSONObject, err: (String) -> Unit): SettingsPatch {
        fun money(key: String, min: Double = 0.0): Double? {
            val v = finite(o, key) ?: return null
            if (v.isNaN() || v < min || v > MAX_MONEY) {
                err("الإعدادات: القيمة '$key' غير صالحة")
                return null
            }
            return v
        }
        fun flag(key: String): Boolean? = if (o.has(key) && !o.isNull(key)) o.optBoolean(key) else null
        return SettingsPatch(
            initialBaselineEgp = money("initialBaselineEgp"),
            targetGoalEgp = money("targetGoalEgp"),
            confirmedCashBackEgp = money("confirmedCashBackEgp"),
            cashBackSourceNotes = if (o.has("cashBackSourceNotes") && !o.isNull("cashBackSourceNotes")) o.optString("cashBackSourceNotes").take(1000) else null,
            manual24BuyPrice = money("manual24BuyPrice"),
            manual21BuyPrice = money("manual21BuyPrice"),
            useManualPrices = flag("useManualPrices"),
            autoAddBullionCashBack = flag("autoAddBullionCashBack"),
            priceAlertsEnabled = flag("priceAlertsEnabled"),
            priceAlertThresholdEgp = money("priceAlertThresholdEgp")
        )
    }

    private fun parseItem(o: JSONObject, n: Int, err: (String) -> Unit): GoldItem? {
        var ok = true
        fun bad(msg: String) { err("عنصر رقم $n: $msg"); ok = false }

        val title = o.optString("title", "").trim()
        if (title.isEmpty()) bad("الاسم فارغ")
        if (title.length > 200) bad("الاسم أطول من 200 حرف")

        val type = try {
            GoldType.valueOf(o.optString("type", ""))
        } catch (e: IllegalArgumentException) {
            bad("النوع غير معروف"); GoldType.BULLION
        }

        val weight = finite(o, "weightGrams")
        if (weight == null || weight.isNaN() || weight <= 0.0 || weight > MAX_WEIGHT_GRAMS) bad("الوزن غير صالح")

        val karat = o.optInt("karat", 24)
        if (karat !in 1..24) bad("العيار يجب أن يكون بين 1 و 24")

        val cost = finite(o, "purchaseCostEgp")
        if (cost != null && (cost.isNaN() || cost < 0.0 || cost > MAX_MONEY)) bad("تكلفة الشراء غير صالحة")

        val cashBack = finite(o, "cashBackRatePerGram")
        if (cashBack != null && (cashBack.isNaN() || cashBack < 0.0 || cashBack > 100_000.0)) bad("معدل الكاش باك غير صالح")

        if (!ok) return null
        fun optText(key: String): String? =
            if (o.has(key) && !o.isNull(key)) o.optString(key).take(2000).ifEmpty { null } else null

        return GoldItem(
            id = 0, // fresh IDs: never collide with existing rows
            title = title,
            type = type,
            weightGrams = weight!!,
            karat = karat,
            purchaseCostEgp = cost,
            brand = o.optString("brand", "BTC").take(200),
            condition = o.optString("condition", "مغلفة وسليمة").take(200),
            notes = o.optString("notes", "").take(4000),
            cashBackRatePerGram = cashBack,
            barcode = optText("barcode"),
            imageUri = optText("imageUri"),
            invoiceUri = optText("invoiceUri"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun parseValuation(o: JSONObject, n: Int, err: (String) -> Unit): ValuationRecord? {
        fun num(key: String): Double? {
            val v = finite(o, key)
            if (v == null || v.isNaN() || Math.abs(v) > MAX_MONEY * 100) {
                err("سجل تقييم رقم $n: القيمة '$key' غير صالحة")
                return null
            }
            return v
        }
        val bullion = num("bullionValueEgp")
        val jewelry = num("jewelryValueEgp")
        val cashBack = num("cashBackEgp")
        val total = num("totalLiquidationEgp")
        val g24 = num("gram24PriceEgp")
        val g21 = num("gram21PriceEgp")
        val ounce = num("ounceUsdPrice")
        val profit = num("profitVsBaselineEgp")
        val pct = num("percentChange")
        if (listOf(bullion, jewelry, cashBack, total, g24, g21, ounce, profit, pct).any { it == null }) return null
        return ValuationRecord(
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            bullionValueEgp = bullion!!,
            jewelryValueEgp = jewelry!!,
            cashBackEgp = cashBack!!,
            totalLiquidationEgp = total!!,
            gram24PriceEgp = g24!!,
            gram21PriceEgp = g21!!,
            ounceUsdPrice = ounce!!,
            profitVsBaselineEgp = profit!!,
            percentChange = pct!!,
            notes = o.optString("notes", "").take(2000)
        )
    }
}
