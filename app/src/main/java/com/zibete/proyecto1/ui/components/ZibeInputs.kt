package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch

@Composable
fun ZibeInputField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    containerColor: Color = LocalZibeExtendedColors.current.contentLightBg,
    showErrorUnderneath: Boolean = true
) {
    val zibeColors = LocalZibeExtendedColors.current

    val accentColor = zibeColors.accent
    val hintColor = zibeColors.hintText
    val iconTint = zibeColors.hintText
    val textColor = zibeColors.lightText
    val errorColor = zibeColors.snackRed

    val inputElevation = dimensionResource(R.dimen.zibe_input_elevation)

    val shape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.zibe_input_corner_top),
        topEnd = dimensionResource(R.dimen.zibe_input_corner_top),
        bottomStart = dimensionResource(R.dimen.zibe_input_corner_top),
        bottomEnd = dimensionResource(R.dimen.zibe_input_corner_top)
    )
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .shadow(inputElevation, shape)
                .background(containerColor, shape),
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
            singleLine = singleLine,
            label = { Text(text = label, color = hintColor) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = shape,
            enabled = enabled,
            isError = error != null,
            visualTransformation = visualTransformation,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions.Default,
        )
        if (showErrorUnderneath && error != null) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 0.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerColor.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = error,
                    color = errorColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ZibeInputPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
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
        error = error,
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
    enabled: Boolean = true,
    error: String? = null,
    showErrorUnderneath: Boolean = true
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
        error = error,
        containerColor = LocalZibeExtendedColors.current.contentDarkBg,
        showErrorUnderneath = showErrorUnderneath
    )
}

@Composable
fun ZibeInputPasswordFieldDark(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    error: String? = null
) {
    ZibeInputPasswordField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        enabled = enabled,
        visible = visible,
        onToggleVisible = onToggleVisible,
        error = error,
        containerColor = LocalZibeExtendedColors.current.contentDarkBg
    )
}

@Composable
fun ZibeInputBirthdate(
    value: String,
    age: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.birth_date),
    error: String? = null,
    enabled: Boolean = true,
    containerColor: Color = LocalZibeExtendedColors.current.contentLightBg
) {
    val zibeColors = LocalZibeExtendedColors.current
    val errorColor = zibeColors.snackRed
    val inputPadding = dimensionResource(R.dimen.zibe_input_padding)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(inputPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ZibeInputField(
                    value = value,
                    onValueChange = { },
                    label = label,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.EditCalendar,
                            contentDescription = stringResource(R.string.content_description_edit_birthdate)
                        )
                    },
                    enabled = enabled,
                    readOnly = true,
                    error = error,
                    showErrorUnderneath = false,
                    containerColor = containerColor
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag(TestTags.BIRTHDATE_PICKER)
                        .clickable(enabled = enabled) { onClick() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.birth_date),
                        tint = zibeColors.hintText,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = dimensionResource(R.dimen.element_spacing_small))
                    )
                }
            }

            if (value.isNotBlank() && age.isNotBlank()) {
                ZibeInputField(
                    value = age,
                    onValueChange = {},
                    label = stringResource(id = R.string.age),
                    enabled = false,
                    modifier = Modifier.width(80.dp),
                    containerColor = containerColor
                )
            }
        }

        if (error != null) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerColor.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = error,
                    color = errorColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ZibeInputBirthdateDark(
    value: String,
    age: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.birth_date),
    error: String? = null,
    enabled: Boolean = true,
    containerColor: Color = LocalZibeExtendedColors.current.contentDarkBg
) {
    ZibeInputBirthdate(
        value = value,
        age = age,
        onClick = onClick,
        modifier = modifier,
        label = label,
        error = error,
        enabled = enabled,
        containerColor = containerColor
    )
}

