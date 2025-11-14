package com.zibete.proyecto1.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

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
    readOnly: Boolean = false
)
 {
     val zibeColors = LocalZibeExtendedColors.current

     val containerColor = zibeColors.inputBackground
     val borderColor = zibeColors.border
     val hintColor = zibeColors.mutedText
     val iconTint = zibeColors.mutedText
     val textColor = Color.White

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .shadow(8.dp)
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
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),

            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedIndicatorColor = borderColor,
                unfocusedIndicatorColor = borderColor,
                disabledIndicatorColor = borderColor,
                errorIndicatorColor = borderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = hintColor,
                unfocusedLabelColor = hintColor,
                focusedLeadingIconColor = iconTint,
                unfocusedLeadingIconColor = iconTint,
                focusedTrailingIconColor = iconTint,
                unfocusedTrailingIconColor = iconTint,
                cursorColor = borderColor
            ),

            visualTransformation = visualTransformation,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions.Default,
        )
    }
}
