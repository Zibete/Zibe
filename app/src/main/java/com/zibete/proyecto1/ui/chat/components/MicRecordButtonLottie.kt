package com.zibete.proyecto1.ui.chat.components

import com.zibete.proyecto1.core.designsystem.R as DsR
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlin.math.roundToInt

@Composable
fun MicRecordButton(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onMicPressed: () -> Unit,
    onMicMoved: (Float, Float, Float) -> Unit,
    onMicReleased: () -> Unit,
    onPressStateChange: (Boolean) -> Unit = {},
    onCenterPositioned: (Offset) -> Unit = {},
    onPointerInWindowChanged: (Offset) -> Unit = {}
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    var buttonCenterInWindow by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val buttonSizePx = with(density) { dimensionResource(DsR.dimen.zibe_btn_height).toPx() }
    val marginPx = with(density) { 8.dp.toPx() }
    val maxLeftOffset = -(screenWidthPx - buttonSizePx - marginPx).coerceAtLeast(0f)

    val onMicPressedUpdated by rememberUpdatedState(onMicPressed)
    val onMicMovedUpdated by rememberUpdatedState(onMicMoved)
    val onMicReleasedUpdated by rememberUpdatedState(onMicReleased)
    val onPressStateChangeUpdated by rememberUpdatedState(onPressStateChange)
    val onCenterPositionedUpdated by rememberUpdatedState(onCenterPositioned)
    val onPointerInWindowChangedUpdated by rememberUpdatedState(onPointerInWindowChanged)


    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                widthPx = coords.size.width.toFloat()

                val p = coords.positionInWindow()
                val center = Offset(
                    p.x + coords.size.width / 2f,
                    p.y + coords.size.height / 2f
                )

                buttonCenterInWindow = center
                onCenterPositionedUpdated(center)

                // Cuando no está presionado, fijamos el overlay al centro del botón (eje Y constante).
                if (!pressed) onPointerInWindowChangedUpdated(center)
            }
            .pointerInput(widthPx) {
                if (widthPx <= 0f) return@pointerInput

                awaitEachGesture {
                    val center = buttonCenterInWindow ?: return@awaitEachGesture

                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x

                    if (!pressed) {
                        pressed = true
                        onPressStateChangeUpdated(true)
                    }

                    onMicPressedUpdated()
                    onPointerInWindowChangedUpdated(center)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()

                        if (change.changedToUp() || !change.pressed) {
                            pressed = false
                            onPressStateChangeUpdated(false)
                            onMicReleasedUpdated()
                            onPointerInWindowChangedUpdated(center) // vuelve al eje original
                            break
                        }

                        // Mantener lógica legacy: coords locales del dedo (x puede ser negativo al salir del view)
                        onMicMovedUpdated(change.position.x, change.position.y, widthPx)

                        // Visual/overlay: SOLO desplazamiento horizontal (Y fijo)
                        val deltaX = (change.position.x - downX).coerceIn(maxLeftOffset, 0f)
                        onPointerInWindowChangedUpdated(
                            Offset(
                                x = center.x + deltaX,
                                y = center.y
                            )
                        )

                        change.consume()
                    }
                }
            }
    ) {
        ChatActionCircleButton(
            iconVector = Icons.Filled.Mic,
            onClick = {}
        )
    }
}

@Composable
fun MicRecordOverlay(
    isVisible: Boolean,
    pointerInWindow: Offset?,
    rootPositionInWindow: Offset
) {
    if (!isVisible || pointerInWindow == null) return

    val overlaySize = 200.dp
    val overlaySizePx = with(LocalDensity.current) { overlaySize.toPx() }
    val localCenter = pointerInWindow - rootPositionInWindow
    val topLeft = Offset(
        localCenter.x - overlaySizePx / 2f,
        localCenter.y - overlaySizePx / 2f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
            .zIndex(60f)
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        topLeft.x.roundToInt(),
                        topLeft.y.roundToInt()
                    )
                }
                .size(overlaySize),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LottieAnimationView(context).apply {
                        setAnimation(R.raw.lottie_recording)
                        repeatCount = LottieDrawable.INFINITE
                        clipToCompositionBounds = false
                        playAnimation()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MicRecordButtonPreview() {
    ZibeTheme {
        MicRecordButton(
            isRecording = false,
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {}
        )
    }
}

