package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeTextStyles
import com.zibete.proyecto1.ui.theme.ZibeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZibeBottomSheet(
    isOpen: Boolean,
    onCancel: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    showCancelButton: Boolean = true, // Added for flexibility in redesign
    content: @Composable ColumnScope.() -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
            sheetState = sheetState,
            containerColor = zibeColors.cardBackground.copy(alpha = 0.98f),
            tonalElevation = 0.dp,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = zibeColors.accent.copy(alpha = 0.4f))
            },
            modifier = Modifier,
            content = {
                val inputPadding = dimensionResource(R.dimen.zibe_input_padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = dimensionResource(R.dimen.screen_padding))
                        .padding(bottom = dimensionResource(R.dimen.element_spacing_xl)),
                    verticalArrangement = Arrangement.spacedBy(inputPadding),
                    content = content
                )
            }
        )
    }
}

@Composable
fun SheetHeader(
    title: String,
    subtitle: String? = null,
    titleStyle: TextStyle? = null,
    subtitleStyle: TextStyle? = null
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTextStyles = LocalZibeTextStyles.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = titleStyle ?: zibeTextStyles.brandSubtitle,
            color = zibeColors.lightText,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.element_spacing_xs)))

        subtitle?.let {
            Text(
                text = it,
                style = subtitleStyle ?: zibeTextStyles.body,
                color = zibeColors.hintText
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.element_spacing_medium)))
    }
}

@Composable
fun SheetActions(
    confirmText: String = stringResource(R.string.action_accept),
    cancelText: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmEnabled: Boolean = true,
    isConfirmLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(top = dimensionResource(R.dimen.element_spacing_small)),
        horizontalArrangement = Arrangement
            .spacedBy(dimensionResource(R.dimen.element_spacing_small))
    ) {
        // Botón Cancelar
        ZibeButtonSecondary(
            text = cancelText,
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            enabled = !isConfirmLoading
        )

        // Botón Principal
        ZibeButtonPrimary(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            enabled = confirmEnabled && !isConfirmLoading,
            isLoading = isConfirmLoading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ZibeBottomSheetPreviewField() {
    ZibeTheme {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        ZibeBottomSheet(
            isOpen = true,
            onCancel = {},
            content = {
                SheetHeader(
                    title = "Preview Title",
                    subtitle = "Podrás actualizar tu dirección de contacto. Tené en cuenta que te enviaremos un enlace de verificación para asegurar que la nueva cuenta te pertenece."
                )

                ZibeInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email"
                )

                ZibeInputPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    enabled = true,
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Main content of the bottom sheet goes here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(22.dp))
                SheetActions(
                    confirmText = "Eliminar cuenta",
                    onConfirm = {},
                    onCancel = {}
                )
            }
        )
    }
}
