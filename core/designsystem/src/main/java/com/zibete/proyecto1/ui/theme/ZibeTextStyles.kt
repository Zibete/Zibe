package com.zibete.proyecto1.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val brandLarge: TextStyle,
    val brandTitle: TextStyle,
    val brandSubtitle: TextStyle,
    val brandLabel: TextStyle
)

val LocalZibeTypography = staticCompositionLocalOf {
    ZibeTypography(
        h1 = TextStyle.Default,
        h2 = TextStyle.Default,
        subtitle = TextStyle.Default,
        body = TextStyle.Default,
        label = TextStyle.Default,
        actionLabel = TextStyle.Default,
        brandLarge = TextStyle.Default,
        brandTitle = TextStyle.Default,
        brandSubtitle = TextStyle.Default,
        brandLabel = TextStyle.Default,
    )
}

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
        brandLarge = TextStyle(
            fontFamily = Axis,
            fontWeight = FontWeight.ExtraBold,
            fontSize = with(density) { dimensionResource(R.dimen.text_size_brand_large).toSp() }
        ),
        brandTitle = TextStyle(
            fontFamily = Axis,
            fontWeight = FontWeight.ExtraBold,
            fontSize = with(density) { dimensionResource(R.dimen.text_size_brand_title).toSp() }
        ),
        brandSubtitle = TextStyle(
            fontFamily = Axis,
            fontWeight = FontWeight.Medium,
            fontSize = with(density) { dimensionResource(R.dimen.text_size_brand_subtitle).toSp() }
        ),
        brandLabel = TextStyle(
            fontFamily = Axis,
            fontWeight = FontWeight.Normal,
            fontSize = with(density) { dimensionResource(R.dimen.label_large_size).toSp() }
        )
    )
}

@Composable
fun rememberZibeTextStyles() = rememberZibeTypographyData()

@Preview(name = "Typography Catalog", showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ZibeTypographyCatalogPreview() {
    ZibeTheme {
        val typography = LocalZibeTypography.current
        val colors = LocalZibeExtendedColors.current
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.gradientZibe),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "ZIBE DESIGN SYSTEM",
                    style = typography.brandTitle,
                    color = colors.lightText
                )
                
                HorizontalDivider(color = colors.lightText.copy(alpha = 0.2f))

                // Group: Branding
                TypographySection("BRANDING (AXIS)", listOf(
                    "Large" to typography.brandLarge,
                    "Title" to typography.brandTitle,
                    "Subtitle" to typography.brandSubtitle,
                    "Label" to typography.brandLabel
                ))

                HorizontalDivider(color = colors.lightText.copy(alpha = 0.1f))

                // Group: Hierarchy
                TypographySection("HIERARCHY (POPPINS)", listOf(
                    "H1 Headline" to typography.h1,
                    "H2 Headline" to typography.h2,
                    "Subtitle" to typography.subtitle,
                    "Body Content" to typography.body,
                    "Label Regular" to typography.label,
                    "Action Link" to typography.actionLabel
                ))
            }
        }
    }
}

@Composable
private fun TypographySection(title: String, styles: List<Pair<String, TextStyle>>) {
    val colors = LocalZibeExtendedColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            ),
            color = colors.lightText.copy(alpha = 0.5f)
        )
        
        styles.forEach { (name, style) ->
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = name,
                        style = TextStyle(fontSize = 10.sp),
                        color = colors.lightText.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "  ${style.fontSize.value.toInt()}sp",
                        style = TextStyle(fontSize = 9.sp),
                        color = colors.lightText.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = "Zibe Experience Design",
                    style = style,
                    color = colors.lightText
                )
            }
        }
    }
}
