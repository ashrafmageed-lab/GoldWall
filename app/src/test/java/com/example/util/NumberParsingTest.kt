package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParsingTest {
    @Test fun plainAndGrouped() {
        assertEquals(7131.5, NumberParsing.parseDouble("7131.5")!!, 0.0)
        assertEquals(1234567.25, NumberParsing.parseDouble("1,234,567.25")!!, 0.0)
        assertEquals(1234567.0, NumberParsing.parseDouble("1,234,567")!!, 0.0)
        assertEquals(1234.5, NumberParsing.parseDouble("1234,5")!!, 0.0)
        assertEquals(10.0, NumberParsing.parseDouble(" 10 ")!!, 0.0)
        assertEquals(-5.5, NumberParsing.parseDouble("-5.5")!!, 0.0)
    }

    @Test fun arabicIndicDigitsAndSeparators() {
        assertEquals(7131.5, NumberParsing.parseDouble("٧١٣١٫٥")!!, 0.0)
        assertEquals(6240.0, NumberParsing.parseDouble("٦٢٤٠")!!, 0.0)
        assertEquals(1500.25, NumberParsing.parseDouble("۱۵۰۰٫۲۵")!!, 0.0)
        assertEquals(1234567.0, NumberParsing.parseDouble("١٬٢٣٤٬٥٦٧")!!, 0.0)
    }

    @Test fun rejectsGarbageAndNonFinite() {
        assertNull(NumberParsing.parseDouble(null))
        assertNull(NumberParsing.parseDouble(""))
        assertNull(NumberParsing.parseDouble("abc"))
        assertNull(NumberParsing.parseDouble("NaN"))
        assertNull(NumberParsing.parseDouble("Infinity"))
        assertNull(NumberParsing.parseDouble("1e5"))
        assertNull(NumberParsing.parseDouble("12d"))
        assertNull(NumberParsing.parseDouble("1.2.3"))
    }
}
