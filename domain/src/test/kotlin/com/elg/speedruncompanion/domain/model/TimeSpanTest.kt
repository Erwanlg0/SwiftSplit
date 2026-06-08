package com.elg.speedruncompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeSpanTest {

    @Test
    fun testParseSimpleTime() {
        val parsed = TimeSpan.fromTimeString("00:05:30.500")
        assertEquals(330500L, parsed?.totalMilliseconds)
    }

    @Test
    fun testParseLiveSplitTime() {
        val parsed = TimeSpan.fromTimeString("01:23:45.6789012")
        val expected = 3600000L + 1380000L + 45000L + 678L
        assertEquals(expected, parsed?.totalMilliseconds)
    }

    @Test
    fun testFormattedDefault() {
        val ts = TimeSpan(330500)
        assertEquals("5:30.500", ts.formatted())
    }

    @Test
    fun testFormattedWithLeadingZeros() {
        val ts = TimeSpan(330500)
        assertEquals("00:05:30.500", ts.formatted(TimeFormatOptions(showLeadingZeros = true)))
    }

    @Test
    fun testFormattedWithOneDecimalPlace() {
        val ts = TimeSpan(330500)
        assertEquals("5:30.5", ts.formatted(TimeFormatOptions(decimalPlaces = 1)))
    }

    @Test
    fun testFormattedWithTwoDecimalPlaces() {
        val ts = TimeSpan(330500)
        assertEquals("5:30.50", ts.formatted(TimeFormatOptions(decimalPlaces = 2)))
    }

    @Test
    fun testFormattedWithoutFraction() {
        val ts = TimeSpan(330500)
        assertEquals("5:30", ts.formatted(TimeFormatOptions(showFraction = false)))
    }
}
