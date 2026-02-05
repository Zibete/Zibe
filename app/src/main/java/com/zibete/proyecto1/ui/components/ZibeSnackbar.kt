package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme

enum class ZibeSnackType { SUCCESS, ERROR, WARNING, INFO }

class ZibeSnackVisuals(
    override val message: String,
    val type: ZibeSnackType,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

suspend fun SnackbarHostState.showZibeMessage(
    message: String,
    snackType: ZibeSnackType = ZibeSnackType.INFO,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(
        ZibeSnackVisuals(
            message = message,
            type = snackType,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration
        )
    )
}

@Composable
private fun ZibeSnackPill(
    type: ZibeSnackType,
    message: String,
    actionLabel: String?,
    withDismissAction: Boolean,
    onAction: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val zibeColors = LocalZibeExtendedColors.current

    val (icon, iconTint) = remember(type) {
        when (type) {
            ZibeSnackType.SUCCESS -> Icons.Filled.CheckCircle to zibeColors.snackGreen
            ZibeSnackType.ERROR -> Icons.Filled.Cancel to zibeColors.snackRed
            ZibeSnackType.WARNING -> Icons.Filled.Warning to zibeColors.snackYellow
            ZibeSnackType.INFO -> Icons.Filled.Info to zibeColors.snackBlue
        }
    }

    val shape = RoundedCornerShape(24.dp)

    Surface(
        color = zibeColors.snackBackground,
        contentColor = zibeColors.lightText,
        shape = shape,
        modifier = modifier
            .padding(all = 4.dp)
            .shadow(elevation = 10.dp, shape = shape)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 560.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.width(10.dp))
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = LocalZibeTypography.current.actionLabel,
                        color = zibeColors.snackAction,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (withDismissAction && onDismiss != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = zibeColors.lightText.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZibeSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data: SnackbarData ->
        val visuals = data.visuals as? ZibeSnackVisuals
            ?: ZibeSnackVisuals(message = data.visuals.message, type = ZibeSnackType.INFO)

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                value == SwipeToDismissBoxValue.EndToStart ||
                        value == SwipeToDismissBoxValue.StartToEnd
            }
        )

        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
        }

        // ✅ centrado horizontal, ancho se ajusta al contenido
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                content = {
                    ZibeSnackPill(
                        type = visuals.type,
                        message = visuals.message,
                        actionLabel = visuals.actionLabel,
                        withDismissAction = visuals.withDismissAction,
                        onAction = {
                            data.performAction()
                        },
                        onDismiss = {
                            data.dismiss()
                        }
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ZibeSnackbarAllCasesStackedPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZibeSnackPill(
                type = ZibeSnackType.SUCCESS,
                message = "Perfil actualizado",
                actionLabel = null,
                withDismissAction = false,
                onAction = null,
                onDismiss = null
            )

            ZibeSnackPill(
                type = ZibeSnackType.ERROR,
                message = "No se pudo guardar. Reintentá.",
                actionLabel = "REINTENTAR",
                withDismissAction = true,
                onAction = {},
                onDismiss = {}
            )

            ZibeSnackPill(
                type = ZibeSnackType.WARNING,
                message = "Conexión inestable",
                actionLabel = null,
                withDismissAction = false,
                onAction = null,
                onDismiss = null
            )

            ZibeSnackPill(
                type = ZibeSnackType.INFO,
                message = "Chat oculto",
                actionLabel = "DESHACER",
                withDismissAction = false,
                onAction = {},
                onDismiss = null
            )

            ZibeSnackPill(
                type = ZibeSnackType.WARNING,
                message = "Acción pendiente: se requiere confirmación",
                actionLabel = "VER",
                withDismissAction = true,
                onAction = {},
                onDismiss = {}
            )
        }
    }
}

