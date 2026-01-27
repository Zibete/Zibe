package com.zibete.proyecto1.core.validation

import android.util.Patterns
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText

object CredentialValidators {

    const val MIN_PASSWORD_LEN = 8

    fun validateEmail(email: String, compareTo: String? = null): UiText? {
        val trimmedEmail = email.trim()
        val trimmedCompareTo = compareTo?.trim()

        return when {
            trimmedEmail.isBlank() ->
                UiText.StringRes(R.string.err_email_required)

            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                UiText.StringRes(R.string.err_invalid_format_email)

            trimmedCompareTo != null && trimmedEmail.equals(trimmedCompareTo, ignoreCase = true) ->
                UiText.StringRes(R.string.err_emails_do_not_match)

            else -> null
        }
    }

    fun validateNewPassword(password: String, compareTo: String?): UiText? {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        return when {
            password.length < MIN_PASSWORD_LEN ->
                UiText.StringRes(
                    R.string.err_invalid_format_password,
                    listOf(MIN_PASSWORD_LEN)
                )

            !hasLetter ->
                UiText.StringRes(R.string.err_password_letter)

            !hasDigit ->
                UiText.StringRes(R.string.err_password_digit)

            compareTo != null && password.equals(compareTo, ignoreCase = false) ->
                UiText.StringRes(
                    R.string.err_passwords_do_not_match
                )

            else -> null
        }
    }
}
