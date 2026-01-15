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

/**
 * Carga segura (anti "bitmap too large") para fotos de perfil / avatars.
 *
 * - Espera a que el ImageView tenga tamaño real (doOnLayout)
 * - Fuerza override(w,h) para que Glide decodifique a ese tamaño
 * - Downsample + centerCrop/centerInside (según useCase)
 */
fun ImageView.loadProfileImageSafe(
    model: Any?,
    @DrawableRes placeholderRes: Int? = null,
    @DrawableRes errorRes: Int? = null,
    centerCrop: Boolean = true,
    onLoading: ((Boolean) -> Unit)? = null
) {
    if (model == null) {
        // Si te mandan null, deja placeholder/error (si existen) y listo
        placeholderRes?.let { setImageResource(it) } ?: run { setImageDrawable(null) }
        return
    }

    // Si todavía no está medido, esperamos (clave para override correcto).
    doOnLayout {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)

        onLoading?.invoke(true)

        val req = Glide.with(this)
            .load(model)
            .apply(
                RequestOptions()
                    .override(w, h) // <- anti bitmap gigante
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
