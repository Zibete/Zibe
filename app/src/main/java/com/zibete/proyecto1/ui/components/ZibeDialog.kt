package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibeDialog(
    title: String,
    content: @Composable () -> Unit,
    confirmText: String = stringResource(R.string.action_accept),
    cancelText: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean = true,
    confirmEnabled: Boolean = true
) {
    val zibeColors = LocalZibeExtendedColors.current

    AlertDialog(
        onDismissRequest = { if (enabled) onCancel() },
        containerColor = zibeColors.snackbarSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall)
                },
        text = content,
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
                onClick = { onCancel() },
                enabled = enabled
            ) {
                Text(cancelText)
            }
        }
    )
}

@Preview(showBackground = false)
@Composable
fun ZibeDialogPreview() {
    ZibeTheme {
        ZibeDialog(
            title = stringResource(R.string.reset_password_title),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_medium))) {
                    Text(
                        text = stringResource(R.string.reset_password_content)
                    )
                    ZibeInputField(
                        value = "",
                        onValueChange = {},
                        label = "Empty Field"
                    )
                }
            },
            onConfirm = {},
            onCancel = {}
        )
    }
}
