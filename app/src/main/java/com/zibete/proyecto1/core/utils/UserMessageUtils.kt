package com.zibete.proyecto1.core.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeSnackType

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
        type: ZibeSnackType? = null,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: ((View) -> Unit)? = null
    ) {
        val (bgColorRes, iconRes) = when (type) {
            ZibeSnackType.SUCCESS -> R.color.zibe_green to R.drawable.ic_check_24
            ZibeSnackType.ERROR   -> R.color.zibe_red to R.drawable.ic_baseline_cancel_24
            ZibeSnackType.WARNING -> R.color.zibe_yellow to R.drawable.ic_warning_24
            ZibeSnackType.INFO    -> R.color.zibe_blue to R.drawable.ic_info_24
            null                  -> null to 0
        }

        val snackbar = Snackbar.make(root, "", duration)

        val parent = snackbar.view as ViewGroup
        parent.setBackgroundColor(0x00000000)

        val customView = LayoutInflater.from(root.context)
            .inflate(R.layout.layout_snackbar_zibe, parent, false)

        // Texto
        customView.findViewById<TextView>(R.id.snack_text).text = message

        // Ícono
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

        // Fondo
        bgColorRes?.let {
            customView.backgroundTintList =
                ContextCompat.getColorStateList(root.context, it)
        }

        parent.removeAllViews()
        parent.addView(customView)

        snackbar.show()
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
        positiveText: String = context.getString(R.string.action_accept),
        negativeText: String = context.getString(R.string.action_cancel),
        choices: Array<String>? = null,
        selectedIndex: Int = -1,
        onChoiceSelected: ((index: Int) -> Unit)? = null,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setCancelable(false)
            .apply {
                if (choices != null) {
                    setSingleChoiceItems(choices, selectedIndex) { _, index ->
                        onChoiceSelected?.invoke(index)
                    }
                } else {
                    setMessage(message)
                }
            }
            .setPositiveButton(positiveText) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                onCancel?.invoke()
                dialog.dismiss()
            }
            .show()
    }

    @JvmStatic
    fun alert(
        context: Context,
        message: String,
        title: String? = null,
        positiveText: String = context.getString(R.string.action_accept),
        onConfirm: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { dialog, _ ->
                onConfirm?.invoke()
                dialog.dismiss()
            }
            .show()
    }

}
