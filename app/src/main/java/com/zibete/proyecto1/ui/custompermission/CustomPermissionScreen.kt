package com.zibete.proyecto1.ui.custompermission

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeMessageDialog
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun CustomPermissionScreen(
    mode: PermissionEducationMode,
    onPermissionFlowCompleted: () -> Unit,
    onLocationDenied: () -> Unit,
    permissionViewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        permissionViewModel::onLocationResult
    )
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        permissionViewModel::onNotificationResult
    )

    LaunchedEffect(mode) { permissionViewModel.configure(mode) }
    LaunchedEffect(permissionViewModel) {
        permissionViewModel.events.collect { event ->
            when (event) {
                PermissionUiEvent.Completed -> onPermissionFlowCompleted()
                PermissionUiEvent.LocationDenied -> onLocationDenied()
                is PermissionUiEvent.RequestPermission -> when (event.request) {
                    PermissionRequest.LOCATION ->
                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                    PermissionRequest.NOTIFICATIONS ->
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    PermissionEducationContent(
        mode = uiState.mode,
        requestInFlight = uiState.requestInFlight != null,
        onContinue = {
            permissionViewModel.onContinueClicked(
                shouldShowLocationRationale = ActivityCompat
                    .shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
            )
        }
    )

    if (uiState.showLocationRationaleDialog) {
        ZibeDialog(
            title = stringResource(R.string.permission_rationale_title),
            content = {
                Text(
                    text = stringResource(R.string.permission_rationale_message),
                    textAlign = TextAlign.Start,
                    color = LocalZibeExtendedColors.current.hintText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onConfirm = permissionViewModel::onLocationRationaleAccepted,
            onCancel = permissionViewModel::onLocationRationaleDismissed
        )
    }

    if (uiState.showLocationDeniedDialog) {
        ZibeMessageDialog(
            title = stringResource(R.string.permission_denied_title),
            textContent = {
                Text(
                    text = stringResource(R.string.permission_denied_message),
                    textAlign = TextAlign.Start,
                    color = LocalZibeExtendedColors.current.hintText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            onConfirm = permissionViewModel::onLocationDeniedAcknowledged
        )
    }
}

@Composable
fun PermissionEducationContent(
    mode: PermissionEducationMode,
    requestInFlight: Boolean,
    onContinue: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PERMISSION_SCREEN)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.lightText,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (mode == PermissionEducationMode.COMBINED) {
                stringResource(R.string.permission_setup_intro)
            } else {
                stringResource(R.string.permission_notification_only_intro)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.hintText
        )
        Spacer(Modifier.height(20.dp))

        if (mode == PermissionEducationMode.COMBINED) {
            PermissionSection(
                iconRes = R.drawable.ic_baseline_location_on_24,
                title = stringResource(R.string.permission_location_title),
                badge = stringResource(R.string.permission_required_badge),
                description = stringResource(R.string.permission_location_description)
            )
            Spacer(Modifier.height(12.dp))
        }

        PermissionSection(
            iconRes = R.drawable.ic_notifications_24dp,
            title = stringResource(R.string.permission_notifications_title),
            badge = stringResource(R.string.permission_optional_badge),
            description = stringResource(R.string.permission_notifications_description)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (mode == PermissionEducationMode.COMBINED) {
                stringResource(R.string.permission_setup_clarification)
            } else {
                stringResource(R.string.permission_notification_only_clarification)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.hintText
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_legal_links),
            style = MaterialTheme.typography.bodySmall,
            color = colors.hintText.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(24.dp))
        ZibeButtonPrimary(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = !requestInFlight,
            isLoading = requestInFlight
        )
    }
}

@Composable
private fun PermissionSection(
    iconRes: Int,
    title: String,
    badge: String,
    description: String
) {
    val colors = LocalZibeExtendedColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.cardBackground
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = colors.accent
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.lightText,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.hintText
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PermissionEducationPreview() {
    ZibeTheme {
        PermissionEducationContent(
            mode = PermissionEducationMode.COMBINED,
            requestInFlight = false,
            onContinue = {}
        )
    }
}
