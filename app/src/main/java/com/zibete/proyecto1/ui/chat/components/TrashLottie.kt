package com.zibete.proyecto1.ui.chat.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

@Composable
fun TrashLottie(
    isVisible: Boolean,
    isHighlighted: Boolean,
    playDrop: Boolean,
    onDropFinished: () -> Unit,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val colors = LocalZibeExtendedColors.current
    val highlightScale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.08f else 1f,
        label = "trashScale"
    )
    val onDropFinishedState by rememberUpdatedState(onDropFinished)
    val playDropState by rememberUpdatedState(playDrop)
    var hasFinishedDrop by remember { mutableStateOf(false) }
    val borderModifier = if (isHighlighted) {
        Modifier.border(2.dp, colors.accent, CircleShape)
    } else {
        Modifier
    }

    LaunchedEffect(playDrop) {
        if (playDrop) {
            hasFinishedDrop = false
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = highlightScale
                scaleY = highlightScale
            }
            .background(colors.contentDarkBg, CircleShape)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                LottieAnimationView(context).apply {
                    setAnimation(R.raw.lottie_trash)
                    repeatCount = 0
                    progress = 0f
                    addAnimatorListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (playDropState && !hasFinishedDrop) {
                                hasFinishedDrop = true
                                onDropFinishedState()
                            }
                        }
                    })
                }
            },
            update = { view ->
                if (playDrop && !hasFinishedDrop) {
                    view.repeatCount = 0
                    if (!view.isAnimating) {
                        view.progress = 0f
                        view.playAnimation()
                    }
                } else {
                    view.cancelAnimation()
                    view.progress = 0f
                }
            }
        )
    }
}
