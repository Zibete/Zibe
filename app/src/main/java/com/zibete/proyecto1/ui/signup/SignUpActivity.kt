package com.zibete.proyecto1.ui.signup

import android.Manifest
import android.R
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserMessageUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SignUpActivity : ComponentActivity() {

    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFirebase()

        setContent {
            ZibeTheme {
                SignUpScreen(
                    onBack = { finish() },
                    onRegister = { email, pass, name, birthday, desc ->
                        doSignUp(email, pass, name, birthday, desc)
                    }
                )
            }
        }
    }

    // --------------------------
    // 🔹 Lógica Firebase / registro
    // --------------------------
    private fun setupFirebase() {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { t ->
            if (t.isSuccessful) myInstallId = t.result
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
            if (t.isSuccessful) myFcmToken = t.result
        }
    }

    private fun doSignUp(
        email: String,
        password: String,
        name: String,
        birthday: String,
        desc: String
    ) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || birthday.isEmpty()) {
            toast("Por favor, completá todos los campos")
            return
        }

        if (!isAdult(birthday)) {
            UserMessageUtils.showInfo(
                findViewById(R.id.content),
                "Lo sentimos, debe ser mayor de 18 años para utilizar la App"
            )
            return
        }

        val dlg = UserMessageUtils.showProgress(this, "Registrando...")

        FirebaseRefs.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    dlg.dismiss()
                    toast("E-mail o contraseña inválidos")
                    return@addOnCompleteListener
                }

                val user = FirebaseRefs.auth.currentUser ?: return@addOnCompleteListener
                writeUserProfile(user, dlg, email, name, birthday, desc)
            }
    }

    private fun writeUserProfile(
        user: FirebaseUser,
        dlg: AlertDialog,
        email: String,
        name: String,
        birthday: String,
        desc: String
    ) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Calendar.getInstance().time)
        val age = DateUtils.calcAge(birthday)

        val data = hashMapOf<String, Any?>(
            "id" to user.uid,
            "nombre" to name,
            "birthDay" to birthday,
            "date" to nowStr,
            "age" to age,
            "mail" to email,
            "foto" to getString(com.zibete.proyecto1.R.string.URL_PHOTO_DEF),
            "estado" to true,
            "installId" to myInstallId,
            "fcmToken" to myFcmToken,
            "token" to myInstallId,
            "distance" to 0,
            "descripcion" to desc.ifEmpty { "" },
            "latitud" to 0,
            "longitud" to 0
        )

        val userRef: DatabaseReference = FirebaseRefs.refCuentas.child(user.uid)
        userRef.setValue(data).addOnCompleteListener { setTask ->
            dlg.dismiss()
            if (!setTask.isSuccessful) {
                toast("Error guardando datos")
                return@addOnCompleteListener
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(getString(com.zibete.proyecto1.R.string.URL_PHOTO_DEF).toUri())
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener {
                    ActivityCompat.requestPermissions(
                        this@SignUpActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        Constants.REQUEST_LOCATION
                    )
                }
        }
    }

    private fun isAdult(birthStr: String): Boolean = try {
        DateUtils.calcAge(birthStr) >= 18
    } catch (_: Exception) {
        false
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}