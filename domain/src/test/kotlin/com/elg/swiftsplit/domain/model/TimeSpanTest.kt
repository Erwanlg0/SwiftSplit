package com.elg.swiftsplit.domain.model

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
        assertEquals("5:30.50", ts.formatted(TimeFormatOptions(TimeFormatPattern.OPT_HH_OPT_MM_SS_SS)))
    }

    @Test
    fun testFormattedWithLeadingZeros() {
        val ts = TimeSpan(330500)
        assertEquals("00:05:30.50", ts.formatted(TimeFormatOptions(TimeFormatPattern.HH_MM_SS_SS)))
    }

    @Test
    fun testFormattedWithOneDecimalPlace() {
        val ts = TimeSpan(330500)
        assertEquals("5:30.5", ts.formatted(TimeFormatOptions(TimeFormatPattern.OPT_HH_OPT_MM_SS_S)))
    }

    @Test
    fun testFormattedWithTwoDecimalPlaces() {
        val ts = TimeSpan(330500)
        assertEquals("5:30.50", ts.formatted(TimeFormatOptions(TimeFormatPattern.OPT_HH_OPT_MM_SS_SS)))
    }

    @Test
    fun testFormattedWithoutFraction() {
        val ts = TimeSpan(330500)
        assertEquals("5:30", ts.formatted(TimeFormatOptions(TimeFormatPattern.OPT_HH_MM_SS)))
    }
}
