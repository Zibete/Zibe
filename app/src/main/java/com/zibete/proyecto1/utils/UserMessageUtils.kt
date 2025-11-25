package com.zibete.proyecto1.utils

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL

/**
 * Utilidades centralizadas para mensajes al usuario:
 * - Snackbars custom (Zibe)
 * - Diálogos de confirmación con estilo AlertDialogApp
 */
object UserMessageUtils {

    // ========= SNACKBARS =========

    @JvmStatic
    fun showSnack(
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
        layout.setBackgroundColor(0x00000000) // fondo del snackbar transparente

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

        // Color fondo de la card
        customView.backgroundTintList =
            ContextCompat.getColorStateList(root.context, bgColor)

        layout.removeAllViews()
        layout.addView(customView)

        snackbar.show()
    }

    @JvmStatic
    fun showInfo(root: View, message: String) {
        showSnack(
            root = root,
            message = message,
            duration = Snackbar.LENGTH_SHORT,
            bgColor = R.color.colorC,
            actionText = null,
            action = null,
            iconRes = R.drawable.ic_info_24
        )
    }

    // ========= DIÁLOGOS =========

    /**
     * Diálogo de confirmación genérico con estilo AlertDialogApp.
     * Úsalo para acciones sensibles: bloquear, eliminar, salir de grupo, etc.
     */
    @JvmStatic
    fun confirm(
        context: Context,
        title: String,
        message: String,
        positiveText: String = DIALOG_ACCEPT,
        negativeText: String = DIALOG_CANCEL,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogApp))
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveText) { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                dialog.dismiss()
                onCancel?.invoke()
            }
            .show()
    }

    // ========= PROGRESS DIALOG =========

    /**
     * Muestra un diálogo de progreso moderno, estilo Zibe.
     * Retorna el AlertDialog para poder ocultarlo luego con .dismiss().
     */
    @JvmStatic
    fun showProgress(context: Context, message: String): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_progress_zibe, null)
        view.findViewById<TextView>(R.id.progress_text).text = message

        val dialog = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogApp))
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.show()
        return dialog
    }


    /**
     * Muestra un Toast con el mensaje dado estilo Zibe.
     */
    private fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

}
