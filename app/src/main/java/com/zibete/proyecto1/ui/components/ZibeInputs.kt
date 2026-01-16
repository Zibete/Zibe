package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.VisualTransformation
import com.zibete.proyecto1.R

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

    enabled: Boolean = true
) {
    val zibeColors = LocalZibeExtendedColors.current

    val containerColor = zibeColors.inputBackground
    val accentColor = zibeColors.border
    val hintColor = zibeColors.hintText
    val iconTint = zibeColors.hintText
    val textColor = zibeColors.lightText
    val errorColor = zibeColors.zibeRed
    val inputBackground = zibeColors.inputBackground

    val inputPadding = dimensionResource(R.dimen.zibe_input_padding)
    val inputElevation = dimensionResource(R.dimen.zibe_input_elevation)
    val inputCornerTop = dimensionResource(R.dimen.zibe_input_corner_top)
    val inputCornerBottom = dimensionResource(R.dimen.zibe_input_corner_bottom)

    Box(
        modifier = Modifier
            .padding(bottom = inputPadding)
            .fillMaxWidth()
            .shadow(inputElevation)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = singleLine,
            label = { Text(text = label, color = hintColor) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(
                topStart = inputCornerTop,
                topEnd = inputCornerTop,
                bottomStart = inputCornerBottom,
                bottomEnd = inputCornerBottom
            ),

            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedIndicatorColor = accentColor,
                unfocusedIndicatorColor = containerColor,
                disabledIndicatorColor = inputBackground,
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
}
