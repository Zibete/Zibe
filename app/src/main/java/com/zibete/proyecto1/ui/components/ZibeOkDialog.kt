package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ZibeOkDialog(
    title: String,
    textContent: @Composable () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    enabled: Boolean = true
) {
    val zibeColors = LocalZibeExtendedColors.current

    AlertDialog(
        onDismissRequest = {
            // No hace nada: solo se cierra con OK
        },
        containerColor = zibeColors.snackbarSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = textContent,
        confirmButton = {
            TextButton(
                onClick = { if (enabled) onConfirm() },
                enabled = enabled
            ) {
                Text(confirmText)
            }
        }
    )
}
