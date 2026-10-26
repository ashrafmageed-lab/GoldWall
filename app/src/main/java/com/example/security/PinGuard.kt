package com.example.security

import android.content.Context

/**
 * Persistent brute-force protection for the PIN.
 * The first [FREE_ATTEMPTS] - 1 failures are free; from the [FREE_ATTEMPTS]th failure on, the
 * lock-out doubles each time (30s, 60s, 120s ... capped at 1 hour). Survives app restarts.
 */
class PinGuard(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun failures(): Int = prefs.getInt(KEY_FAILS, 0)

    fun attemptsLeft(): Int = (FREE_ATTEMPTS - failures()).coerceAtLeast(0)

    fun remainingLockMillis(now: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        return (until - now).coerceIn(0L, MAX_LOCK_MS)
    }

    /** Records a failed attempt; returns the lock-out duration in millis (0 when none). */
    fun recordFailure(now: Long = System.currentTimeMillis()): Long {
        val fails = failures() + 1
        var delayMs = 0L
        if (fails >= FREE_ATTEMPTS) {
            val step = (fails - FREE_ATTEMPTS).coerceAtMost(10)
            delayMs = (BASE_LOCK_MS shl step).coerceAtMost(MAX_LOCK_MS)
        }
        prefs.edit()
            .putInt(KEY_FAILS, fails)
            .putLong(KEY_UNTIL, if (delayMs > 0) now + delayMs else 0L)
            .apply()
        return delayMs
    }

    fun reset() {
        prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_UNTIL, 0L).apply()
    }

    companion object {
        const val FREE_ATTEMPTS = 5
        const val BASE_LOCK_MS = 30_000L
        const val MAX_LOCK_MS = 3_600_000L
        private const val PREFS = "vault_guard"
        private const val KEY_FAILS = "fails"
        private const val KEY_UNTIL = "locked_until"
    }
}
