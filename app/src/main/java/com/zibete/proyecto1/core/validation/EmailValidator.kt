package com.zibete.proyecto1.core.validation

fun interface EmailValidator {
    fun isValid(email: String): Boolean
}
