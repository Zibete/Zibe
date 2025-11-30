package com.zibete.proyecto1.utils

import android.app.Application
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import com.zibete.proyecto1.data.UserPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {
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

    fun today(): String {
        val c = Calendar.getInstance()
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
    }
    fun yesterday(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, -1)
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
    }

    fun now(): String {
        val c = Calendar.getInstance()
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault()).format(c.time)
    }

}
