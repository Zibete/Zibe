package com.zibete.proyecto1.ui.extensions

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

fun ImageView.loadProfileImageSafe(
    model: Any?,
    @DrawableRes placeholderRes: Int? = null,
    @DrawableRes errorRes: Int? = null,
    centerCrop: Boolean = true,
    onLoading: ((Boolean) -> Unit)? = null
) {
    if (model == null) {
        placeholderRes?.let { setImageResource(it) } ?: run { setImageDrawable(null) }
        return
    }

    doOnLayout {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)

        onLoading?.invoke(true)

        val req = Glide.with(this)
            .load(model)
            .apply(
                RequestOptions()
                    .override(w, h)
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .dontAnimate()
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    onLoading?.invoke(false)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onLoading?.invoke(false)
                    return false
                }
            })

        if (placeholderRes != null) req.placeholder(placeholderRes)
        if (errorRes != null) req.error(errorRes)

        if (centerCrop) req.centerCrop() else req.centerInside()

        req.into(this)
    }
}
