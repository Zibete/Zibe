package com.zibete.proyecto1.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    private var locale = Locale.getDefault()



    @JvmStatic
    fun calcAge(birthDay: String?): Int {
        try {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fechaNac = LocalDate.parse(birthDay, fmt)
            val hoy = LocalDate.now()
            return Period.between(fechaNac, hoy).years
        } catch (e: Exception) {
            return 0
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


    private val BIRTHDAY_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

    fun millisToBirthDate(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val ld = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        return ld.format(BIRTHDAY_FORMATTER)
    }

    fun birthDateToMillis(date: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        return runCatching {
            val ld = LocalDate.parse(date, BIRTHDAY_FORMATTER)
            ld.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }.getOrNull()
    }


    class SimpleWatcher(
        private val onChanged: (String) -> Unit
    ) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun afterTextChanged(s: Editable?) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString().orEmpty())
        }
    }

    object AppChecks {

        fun hasInternetConnection(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun formatLastSeen(lastSeenMs: Long): String {
        if (lastSeenMs <= 0L) return ""

        val d = Date(lastSeenMs)
        val date = SimpleDateFormat("dd/MM/yyyy", locale).format(d)
        val time = SimpleDateFormat("HH:mm", locale).format(d)

        return "$date a las $time"
    }



}
