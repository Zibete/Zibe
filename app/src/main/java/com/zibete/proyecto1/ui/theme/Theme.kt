package com.zibete.proyecto1.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R

val LocalZibeExtendedColors = staticCompositionLocalOf { ZibeExtendedColorSet() }

class ZibeExtendedColorSet(
    val surfaceBright: Color = Color(0xFF1E2A38),
    val gradientZibe: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF3B93),  // zibe_gradient_start
            Color(0xFF8B3CFF),  // zibe_gradient_medium
            Color(0xFF3A54BF)   // zibe_gradient_end
        )
    )
)

@Composable
fun ZibeTheme(
    content: @Composable () -> Unit
) {
        CompositionLocalProvider(
        LocalZibeExtendedColors provides ZibeExtendedColorSet()
    ) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = ZibeTypography,
            shapes = ZibeShapes,
            content = content
        )
    }

}

// 1) Tipografía Poppins
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
             Font(R.font.poppins_medium, FontWeight.Medium),
             Font(R.font.poppins_semibold, FontWeight.SemiBold),
             Font(R.font.poppins_bold, FontWeight.Bold)
)

// 2) Typography global
val ZibeTypography = Typography(
            headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.SemiBold),
            headlineSmall = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Normal),
            bodyMedium = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Normal),
            labelLarge = TextStyle(fontFamily = Poppins,fontWeight = FontWeight.Medium) // Botones
)

// 3) Colores (modifica armas tu paleta)
val DarkColors = darkColorScheme(

    // PRIMARIOS
    primary = Color(0xFF8B3CFF),          // zibe_purple
    primaryContainer = Color(0xFF6A28C8), // zibe_purple_dark
    onPrimary = Color.White,

    // SECUNDARIOS (puede ser tu rosa)
    secondary = Color(0xFFF6286D),        // zibe_pink
    onSecondary = Color.White,

    // TERTIARY (tu azul)
    tertiary = Color(0xFF3A54BF),         // zibe_blue
    onTertiary = Color.White,

    // BACKGROUND / SURFACE
    background = Color(0xFF0D1B2A),       // zibe_night_start
    surface = Color(0xFF1B263B),          // zibe_night_end
    surfaceVariant = Color(0xFF233042),   // zibe_surface
    onBackground = Color.White,
    onSurface = Color(0xFFA3B1C6),        // muted

    // ERROR
    error = Color(0xFFF6286D),            // usás el pink como error también
    onError = Color.White
)

// 4) Shapes (redondeo global de botones/inputs)
val ZibeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)
