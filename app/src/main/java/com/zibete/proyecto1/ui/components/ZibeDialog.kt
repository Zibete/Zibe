package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zibete.proyecto1.R

@Composable
fun ZibeDialog(
    title: String,
    textContent: @Composable () -> Unit,
    confirmText: String = stringResource(R.string.action_accept),
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.action_cancel),
    onDismiss: () -> Unit,
    enabled: Boolean = true,
    confirmEnabled: Boolean = true // <-- Nuevo parámetro
) {
    val zibeColors = LocalZibeExtendedColors.current

    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = zibeColors.snackbarSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall)
                },
        text = textContent,
        confirmButton = {
            TextButton(
                onClick = { onConfirm() },
                enabled = enabled && confirmEnabled
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = enabled
            ) {
                Text(dismissText)
            }
        }
    )
}


