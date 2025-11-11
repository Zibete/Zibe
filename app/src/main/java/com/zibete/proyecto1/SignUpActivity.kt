package com.zibete.proyecto1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.facebook.login.LoginManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.Splash.SplashActivity
import com.zibete.proyecto1.utils.Constants.REQUEST_LOCATION
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.UserMessageUtils.showInfo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtDesc: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText

    private lateinit var tilBirthdate: TextInputLayout
    private lateinit var edtBirthdate: TextInputEditText

    private lateinit var mAuth: FirebaseAuth

    private var email = ""
    private var password = ""
    private var name = ""
    private var birthday = ""
    private var desc = ""

    // IDs / tokens
    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        setupToolbar()
        bindViews()
        setupFirebase()
        setupBirthdatePicker()
    }

    // Toolbar con flecha de back
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.datos)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun bindViews() {
        edtName = findViewById(R.id.edt_name)
        edtDesc = findViewById(R.id.edt_descripcion)
        edtEmail = findViewById(R.id.edt_mail2)
        edtPass = findViewById(R.id.edt_pass2)
        tilBirthdate = findViewById(R.id.til_birthdate)
        edtBirthdate = findViewById(R.id.edt_birthdate)

        findViewById<View>(R.id.bt_registro).setOnClickListener { register(it) }
    }

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()

        FirebaseInstallations.getInstance().id.addOnCompleteListener { t ->
            if (t.isSuccessful) myInstallId = t.result
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
            if (t.isSuccessful) myFcmToken = t.result
        }
    }

    private fun setupBirthdatePicker() {
        val openPicker = View.OnClickListener { showBirthdatePicker() }
        edtBirthdate.setOnClickListener(openPicker)
        tilBirthdate.setEndIconOnClickListener(openPicker)
    }

    private fun showBirthdatePicker() {
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }

        val constraints = CalendarConstraints.Builder().build()

        var selection: Long? = null
        try {
            val txt = edtBirthdate.text?.toString()?.trim().orEmpty()
            if (txt.isNotEmpty()) {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val d = fmt.parse(txt)
                if (d != null) selection = d.time
            }
        } catch (_: Exception) { }

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.fecha_nacimiento))
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ZibeDatePickerOverlay)
            .setSelection(selection)
            .build()

        picker.addOnPositiveButtonClickListener { selectionUtc ->
            val out = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formatted = out.format(Date(selectionUtc ?: return@addOnPositiveButtonClickListener))
            edtBirthdate.setText(formatted)
            tilBirthdate.error = null
        }

        picker.show(supportFragmentManager, "zibe_birthdate")
    }

    // Click en “Registrarme”
    fun register(view: View?) {
        email = edtEmail.text.toString().trim()
        password = edtPass.text.toString().trim()
        name = edtName.text.toString().trim()
        birthday = edtBirthdate.text.toString().trim()
        desc = edtDesc.text.toString().trim()

        when {
            email.isEmpty() -> { toast("Introduzca un e-mail"); return }
            password.isEmpty() -> { toast("Introduzca una contraseña"); return }
            name.isEmpty() -> { toast("Introduzca un Nombre"); return }
            birthday.isEmpty() -> { toast("Introduzca su fecha de nacimiento"); return }
        }

        if (!isAdult(birthday)) {
            showInfo(
                findViewById(android.R.id.content),
                "Lo sentimos, debe ser mayor de 18 años para utilizar la App"
            )
            return
        }

        doSignUp()
    }

    private fun isAdult(birthStr: String): Boolean = try {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val birth = LocalDate.parse(birthStr, fmt)
        Period.between(birth, LocalDate.now()).years >= 18
    } catch (_: Exception) { false }

    private fun doSignUp() {
        val dlg = UserMessageUtils.showProgress(this, "Registrando...")

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    dlg.dismiss()
                    toast("Introduzca un e-mail o password válidos")
                    updateUI(null)
                    return@addOnCompleteListener
                }

                val user = mAuth.currentUser
                if (user == null) {
                    dlg.dismiss()
                    toast("No se pudo obtener el usuario")
                    updateUI(null)
                    return@addOnCompleteListener
                }

                FirebaseInstallations.getInstance().id
                    .addOnCompleteListener { fidTask ->
                        if (fidTask.isSuccessful) myInstallId = fidTask.result

                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { fcmTask ->
                                if (fcmTask.isSuccessful) myFcmToken = fcmTask.result
                                writeUserProfile(user, dlg)
                            }
                    }
            }
    }

    private fun writeUserProfile(user: FirebaseUser, dlg: androidx.appcompat.app.AlertDialog) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nowStr = dateFormat.format(Calendar.getInstance().time)

        val age = DateUtils.calcAge(birthday)

        val data = hashMapOf<String, Any?>(
            "id" to user.uid,
            "nombre" to name,
            "birthDay" to birthday,
            "date" to nowStr,
            "age" to age,
            "mail" to email,
            "foto" to getString(R.string.URL_PHOTO_DEF),
            "estado" to true,
            "installId" to myInstallId,
            "fcmToken" to myFcmToken,
            "token" to myInstallId, // compat vieja
            "distance" to 0,
            "descripcion" to desc.ifEmpty { "" },
            "latitud" to 0,
            "longitud" to 0
        )

        val userRef: DatabaseReference = refCuentas.child(user.uid)

        userRef.setValue(data).addOnCompleteListener { setTask ->
            if (!setTask.isSuccessful) {
                dlg.dismiss()
                toast("Error guardando datos")
                updateUI(null)
                return@addOnCompleteListener
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(getString(R.string.URL_PHOTO_DEF).toUri())
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener {
                    dlg.dismiss()
                    ActivityCompat.requestPermissions(
                        this@SignUpActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION
                    )
                }
        }
    }

    // Permisos de ubicación tras completar registro
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, SplashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            } else {
                updateUI(null)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        } else {
            FirebaseAuth.getInstance().signOut()
            LoginManager.getInstance().logOut()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()


}
