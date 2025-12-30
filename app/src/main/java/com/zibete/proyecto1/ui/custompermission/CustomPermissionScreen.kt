package com.zibete.proyecto1.ui.custompermission

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zibete.proyecto1.R
import com.zibete.proyecto1.di.PermissionRequesterEntryPoint
import com.zibete.proyecto1.di.RationaleEntryPoint
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeOkDialog
import com.zibete.proyecto1.ui.constants.*
import com.zibete.proyecto1.ui.constants.Constants.UiTags.PERMISSION_SCREEN
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
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

    // La UI solo “escucha” eventos del VM y ejecuta acciones externas
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
            .background(zibeColors.gradientZibe)
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
                        contentDescription = LOGO_CONTENT_DESC,
                        modifier = Modifier.height(90.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = PERMISSION_LOCATION_MESSAGE,
                        color = zibeColors.mutedText,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = PERMISSION_LEGAL_DISCLAIMER,
                        color = zibeColors.mutedText,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ZibeButton(
                    text = BUTTON_START,
                    onClick = { permissionViewModel.onStartClicked(rationaleProvider.shouldShowRationale()) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = false
                )
            }
        }
    }

    if (uiState.showRationaleDialog) {
        ZibeDialog(
            title = PERMISSION_RATIONALE_TITLE,
            textContent = {
                Text(
                    text = PERMISSION_RATIONALE_MESSAGE,
                    textAlign = TextAlign.Start,
                    color = LocalZibeExtendedColors.current.mutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmText = DIALOG_ACCEPT,
            onConfirm = { permissionViewModel.onRationaleAccept() },
            dismissText = DIALOG_CANCEL,
            onDismiss = { permissionViewModel.onRationaleDismiss() }
        )
    }

    if (uiState.showDeniedDialog) {
        ZibeOkDialog(
            title = PERMISSION_DENIED_TITLE,
            textContent = {
                Text(
                    text = PERMISSION_DENIED_MESSAGE,
                    textAlign = TextAlign.Start,
                    color = LocalZibeExtendedColors.current.mutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmText = DIALOG_OK,
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
