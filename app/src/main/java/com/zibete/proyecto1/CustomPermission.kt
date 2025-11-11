package com.zibete.proyecto1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.facebook.login.LoginManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.Splash.SplashActivity
import com.zibete.proyecto1.utils.Constants.REQUEST_LOCATION

class CustomPermission : AppCompatActivity() {
    var start: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_permission)

        start = findViewById(R.id.btnStart)
        start!!.setOnClickListener {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@CustomPermission,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val snack = Snackbar.make(
                    findViewById(android.R.id.content),
                    "Zibe necesita acceso a su ubicación para poder funcionar",
                    Snackbar.LENGTH_SHORT
                )
                snack.setBackgroundTint(getResources().getColor(R.color.colorC))
                val tv = snack.getView()
                    .findViewById<View?>(com.google.android.material.R.id.snackbar_text) as TextView
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                snack.show()
            }

            ActivityCompat.requestPermissions(
                this@CustomPermission,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this@CustomPermission, SplashActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val snack = Snackbar.make(
                    this@CustomPermission.findViewById(android.R.id.content),
                    "Se cerrará su sesión",
                    Snackbar.LENGTH_INDEFINITE
                )
                snack.setAction("OK") {
                    snack.dismiss()
                    FirebaseAuth.getInstance().signOut()
                    LoginManager.getInstance().logOut()
                    val intent = Intent(this@CustomPermission, SplashActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                snack.setBackgroundTint(getResources().getColor(R.color.colorC))
                val tv = snack.getView()
                    .findViewById<View?>(com.google.android.material.R.id.snackbar_text) as TextView
                tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                snack.show()
            }
        }
    }
}