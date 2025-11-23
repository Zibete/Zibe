package com.zibete.proyecto1.utils

import android.graphics.drawable.AnimationDrawable
import android.view.View
import eightbitlab.com.blurview.BlurTarget
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

    /** Configura el efecto glass (BlurView 3.x con BlurTarget)  */
    fun applyGlassEffect(blurView: BlurView?, itemView: View) {
        if (blurView == null) return

        val radius = 16f

        // El BlurTarget es el "contenedor base" a desenfocar
        val blurTarget = BlurTarget(itemView.rootView.context)

        blurView.setupWith(blurTarget)
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)

        // sombreado suave para contraste
        blurView.setOverlayColor(0x1A000000)
    }
}
