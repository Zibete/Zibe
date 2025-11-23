package com.zibete.proyecto1.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ResizableImageViewProfile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        adjustViewBounds = true
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = drawable

        // Si no hay imagen, medir normal
        if (d == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val intrinsicW = d.intrinsicWidth
        val intrinsicH = d.intrinsicHeight

        // Caso raro: drawables vectoriales con -1 o 0
        if (intrinsicW <= 0 || intrinsicH <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val imageRatio = intrinsicW.toFloat() / intrinsicH.toFloat()

        val height = if (width > 0) {
            (width / imageRatio).toInt()
        } else {
            intrinsicH
        }

        setMeasuredDimension(width, height)
    }
}
