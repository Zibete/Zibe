package com.zibete.proyecto1.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.zibete.proyecto1.R

object SnackUtils {

    @JvmStatic
    fun show(
        root: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        @ColorRes bgColor: Int = R.color.colorC,
        actionText: String? = null,
        action: ((View) -> Unit)? = null,
        @DrawableRes iconRes: Int = 0
    ) {
        val snackbar = Snackbar.make(root, "", duration)

        val layout = snackbar.view as SnackbarLayout
        layout.setBackgroundColor(0x00000000) // transparente

        val customView = LayoutInflater.from(root.context)
            .inflate(R.layout.layout_snackbar_zibe, layout, false)

        // Texto
        customView.findViewById<TextView>(R.id.snack_text).text = message

        // Ícono opcional
        val iv = customView.findViewById<ImageView>(R.id.snack_icon)
        if (iconRes != 0) {
            iv.visibility = View.VISIBLE
            iv.setImageResource(iconRes)
        } else {
            iv.visibility = View.GONE
        }

        // Acción opcional
        val tvAction = customView.findViewById<TextView>(R.id.snack_action)
        if (actionText != null && action != null) {
            tvAction.visibility = View.VISIBLE
            tvAction.text = actionText
            tvAction.setOnClickListener { v ->
                action(v)
                snackbar.dismiss()
            }
        } else {
            tvAction.visibility = View.GONE
        }

        // Color fondo card
        customView.backgroundTintList =
            ContextCompat.getColorStateList(root.context, bgColor)

        layout.removeAllViews()
        layout.addView(customView)
        snackbar.show()
    }

    @JvmStatic
    fun showInfo(root: View, message: String) {
        show(
            root = root,
            message = message,
            duration = Snackbar.LENGTH_SHORT,
            bgColor = R.color.colorC,
            actionText = null,
            action = null,
            iconRes = R.drawable.ic_info_24
        )
    }
}
