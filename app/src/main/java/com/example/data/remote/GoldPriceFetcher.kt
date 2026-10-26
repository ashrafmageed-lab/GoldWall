package com.example.data.remote

import android.util.Log
import com.example.data.model.GoldPriceState
import com.example.data.model.PriceStatus
import com.example.util.NumberParsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class HistoricalPricePoint(
    val timestamp: Long,
    val priceUsd: Double
)

class GoldPriceFetcher(
    private val client: OkHttpClient = SharedHttp.client
) {

    /**
     * Local jeweler prices come from two independent Egyptian sources and are only marked LIVE when
     * both agree. A single source is marked RECENT. Contradicting sources are rejected (STALE with
     * the previous prices kept). Nothing is ever invented: no default prices, no default ounce.
     */
    suspend fun fetchLivePrices(current: GoldPriceState): GoldPriceState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val (spotOunce, isagha, goldToday) = coroutineScope {
            val ounceJob = async { runCatching { fetchAccurateGlobalOunce() }.getOrNull() }
            val aJob = async { runCatching { fetchFromISagha() }.getOrNull() }
            val bJob = async { runCatching { fetchFromGoldPriceToday() }.getOrNull() }
            Triple(ounceJob.await(), aJob.await(), bJob.await())
        }

        val a = isagha?.takeIf { PriceParsing.isPlausible(it) }
        val b = goldToday?.takeIf { PriceParsing.isPlausible(it) }
        val ounce = spotOunce ?: a?.ounceUsd ?: current.ounceUsd

        fun build(rates: MarketRates, name: String, status: PriceStatus) = GoldPriceState(
            gram24BuyEgp = rates.k24Buy,
            gram24SellEgp = rates.k24Sell,
            gram21BuyEgp = rates.k21Buy,
            gram21SellEgp = rates.k21Sell,
            gram18BuyEgp = rates.k18Buy,
            gram18SellEgp = rates.k18Sell,
            ounceUsd = ounce,
            lastUpdated = now,
            sourceName = name,
            ounceSourceName = if (spotOunce != null) "بورصة الذهب العالمية (XAU/USD)" else current.ounceSourceName,
            ounceSourceUrl = current.ounceSourceUrl,
            sourceStatus = status,
            isManual = false,
            warningMessage = null
        )

        when {
            a != null && b != null -> {
                if (PriceParsing.sourcesAgree(a, b)) {
                    build(a, "آي صاغة + سعر الذهب اليوم (متطابقان)", PriceStatus.LIVE)
                } else {
                    Log.w("GoldPriceFetcher", "Sources disagree: ${a.k21Buy} vs ${b.k21Buy}")
                    current.copy(
                        sourceStatus = PriceStatus.STALE,
                        warningMessage = "المصدران يعرضان أسعاراً متناقضة (${a.k21Buy} مقابل ${b.k21Buy} ج.م للعيار 21)، لذلك لم تُحدَّث الأسعار. تحقق من السعر أو أدخله يدوياً."
                    )
                }
            }
            a != null -> build(a, "آي صاغة (مصدر واحد)", PriceStatus.RECENT)
            b != null -> build(b, "سعر الذهب اليوم (مصدر واحد)", PriceStatus.RECENT)
            else -> current.copy(
                ounceUsd = ounce,
                sourceStatus = PriceStatus.STALE,
                warningMessage = "تعذر تحديث الأسعار عبر الإنترنت. الأسعار المعروضة هي آخر أسعار مسجلة."
            )
        }
    }

    /** Spot XAU/USD: Gold-API first, Coinbase PAXG as a secondary reference. */
    fun fetchAccurateGlobalOunce(): Double? {
        try {
            val body = SharedHttp.getBody(client, jsonRequest("https://api.gold-api.com/price/XAU"))
            if (body != null) {
                val price = JSONObject(body).optDouble("price", Double.NaN)
                if (!price.isNaN() && price in 1000.0..100_000.0) return price
            }
        } catch (e: Exception) {
            Log.w("GoldPriceFetcher", "Gold-API fetch failed: ${e.message}")
        }
        try {
            val body = SharedHttp.getBody(client, jsonRequest("https://api.coinbase.com/v2/prices/PAXG-USD/spot"))
            if (body != null) {
                val amount = JSONObject(body).optJSONObject("data")?.optString("amount")
                val price = NumberParsing.parseDouble(amount)
                if (price != null && price in 1000.0..100_000.0) return price
            }
        } catch (e: Exception) {
            Log.w("GoldPriceFetcher", "Coinbase PAXG fetch failed: ${e.message}")
        }
        return null
    }

    fun fetchGlobalOunceFromInvestingCom(): Double? = fetchAccurateGlobalOunce()

    /** Real hourly/daily candles (PAXG proxy) for the market-price chart. */
    suspend fun fetchHistoricalGoldPoints(timeframeCode: String): List<HistoricalPricePoint> = withContext(Dispatchers.IO) {
        val (interval, limit) = when (timeframeCode) {
            "1D" -> Pair("1h", 24)
            "1W" -> Pair("4h", 42)
            "1M" -> Pair("1d", 30)
            "3M" -> Pair("1d", 90)
            else -> Pair("1d", 30)
        }
        try {
            val body = SharedHttp.getBody(
                client,
                jsonRequest("https://api.binance.com/api/v3/klines?symbol=PAXGUSDT&interval=$interval&limit=$limit")
            )
            if (body != null) {
                val arr = JSONArray(body)
                val points = ArrayList<HistoricalPricePoint>(arr.length())
                for (i in 0 until arr.length()) {
                    val candle = arr.getJSONArray(i)
                    val close = NumberParsing.parseDouble(candle.getString(4)) ?: continue
                    points.add(HistoricalPricePoint(candle.getLong(0), close))
                }
                if (points.isNotEmpty()) return@withContext points
            }
        } catch (e: Exception) {
            Log.w("GoldPriceFetcher", "Historical candles fetch failed: ${e.message}")
        }
        emptyList()
    }

    private fun fetchFromISagha(): MarketRates? {
        val request = Request.Builder()
            .url("https://isagha.com/prices")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ar,en;q=0.9")
            .build()
        val html = SharedHttp.getBody(client, request) ?: return null
        return PriceParsing.parseISagha(html)
    }

    private fun fetchFromGoldPriceToday(): MarketRates? {
        val request = Request.Builder()
            .url("https://www.gold-price-today.com/egypt/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ar,en;q=0.9")
            .build()
        val html = SharedHttp.getBody(client, request) ?: return null
        return PriceParsing.parseGoldPriceToday(html)
    }

    private fun jsonRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "GoldVaultApp/1.0")
        .header("Accept", "application/json")
        .build()
}
