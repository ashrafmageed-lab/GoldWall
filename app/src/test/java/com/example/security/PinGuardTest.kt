package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinGuardTest {

    private lateinit var guard: PinGuard

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        guard = PinGuard(context)
        guard.reset()
    }

    @Test
    fun firstFailures_areFree_thenLockoutStarts() {
        val now = 1_000_000L
        repeat(PinGuard.FREE_ATTEMPTS - 1) {
            assertEquals(0L, guard.recordFailure(now))
        }
        val lock = guard.recordFailure(now)
        assertEquals(PinGuard.BASE_LOCK_MS, lock)
        assertTrue(guard.remainingLockMillis(now) > 0)
        assertEquals(0L, guard.remainingLockMillis(now + lock + 1))
    }

    @Test
    fun lockoutDoubles_andIsCapped() {
        var now = 0L
        repeat(PinGuard.FREE_ATTEMPTS - 1) { guard.recordFailure(now) }
        val first = guard.recordFailure(now)
        val second = guard.recordFailure(now)
        assertEquals(first * 2, second)
        repeat(30) { guard.recordFailure(now) }
        assertEquals(PinGuard.MAX_LOCK_MS, guard.recordFailure(now))
    }

    @Test
    fun success_resetsCounters() {
        repeat(PinGuard.FREE_ATTEMPTS) { guard.recordFailure(0L) }
        guard.reset()
        assertEquals(0, guard.failures())
        assertEquals(0L, guard.remainingLockMillis(0L))
    }
}
