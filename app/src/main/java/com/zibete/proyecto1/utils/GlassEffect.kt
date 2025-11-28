package com.zibete.proyecto1.utils

import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView

object GlassEffect {

    fun startGlowIfAny(glowBorder: View?) {
        if (glowBorder == null) return
        val bg = glowBorder.background
        if (bg is AnimationDrawable) {
            bg.setEnterFadeDuration(600)
            bg.setExitFadeDuration(600)
            bg.start()
        }
    }

    /**
     * Configura el efecto glass usando BlurView 3.x (sin setBlurAlgorithm)
     */
    fun applyGlassEffect(blurView: BlurView?, itemView: View) {
        if (blurView == null) return

        val radius = 16f

        // Root sobre el que se va a tomar el contenido a desenfocar
        val rootView = itemView.rootView as? ViewGroup ?: return
        val windowBackground = rootView.background

//        blurView.setupWith(rootView)
//            .setFrameClearDrawable(windowBackground) // opcional
//            .setBlurRadius(radius)
//            .setBlurAutoUpdate(true)
//            .setHasFixedTransformationMatrix(true)

        // sombreado suave para contraste
        blurView.setOverlayColor(0x1A000000.toInt())
    }
}
