package com.zibete.proyecto1.ui.users
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun PrivacySettingsDialogContent(
    isOnlineVisible: Boolean,
    onOnlineVisibleChanged: (Boolean) -> Unit,
    isAgeVisible: Boolean,
    onAgeVisibleChanged: (Boolean) -> Unit,
    ageRange: ClosedFloatingPointRange<Float> = 18f..50f,
    onAgeRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // === "¿Quién puede ver que estoy en línea?" ===
        PrivacySwitchRow(
            text = stringResource(R.string.online_upercase),
            checked = isOnlineVisible,
            onCheckedChange = onOnlineVisibleChanged
        )

        // === "¿Quién puede ver mi edad?" ===
        PrivacySwitchRow(
            text = stringResource(R.string.Edad),
            checked = isAgeVisible,
            onCheckedChange = onAgeVisibleChanged
        )

        // === Rango de edad (solo visible si el switch de arriba está activado) ===
        AnimatedVisibility(visible = isAgeVisible) {
            AgeRangeSelector(
                selectedRange = ageRange,
                onRangeChange = onAgeRangeChanged
            )
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall, // 18sp SemiBold Poppins
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,      // Rosa F6286D
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFFA3B1C6),
                uncheckedTrackColor = Color(0xFF1E2A38),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AgeRangeSelector(
    selectedRange: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val start = selectedRange.start.toInt()
    val end = selectedRange.endInclusive.toInt()

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Spinner de edad mínima
        AgeDropdown(
            selectedAge = start,
            onAgeSelected = { newStart ->
                onRangeChange(newStart.coerceAtMost(end).toFloat()..selectedRange.endInclusive)
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "–",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )

        // Spinner de edad máxima
        AgeDropdown(
            selectedAge = end,
            onAgeSelected = { newEnd ->
                onRangeChange(selectedRange.start..newEnd.coerceAtLeast(start).toFloat())
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeDropdown(
    selectedAge: Int,
    onAgeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedAge.toString(),
            onValueChange = { },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF233042),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .menuAnchor()
                .height(56.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .exposedDropdownSize()
        ) {
            (16..80).forEach { age ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = age.toString(),
                            color = if (age == selectedAge) MaterialTheme.colorScheme.primary else Color.White
                        )
                    },
                    onClick = {
                        onAgeSelected(age)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Preview para que veas cómo queda exactamente con tu tema
@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
fun PrivacySettingsPreview() {
    ZibeTheme {
        Surface(color = Color(0xFF0D1B2A)) {
            PrivacySettingsDialogContent(
                isOnlineVisible = true,
                onOnlineVisibleChanged = {},
                isAgeVisible = true,
                onAgeVisibleChanged = {},
                ageRange = 20f..45f,
                onAgeRangeChanged = {}
            )
        }
    }
}