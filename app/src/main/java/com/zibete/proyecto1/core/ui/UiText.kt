package com.zibete.proyecto1.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed class UiText {

    data class StringRes(
        val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class Dynamic(
        val value: String
    ) : UiText()

    @Composable
    fun asString(): String = asString(LocalContext.current)

    fun asString(context: Context): String =
        when (this) {
            is StringRes -> context.getString(resId, *args.toTypedArray())
            is Dynamic -> value
        }
}

fun String?.toUiText(resWithArgs: Int, resFallback: Int): UiText {
    return if (this != null) UiText.StringRes(resWithArgs, listOf(this))
    else UiText.StringRes(resFallback)
}