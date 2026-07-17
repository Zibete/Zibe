package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstContactSheet(
    isOpen: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onCancel
    ) {
        Column(
            modifier = Modifier.testTag(FIRST_DM_SHEET),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetHeader(
                title = stringResource(R.string.discover_first_dm_title),
                subtitle = stringResource(R.string.discover_first_dm_message)
            )
            SheetActions(
                confirmText = stringResource(R.string.discover_first_dm_confirm),
                cancelText = stringResource(R.string.action_cancel),
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        }
    }
}
