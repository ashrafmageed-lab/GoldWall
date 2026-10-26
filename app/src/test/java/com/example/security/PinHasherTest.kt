package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hash_isSaltedAndVerifiable() {
        val a = PinHasher.hash("2580")
        val b = PinHasher.hash("2580")
        assertNotEquals("same PIN must hash differently (random salt)", a, b)
        assertTrue(PinHasher.isHashed(a))
        assertTrue(PinHasher.verify("2580", a))
        assertTrue(PinHasher.verify("2580", b))
        assertFalse(PinHasher.verify("2581", a))
    }

    @Test
    fun hash_neverContainsThePlainPin() {
        assertFalse(PinHasher.hash("2580").contains("2580"))
    }

    @Test
    fun emptyAndLegacyDefault_areTreatedAsNoPin() {
        assertFalse(PinHasher.hasPin(""))
        assertFalse(PinHasher.hasPin("1234"))
        assertFalse(PinHasher.verify("1234", ""))
        assertFalse(PinHasher.verify("1234", "1234"))
    }

    @Test
    fun legacyPlainPin_stillVerifiesAndIsFlaggedForUpgrade() {
        assertTrue(PinHasher.hasPin("5678"))
        assertTrue(PinHasher.isLegacyPlain("5678"))
        assertTrue(PinHasher.verify("5678", "5678"))
        assertFalse(PinHasher.verify("1234", "5678"))
        assertFalse(PinHasher.isLegacyPlain(PinHasher.hash("5678")))
    }

    @Test
    fun malformedStoredValues_failClosed() {
        assertFalse(PinHasher.verify("2580", "pbkdf2:abc:zz:zz"))
        assertFalse(PinHasher.verify("2580", "pbkdf2:100000:00"))
        assertFalse(PinHasher.verify("2580", "pbkdf2:0:00:00"))
    }

    @Test
    fun formatAndWeakPinRules() {
        assertTrue(PinHasher.isValidFormat("0831"))
        assertFalse(PinHasher.isValidFormat("083"))
        assertFalse(PinHasher.isValidFormat("08a1"))
        assertTrue(PinHasher.isWeak("1234"))
        assertTrue(PinHasher.isWeak("0000"))
        assertFalse(PinHasher.isWeak("0831"))
        assertEquals(4, "0831".length)
    }
}
