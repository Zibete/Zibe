package com.zibete.proyecto1.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import kotlinx.coroutines.delay

@Composable
fun ZibeAnimatedQuotesCard(
    strings: List<String>,
    interval: Long = 3000L,
    modifier: Modifier = Modifier,
) {
    val zibeColors = LocalZibeExtendedColors.current

    val containerColor = zibeColors.cardQuotesBackground
    val textColor = zibeColors.mutedText

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

@Preview
@Composable
fun ZibeAnimatedQuotesCardPreview() {
    val sampleQuotes = listOf(
        "La vida es 10% lo que me ocurre y 90% cómo reacciono a ello.",
        "El éxito no es la clave de la felicidad. La felicidad es la clave del éxito.",
        "No cuentes los días, haz que los días cuenten.",
        "La única forma de hacer un gran trabajo es amar lo que haces."
    )

    ZibeAnimatedQuotesCard(
        strings = sampleQuotes,
        modifier = Modifier.padding(16.dp)
    )
}
