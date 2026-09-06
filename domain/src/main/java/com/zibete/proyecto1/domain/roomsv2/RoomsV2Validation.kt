package com.zibete.proyecto1.domain.roomsv2

import java.text.Normalizer
import java.util.Locale

object RoomsV2Validation {
    const val MIN_ROOM_NAME_CHARS = 3
    const val MAX_ROOM_NAME_CHARS = 60
    const val MAX_DESCRIPTION_CHARS = 280
    const val MIN_ALIAS_CHARS = 3
    const val MAX_ALIAS_CHARS = 24
    const val MAX_MESSAGE_CHARS = 4_000
    const val MAX_CLIENT_MESSAGE_ID_CHARS = 120

    private val safeAlias = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} ._-]*$")
    private val unsafeFirebasePathChars = Regex("[.#$\\[\\]/]")

    fun collapseSpaces(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    fun normalizeAlias(value: String): String =
        collapseSpaces(Normalizer.normalize(value, Normalizer.Form.NFKC))
            .lowercase(Locale.ROOT)

    fun validateRoomName(value: String): String? {
        val normalized = collapseSpaces(value)
        return when {
            normalized.length !in MIN_ROOM_NAME_CHARS..MAX_ROOM_NAME_CHARS ->
                "El nombre debe tener entre $MIN_ROOM_NAME_CHARS y $MAX_ROOM_NAME_CHARS caracteres"
            normalized.any { it.code < 32 } -> "El nombre contiene caracteres no permitidos"
            else -> null
        }
    }

    fun validateDescription(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.length > MAX_DESCRIPTION_CHARS ->
                "La descripción supera el máximo de $MAX_DESCRIPTION_CHARS caracteres"
            normalized.any { it.code < 32 && it !in "\n\r\t" } ->
                "La descripción contiene caracteres no permitidos"
            else -> null
        }
    }

    fun validateAlias(value: String): String? {
        val normalized = normalizeAlias(value)
        return when {
            normalized.length !in MIN_ALIAS_CHARS..MAX_ALIAS_CHARS ->
                "El alias debe tener entre $MIN_ALIAS_CHARS y $MAX_ALIAS_CHARS caracteres"
            !safeAlias.matches(normalized) ->
                "El alias solo puede contener letras, números, espacios, punto, guion y guion bajo"
            else -> null
        }
    }

    fun validateMessage(value: String): String? {
        val text = value.trim()
        return when {
            text.isBlank() -> "El mensaje no puede estar vacío"
            text.length > MAX_MESSAGE_CHARS -> "El mensaje supera el límite permitido"
            else -> null
        }
    }

    fun validateClientMessageId(value: String): String? = when {
        value.isBlank() || value.length > MAX_CLIENT_MESSAGE_ID_CHARS ->
            "Identificador de mensaje inválido"
        unsafeFirebasePathChars.containsMatchIn(value) || value.any { it.code < 32 } ->
            "Identificador de mensaje inválido"
        else -> null
    }
}