@Composable
fun ZibeAboutField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxChars: Int = 280,
    error: String? = null,
    showCounter: Boolean = true,
    testTag: String? = null,
    minHeight: Dp = 120.dp,
    maxHeight: Dp = 240.dp,
    initialHeight: Dp = 140.dp,
    resizable: Boolean = true,
    bringIntoViewOnFocus: Boolean = true,
    bringIntoViewExtraBottom: Dp = 10.dp,
    leadingIcon: (@Composable () -> Unit)? = {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_edit_24),
            contentDescription = label
        )
    },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        capitalization = KeyboardCapitalization.Sentences,
        autoCorrectEnabled = true,
        keyboardType = KeyboardType.Text
    ),
    containerColor: Color = LocalZibeExtendedColors.current.contentDarkBg
) {
    val zibeColors = LocalZibeExtendedColors.current

    val hintColor = zibeColors.hintText
    val errorColor = zibeColors.snackRed

    val scope = rememberCoroutineScope()
    val bringIntoViewRequester =
        remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val extraBottomPx = with(LocalDensity.current) { bringIntoViewExtraBottom.toPx() }

    var heightDpValue by rememberSaveable(resizable) { mutableFloatStateOf(initialHeight.value) }
    val height: Dp = heightDpValue.dp.coerceIn(minHeight, maxHeight)

    Column(modifier = modifier.fillMaxWidth()) {

        Box(modifier = Modifier.fillMaxWidth()) {

            ZibeInputField(
                value = value,
                onValueChange = { newText ->
                    val next = if (newText.length > maxChars) newText.take(maxChars) else newText
                    if (next != value) onValueChange(next)
                },
                label = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                    .onGloballyPositioned { coords -> fieldSize = coords.size }
                    .then(
                        if (bringIntoViewOnFocus) {
                            Modifier
                                .onFocusEvent { st ->
                                    if (st.isFocused) {
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView(
                                                Rect(
                                                    left = 0f,
                                                    top = 0f,
                                                    right = fieldSize.width.toFloat(),
                                                    bottom = fieldSize.height.toFloat() + extraBottomPx
                                                )
                                            )
                                        }
                                    }
                                }
                                .bringIntoViewRequester(bringIntoViewRequester)
                        } else Modifier
                    ),
                singleLine = false,
                leadingIcon = leadingIcon,
                keyboardOptions = keyboardOptions,
                enabled = enabled,
                error = null,
                containerColor = containerColor
            )

            if (resizable) {
                val density = LocalDensity.current
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dyDp = with(density) { dragAmount.y.toDp() }
                                heightDpValue = (heightDpValue + dyDp.value)
                                    .coerceIn(minHeight.value, maxHeight.value)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = hintColor.copy(alpha = 0.85f)
                    )
                }
            }
        }

        val showBottomRow = showCounter || error != null
        if (showBottomRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (error != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerColor.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = error,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Counter
                if (showCounter) {
                    Text(
                        text = "${value.length}/$maxChars",
                        style = MaterialTheme.typography.bodySmall,
                        color = hintColor,
                        modifier = Modifier.padding(start = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Preview(name = "Dark Inputs on Gradient", showBackground = true)
@Composable
fun ZibeInputsDarkOnGradientPreview() {
    ZibeTheme {
        val zibeColors = LocalZibeExtendedColors.current
        val inputSpacing = dimensionResource(R.dimen.zibe_input_padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(zibeColors.gradientZibe)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(inputSpacing)
        ) {
            ZibeInputFieldDark(value = "", onValueChange = {}, label = "Dark Empty")
            ZibeInputFieldDark(value = "Input text", onValueChange = {}, label = "Dark Filled")
            ZibeInputFieldDark(
                value = "Invalid input",
                onValueChange = {},
                label = "Dark Error",
                error = "Error message"
            )

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

            ZibeInputBirthdateDark(
                value = "10/05/1990",
                age = "34",
                onClick = {},
                error = "Must be over 18"
            )
        }
    }
}

@Preview(name = "Light Inputs on Surface", showBackground = true)
@Composable
fun ZibeInputsLightOnSurfacePreview() {
    ZibeTheme {
        val inputSpacing = dimensionResource(R.dimen.zibe_input_padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(inputSpacing)
        ) {
            ZibeInputField(value = "", onValueChange = {}, label = "Light Empty")
            ZibeInputField(value = "Input text", onValueChange = {}, label = "Light Filled")
            ZibeInputField(
                value = "Invalid input",
                onValueChange = {},
                label = "Light Error",
                error = "Error message"
            )

            ZibeInputPasswordField(
                value = "password123",
                onValueChange = {},
                label = "Light Password Hidden",
                enabled = true,
                visible = false,
                onToggleVisible = {}
            )
            ZibeInputPasswordField(
                value = "password123",
                onValueChange = {},
                label = "Light Password Visible",
                enabled = true,
                visible = true,
                onToggleVisible = {}
            )

            ZibeInputBirthdate(
                value = "10/05/1990",
                age = "34",
                onClick = {},
                error = "Must be over 18"
            )
        }
    }
}

@Preview(name = "About Field Preview", showBackground = true)
@Composable
fun ZibeAboutFieldPreview() {
    ZibeTheme {
        val zibeColors = LocalZibeExtendedColors.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(zibeColors.gradientZibe)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZibeAboutField(
                value = "",
                onValueChange = {},
                label = "About Me (Empty)"
            )
            ZibeAboutField(
                value = "This is a sample bio text to show how it looks when it is filled with some information.",
                onValueChange = {},
                label = "About Me (Filled)"
            )
            ZibeAboutField(
                value = "Some text",
                onValueChange = {},
                label = "About Me (Error)",
                error = "Something went wrong went wrong went wrong went wrong"
            )
            ZibeAboutField(
                value = "You can resize this field using the handle at the bottom right.",
                onValueChange = {},
                label = "About Me (Resizable)",
                resizable = true
            )
        }
    }
}