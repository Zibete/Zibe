package com.zibete.proyecto1.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZibeBirthDatePickerDialog(
    isOpen: Boolean,
    initialIso: String?,
    onDismiss: () -> Unit,
    onConfirmIso: (String) -> Unit,
    showModeToggle: Boolean = true
) {
    if (!isOpen) return

    val zibeTypography = LocalZibeTypography.current
    val initialMillis = remember(initialIso) {
        val parsed = if (initialIso.isNullOrBlank()) {
            null
        } else {
            runCatching { Instant.parse(initialIso) }.getOrNull()
        }
        (parsed ?: Instant.now()).toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Instant.now().toEpochMilli()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        onConfirmIso(millisToIso(ms))
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.action_accept),
                    style = zibeTypography.label
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = zibeTypography.label
                )
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = showModeToggle
        )
    }
}
