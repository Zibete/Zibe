package com.zibete.proyecto1.core.ui

import android.content.Context

sealed class UiText {

    data class StringRes(
        val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class Dynamic(
        val value: String
    ) : UiText()

    fun asString(context: Context): String =
        when (this) {
            is StringRes -> context.getString(resId, *args.toTypedArray())
            is Dynamic -> value
        }
}
