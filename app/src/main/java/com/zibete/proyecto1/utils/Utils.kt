package com.zibete.proyecto1.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import android.content.Context
import androidx.fragment.app.Fragment
import com.zibete.proyecto1.data.UserPreferencesRepository

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


    // Esto permite usar "context.repo" en cualquier lado
    val Context.repo: UserPreferencesRepository
        get() = UserPreferencesRepository.getInstance(this)

    // Esto permite usar "repo" directo en cualquier Fragment
    val Fragment.repo: UserPreferencesRepository
        get() = UserPreferencesRepository.getInstance(requireContext())
}
