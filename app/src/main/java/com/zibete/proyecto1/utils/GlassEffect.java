package com.zibete.proyecto1.utils;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

public class GlassEffect {


    public static void startGlowIfAny(View glowBorder) {
        if (glowBorder == null) return;
        Drawable bg = glowBorder.getBackground();
        if (bg instanceof AnimationDrawable) {
            AnimationDrawable anim = (AnimationDrawable) bg;
            anim.setEnterFadeDuration(600);
            anim.setExitFadeDuration(600);
            anim.start();
        }
    }

    /** Configura el efecto glass (BlurView 3.x con BlurTarget) */
    public static void applyGlassEffect(BlurView blurView, View itemView) {
        if (blurView == null) return;

        float radius = 16f;

        // El BlurTarget es el "contenedor base" a desenfocar
        BlurTarget blurTarget = new BlurTarget(itemView.getRootView().getContext());

        blurView.setupWith(blurTarget)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true);

        // sombreado suave para contraste
        blurView.setOverlayColor(0x1A000000);
    }


}
