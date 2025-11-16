package com.zibete.proyecto1.ui.components
// --- NUEVOS IMPORTS CORREGIDOS ---
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue // CAMBIADO
import androidx.compose.material3.rememberSwipeToDismissBoxState // CAMBIADO

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

enum class ZibeSnackType { SUCCESS, ERROR, WARNING, INFO }

// Helper para mostrar mensajes tipados
suspend fun SnackbarHostState.showZibeMessage(
    type: ZibeSnackType,
    message: String
) {
    currentSnackbarData?.dismiss()
    // Encodeamos el tipo en el texto para que el host lo pueda leer
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
fun ZibeSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val zibeColors = LocalZibeExtendedColors.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState = hostState) { data ->
            // 1. Crear estado de dismiss
            val dismissState = rememberSwipeToDismissBoxState(
                // 2. Solo confirmamos el cambio, NO llamamos a dismiss()
                confirmValueChange = {
                    // Permitimos que el estado cambie si el usuario deslizó lo suficiente
                    it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart
                }
            )

            // Observamos cuando el valor *actual* del estado (después de la animación) cambia a un estado descartado.
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                ) {
                    // Aquí sí llamamos a dismiss() para notificar al SnackbarHostState
                    data.dismiss()
                }
            }

            // 1) Decodificar tipo desde el mensaje
            val raw = data.visuals.message
            val (type, cleanMsg) = when {
                raw.startsWith("[success]") -> ZibeSnackType.SUCCESS to raw.removePrefix("[success]")
                raw.startsWith("[error]")   -> ZibeSnackType.ERROR   to raw.removePrefix("[error]")
                raw.startsWith("[warning]") -> ZibeSnackType.WARNING to raw.removePrefix("[warning]")
                raw.startsWith("[info]")    -> ZibeSnackType.INFO    to raw.removePrefix("[info]")
                else                        -> ZibeSnackType.INFO    to raw
            }

            // 2) Colores + ícono según tipo
            val (iconColor, iconRes) = when (type) {
                ZibeSnackType.SUCCESS -> zibeColors.zibeGreen to R.drawable.ic_check_24
                ZibeSnackType.ERROR   -> zibeColors.zibeRed to R.drawable.ic_baseline_cancel_24
                ZibeSnackType.WARNING -> zibeColors.zibeYellow to R.drawable.ic_warning_24
                ZibeSnackType.INFO    -> zibeColors.zibeBlue to R.drawable.ic_info_24
            }

            // 3. Snackbar
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    // Vacío
                },
                content = {

                    Snackbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(10.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = zibeColors.snackbarSurface,
                        contentColor = zibeColors.mutedText
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = iconColor
                            )
                            Text(
                                text = cleanMsg.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}
