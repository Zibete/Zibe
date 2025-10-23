package com.zibete.proyecto1;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.zibete.proyecto1.MainActivity.ref_zibe;

public class ReportActivity extends AppCompatActivity {

    Toolbar toolbar_ajustes;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    EditText edt_comentarios;
    Button btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_report);

        toolbar_ajustes = findViewById(R.id.toolbar_ajustes);
        edt_comentarios = findViewById(R.id.edt_comentarios);
        btn_send = findViewById(R.id.btn_send);

        setSupportActionBar(toolbar_ajustes);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        btn_send.setEnabled(false);

        edt_comentarios.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_comentarios.length() == 0 ){
                    btn_send.setEnabled(false);
                }            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(edt_comentarios.length() == 0)){
                    btn_send.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_comentarios.length() == 0 ){
                    btn_send.setEnabled(false);
                }
            }
        });


        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Calendar c = Calendar.getInstance();
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss");

                String date = dateFormat.format(c.getTime());

                ref_zibe.child("Comentarios").child(date).child("ID").setValue(user.getUid());
                ref_zibe.child("Comentarios").child(date).child("nombre").setValue(user.getDisplayName());
                ref_zibe.child("Comentarios").child(date).child("email").setValue(user.getEmail());
                ref_zibe.child("Comentarios").child(date).child("mensaje").setValue(edt_comentarios.getText().toString());


                new AlertDialog.Builder(new ContextThemeWrapper(ReportActivity.this, R.style.AlertDialogApp))
                        .setTitle("¡Mensaje enviado!")
                        .setMessage("¡Muchas gracias! El mensaje ha sido enviado a nuestro equipo de soporte")
                        .setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                Intent intent = new Intent(ReportActivity.this, SplashActivity.class);
                                startActivity(intent);
                                finish();


                            }
                        })

                        .show();

            }
        });


    }


    @Override
    protected void onPause() {
        super.onPause();
        new Constants().StateOffLine(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Constants().StateOnLine(getApplicationContext(), user.getUid());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
