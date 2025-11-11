package com.zibete.proyecto1.Splash

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.facebook.login.LoginManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.AuthActivity
import com.zibete.proyecto1.CustomPermission
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.FirebaseRefs.auth
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private var userToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)

        // FCM token (no bloqueante)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userToken = task.result
                }
            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        progressBar.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            if (hasInternetConnection()) {
                if (currentUser != null) {
                    // Usuario logueado → chequeo permisos ubicación + token
                    if (!hasLocationPermission()) {
                        startActivity(Intent(this, CustomPermission::class.java))
                        finish()
                    } else {
                        queryToken(currentUser)
                    }
                } else {
                    // Sin usuario → flujo a Auth
                    updateUI(null)
                }
            } else {
                progressBar.visibility = View.INVISIBLE
                showNoInternetSnackbar()
            }
        }, 1000)
    }

    // ====== LÓGICA TOKEN / SESIÓN ÚNICA ======

    private fun queryToken(user: FirebaseUser) {
        val token = userToken

        if (token.isNullOrEmpty()) {
            // Si no tengo token aún, igual sigo al flujo normal
            updateUI(user)
            return
        }

        refCuentas.orderByChild("token").equalTo(token)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val count = snapshot.childrenCount

                        if (count == 1L) {
                            // Puede ser mi cuenta o la de otro
                            for (child in snapshot.children) {
                                if (child.key == user.uid) {
                                    // Soy yo → ok
                                    updateUI(user)
                                } else {
                                    // Otro usuario con mi token
                                    showTokenDialog(child, user, flag = 1)
                                }
                            }
                        } else {
                            // Más de uno con mi token → tomo cualquiera que no sea yo
                            for (child in snapshot.children) {
                                if (child.key != user.uid) {
                                    showTokenDialog(child, user, flag = 2)
                                    break
                                }
                            }
                        }
                    } else {
                        // Nadie tiene mi token → si ya hay token en mi cuenta, actualizo
                        refCuentas.child(user.uid).child("token")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(ds: DataSnapshot) {
                                    if (ds.exists()) {
                                        ds.ref.setValue(token)
                                    }
                                    updateUI(user)
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showTokenDialog(snapshot: DataSnapshot, user: FirebaseUser, flag: Int) {
        val mail = snapshot.child("mail").getValue(String::class.java) ?: "otra cuenta"

        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
            .setTitle("Un momento...")
            .setMessage(
                "Ya hay una cuenta asociada a este dispositivo. Si continúa, se desvinculará a $mail. ¿Desea continuar?"
            )
            .setCancelable(false)
            .setPositiveButton("Continuar") { dialog, _ ->
                val token = userToken ?: ""
                // Asigno token a mi cuenta
                refCuentas.child(user.uid).child("token").setValue(token)
                // Limpio token de la otra cuenta
                snapshot.ref.child("token").setValue("")
                dialog.dismiss()
                updateUI(user)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                if (flag == 2) {
                    // Si había varios, limpio mi token
                    refCuentas.child(user.uid).child("token").setValue("")
                }
                updateUI(null)
            }
            .show()
    }

    // ====== RUTEO SEGÚN ESTADO DE USUARIO ======

    private fun updateUI(user: FirebaseUser?) {
        val prefs = getSharedPreferences("flag_Splash", MODE_PRIVATE)
        val bandActivity = prefs.getBoolean("flag_Splash", false)
        val editor = prefs.edit()

        if (user == null) {
            // Sin sesión → ir a Auth
            intentAuth(editor)
            return
        }

        // Usuario logueado → verifico datos en /Cuentas
        refCuentas.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (!ds.exists()) {
                        // No tiene nodo en Cuentas → lo creo básico y voy a editar perfil
                        createUserNodeFromAuth(user, ds)
                        intentEditProfile()
                    } else {
                        val birthDay = ds.child("birthDay").getValue(String::class.java) ?: ""

                        if (!bandActivity) {
                            // Primera vez después de instalar/limpiar
                            editor.putBoolean("flag_Splash", true).apply()
                            intentEditProfile()
                        } else {
                            // Ya pasó por splash antes
                            if (birthDay.isEmpty()) {
                                intentEditProfile()
                            } else {
                                intentMain()
                            }
                        }
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createUserNodeFromAuth(user: FirebaseUser, snapshot: DataSnapshot) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val now = dateFormat.format(Calendar.getInstance().time)

        val newUser = Users(
            user.uid,
            user.displayName ?: "",
            "",
            now,
            0,
            user.email ?: "",
            user.photoUrl?.toString() ?: "",
            true,
            userToken ?: "",
            0.0,
            "",
            0.0,
            0.0
        )

        snapshot.ref.setValue(newUser)
    }

    private fun intentAuth(editor: SharedPreferences.Editor) {
        editor.putBoolean("flag_Splash", false).apply()
        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()

        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun intentEditProfile() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("flagIntent", 0)
        startActivity(intent)
    }

    private fun intentMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("flagIntent", 1)
        startActivity(intent)
    }

    // ====== UI helpers ======

    private fun showNoInternetSnackbar() {
        val root = findViewById<View>(android.R.id.content)
        val snack = Snackbar.make(
            root,
            "No hay conexión a Internet en este momento",
            Snackbar.LENGTH_INDEFINITE
        )
        snack.setAction("Reintentar") {
            snack.dismiss()
            onStart()
        }
        snack.setBackgroundTint(getColor(R.color.colorC))
        snack.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        ).textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }

    // ====== Utilidades técnicas ======

    private fun hasInternetConnection(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        val info = cm.activeNetworkInfo
        return info != null && info.isConnected
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
