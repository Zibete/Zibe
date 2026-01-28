package com.zibete.proyecto1.core.validation

import android.util.Patterns

class AndroidEmailValidator : EmailValidator {
    override fun isValid(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
