package com.zibete.proyecto1.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.zibete.proyecto1.core.constants.AUTH_ERR_INVALID_CREDENTIALS
import com.zibete.proyecto1.core.constants.AUTH_ERR_REAUTHENTICATION_REQUIRED
import com.zibete.proyecto1.core.constants.AUTH_ERR_USER_NOT_FOUND
import com.zibete.proyecto1.core.constants.ERR_ZIBE
import com.zibete.proyecto1.core.constants.ERR_NETWORK_CONNECTION
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_EMAIL_IN_USE
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_GENERIC_PREFIX
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_INVALID_EMAIL
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_INVALID_PASSWORD
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_UNEXPECTED_PREFIX

// Usa tus constantes importadas para evitar hardcoding
fun getAuthErrorMessage(e: Throwable?): String {
    if (e == null) return ERR_ZIBE

    return when (e) {
        // 1. Excepciones específicas de Firebase (Clases)
        is FirebaseAuthInvalidCredentialsException -> AUTH_ERR_INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException -> AUTH_ERR_USER_NOT_FOUND
        is FirebaseAuthUserCollisionException -> SIGNUP_ERR_EMAIL_IN_USE
        is FirebaseNetworkException -> ERR_NETWORK_CONNECTION
        is FirebaseAuthRecentLoginRequiredException -> AUTH_ERR_REAUTHENTICATION_REQUIRED

        // 2. Excepción genérica de Firebase con ErrorCodes (Strings internos)
        is FirebaseAuthException -> {
            when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> SIGNUP_ERR_EMAIL_IN_USE
                "ERROR_WEAK_PASSWORD"        -> SIGNUP_ERR_INVALID_PASSWORD
                "ERROR_INVALID_EMAIL"        -> SIGNUP_ERR_INVALID_EMAIL
                else -> SIGNUP_ERR_GENERIC_PREFIX + " (${e.errorCode})"
            }
        }

        // 3. Fallo genérico (Cualquier otra Exception)
        else -> {
            val technicalInfo = null ?: e.javaClass.simpleName
            "$SIGNUP_ERR_UNEXPECTED_PREFIX ($technicalInfo)"
        }
    }
}


