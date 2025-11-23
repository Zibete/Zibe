package com.zibete.proyecto1.ui.custompermission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeDialog
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
            .background(colorResource(id = R.color.backSplash))
            .padding(horizontal = 20.dp, vertical = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
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
                    horizontalAlignment = Alignment.Start
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo Zibe",
                        modifier = Modifier
                            .height(50.dp)
                            .wrapContentWidth()
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "Para una mejor experiencia, ZIBE almacenará tu información de localización, por lo que necesitaremos acceder a tu ubicación cuando la App esté en uso. De esta manera podremos proporcionarte personas que estén cerca de ti para que puedas interactuar con ellas.",
                        color = colorResource(id = R.color.colorClaro2),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "Al pulsar “Comenzar” aceptas las Condiciones de Servicio y la Política de Privacidad.",
                        color = colorResource(id = R.color.colorClaro2),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.colorE)
                    )
                ) {
                    Text(
                        text = "Comenzar",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Diálogo de rationale
    if (showRationaleDialog) {
        ZibeDialog(
            title = "Permiso de ubicación",
            textContent = {
                Text(
                    text = "Zibe necesita acceso a tu ubicación para poder funcionar correctamente y mostrarte personas cercanas.",
                    textAlign = TextAlign.Start
                )
            },
            confirmText = "Aceptar",
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
            title = "Permiso denegado",
            textContent = {
                Text(
                    text = "Se cerrará tu sesión porque Zibe necesita acceso a tu ubicación para funcionar.",
                    textAlign = TextAlign.Start
                )
            },
            confirmText = "OK",
            onConfirm = {
                showLogoutDialog = false
                onForceLogout()
            },
            dismissText = "Cancelar",
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
