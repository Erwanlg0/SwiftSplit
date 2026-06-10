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
    fun testParseNegativeTime() {
        val parsed = TimeSpan.fromTimeString("-00:00:10.500")
        assertEquals(-10500L, parsed?.totalMilliseconds)
    }

    @Test
    fun testParseDaysTime() {
        val parsed = TimeSpan.fromTimeString("1.02:03:04.500")
        // 1 day = 86400s, 2h = 7200s, 3m = 180s, 4s = 4s. Total = 93784.5s
        assertEquals(93784500L, parsed?.totalMilliseconds)
    }

    @Test
    fun testParseLiveSplitTime() {
        val parsed = TimeSpan.fromTimeString("01:23:45.6789012")
        val expected = 3600000L + 1380000L + 45000L + 678L
        assertEquals(expected, parsed?.totalMilliseconds)
    }

    @Test
    fun testParseShortTimeWithoutColons() {
        val parsed = TimeSpan.fromTimeString("0.17")
        assertEquals(170L, parsed?.totalMilliseconds)

        val parsedSec = TimeSpan.fromTimeString("15")
        assertEquals(15000L, parsedSec?.totalMilliseconds)
    }

    @Test
    fun testParseInvalidTime() {
        assertEquals(null, TimeSpan.fromTimeString("abc"))
        assertEquals(null, TimeSpan.fromTimeString(""))
        assertEquals(null, TimeSpan.fromTimeString("::"))
    }

    @Test
    fun testFormattedWithSign() {
        assertEquals("+5:30.50", TimeSpan(330500).formattedWithSign(showMilliseconds = true, decimalPlaces = 2))
        assertEquals("-5:30.50", TimeSpan(-330500).formattedWithSign(showMilliseconds = true, decimalPlaces = 2))
        assertEquals("0.00", TimeSpan(0).formattedWithSign(showMilliseconds = true, decimalPlaces = 2))
        assertEquals("+1:02:03.04", TimeSpan.fromHours(1.0).plus(TimeSpan.fromMinutes(2.0)).plus(TimeSpan.fromSeconds(3.04)).formattedWithSign())
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
