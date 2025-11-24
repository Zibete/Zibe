package com.zibete.proyecto1.ui.custompermission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeButton
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.constants.BUTTON_START
import com.zibete.proyecto1.ui.constants.PERMISSION_DENIED_MESSAGE
import com.zibete.proyecto1.ui.constants.PERMISSION_DENIED_TITLE
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_OK
import com.zibete.proyecto1.ui.constants.PERMISSION_LEGAL_DISCLAIMER
import com.zibete.proyecto1.ui.constants.PERMISSION_LOCATION_MESSAGE
import com.zibete.proyecto1.ui.constants.LOGO_CONTENT_DESC
import com.zibete.proyecto1.ui.constants.PERMISSION_RATIONALE_MESSAGE
import com.zibete.proyecto1.ui.constants.PERMISSION_RATIONALE_TITLE
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

// Helper para obtener la Activity desde el Context
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun CustomPermissionScreen(
    onPermissionGranted: () -> Unit,
    onForceLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val zibeColors = LocalZibeExtendedColors.current

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                showLogoutDialog = true
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(zibeColors.gradientZibe) // fondo más “serio” pero ZIBE
            .padding(horizontal = 20.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
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
                        modifier = Modifier
                            .height(90.dp),
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
                    onClick = {
                        if (activity != null) {
                            val shouldShowRationale =
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )

                            if (shouldShowRationale) {
                                showRationaleDialog = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = false
                )

            }
        }
    }

    // Diálogo de rationale
    if (showRationaleDialog) {
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
            onConfirm = {
                showRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                showRationaleDialog = false
            }
        )
    }

    // Diálogo cuando niegan el permiso
    if (showLogoutDialog) {
        ZibeDialog(
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
            onConfirm = {
                showLogoutDialog = false
                onForceLogout()
            },
            dismissText = DIALOG_CANCEL,
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CustomPermissionScreenPreview() {
    ZibeTheme {
        CustomPermissionScreen(
            onPermissionGranted = {},
            onForceLogout = {}
        )
    }
}
