package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.core.designsystem.R as DsR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme


@Composable
fun ZibeCollapsingFabStack(
    modifier: Modifier = Modifier,
    collapsed: Boolean,
    primaryText: @Composable () -> Unit,
    primaryIcon: @Composable () -> Unit,
    primaryEnabled: Boolean,
    primaryLoading: Boolean,
    onPrimaryClick: () -> Unit,
    secondaryText: (@Composable () -> Unit)? = null,
    secondaryIcon: (@Composable () -> Unit)? = null,
    secondaryEnabled: Boolean = true,
    onSecondaryClick: (() -> Unit)? = null,
    onHeightPxChanged: (Int) -> Unit = {}
) {
    val spacing = dimensionResource(DsR.dimen.element_spacing_medium)

    val secondaryAvailable =
        secondaryText != null && secondaryIcon != null && onSecondaryClick != null

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .wrapContentWidth(Alignment.End),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.onSizeChanged { onHeightPxChanged(it.height) }
        ) {
            if (secondaryAvailable) {
                ZibeSecondaryFAB(
                    text = secondaryText,
                    icon = secondaryIcon,
                    onClick = onSecondaryClick,
                    enabled = secondaryEnabled,
                    expanded = !collapsed
                )
                Spacer(modifier = Modifier.height(spacing))
            }

            ZibePrimaryFAB(
                text = primaryText,
                icon = primaryIcon,
                onClick = onPrimaryClick,
                enabled = primaryEnabled,
                isLoading = primaryLoading,
                expanded = !collapsed
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeCollapsingFabStackPreview() {
    ZibeTheme {
        ZibeCollapsingFabStack(
            collapsed = false,
            primaryText = { Text("Guardar Cambios") },
            primaryIcon = { Icon(Icons.Default.Save, null) },
            primaryEnabled = true,
            primaryLoading = false,
            onPrimaryClick = {},
            secondaryText = { Text("Añadir Foto") },
            secondaryIcon = { Icon(Icons.Default.Add, null) },
            onSecondaryClick = {}
        )
    }
}


