package com.zibete.proyecto1.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeSnackType
import kotlin.math.abs

/**
 * Utilidades centralizadas para mensajes al usuario:
 * - Snackbars custom (Zibe)
 * - Diálogos de confirmación con estilo AlertDialogApp
 */
object UserMessageUtils {

    @Deprecated("Migrado a Compose. Usar ZibeSnackbar en su lugar.")
    @JvmStatic
    fun showSnack(
        root: View,
        message: String,
        type: ZibeSnackType? = null,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: ((View) -> Unit)? = null
    ) {
        val (iconColorRes, iconRes) = when (type) {
            ZibeSnackType.SUCCESS -> DsR.color.zibe_green to R.drawable.ic_check_24
            ZibeSnackType.ERROR -> DsR.color.zibe_red to R.drawable.ic_baseline_cancel_24
            ZibeSnackType.WARNING -> DsR.color.zibe_yellow to R.drawable.ic_warning_24
            ZibeSnackType.INFO -> DsR.color.zibe_blue to R.drawable.ic_info_24
            null -> null to 0
        }

        val coordinator = root.findCoordinatorParent()
        val snackbar = Snackbar.make(coordinator ?: root, "", duration)
        val snackView = snackbar.view as ViewGroup

        snackView.setPadding(0, 0, 0, 0)

        val customView = LayoutInflater.from(root.context)
            .inflate(R.layout.layout_snackbar_zibe, snackView, false)

        // swipe (CoordinatorLayout o fallback universal)
        attachUniversalSwipeToDismiss(snackbar, customView)

        // Texto
        customView.findViewById<TextView>(R.id.snack_text).apply {
            text = message.trim()
            setTextColor(ContextCompat.getColor(root.context, DsR.color.zibe_hint_text))
        }

        // Icono
        val iv = customView.findViewById<ImageView>(R.id.snack_icon)
        if (iconRes != 0 && iconColorRes != null) {
            iv.visibility = View.VISIBLE
            iv.setImageResource(iconRes)
            iv.imageTintList =
                ContextCompat.getColorStateList(root.context, iconColorRes)
        } else {
            iv.visibility = View.GONE
        }

        // Acción
        val tvAction = customView.findViewById<TextView>(R.id.snack_action)
        if (actionText != null && action != null) {
            tvAction.visibility = View.VISIBLE
            tvAction.text = actionText
            tvAction.setOnClickListener {
                action(it)
                snackbar.dismiss()
            }
        } else {
            tvAction.visibility = View.GONE
        }

        snackView.removeAllViews()
        snackView.addView(customView)
        snackView.setBackgroundColor(Color.TRANSPARENT)

        snackbar.show()
    }

    private fun View.findCoordinatorParent(): CoordinatorLayout? {
        var p = parent
        while (p is View) {
            if (p is CoordinatorLayout) return p
            p = p.parent
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachUniversalSwipeToDismiss(
        snackbar: Snackbar,
        swipeTarget: View // tu customView (layout_snackbar_zibe)
    ) {
        val snackView = snackbar.view

        // 1) Fast path: CoordinatorLayout => SwipeDismissBehavior real
        (snackView.layoutParams as? CoordinatorLayout.LayoutParams)?.let { params ->
            params.behavior = SwipeDismissBehavior<View>().apply {
                setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
                setListener(object : SwipeDismissBehavior.OnDismissListener {
                    override fun onDismiss(view: View) = snackbar.dismiss()
                    override fun onDragStateChanged(state: Int) = Unit
                })
            }
            snackView.layoutParams = params
            return
        }

        // 2) Fallback: swipe manual (sirve en cualquier parent)
        val slop = ViewConfiguration.get(swipeTarget.context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var dragging = false

        swipeTarget.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    dragging = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY

                    if (!dragging) {
                        // arrancamos swipe solo si es claramente horizontal
                        if (abs(dx) > slop && abs(dx) > abs(dy)) {
                            dragging = true
                        } else {
                            return@setOnTouchListener false
                        }
                    }

                    // follow finger
                    snackView.translationX = dx
                    snackView.alpha =
                        (1f - (abs(dx) / snackView.width.toFloat())).coerceIn(0.2f, 1f)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)

                    val dx = snackView.translationX
                    val shouldDismiss = abs(dx) > snackView.width * 0.33f

                    if (shouldDismiss) {
                        val target =
                            if (dx > 0) snackView.width.toFloat() else -snackView.width.toFloat()
                        snackView.animate()
                            .translationX(target)
                            .alpha(0f)
                            .setDuration(160)
                            .withEndAction { snackbar.dismiss() }
                            .start()
                    } else {
                        snackView.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(160)
                            .start()
                    }
                    true
                }

                else -> false
            }
        }
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
        message: String = "",
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
    fun dialog(
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



