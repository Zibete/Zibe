package com.zibete.proyecto1.core.utils

import android.content.Context
import com.zibete.proyecto1.R
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val UI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val UI_HOUR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun ageCalculator(birthDateIso: String): Int {
        val birth = LocalDate.parse(birthDateIso) // ISO-8601 (yyyy-MM-dd)
        val today = LocalDate.now()
        return Period.between(birth, today).years
    }

    fun formatConversationTimestamp(ms: Long, context: Context): String {
        if (ms <= 0L) return ""

        return when {
            isToday(ms) -> formatHour(ms)
            isYesterday(ms) -> context.getString(R.string.yesterday)
            else -> formatUiDate(ms)
        }
    }

    fun formatDateChatTimestamp(ms: Long, context: Context): String {
        if (ms <= 0L) return ""

        return when {
            isToday(ms) -> context.getString(R.string.today)
            isYesterday(ms) -> context.getString(R.string.yesterday)
            else -> formatUiDate(ms)
        }
    }

    fun formatAudioDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun zoneId(): ZoneId = ZoneId.systemDefault()

    fun zonedDateTime(millis: Long): ZonedDateTime =
        Instant.ofEpochMilli(millis).atZone(zoneId())

    fun isToday(ms: Long): Boolean =
        zonedDateTime(ms).toLocalDate() == LocalDate.now(zoneId())

    fun isYesterday(ms: Long): Boolean =
        zonedDateTime(ms).toLocalDate() == LocalDate.now(zoneId()).minusDays(1)

    fun formatHour(ms: Long): String =
        UI_HOUR_FORMATTER.format(zonedDateTime(ms))

    fun formatUiDate(ms: Long): String =
        UI_DATE_FORMATTER.format(zonedDateTime(ms))

    fun isoToUiDate(iso: String): String =
        LocalDate.parse(iso).format(UI_DATE_FORMATTER)

    fun formatLastSeen(ms: Long, context: Context): String {
        if (ms <= 0L) return ""

        val date = formatUiDate(ms)
        val time = formatHour(ms)

        return context.getString(R.string.last_seen_format, date, time)
    }

    fun isoToMillis(isoDate: String): Long? =
        runCatching {
            LocalDate.parse(isoDate) // ISO yyyy-MM-dd
                .atStartOfDay(zoneId())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()

    fun millisToIso(ms: Long): String {
        return Instant.ofEpochMilli(ms)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString() // yyyy-MM-dd
    }

    fun now(): Long = System.currentTimeMillis()

    fun isAdult(birthStr: String): Boolean = try {
        ageCalculator(birthStr) >= 18
    } catch (_: Exception) {
        false
    }

}