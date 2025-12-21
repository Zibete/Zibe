package com.zibete.proyecto1.utils

import android.text.Editable
import android.text.TextWatcher
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    private val locale = Locale.getDefault()
    private val zone = ZoneId.systemDefault()

    // Formateadores reutilizables (Evitan recolector de basura constante)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    private val preciseFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss:SSS", locale)

    // --- LÓGICA DE EDAD ---

    @JvmStatic
    fun calcAge(birthDay: String?): Int {
        return runCatching {
            val fechaNac = LocalDate.parse(birthDay, dayFormatter)
            Period.between(fechaNac, LocalDate.now()).years
        }.getOrDefault(0)
    }

    // --- FECHAS COMO STRING (Para UI o Logs) ---

    fun today(): String = LocalDate.now().format(dayFormatter)

    fun time(): String = LocalTime.now().format(timeFormatter)

    fun dateTime(): String = LocalDateTime.now().format(fullFormatter)

    fun yesterday(): String = LocalDate.now().minusDays(1).format(dayFormatter)

    fun now(): String = LocalDateTime.now().format(preciseFormatter)

    // --- CONVERSORES MILIS <-> FECHA ---

    fun millisToBirthDate(millis: Long): String {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().format(dayFormatter)
    }

    fun birthDateToMillis(date: String): Long? {
        return runCatching {
            LocalDate.parse(date, dayFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }

    // --- FORMATOS ESPECIALES PARA CHAT ---

    fun formatLastSeen(lastSeenMs: Long): String {
        if (lastSeenMs <= 0L) return ""

        val ldt = Instant.ofEpochMilli(lastSeenMs).atZone(zone).toLocalDateTime()
        val date = ldt.toLocalDate()
        val today = LocalDate.now()

        val datePart = when (date) {
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            else -> date.format(dayFormatter)
        }

        return "$datePart a las ${ldt.format(timeFormatter)}"
    }

    /**
     * Ideal para la lista de chats: muestra solo hora si es hoy,
     * "Ayer" si fue ayer, o la fecha si es más antiguo.
     */
    fun formatChatTimestamp(ms: Long): String {
        val msgDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val today = LocalDate.now()

        return when (msgDate) {
            today -> Instant.ofEpochMilli(ms).atZone(zone).format(timeFormatter)
            today.minusDays(1) -> "Ayer"
            else -> msgDate.format(dayFormatter)
        }
    }

}