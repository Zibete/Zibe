package com.zibete.proyecto1.core.validation

import android.util.Patterns

object CredentialValidators {

    const val MIN_PASSWORD_LEN = 6

    fun isValidEmail(email: String): Boolean {
        val e = email.trim()
        return e.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(e).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LEN
    }
}
