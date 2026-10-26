package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One shared OkHttp client (connection pool + timeouts) and a small retry helper. */
object SharedHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Blocking GET (call from Dispatchers.IO). Retries once with backoff on I/O errors, 429 and 5xx.
     * Returns the body text, or null when every attempt failed.
     */
    fun getBody(client: OkHttpClient, request: Request, attempts: Int = 2): String? {
        var attempt = 0
        while (attempt < attempts) {
            attempt++
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return response.body?.string()
                    val retryable = response.code == 429 || response.code >= 500
                    if (!retryable) return null
                }
            } catch (e: IOException) {
                // fall through to retry
            }
            if (attempt < attempts) {
                try {
                    Thread.sleep(400L * attempt)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
        }
        return null
    }
}
