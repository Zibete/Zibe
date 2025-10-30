package com.zibete.proyecto1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.utils.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.zibete.proyecto1.utils.FirebaseRefs.ref_zibe;

public class ReportActivity extends AppCompatActivity {

    private Toolbar toolbar_ajustes;
    private TextInputEditText edt_comentarios;
    private MaterialButton btn_send;
    private @Nullable FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_report);

        // Toolbar
        toolbar_ajustes = findViewById(R.id.toolbar_ajustes);
        setSupportActionBar(toolbar_ajustes);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar_ajustes.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Views
        edt_comentarios = findViewById(R.id.edt_comentarios);
        btn_send = findViewById(R.id.btn_send);

        // Estado inicial
        btn_send.setEnabled(false);
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Habilitar botón si hay texto
        edt_comentarios.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btn_send.setEnabled(s != null && s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {
                btn_send.setEnabled(s != null && s.toString().trim().length() > 0);
            }
        });

        // Enviar
        btn_send.setOnClickListener(v -> {
            String mensaje = edt_comentarios.getText() == null ? "" : edt_comentarios.getText().toString().trim();
            if (mensaje.isEmpty()) {
                btn_send.setEnabled(false);
                return;
            }

            // Timestamp legible
            String date = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());

            if (user != null) {
                ref_zibe.child("Comentarios").child(date).child("ID").setValue(user.getUid());
                ref_zibe.child("Comentarios").child(date).child("nombre").setValue(user.getDisplayName());
                ref_zibe.child("Comentarios").child(date).child("email").setValue(user.getEmail());
            }
            ref_zibe.child("Comentarios").child(date).child("mensaje").setValue(mensaje);

            // Dialogo Material con overlay Zibe.Dialog (definido en styles)
            new MaterialAlertDialogBuilder(ReportActivity.this)
                    .setTitle("¡Mensaje enviado!")
                    .setMessage("¡Muchas gracias! El mensaje ha sido enviado a nuestro equipo de soporte.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dlg, idBtn) -> {
                        startActivity(new Intent(ReportActivity.this, SplashActivity.class));
                        finish();
                    })
                    .show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (user != null) UserRepository.setUserOffline(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) UserRepository.setUserOnline(getApplicationContext(), user.getUid());
    }
}
