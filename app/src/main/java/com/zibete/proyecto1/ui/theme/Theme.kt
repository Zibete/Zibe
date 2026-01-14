package com.zibete.proyecto1.ui.theme

import LocalZibeExtendedColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zibete.proyecto1.R
import rememberZibeExtendedColors

@Composable
fun ZibeTheme(content: @Composable () -> Unit) {
    val extendedColors = rememberZibeExtendedColors()
    val zibeShapes = rememberZibeShapes()
    val zibeTypography = rememberZibeTypography()

    val zibeColorScheme = darkColorScheme(
        primary = colorResource(R.color.zibe_pink),
        onPrimary = colorResource(R.color.white),
        secondary = colorResource(R.color.zibe_purple),
        surface = colorResource(R.color.zibe_night_start),
        onSurface = colorResource(R.color.zibe_text_light),
        error = colorResource(R.color.zibe_red),
        surfaceContainerHigh = colorResource(R.color.zibe_dark_bg)
    )

    CompositionLocalProvider(
        LocalZibeExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = zibeColorScheme,
            typography = zibeTypography,
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

@Composable
fun rememberZibeTypography(): Typography {
    val poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    val headlineLargeSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.head_line_large_size).toSp() }
    val headlineMediumSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.head_line_medium_size).toSp() }
    val headlineSmallSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.head_line_small_size).toSp() }
    val bodyLargeSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.body_large_size).toSp() }
    val bodyMediumSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.body_medium_size).toSp() }
    val labelLargeSize =
        with(LocalDensity.current) { dimensionResource(R.dimen.label_large_size).toSp() }

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Bold,
            fontSize = headlineLargeSize
        ),
        headlineMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = headlineMediumSize
        ),
        headlineSmall = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize = headlineSmallSize
        ),
        bodyLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Normal,
            fontSize = bodyLargeSize
        ),
        bodyMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Normal,
            fontSize = bodyMediumSize
        ),
        labelLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize = labelLargeSize
        ),
    )
}