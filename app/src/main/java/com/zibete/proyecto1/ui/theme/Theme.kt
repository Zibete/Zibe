package com.zibete.proyecto1.ui.theme

import LocalZibeExtendedColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.zibete.proyecto1.R
import rememberZibeExtendedColors

@Composable
fun ZibeTheme(content: @Composable () -> Unit) {
    val zibeExtendedColors = rememberZibeExtendedColors()
    val zibeShapes = rememberZibeShapes()
    val zibeTypographyData = rememberZibeTypographyData()
    
    // Create Material3 Typography from ZibeTypographyData
    val materialTypography = Typography(
        headlineLarge = zibeTypographyData.h1,
        headlineMedium = zibeTypographyData.h2,
        headlineSmall = zibeTypographyData.subtitle,
        bodyLarge = zibeTypographyData.body,
        labelLarge = zibeTypographyData.label
    )

    val zibeColorScheme = darkColorScheme(
        primary = zibeExtendedColors.accent,
        onPrimary = zibeExtendedColors.lightText,
        secondary = zibeExtendedColors.zibeGradientMedium,
        surface = zibeExtendedColors.cardBackground,
        onSurface = zibeExtendedColors.lightText,
        error = zibeExtendedColors.snackRed,
        surfaceContainerHigh = zibeExtendedColors.cardBackground
    )

    CompositionLocalProvider(
        LocalZibeExtendedColors provides zibeExtendedColors,
        LocalZibeTypography provides zibeTypographyData
    ) {
        MaterialTheme(
            colorScheme = zibeColorScheme,
            typography = materialTypography,
            shapes = zibeShapes,
            content = content
        )
    }
}

@Composable
fun rememberZibeShapes(): Shapes {
    return Shapes(
        small = RoundedCornerShape(dimensionResource(R.dimen.corner_small)),
        medium = RoundedCornerShape(dimensionResource(R.dimen.corner_medium)),
        large = RoundedCornerShape(dimensionResource(R.dimen.corner_xl))
    )
}
