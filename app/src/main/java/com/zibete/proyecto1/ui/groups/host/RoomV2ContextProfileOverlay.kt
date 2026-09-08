package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode

@Composable
fun RoomV2HostWithProfileRoute(
    viewModel: RoomV2HostViewModel,
    profileViewModel: RoomV2ContextProfileViewModel,
) {
    val hostState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var showProfilePicker by rememberSaveable { mutableStateOf(false) }

    val realParticipants = remember(hostState.participants, hostState.myIdentityId) {
        hostState.participants.filter { identity ->
            identity.mode == RoomV2IdentityMode.REAL &&
                identity.identityId != hostState.myIdentityId
        }
    }

    LaunchedEffect(hostState.selectedTab, realParticipants.size) {
        if (hostState.selectedTab != RoomV2HostTab.PEOPLE || realParticipants.isEmpty()) {
            showProfilePicker = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RoomV2HostRoute(viewModel = viewModel)

        if (
            hostState.selectedTab == RoomV2HostTab.PEOPLE &&
            realParticipants.isNotEmpty()
        ) {
            Button(
                onClick = { showProfilePicker = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                enabled = profileState.loadingIdentityId == null,
            ) {
                Text(stringResource(R.string.rooms_v2_profiles_action))
            }
        }
    }

    if (showProfilePicker) {
        AlertDialog(
            onDismissRequest = { showProfilePicker = false },
            title = { Text(stringResource(R.string.rooms_v2_profiles_title)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                ) {
                    items(realParticipants, key = { it.identityId }) { identity ->
                        TextButton(
                            onClick = {
                                showProfilePicker = false
                                profileViewModel.open(hostState.roomId, identity)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(identity.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfilePicker = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

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
                                fontWeight = FontWeight.SemiBold,
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
