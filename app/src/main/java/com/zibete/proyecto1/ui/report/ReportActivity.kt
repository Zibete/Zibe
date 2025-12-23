package com.zibete.proyecto1.ui.report

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ServerValue
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint

class ReportActivity : AppCompatActivity() {
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer

    private val myUid: String get() = userRepository.myUid

    private lateinit var toolbar: MaterialToolbar
    private lateinit var edtComentarios: TextInputEditText
    private lateinit var btnSend: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_report)

        setupToolbar()
        setupViews()
        setupListeners()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.material_toolbar) // Asegurate que en dialog_report sea MaterialToolbar
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

            val ref = firebaseRefsContainer.refZibe
                .child("Comentarios")
                .push() // 🔑 key única (mejor que now())

            ref.apply {
                child("id").setValue(myUid)
                child("nombre").setValue(userRepository.myUserName)
                child("email").setValue(userRepository.myEmail)
                child("mensaje").setValue(mensaje)
                child("createdAt").setValue(ServerValue.TIMESTAMP)
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("¡Mensaje enviado!")
                .setMessage("¡Muchas gracias! El mensaje ha sido enviado a nuestro equipo de soporte.")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                }
                .show()
        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}