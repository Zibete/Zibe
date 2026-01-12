
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.zibete.proyecto1.R

class ZibeExtendedColors(
    val zibeRed: Color,
    val zibeYellow: Color,
    val zibeGreen: Color,
    val zibeBlue: Color,
    val zibeGradientStart: Color,
    val zibeGradientMedium: Color,
    val zibeGradientEnd: Color,
    val cardQuotesBackground: Color,
    val inputBackground: Color,
    val mutedText: Color,
    val pinkBubble: Color,
    val blueBubble: Color,
    val snackbarSurface: Color,
    val border: Color,
    val cardBackground: Color,
) {
    val gradientZibe: Brush = Brush.verticalGradient(
        colors = listOf(zibeGradientStart, zibeGradientMedium, zibeGradientEnd)
    )
}

// El Local para acceder desde cualquier Composable
val LocalZibeExtendedColors = staticCompositionLocalOf<ZibeExtendedColors>{
    error("ZibeExtendedColors not provided")
}

@Composable
fun rememberZibeExtendedColors(): ZibeExtendedColors {
    return ZibeExtendedColors(
        zibeRed = colorResource(R.color.zibe_red),
        zibeYellow = colorResource(R.color.zibe_yellow),
        zibeGreen = colorResource(R.color.zibe_green),
        zibeBlue = colorResource(R.color.zibe_blue),
        zibeGradientStart = colorResource(R.color.zibe_gradient_start),
        zibeGradientMedium = colorResource(R.color.zibe_gradient_medium),
        zibeGradientEnd = colorResource(R.color.zibe_gradient_end),
        cardQuotesBackground = colorResource(R.color.zibe_card),
        inputBackground = colorResource(R.color.zibe_input_bg),
        mutedText = colorResource(R.color.zibe_text_muted),
        pinkBubble = colorResource(R.color.pink_bubble),
        blueBubble = colorResource(R.color.blue_bubble),
        snackbarSurface = colorResource(R.color.snackbar_surface),
        border = colorResource(R.color.zibe_pink),
        cardBackground = colorResource(R.color.zibe_night_end)
    )
}


