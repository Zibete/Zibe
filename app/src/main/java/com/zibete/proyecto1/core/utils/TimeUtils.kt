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
import java.time.format.DateTimeParseException
import java.util.Locale

object TimeUtils {

    private val UI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val UI_HOUR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val ISO_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    /**
     * Parses an ISO-8601 string. 
     * Since the input can be a full timestamp (yyyy-MM-ddTHH:mm:ssZ), 
     * we use ISO_DATE_TIME to handle it and extract the LocalDate.
     * If it's just a date (yyyy-MM-dd), we attempt to parse it as LocalDate directly.
     */
    private fun parseIsoDate(iso: String): LocalDate {
        return try {
            LocalDate.parse(iso)
        } catch (e: DateTimeParseException) {
            LocalDate.parse(iso, ISO_DATE_TIME_FORMATTER)
        }
    }

    fun ageCalculator(birthDateIso: String): Int {
        val birth = try {
            parseIsoDate(birthDateIso)
        } catch (e: Exception) {
            return 0
        }
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

    fun formatHeaderDate(ms: Long): String {
        if (ms <= 0L) return ""
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
        return formatter.format(zonedDateTime(ms))
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

    fun isoToUiDate(iso: String): String = try {
        parseIsoDate(iso).format(UI_DATE_FORMATTER)
    } catch (e: Exception) {
        ""
    }

    fun formatLastSeen(ms: Long, context: Context): String {
        if (ms <= 0L) return ""

        val date = formatUiDate(ms)
        val time = formatHour(ms)

        return context.getString(R.string.last_seen_format, date, time)
    }

    fun isoToMillis(isoDate: String): Long? =
        runCatching {
            parseIsoDate(isoDate)
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
