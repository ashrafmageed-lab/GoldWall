package com.example.support

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/** Offline HTTP client for tests: answers by host, never touches the real network. */
object FakeHttp {

    fun isaghaHtml(k24: Double? = 7000.0, k21: Double = 6125.0, k18: Double = 5250.0) = PriceHtml.isaghaHtml(k24, k21, k18)

    fun goldPriceTodayHtml(k21: Double, k24: Double = k21 * 24.0 / 21.0) = PriceHtml.goldPriceTodayHtml(k21, k24)

    /** [routes] maps a host fragment to a response body; hosts not listed fail like a dead network. */
    fun client(routes: Map<String, String>): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request()
            val host = request.url.host
            val key = routes.keys.firstOrNull { host.contains(it) }
                ?: throw IOException("offline: $host")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(routes.getValue(key).toResponseBody("text/html".toMediaType()))
                .build()
        })
        .build()
}
