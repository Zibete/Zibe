package com.zibete.proyecto1.domain.rooms

import java.text.Normalizer
import java.util.Locale

enum class RoomValidationField {
    ROOM_NAME,
    DESCRIPTION,
    ALIAS,
    PUBLIC_IDENTITY,
    ROOM_KEY,
    EVENT_CONTENT,
    LAST_READ_AT
}

enum class RoomValidationError {
    REQUIRED,
    TOO_SHORT,
    TOO_LONG,
    INVALID_CHARACTERS,
    INVALID_VALUE
}

data class RoomValidationIssue(
    val field: RoomValidationField,
    val error: RoomValidationError
)

sealed interface RoomTextValidation {
    data class Valid(
        val value: String,
        val normalizedKey: String = value
    ) : RoomTextValidation

    data class Invalid(val issue: RoomValidationIssue) : RoomTextValidation
}

object RoomValidator {
    const val ROOM_NAME_MIN_LENGTH = 3
    const val ROOM_NAME_MAX_LENGTH = 48
    const val DESCRIPTION_MAX_LENGTH = 280
    const val ALIAS_MIN_LENGTH = 3
    const val ALIAS_MAX_LENGTH = 24
    const val PUBLIC_IDENTITY_MAX_LENGTH = 80
    const val ROOM_KEY_MAX_LENGTH = 120
    const val EVENT_CONTENT_MAX_LENGTH = 200

    private val roomNamePattern = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N} _'-]*")
    private val aliasPattern = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]*")
    private val firebaseForbiddenCharacters = Regex("[.#$\\[\\]/]")
    private val disallowedControlCharacters = Regex("[\\p{Cc}&&[^\\n\\t]]")
    private val combiningMarks = Regex("\\p{M}+")
    private val nonIndexCharacters = Regex("[^\\p{L}\\p{N}_-]+")
    private val repeatedDashes = Regex("-+")

    fun validateRoomName(value: String): RoomTextValidation = validatePatternedText(
        value = value,
        field = RoomValidationField.ROOM_NAME,
        minLength = ROOM_NAME_MIN_LENGTH,
        maxLength = ROOM_NAME_MAX_LENGTH,
        pattern = roomNamePattern
    )

    fun validateDescription(value: String): RoomTextValidation {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> invalid(RoomValidationField.DESCRIPTION, RoomValidationError.REQUIRED)
            trimmed.length > DESCRIPTION_MAX_LENGTH ->
                invalid(RoomValidationField.DESCRIPTION, RoomValidationError.TOO_LONG)
            disallowedControlCharacters.containsMatchIn(trimmed) ->
                invalid(RoomValidationField.DESCRIPTION, RoomValidationError.INVALID_CHARACTERS)
            else -> RoomTextValidation.Valid(trimmed)
        }
    }

    fun validateAlias(value: String): RoomTextValidation = validatePatternedText(
        value = value,
        field = RoomValidationField.ALIAS,
        minLength = ALIAS_MIN_LENGTH,
        maxLength = ALIAS_MAX_LENGTH,
        pattern = aliasPattern
    )

    fun validatePublicIdentity(value: String): RoomTextValidation {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() ->
                invalid(RoomValidationField.PUBLIC_IDENTITY, RoomValidationError.REQUIRED)
            trimmed.length > PUBLIC_IDENTITY_MAX_LENGTH ->
                invalid(RoomValidationField.PUBLIC_IDENTITY, RoomValidationError.TOO_LONG)
            disallowedControlCharacters.containsMatchIn(trimmed) ->
                invalid(RoomValidationField.PUBLIC_IDENTITY, RoomValidationError.INVALID_CHARACTERS)
            else -> RoomTextValidation.Valid(trimmed)
        }
    }

    fun validateRoomKey(value: String): RoomTextValidation {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> invalid(RoomValidationField.ROOM_KEY, RoomValidationError.REQUIRED)
            trimmed.length > ROOM_KEY_MAX_LENGTH ->
                invalid(RoomValidationField.ROOM_KEY, RoomValidationError.TOO_LONG)
            firebaseForbiddenCharacters.containsMatchIn(trimmed) ||
                disallowedControlCharacters.containsMatchIn(trimmed) ->
                invalid(RoomValidationField.ROOM_KEY, RoomValidationError.INVALID_CHARACTERS)
            else -> RoomTextValidation.Valid(trimmed)
        }
    }

    fun validateEventContent(value: String): RoomTextValidation {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() ->
                invalid(RoomValidationField.EVENT_CONTENT, RoomValidationError.REQUIRED)
            trimmed.length > EVENT_CONTENT_MAX_LENGTH ->
                invalid(RoomValidationField.EVENT_CONTENT, RoomValidationError.TOO_LONG)
            disallowedControlCharacters.containsMatchIn(trimmed) ->
                invalid(RoomValidationField.EVENT_CONTENT, RoomValidationError.INVALID_CHARACTERS)
            else -> RoomTextValidation.Valid(trimmed)
        }
    }

    fun normalizeIndexKey(value: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return decomposed
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(nonIndexCharacters, "-")
            .replace(repeatedDashes, "-")
            .trim('-')
    }

    private fun validatePatternedText(
        value: String,
        field: RoomValidationField,
        minLength: Int,
        maxLength: Int,
        pattern: Regex
    ): RoomTextValidation {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> invalid(field, RoomValidationError.REQUIRED)
            trimmed.length < minLength -> invalid(field, RoomValidationError.TOO_SHORT)
            trimmed.length > maxLength -> invalid(field, RoomValidationError.TOO_LONG)
            firebaseForbiddenCharacters.containsMatchIn(trimmed) || !pattern.matches(trimmed) ->
                invalid(field, RoomValidationError.INVALID_CHARACTERS)
            else -> RoomTextValidation.Valid(
                value = trimmed,
                normalizedKey = normalizeIndexKey(trimmed)
            )
        }
    }

    private fun invalid(
        field: RoomValidationField,
        error: RoomValidationError
    ): RoomTextValidation.Invalid = RoomTextValidation.Invalid(RoomValidationIssue(field, error))
}
