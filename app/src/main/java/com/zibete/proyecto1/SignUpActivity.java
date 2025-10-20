package com.zibete.proyecto1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging; // (API nueva de FCM)
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.zibete.proyecto1.MainActivity.REQUEST_LOCATION;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;
import static com.zibete.proyecto1.MainActivity.ref_datos;

public class SignUpActivity extends AppCompatActivity {

    private EditText edt_name, edt_age, edt_desc, edt1, edt2;
    private String birthDay;
    private ProgressDialog progress;
    private FirebaseAuth mAuth;
    String email, password, name, birthday, desc;
    private String userToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        this.setTitle(R.string.datos);
        edt_name = findViewById(R.id.edt_name);
        edt_age = findViewById(R.id.edt_age);
        edt_desc = findViewById(R.id.edt_descripcion);
        edt1 = findViewById(R.id.edt_mail2);
        edt2 = findViewById(R.id.edt_pass2);
        progress = new ProgressDialog (this,R.style.AlertDialogApp);
        mAuth = FirebaseAuth.getInstance();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    userToken = task.getResult();
                });

        edt_age.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Calendar calendar = Calendar.getInstance();

                DatePickerDialog datePickerDialog = new DatePickerDialog(SignUpActivity.this, AlertDialog.THEME_HOLO_LIGHT, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        calendar.set(Calendar.YEAR,year);
                        calendar.set(Calendar.MONTH,month);
                        calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Date date = calendar.getTime();
                        birthDay = simpleDateFormat.format(date);
                        edt_age.setText(birthDay);
                    }
                },calendar.get(Calendar.YEAR)-18,calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });


    }



    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(SignUpActivity.this, SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

            }else{

                progress.hide();
                updateUI(null);

            }
        }
    }


    public void Register(View view){

        email = edt1.getText().toString().trim();
        password = edt2.getText().toString().trim();
        name = edt_name.getText().toString().trim();
        birthday = edt_age.getText().toString().trim();
        desc = edt_desc.getText().toString().trim();

        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Introduzca un e-mail",Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Introduzca una contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name)){
            Toast.makeText(this, "Introduzca un Nombre",Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(birthday)){
            Toast.makeText(this, "Introduzca su fecha de nacimiento", Toast.LENGTH_SHORT).show();
            return;
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate fechaNac = LocalDate.parse(birthday, fmt);
                LocalDate ahora = LocalDate.now();
                Period periodo = Period.between(fechaNac, ahora);
                int edad = periodo.getYears();
                if (edad<18){

                    final Snackbar snack = Snackbar.make(this.findViewById(android.R.id.content), "Lo sentimos, debe ser mayor de 18 años para utilizar la App", Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                        }
                    });
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                    return;
                }
            }
        }
        SignUp(null);
/*
        ref_cuentas.orderByChild("token").equalTo(userToken).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull final DataSnapshot snapshot, @Nullable String previousChildName) {

                if (snapshot.exists()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(SignUpActivity.this, R.style.AlertDialogApp));
                    builder.setTitle("Un momento...");
                    builder.setMessage("Ya hay una cuenta asociada a este dispositivo, si continúa, se desvinculará su anterior cuenta. ¿Desea continuar?");
                    builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {
                                SignUp(snapshot);
                            }
                        });


                        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                return;

                            }
                        });
                        builder.setCancelable(false);
                        builder.show();

                }else {
                    SignUp(null);
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

 */

    }

    public void SignUp(final DataSnapshot snapshot) {
        progress.setMessage("Registrando...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {

                    final FirebaseUser user = mAuth.getCurrentUser();
                    final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                    Users uu = new Users(
                            user.getUid(),
                            name,
                            birthday,
                            dateFormat.format(Calendar.getInstance().getTime()),
                            0,
                            email,
                            getApplicationContext().getString(R.string.URL_PHOTO_DEF),
                            true,
                            userToken,
                            0,
                            desc,
                            0,
                            0);
                    ref_cuentas.child(user.getUid()).setValue(uu);


                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(edt_name.getText().toString().trim())
                            .setPhotoUri(Uri.parse(getString(R.string.URL_PHOTO_DEF)))
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                ActivityCompat.requestPermissions(SignUpActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION);
                            }
                        }
                    });
                }else{
                    progress.hide();
                    Toast.makeText(SignUpActivity.this, "Introduzca un e-mail o password válidos", Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }
        });
    }


    private void updateUI(FirebaseUser user) {

        if(user != null){

            Intent intent = new Intent (SignUpActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            progress.dismiss();
            finish();

        }else{

            FirebaseAuth.getInstance().signOut();
            com.facebook.login.LoginManager.getInstance().logOut();
        }
    }
}



