package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.unit.dp
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
fun ZibeSnackbar(
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
                ZibeSnackType.SUCCESS -> zibeColors.snackGreen to R.drawable.ic_check_24
                ZibeSnackType.ERROR   -> zibeColors.snackRed   to R.drawable.ic_baseline_cancel_24
                ZibeSnackType.WARNING -> zibeColors.snackYellow to R.drawable.ic_warning_24
                ZibeSnackType.INFO    -> zibeColors.snackBlue  to R.drawable.ic_info_24
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
                            .wrapContentHeight() // Asegura que se estire según el contenido
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight() // Se estira verticalmente
                                .padding(vertical = 4.dp) // Espaciado interno extra para textos largos
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text(
                                text = cleanMsg.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = dimensionResource(R.dimen.zibe_snack_padding_text))
                                    .fillMaxWidth() // Permite que el texto use todo el ancho y haga wrap
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SnackbarItemPreview(type: ZibeSnackType, message: String) {
    val zibeColors = LocalZibeExtendedColors.current
    val (iconColor, iconRes) = when (type) {
        ZibeSnackType.SUCCESS -> zibeColors.snackGreen to R.drawable.ic_check_24
        ZibeSnackType.ERROR   -> zibeColors.snackRed   to R.drawable.ic_baseline_cancel_24
        ZibeSnackType.WARNING -> zibeColors.snackYellow to R.drawable.ic_warning_24
        ZibeSnackType.INFO    -> zibeColors.snackBlue  to R.drawable.ic_info_24
    }

    Snackbar(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        containerColor = zibeColors.snackbarSurface,
        contentColor = zibeColors.hintText
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconColor)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp).fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeSnackbarShowcasePreview() {
    ZibeTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SnackbarItemPreview(ZibeSnackType.SUCCESS, "Operación completada con éxito")
            SnackbarItemPreview(ZibeSnackType.ERROR, "Error: No se pudo conectar al servidor")
            SnackbarItemPreview(ZibeSnackType.WARNING, "Advertencia: Tu sesión expirará pronto")
            SnackbarItemPreview(ZibeSnackType.INFO, "Información: Nueva actualización disponible")
            SnackbarItemPreview(ZibeSnackType.ERROR, "Este es un mensaje de error extremadamente largo que debería forzar al componente a estirarse verticalmente para demostrar que el texto no se corta y que el icono permanece centrado correctamente.")
            SnackbarItemPreview(ZibeSnackType.SUCCESS, "¡Bien hecho! Has configurado todo correctamente y ahora puedes disfrutar de todas las funcionalidades de Zibe sin interrupciones.")
        }
    }
}
