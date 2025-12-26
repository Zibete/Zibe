package com.zibete.proyecto1.utils

import android.text.Editable
import android.text.TextWatcher

class SimpleWatcher(
    private val onChanged: (String) -> Unit
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun afterTextChanged(s: Editable?) = Unit

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        onChanged(s?.toString().orEmpty())
    }
}
