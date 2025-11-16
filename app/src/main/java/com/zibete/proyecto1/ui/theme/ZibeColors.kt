package com.zibete.proyecto1.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalZibeExtendedColors = staticCompositionLocalOf { ZibeExtendedColorSet() }

class ZibeExtendedColorSet(

    val snackbarSurface: Color = Color(0xFF1E2A38),
    val zibeGradientStart: Color = Color(0xFFFF3B93),
    val zibeGradientMedium: Color = Color(0xFF8B3CFF),
    val zibeGradientEnd: Color = Color(0xFF3A54BF),

    val blueBubble: Color = Color(0xFF3A54BF),
    val pinkBubble: Color = Color(0xFFF6286D),

    val inputBackground: Color = Color(0xFF0D1B2A),
    val cardBackground: Color = Color(0xFF1B263B),
    val cardQuotesBackground: Color = Color(0xFF233042),

    val border: Color = Color(0xFFF6286D),

    val mutedText: Color = Color(0xFFA3B1C6),

    val zibeRed: Color = Color(0xFFFF4C4C),
    val zibeYellow: Color = Color(0xFFFFD93B),
    val zibeGreen: Color = Color(0xFF4CAF50),
    val zibeBlue: Color = Color(0xFF3A7BFF),


    val gradientZibe: Brush = Brush.verticalGradient(
        colors = listOf(zibeGradientStart,
            zibeGradientMedium,
            zibeGradientEnd
        )
    )
)

// Colores
val ZibeColorScheme = darkColorScheme(

    primary = Color(0xFFF6286D),              // Pink principal
    onPrimary = Color(0xFFFFFFFF),            // Texto blanco sobre primary

    surfaceContainerHigh = Color(0xFF1E2A38),   // Superficies principales -- Lo usa DatePicker y Dialog


    )


