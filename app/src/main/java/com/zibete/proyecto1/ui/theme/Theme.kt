package com.zibete.proyecto1.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R

@Composable
fun ZibeTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalZibeExtendedColors provides ZibeExtendedColors()
    ) {
        MaterialTheme(
            colorScheme = ZibeColorScheme,
            typography = ZibeTypography,
            shapes = ZibeShapes,
            content = content
        )
    }
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
             Font(R.font.poppins_medium, FontWeight.Medium),
             Font(R.font.poppins_semibold, FontWeight.SemiBold),
             Font(R.font.poppins_bold, FontWeight.Bold)
)

val ZibeTypography = Typography(
            headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.SemiBold),
            headlineSmall = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Normal),
            bodyMedium = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Normal),
            labelLarge = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Medium) // Botones
)

val ZibeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)