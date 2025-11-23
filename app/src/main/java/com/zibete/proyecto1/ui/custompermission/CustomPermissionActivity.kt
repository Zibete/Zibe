package com.zibete.proyecto1.ui.custompermission

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme

class CustomPermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZibeTheme {
                CustomPermissionScreen(
                    onPermissionGranted = {
                        startActivity(Intent(this, SplashActivity::class.java))
                        finish()
                    },
                    onForceLogout = {
                        FirebaseAuth.getInstance().signOut()
                        LoginManager.getInstance().logOut()
                        startActivity(Intent(this, SplashActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
