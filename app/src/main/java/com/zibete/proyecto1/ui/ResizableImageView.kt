package com.zibete.proyecto1.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.roundToInt

class ResizableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable = drawable
        if (drawable == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        if (imageWidth <= 0 || imageHeight <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width.toFloat() * imageHeight / imageWidth).roundToInt()

        setMeasuredDimension(width, height)
    }
}
