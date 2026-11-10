package com.johnpaulpatigas.weightcast.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility helpers for handling date and time conversions and formatting.
 */
object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    fun currentEpochMillis(): Long = System.currentTimeMillis()

    fun toLocalDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
    }

    fun toEpochMillis(localDateTime: LocalDateTime, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDateTime(epochMillis, zoneId).format(dateFormatter)
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDateTime(epochMillis, zoneId).format(timeFormatter)
    }

    fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDateTime(epochMillis, zoneId).format(dateTimeFormatter)
    }

    fun formatMonthYear(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDateTime(epochMillis, zoneId).format(monthYearFormatter)
    }

    fun isFuture(epochMillis: Long, currentMillis: Long = currentEpochMillis()): Boolean {
        return epochMillis > currentMillis
    }

    fun startOfDayEpochMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val localDate = toLocalDateTime(epochMillis, zoneId).toLocalDate()
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
