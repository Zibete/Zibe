package com.zibete.proyecto1.ui.custompermission

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.di.PermissionRequesterEntryPoint
import com.zibete.proyecto1.di.RationaleEntryPoint
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeMessageDialog
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.EntryPointAccessors

@Composable
fun CustomPermissionScreen(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val permissionViewModel: PermissionViewModel = hiltViewModel()
    val uiState by permissionViewModel.uiState.collectAsState()

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    val rationaleProvider = remember {
        EntryPointAccessors.fromActivity(activity, RationaleEntryPoint::class.java)
            .rationaleProvider()
    }
    val requester = remember {
        EntryPointAccessors.fromApplication(appContext, PermissionRequesterEntryPoint::class.java)
            .requester()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        requester.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        requester.bindLauncher(launcher)
    }

    val event by permissionViewModel.event.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            PermissionUiEvent.PermissionGranted -> {
                permissionViewModel.consumeEvent()
                onPermissionGranted()
            }

            PermissionUiEvent.PermissionDenied -> {
                permissionViewModel.consumeEvent()
                onPermissionDenied()
            }

            null -> Unit
        }
    }

    val zibeColors = LocalZibeExtendedColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PERMISSION_SCREEN)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = zibeColors.cardBackground,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.logo_zibe_icon),
                        contentDescription = stringResource(R.string.logo_content_desc),
                        modifier = Modifier.height(90.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.permission_location_message),
                        color = zibeColors.hintText,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.permission_legal_disclaimer),
                        color = zibeColors.hintText,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ZibeButtonPrimary(
                    text = stringResource(R.string.action_start),
                    onClick = { permissionViewModel.onStartClicked(rationaleProvider.shouldShowRationale()) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = false
                )
            }
        }
    }

    if (uiState.showRationaleDialog) {
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
            onConfirm = { permissionViewModel.onRationaleAccept() },
            onCancel = { permissionViewModel.onRationaleDismiss() }
        )
    }

    if (uiState.showDeniedDialog) {
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
            onConfirm = { permissionViewModel.onDeniedOkClicked() }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CustomPermissionScreenPreview() {
    ZibeTheme {
        CustomPermissionScreen(
            onPermissionGranted = {},
            onPermissionDenied = {}
        )
    }
}
