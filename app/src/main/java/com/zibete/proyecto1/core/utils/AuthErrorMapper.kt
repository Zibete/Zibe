package com.zibete.proyecto1.core.utils

import com.zibete.proyecto1.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.facebook.FacebookException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.zibete.proyecto1.core.constants.ERROR_INVALID_EMAIL
import com.zibete.proyecto1.core.constants.ERROR_WEAK_PASSWORD
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_EMAIL_IN_USE
import com.zibete.proyecto1.core.ui.UiText

class AccountNotFoundException : IllegalStateException()
class FirebaseAuthUserNullException : Exception()

fun getAuthErrorMessage(e: Throwable?): UiText {
    if (e == null) return UiText.StringRes(R.string.err_zibe)

    return when (e) {

        // Credential Manager / Google
        is GetCredentialCancellationException -> UiText.StringRes(R.string.login_err_cancelled)
        is NoCredentialException -> UiText.StringRes(R.string.login_err_no_credentials)
        is GoogleIdTokenParsingException -> UiText.StringRes(R.string.login_err_token_parsing)
        is GetCredentialException -> UiText.StringRes(R.string.login_err_generic)

        // Firebase
        is FirebaseAuthInvalidCredentialsException -> UiText.StringRes(R.string.auth_err_invalid_credentials)
        is FirebaseAuthInvalidUserException -> UiText.StringRes(R.string.auth_err_user_not_found)
        is FirebaseAuthUserCollisionException -> UiText.StringRes(R.string.signup_err_mail_in_use)
        is FirebaseNetworkException -> UiText.StringRes(R.string.err_network_connection)
        is FirebaseAuthRecentLoginRequiredException -> UiText.StringRes(R.string.auth_err_reauthentication_required)

        is AccountNotFoundException -> UiText.StringRes(R.string.account_not_found)
        is FirebaseAuthUserNullException -> UiText.StringRes(R.string.signup_err_prefix)

        // Facebook
        is FacebookException -> UiText.StringRes( R.string.signup_facebook_error_prefix, args = listOf(e.localizedMessage ?: ""))

        is FirebaseAuthException -> {
            when (e.errorCode) {
                SIGNUP_ERR_EMAIL_IN_USE -> UiText.StringRes(R.string.signup_err_mail_in_use)
                ERROR_WEAK_PASSWORD -> UiText.StringRes(R.string.signup_err_invalid_format_password)
                ERROR_INVALID_EMAIL -> UiText.StringRes(R.string.signup_err_invalid_format_email)

                else -> UiText.StringRes( R.string.signup_err_prefix, args = listOf(e.errorCode))
            }
        }

        else -> UiText.StringRes( R.string.err_zibe_prefix, args = listOf(e.message ?: ""))
    }
}
