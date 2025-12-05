package com.zibete.proyecto1.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    private val locale = Locale.getDefault()


    @JvmStatic
    fun calcAge(birthDay: String?): Int {
        try {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fechaNac = LocalDate.parse(birthDay, fmt)
            val hoy = LocalDate.now()
            return Period.between(fechaNac, hoy).years
        } catch (e: Exception) {
            return -1 // error de formato
        }
    }

    fun today(): String =
        SimpleDateFormat("dd/MM/yyyy", locale).format(Date())

    fun time(): String =
        SimpleDateFormat("HH:mm", locale).format(Date())

    fun dateTime(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", locale).format(Date())

    fun yesterday(): String =
        SimpleDateFormat("dd/MM/yyyy", locale).format(
            Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time
        )

    fun now(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", locale).format(Date())



}
