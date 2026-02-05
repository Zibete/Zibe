package com.zibete.proyecto1.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.zibete.proyecto1.R

class ZibeExtendedColors(
    val snackRed: Color,
    val snackYellow: Color,
    val snackGreen: Color,
    val snackBlue: Color,
    val zibePink: Color,
    val zibePurple: Color,
    val zibeGradientStart: Color,
    val zibeGradientMedium: Color,
    val zibeGradientEnd: Color,
    val cardQuotesBackground: Color,
    val contentDarkBg: Color,
    val contentLightBg: Color,
    val hintText: Color,
    val lightText: Color,
    val darkText: Color,
    val pinkBubble: Color,
    val blueBubble: Color,
    val snackbarSurface: Color,
    val accent: Color,
    val cardBackground: Color,
    val snackBackground: Color,
    val snackAction: Color
) {
    val gradientZibe: Brush = Brush.verticalGradient(
        colors = listOf(zibeGradientStart, zibeGradientMedium, zibeGradientEnd)
    )
}

// El Local para acceder desde cualquier Composable
val LocalZibeExtendedColors = staticCompositionLocalOf<ZibeExtendedColors> {
    error("ZibeExtendedColors not provided")
}

@Composable
fun rememberZibeExtendedColors(): ZibeExtendedColors {
    return ZibeExtendedColors(
        snackRed = colorResource(R.color.zibe_red),
        snackYellow = colorResource(R.color.zibe_yellow),
        snackGreen = colorResource(R.color.zibe_green),
        snackBlue = colorResource(R.color.zibe_blue),

        zibePink = colorResource(R.color.zibe_pink),
        zibePurple = colorResource(R.color.zibe_purple),

        zibeGradientStart = colorResource(R.color.zibe_gradient_start),
        zibeGradientMedium = colorResource(R.color.zibe_purple),
        zibeGradientEnd = colorResource(R.color.zibe_gradient_end),

        contentDarkBg = colorResource(R.color.zibe_dark_bg),
        contentLightBg = colorResource(R.color.zibe_night_end),
        cardQuotesBackground = colorResource(R.color.zibe_light_bg),

        hintText = colorResource(R.color.zibe_muted_text),
        lightText = colorResource(R.color.zibe_text_light),
        darkText = colorResource(R.color.zibe_text_dark),

        pinkBubble = colorResource(R.color.pink_bubble),
        blueBubble = colorResource(R.color.blue_bubble),

        snackbarSurface = colorResource(R.color.snackbar_surface),
        accent = colorResource(R.color.zibe_pink),
        cardBackground = colorResource(R.color.zibe_dark_bg),

        snackBackground = Color(0xFF1F1F2B),
        snackAction = Color(0xFF6EA8FF)
    )
}


