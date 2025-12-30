package com.zibete.proyecto1.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.zibete.proyecto1.ui.constants.DIALOG_OK
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

@Composable
fun ZibeOkDialog(
    title: String,
    textContent: @Composable () -> Unit,
    confirmText: String = DIALOG_OK,
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
