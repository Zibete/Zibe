package com.zibete.proyecto1.ui.extensions

import android.widget.EditText

fun EditText.setTextIfChanged(newValue: String) {
    val current = text?.toString().orEmpty()
    if (current == newValue) return
    if (hasFocus()) return
    setText(newValue)
}