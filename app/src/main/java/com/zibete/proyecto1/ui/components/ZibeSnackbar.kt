package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

enum class ZibeSnackType { SUCCESS, ERROR, WARNING, INFO }

// Helper para mostrar mensajes tipados
suspend fun SnackbarHostState.showZibeMessage(
    type: ZibeSnackType,
    message: String
) {
    currentSnackbarData?.dismiss()
    val prefix = when (type) {
        ZibeSnackType.SUCCESS -> "[success]"
        ZibeSnackType.ERROR   -> "[error]"
        ZibeSnackType.WARNING -> "[warning]"
        ZibeSnackType.INFO    -> "[info]"
    }
    showSnackbar(prefix + message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZibeSnackHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val zibeColors = LocalZibeExtendedColors.current

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        SnackbarHost(hostState = hostState) { data ->

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    it == SwipeToDismissBoxValue.StartToEnd ||
                            it == SwipeToDismissBoxValue.EndToStart
                }
            )

            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                ) {
                    data.dismiss()
                }
            }

            // Decodificar tipo
            val raw = data.visuals.message
            val (type, cleanMsg) = when {
                raw.startsWith("[success]") -> ZibeSnackType.SUCCESS to raw.removePrefix("[success]")
                raw.startsWith("[error]")   -> ZibeSnackType.ERROR   to raw.removePrefix("[error]")
                raw.startsWith("[warning]") -> ZibeSnackType.WARNING to raw.removePrefix("[warning]")
                raw.startsWith("[info]")    -> ZibeSnackType.INFO    to raw.removePrefix("[info]")
                else                        -> ZibeSnackType.INFO    to raw
            }

            val (iconColor, iconRes) = when (type) {
                ZibeSnackType.SUCCESS -> zibeColors.zibeGreen to R.drawable.ic_check_24
                ZibeSnackType.ERROR   -> zibeColors.zibeRed   to R.drawable.ic_baseline_cancel_24
                ZibeSnackType.WARNING -> zibeColors.zibeYellow to R.drawable.ic_warning_24
                ZibeSnackType.INFO    -> zibeColors.zibeBlue  to R.drawable.ic_info_24
            }

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
                backgroundContent = { /* sin fondo extra */ },
                content = {
                    Snackbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.zibe_snack_padding_horizontal),
                                vertical = dimensionResource(R.dimen.zibe_snack_padding_vertical)
                            )
                            .shadow(
                                elevation = dimensionResource(R.dimen.zibe_snack_shadow_elevation),
                                shape = RoundedCornerShape(dimensionResource(R.dimen.zibe_snack_radius))
                            ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.zibe_snack_radius)),
                        containerColor = zibeColors.snackbarSurface,
                        contentColor = zibeColors.hintText
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = iconColor
                                )
                                Text(
                                    text = cleanMsg.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(
                                        start = dimensionResource(R.dimen.zibe_snack_padding_text)
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeSnackHostSuccessPreview() {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        hostState.showSnackbar("[success] Operación completada con éxito")
    }
    ZibeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ZibeSnackHost(hostState = hostState)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeSnackHostErrorPreview() {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        hostState.showSnackbar("[error] Ha ocurrido un error inesperado")
    }
    ZibeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ZibeSnackHost(hostState = hostState)
        }
    }
}