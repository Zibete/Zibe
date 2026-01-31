package com.zibete.proyecto1.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zibete.proyecto1.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val Axis = FontFamily(
    Font(R.font.axis_extra_bold_800, FontWeight.ExtraBold)
)

@Immutable
data class ZibeTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val actionLabel: TextStyle,
    val brandTitle: TextStyle
)

val LocalZibeTypography = staticCompositionLocalOf {
    ZibeTypography(
        h1 = TextStyle.Default,
        h2 = TextStyle.Default,
        subtitle = TextStyle.Default,
        body = TextStyle.Default,
        label = TextStyle.Default,
        actionLabel = TextStyle.Default,
        brandTitle = TextStyle.Default
    )
}

// Keep LocalZibeTextStyles for backward compatibility if needed, or alias it
val LocalZibeTextStyles = LocalZibeTypography

@Composable
fun rememberZibeTypographyData(): ZibeTypography {
    val density = LocalDensity.current
    
    return ZibeTypography(
        h1 = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { dimensionResource(R.dimen.head_line_large_size).toSp() }
        ),
        h2 = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = with(density) { dimensionResource(R.dimen.head_line_medium_size).toSp() }
        ),
        subtitle = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = with(density) { dimensionResource(R.dimen.head_line_small_size).toSp() }
        ),
        body = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = with(density) { dimensionResource(R.dimen.body_large_size).toSp() }
        ),
        label = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = with(density) { dimensionResource(R.dimen.label_large_size).toSp() }
        ),
        actionLabel = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = with(density) { dimensionResource(R.dimen.label_large_size).toSp() }
        ),
        brandTitle = TextStyle(
            fontFamily = Axis,
            fontWeight = FontWeight.ExtraBold,
            fontSize = with(density) { dimensionResource(R.dimen.head_line_medium_size).toSp() }
        )
    )
}

@Composable
fun rememberZibeTextStyles() = rememberZibeTypographyData()
