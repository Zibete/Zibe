package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = LocalZibeExtendedColors.current.contentLightBg
) {
    val zibeColors = LocalZibeExtendedColors.current

    val accentColor = zibeColors.accent
    val hintColor = zibeColors.hintText
    val iconTint = zibeColors.hintText
    val textColor = zibeColors.lightText
    val errorColor = zibeColors.snackRed

    val inputPadding = dimensionResource(R.dimen.zibe_input_padding)
    val inputElevation = dimensionResource(R.dimen.zibe_input_elevation)

    val shape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.zibe_input_corner_top),
        topEnd = dimensionResource(R.dimen.zibe_input_corner_top),
        bottomStart = dimensionResource(R.dimen.zibe_input_corner_bottom),
        bottomEnd = dimensionResource(R.dimen.zibe_input_corner_bottom)
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = inputPadding)
            .shadow(inputElevation, shape)
            .background(containerColor, shape),
        singleLine = singleLine,
        label = { Text(text = label, color = hintColor) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        enabled = enabled,

        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = accentColor,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = errorColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedLabelColor = hintColor,
            unfocusedLabelColor = hintColor,
            focusedLeadingIconColor = iconTint,
            unfocusedLeadingIconColor = iconTint,
            focusedTrailingIconColor = iconTint,
            unfocusedTrailingIconColor = iconTint,
            cursorColor = accentColor
        ),

        visualTransformation = visualTransformation,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions.Default,
    )
}

@Composable
fun ZibeInputPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    containerColor: Color = LocalZibeExtendedColors.current.contentLightBg
) {

    ZibeInputField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        containerColor = containerColor,
        label = label,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.PASSWORD),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_lock_24),
                contentDescription = stringResource(id = R.string.password)
            )
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    painter = painterResource(
                        id = if (visible)
                            R.drawable.ic_baseline_visibility_24
                        else
                            R.drawable.ic_baseline_visibility_off_24
                    ),
                    contentDescription = stringResource(id = R.string.password)
                )
            }
        },
        visualTransformation = if (visible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true
    )
}

@Composable
fun ZibeInputFieldDark(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    ZibeInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        enabled = enabled,
        containerColor = LocalZibeExtendedColors.current.contentDarkBg
    )
}

@Composable
fun ZibeInputPasswordFieldDark(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    visible: Boolean,
    onToggleVisible: () -> Unit
) {
    ZibeInputPasswordField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        enabled = enabled,
        visible = visible,
        onToggleVisible = onToggleVisible,
        containerColor = LocalZibeExtendedColors.current.contentDarkBg
    )
}

@Preview(showBackground = true, name = "ZibeInputField Preview")
@Composable
fun ZibeInputFieldPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZibeInputField(
                value = "",
                onValueChange = {},
                label = "Empty Field"
            )
            ZibeInputField(
                value = "Input text",
                onValueChange = {},
                label = "Field with text"
            )
        }
    }
}

@Preview(showBackground = true, name = "ZibeInputPasswordField Preview")
@Composable
fun ZibeInputPasswordFieldPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZibeInputPasswordField(
                value = "password123",
                onValueChange = {},
                label = "Password Hidden",
                enabled = true,
                visible = false,
                onToggleVisible = {}
            )
            ZibeInputPasswordField(
                value = "password123",
                onValueChange = {},
                label = "Password Visible",
                enabled = true,
                visible = true,
                onToggleVisible = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "ZibeInputFieldDark Preview")
@Composable
fun ZibeInputFieldDarkPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZibeInputFieldDark(
                value = "",
                onValueChange = {},
                label = "Dark Empty Field"
            )
            ZibeInputFieldDark(
                value = "Input text",
                onValueChange = {},
                label = "Dark Field with text"
            )
        }
    }
}

@Preview(showBackground = true, name = "ZibeInputPasswordFieldDark Preview")
@Composable
fun ZibeInputPasswordFieldDarkPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZibeInputPasswordFieldDark(
                value = "password123",
                onValueChange = {},
                label = "Dark Password Hidden",
                enabled = true,
                visible = false,
                onToggleVisible = {}
            )
            ZibeInputPasswordFieldDark(
                value = "password123",
                onValueChange = {},
                label = "Dark Password Visible",
                enabled = true,
                visible = true,
                onToggleVisible = {}
            )
        }
    }
}
