package com.zibete.proyecto1

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.Splash.SplashActivity
import com.zibete.proyecto1.utils.FirebaseRefs.refZibe
import com.zibete.proyecto1.utils.UserRepository.setUserOffline
import com.zibete.proyecto1.utils.UserRepository.setUserOnline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var edtComentarios: TextInputEditText
    private lateinit var btnSend: MaterialButton

    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_report)

        setupToolbar()
        setupViews()
        setupListeners()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar_ajustes) // Asegurate que en dialog_report sea MaterialToolbar
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViews() {
        edtComentarios = findViewById(R.id.edt_comentarios)
        btnSend = findViewById(R.id.btn_send)
        btnSend.isEnabled = false
    }

    private fun setupListeners() {
        // Habilitar botón solo si hay texto
        edtComentarios.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSend.isEnabled = !s.isNullOrBlank()
            }

            override fun afterTextChanged(s: Editable?) {
                btnSend.isEnabled = !s.isNullOrBlank()
            }
        })

        btnSend.setOnClickListener {
            val mensaje = edtComentarios.text?.toString()?.trim().orEmpty()
            if (mensaje.isEmpty()) {
                btnSend.isEnabled = false
                return@setOnClickListener
            }

            val dateKey = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().time)

            // Guarda datos del usuario si está logueado
            user?.let { u ->
                refZibe.child("Comentarios").child(dateKey).apply {
                    child("ID").setValue(u.uid)
                    child("nombre").setValue(u.displayName)
                    child("email").setValue(u.email)
                }
            }

            // Guarda mensaje
            refZibe.child("Comentarios").child(dateKey)
                .child("mensaje")
                .setValue(mensaje)

            // Dialog Material
            MaterialAlertDialogBuilder(this)
                .setTitle("¡Mensaje enviado!")
                .setMessage("¡Muchas gracias! El mensaje ha sido enviado a nuestro equipo de soporte.")
                .setCancelable(false)
                .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                }
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        user?.uid?.let { setUserOffline(applicationContext, it) }
    }

    override fun onResume() {
        super.onResume()
        user?.uid?.let { setUserOnline(applicationContext, it) }
    }
}
