package com.zibete.proyecto1.core.ui

import android.content.Context
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
sealed class UiText : Parcelable {

    @Parcelize
    data class StringRes(
        val resId: Int,
        val args: List<@RawValue Any> = emptyList()
    ) : UiText()

    @Parcelize
    data class Dynamic(
        val value: String
    ) : UiText()

    @Composable
    fun asString(): String = asString(LocalContext.current)

    fun asString(context: Context): String =
        when (this) {
            is StringRes -> {
                val resolvedArgs = args.map { arg ->
                    if (arg is Int) {
                        try {
                            context.getString(arg)
                        } catch (e: Exception) {
                            arg.toString()
                        }
                    } else {
                        arg.toString()
                    }
                }.toTypedArray()

                context.getString(resId, *resolvedArgs)
            }
            is Dynamic -> value
        }
}

fun String?.toUiText(resWithArgs: Int, resFallback: Int): UiText {
    return if (this != null) UiText.StringRes(resWithArgs, listOf(this))
    else UiText.StringRes(resFallback)
}