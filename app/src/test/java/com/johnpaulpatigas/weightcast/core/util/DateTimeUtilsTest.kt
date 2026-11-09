package com.johnpaulpatigas.weightcast.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DateTimeUtilsTest {

    private val testZone = ZoneId.of("UTC")

    @Test
    fun `toLocalDateTime and toEpochMillis are reciprocal`() {
        val nowMillis = 1700000000000L
        val localDateTime = DateTimeUtils.toLocalDateTime(nowMillis, testZone)
        val backToMillis = DateTimeUtils.toEpochMillis(localDateTime, testZone)
        assertEquals(nowMillis, backToMillis)
    }

    @Test
    fun `isFuture correctly identifies future timestamps`() {
        val now = 1000000L
        assertTrue(DateTimeUtils.isFuture(now + 1000L, currentMillis = now))
        assertFalse(DateTimeUtils.isFuture(now - 1000L, currentMillis = now))
        assertFalse(DateTimeUtils.isFuture(now, currentMillis = now))
    }

    @Test
    fun `startOfDayEpochMillis returns midnight timestamp`() {
        // 2026-11-09T14:30:00Z -> Start of day is 2026-11-09T00:00:00Z
        val dt = LocalDateTime.of(2026, 11, 9, 14, 30, 0)
        val millis = DateTimeUtils.toEpochMillis(dt, testZone)

        val startOfDayMillis = DateTimeUtils.startOfDayEpochMillis(millis, testZone)
        val startOfDayDt = DateTimeUtils.toLocalDateTime(startOfDayMillis, testZone)

        assertEquals(2026, startOfDayDt.year)
        assertEquals(11, startOfDayDt.monthValue)
        assertEquals(9, startOfDayDt.dayOfMonth)
        assertEquals(0, startOfDayDt.hour)
        assertEquals(0, startOfDayDt.minute)
        assertEquals(0, startOfDayDt.second)
    }
}
