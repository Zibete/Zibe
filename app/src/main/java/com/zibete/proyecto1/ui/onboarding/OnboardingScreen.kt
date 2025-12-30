package com.zibete.proyecto1.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.*
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.OnboardingPage
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import com.zibete.proyecto1.ui.constants.BUTTON_BACK
import com.zibete.proyecto1.ui.constants.BUTTON_NEXT
import com.zibete.proyecto1.ui.constants.BUTTON_SKIP
import com.zibete.proyecto1.ui.constants.BUTTON_START
import com.zibete.proyecto1.ui.constants.Constants.UiTags.ONBOARDING_SCREEN

// ------------------------------------------------------
//  TextButton estilizado (misma altura y forma que ZibeButton)
// ------------------------------------------------------
@Composable
fun ZibeTextButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),       // MISMA ALTURA que ZibeButton
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val lastIndex = pages.lastIndex

    // Gradiente animado por página
    val (topColorTarget, bottomColorTarget) = when (pagerState.currentPage) {
        0 -> Color(0xFF4C3EFF) to Color(0xFF151226)
        1 -> Color(0xFF00C9A7) to Color(0xFF002D3A)
        else -> Color(0xFFFF6B6B) to Color(0xFF2B0B3F)
    }

    val topColor by animateColorAsState(topColorTarget, tween(600))
    val bottomColor by animateColorAsState(bottomColorTarget, tween(600))

    val gradientBrush = Brush.verticalGradient(listOf(topColor, bottomColor))

    Box(
        modifier = Modifier
            .testTag(ONBOARDING_SCREEN)
            .fillMaxSize()
            .background(gradientBrush)
            .systemBarsPadding()
    ) {

        // SALTAR arriba
        if (pagerState.currentPage < lastIndex) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 34.dp)
                    .zIndex(1f)
            ) {
                Text(
                    text = BUTTON_SKIP,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CARD central
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.35f)
                )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    AnimatedContent(
                        targetState = pages[index],
                        transitionSpec = {
                            fadeIn(tween(250)) with fadeOut(tween(250))
                        },
                        label = "pageContent"
                    ) { page ->
                        OnboardingPageContent(page)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dots
            DotsIndicator(
                totalDots = pages.size,
                selectedIndex = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------------
            //      BOTONERA: siempre 50% + 50%
            // ------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // IZQUIERDA = ATRÁS (si page > 0), si no → invisible
                if (pagerState.currentPage > 0) {
                    ZibeTextButton(
                        text = BUTTON_BACK,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                } else {
                    // espacio igualado para mantener balance visual
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // DERECHA = SIGUIENTE / COMENZAR
                if (pagerState.currentPage < lastIndex) {
                    ZibeTextButton(
                        text = BUTTON_NEXT,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                } else {
                    // última → COMENZAR = ZibeButton
                    ZibeButton(
                        text = BUTTON_START,
                        modifier = Modifier.weight(1f),
                        onClick = onFinished
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(page.animationRes)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DotsIndicator(totalDots: Int, selectedIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { i ->
            val isSelected = i == selectedIndex
            val size by animateDpAsState(
                if (isSelected) 10.dp else 8.dp, tween(250)
            )
            val color by animateColorAsState(
                if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                tween(250)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun OnboardingPreviewFull() {
    val demoPages = listOf(
        OnboardingPage(R.raw.onboarding1, "Chatea", "Chatea con amigos en tiempo real."),
        OnboardingPage(R.raw.onboarding2, "Descubre", "Encuentra personas cercanas."),
        OnboardingPage(R.raw.onboarding3, "Socializa", "Únete a salas de chat!")
    )

    ZibeTheme {
        OnboardingScreen(
            pages = demoPages,
            onFinished = {}
        )
    }
}
