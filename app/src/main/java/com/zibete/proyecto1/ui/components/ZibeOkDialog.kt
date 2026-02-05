package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibeMessageDialog(
    title: String,
    textContent: @Composable () -> Unit,
    confirmText: String = stringResource(R.string.action_accept),
    onConfirm: () -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current

    AlertDialog(
        onDismissRequest = { /* Empty */ },
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
                onClick = { onConfirm() }
            ) {
                Text(confirmText)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ZibeMessageDialogPreview() {
    ZibeTheme {
        ZibeMessageDialog(
            title = "Importante",
            textContent = {
                Text(
                    text = "Este es un mensaje de prueba para el diálogo de confirmación.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmText = "Aceptar",
            onConfirm = {}
        )
    }
}
