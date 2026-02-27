package com.zibete.proyecto1.ui.chat.components

import com.zibete.proyecto1.core.designsystem.R as DsR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ChatMessageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val zibeColors = LocalZibeExtendedColors.current
    val accentColor = zibeColors.accent
    val hintColor = zibeColors.hintText
    val textColor = zibeColors.lightText
    val containerColor = zibeColors.contentDarkBg
    val inputElevation = dimensionResource(DsR.dimen.zibe_input_elevation)
    val chatComponentsHeight = dimensionResource(DsR.dimen.zibe_btn_height)
    val horizontalPadding = 12.dp

    val shape = RoundedCornerShape(
        topStart = dimensionResource(DsR.dimen.zibe_input_corner_top),
        topEnd = dimensionResource(DsR.dimen.zibe_input_corner_top),
        bottomStart = dimensionResource(DsR.dimen.zibe_input_corner_top),
        bottomEnd = dimensionResource(DsR.dimen.zibe_input_corner_top)
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(chatComponentsHeight)
            .shadow(inputElevation, shape)
            .background(containerColor, shape),
        textStyle = LocalZibeTypography.current.body.copy(color = textColor),
        cursorBrush = SolidColor(accentColor),
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        keyboardActions = KeyboardActions.Default,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = hintColor)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChatMessageTextFieldPreview() {
    ZibeTheme {
        ChatMessageTextField(
            value = "",
            onValueChange = {},
            placeholder = "Type a message..."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatMessageTextFieldWithValuePreview() {
    ZibeTheme {
        ChatMessageTextField(
            value = "Hello, how are you?",
            onValueChange = {},
            placeholder = "Type a message..."
        )
    }
}


