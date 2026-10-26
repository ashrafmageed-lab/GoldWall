package com.example.data.local

import android.content.Context
import com.example.data.model.GoldPriceState
import com.example.data.model.PriceStatus

/** Persists the last successfully fetched prices so cold starts and offline use show real data. */
class PriceCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("price_cache", Context.MODE_PRIVATE)

    fun save(state: GoldPriceState) {
        if (state.sourceStatus != PriceStatus.LIVE && state.sourceStatus != PriceStatus.RECENT) return
        prefs.edit()
            .putString("g24b", state.gram24BuyEgp.toString())
            .putString("g24s", state.gram24SellEgp.toString())
            .putString("g21b", state.gram21BuyEgp.toString())
            .putString("g21s", state.gram21SellEgp.toString())
            .putString("g18b", state.gram18BuyEgp.toString())
            .putString("g18s", state.gram18SellEgp.toString())
            .putString("oz", state.ounceUsd.toString())
            .putLong("ts", state.lastUpdated)
            .putString("src", state.sourceName)
            .apply()
    }

    /** Last real prices, marked RECENT when fresh and STALE (with a warning) when older than [staleAfterMs]. */
    fun load(now: Long = System.currentTimeMillis(), staleAfterMs: Long = STALE_AFTER_MS): GoldPriceState? {
        val ts = prefs.getLong("ts", 0L)
        if (ts <= 0L) return null
        fun d(key: String): Double? = prefs.getString(key, null)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        val g24b = d("g24b") ?: return null
        val g21b = d("g21b") ?: return null
        val g18b = d("g18b") ?: return null
        val stale = now - ts > staleAfterMs
        return GoldPriceState(
            gram24BuyEgp = g24b,
            gram24SellEgp = d("g24s") ?: g24b,
            gram21BuyEgp = g21b,
            gram21SellEgp = d("g21s") ?: g21b,
            gram18BuyEgp = g18b,
            gram18SellEgp = d("g18s") ?: g18b,
            ounceUsd = d("oz") ?: GoldPriceState().ounceUsd,
            lastUpdated = ts,
            sourceName = prefs.getString("src", null) ?: "آخر أسعار محفوظة",
            sourceStatus = if (stale) PriceStatus.STALE else PriceStatus.RECENT,
            warningMessage = if (stale) "هذه آخر أسعار محفوظة وقد تكون قديمة. سيتم التحديث عند توفر الاتصال." else null
        )
    }

    companion object {
        const val STALE_AFTER_MS = 30L * 60L * 1000L
    }
}
