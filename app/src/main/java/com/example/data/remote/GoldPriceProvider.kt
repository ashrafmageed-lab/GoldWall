package com.example.data.remote

import com.example.data.model.GoldPriceState
import com.example.data.model.PriceStatus
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import java.io.IOException

/**
 * Single entry point for market prices. The former goldapi.io path was removed: it used a
 * placeholder API key and treated a theoretical spot value as a jeweler buy price.
 */
open class GoldPriceProvider(
    client: OkHttpClient = SharedHttp.client
) {
    private val fetcher = GoldPriceFetcher(client)
    private var lastKnownOunce: Double = GoldPriceState().ounceUsd

    /** Success only when fresh, plausible prices were obtained (LIVE or RECENT). */
    open suspend fun fetchGoldPrices(): Result<GoldPriceState> {
        return try {
            val state = fetcher.fetchLivePrices(GoldPriceState(ounceUsd = lastKnownOunce))
            if (state.sourceStatus == PriceStatus.LIVE || state.sourceStatus == PriceStatus.RECENT) {
                lastKnownOunce = state.ounceUsd
                Result.success(state)
            } else {
                Result.failure(IOException(state.warningMessage ?: "تعذر تحديث أسعار الذهب. تحقق من اتصال الإنترنت."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IOException("تعذر تحديث أسعار الذهب: ${e.message ?: "خطأ غير معروف"}"))
        }
    }

    open suspend fun fetchHistoricalPoints(timeframeCode: String): List<HistoricalPricePoint> =
        fetcher.fetchHistoricalGoldPoints(timeframeCode)
}
