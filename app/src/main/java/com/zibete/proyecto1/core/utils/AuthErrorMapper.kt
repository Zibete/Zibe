package com.zibete.proyecto1.core.utils

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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.zibete.proyecto1.core.constants.ERR_ACCOUNT_NOT_FOUND

class AccountNotFoundException : IllegalStateException()

fun getAuthErrorMessage(e: Throwable?): String {
    if (e == null) return ERR_ZIBE

    return when (e) {

        // Credential Manager / Google
        is GetCredentialCancellationException -> "Inicio de sesión cancelado"
        is NoCredentialException -> "No hay cuentas disponibles para iniciar sesión con Google"
        is GoogleIdTokenParsingException -> "No se pudo validar el token de Google"
        is GetCredentialException -> "No se pudo iniciar sesión con Google"

        // Firebase
        is FirebaseAuthInvalidCredentialsException -> AUTH_ERR_INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException -> AUTH_ERR_USER_NOT_FOUND
        is FirebaseAuthUserCollisionException -> SIGNUP_ERR_EMAIL_IN_USE
        is FirebaseNetworkException -> ERR_NETWORK_CONNECTION
        is FirebaseAuthRecentLoginRequiredException -> AUTH_ERR_REAUTHENTICATION_REQUIRED

        is AccountNotFoundException -> ERR_ACCOUNT_NOT_FOUND

        is FirebaseAuthException -> {
            when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> SIGNUP_ERR_EMAIL_IN_USE
                "ERROR_WEAK_PASSWORD"        -> SIGNUP_ERR_INVALID_PASSWORD
                "ERROR_INVALID_EMAIL"        -> SIGNUP_ERR_INVALID_EMAIL
                else -> SIGNUP_ERR_GENERIC_PREFIX + " (${e.errorCode})"
            }
        }

        else -> "$SIGNUP_ERR_UNEXPECTED_PREFIX (${e.javaClass.simpleName})"
    }
}



