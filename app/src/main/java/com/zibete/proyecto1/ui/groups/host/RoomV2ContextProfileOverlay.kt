package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R

@Composable
fun RoomV2HostWithProfileRoute(
    viewModel: RoomV2HostViewModel,
    profileViewModel: RoomV2ContextProfileViewModel,
) {
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    RoomV2HostRoute(
        viewModel = viewModel,
        onOpenProfile = { identity ->
            profileViewModel.open(viewModel.uiState.value.roomId, identity)
        },
    )

    RoomV2ContextProfileDialog(
        state = profileState,
        onDismiss = profileViewModel::dismiss,
    )
}

@Composable
private fun RoomV2ContextProfileDialog(
    state: RoomV2ContextProfileUiState,
    onDismiss: () -> Unit,
) {
    when {
        state.loadingIdentityId != null -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.rooms_v2_profile_title)) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.rooms_v2_profile_loading))
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }

        state.error != null -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.rooms_v2_profile_error_title)) },
                text = { Text(state.error.asString()) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }

        state.profile != null -> {
            val profile = state.profile
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(profile.displayName) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (profile.age > 0) {
                            Text(
                                stringResource(R.string.rooms_v2_profile_age, profile.age),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Text(
                            profile.description.ifBlank {
                                stringResource(R.string.rooms_v2_profile_no_description)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }
    }
}
