package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.delay

/**
 * ZibeCard: Card base para toda la app (equivalente a style XML Zibe.Card)
 * - Container: surfaceContainerHigh (zibe_card)
 * - Shape: MaterialTheme.shapes.medium (corner_medium)
 * - Elevation: 3dp (como XML)
 * - Border sutil con zibe border (zibe_pink) con alpha
 * - Soporta clickable opcional
 */
@Composable
fun ZibeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = 3.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = LocalZibeExtendedColors.current.accent.copy(alpha = 0.18f)
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor
    )

    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation)

    if (onClick != null) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = cardElevation,
            border = border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = cardElevation,
            border = border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Card específica para frases (ya existente).
 * La dejo acá para que todo lo “card” viva en un solo archivo.
 */
@Composable
fun ZibeAnimatedQuotesCard(
    strings: List<String>,
    interval: Long = 3000L,
    modifier: Modifier = Modifier,
) {
    val zibeColors = LocalZibeExtendedColors.current

    val containerColor = zibeColors.cardQuotesBackground
    val textColor = zibeColors.hintText

    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(interval)
            index = (index + 1) % strings.size
        }
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = strings[index],
                transitionSpec = {
                    fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                },
                label = "FrasesLoop"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ZibeCardPreview() {
    ZibeTheme {
        ZibeCard {
            Text(
                text = "ZibeCard base",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Usala en toda la app para mantener consistencia visual.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalZibeExtendedColors.current.hintText
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ZibeAnimatedQuotesCardPreview() {
    ZibeTheme {
        ZibeAnimatedQuotesCard(
            strings = listOf(
                "La primera frase de ejemplo para ver cómo queda el diseño.",
                "Segunda frase, un poco más corta.",
                "Y una tercera frase para completar el ciclo de animación."
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}
