package com.zibete.proyecto1.ui.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

@Stable
object ZibeMotion {
    const val DurationShort = 120
    const val DurationMedium = 220
    const val DurationLong = 360
    const val PressedScale = 0.98f

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> responsiveSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun enter(reducedMotion: Boolean): EnterTransition = if (reducedMotion) {
        fadeIn(animationSpec = snap())
    } else {
        fadeIn(tween(DurationMedium, easing = StandardEasing)) +
            scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(DurationMedium, easing = StandardEasing)
            )
    }

    fun exit(reducedMotion: Boolean): ExitTransition = if (reducedMotion) {
        fadeOut(animationSpec = snap())
    } else {
        fadeOut(tween(DurationShort, easing = StandardEasing)) +
            scaleOut(
                targetScale = 0.99f,
                animationSpec = tween(DurationShort, easing = StandardEasing)
            )
    }
}

enum class ZibeHapticFeedback {
    None,
    Selection,
    Confirm
}

fun androidx.compose.ui.hapticfeedback.HapticFeedback.performZibeFeedback(
    feedback: ZibeHapticFeedback
) {
    when (feedback) {
        ZibeHapticFeedback.None -> Unit
        ZibeHapticFeedback.Selection -> performHapticFeedback(HapticFeedbackType.TextHandleMove)
        ZibeHapticFeedback.Confirm -> performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberZibeReducedMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var reducedMotion by remember(resolver) {
        mutableStateOf(
            Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        )
    }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = Settings.Global.getFloat(
                    resolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    return reducedMotion
}

@Composable
fun Modifier.zibePressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = ZibeMotion.PressedScale
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberZibeReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = if (reducedMotion) snap() else ZibeMotion.responsiveSpring(),
        label = "zibePressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.alpha(if (enabled) 1f else 0.6f)
}

@Composable
fun Modifier.zibePressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    hapticFeedback: ZibeHapticFeedback = ZibeHapticFeedback.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current
): Modifier {
    val haptics = LocalHapticFeedback.current
    return zibePressFeedback(
        interactionSource = interactionSource,
        enabled = enabled
    ).clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        role = role
    ) {
        haptics.performZibeFeedback(hapticFeedback)
        onClick()
    }
}
