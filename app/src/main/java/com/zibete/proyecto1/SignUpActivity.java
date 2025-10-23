package com.zibete.proyecto1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.installations.FirebaseInstallations;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



import static com.zibete.proyecto1.MainActivity.REQUEST_LOCATION;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;

public class SignUpActivity extends AppCompatActivity {

    private EditText edt_name, edt_age, edt_desc, edt1, edt2;
    private String birthDay;
    private ProgressDialog progress;
    private FirebaseAuth mAuth;

    private String email, password, name, birthday, desc;

    // IDs/tokens
    private String myInstallId = null;
    private String myFcmToken  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        setTitle(R.string.datos);

        edt_name = findViewById(R.id.edt_name);
        edt_age  = findViewById(R.id.edt_age);
        edt_desc = findViewById(R.id.edt_descripcion);
        edt1     = findViewById(R.id.edt_mail2);
        edt2     = findViewById(R.id.edt_pass2);

        progress = new ProgressDialog(this, R.style.AlertDialogApp);
        mAuth    = FirebaseAuth.getInstance();

        // Pre-cargar FID y FCM (los volvemos a pedir por si fallan en el momento del alta)
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myInstallId = t.getResult(); });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myFcmToken = t.getResult(); });

        // Selector de fecha de nacimiento
        edt_age.setOnClickListener(view -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    new ContextThemeWrapper(SignUpActivity.this, AlertDialog.THEME_HOLO_LIGHT),
                    (DatePicker datePicker, int year, int month, int dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Date date = calendar.getTime();
                        birthDay = sdf.format(date);
                        edt_age.setText(birthDay);
                    },
                    calendar.get(Calendar.YEAR) - 18, // por defecto, -18
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    // Permisos de ubicación tras completar registro
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(SignUpActivity.this, SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                progress.hide();
                updateUI(null);
            }
        }
    }

    // Click en “Registrarme”
    public void Register(View view){
        email    = edt1.getText().toString().trim();
        password = edt2.getText().toString().trim();
        name     = edt_name.getText().toString().trim();
        birthday = edt_age.getText().toString().trim();
        desc     = edt_desc.getText().toString().trim();

        if (TextUtils.isEmpty(email))    { toast("Introduzca un e-mail"); return; }
        if (TextUtils.isEmpty(password)) { toast("Introduzca una contraseña"); return; }
        if (TextUtils.isEmpty(name))     { toast("Introduzca un Nombre"); return; }
        if (TextUtils.isEmpty(birthday)) { toast("Introduzca su fecha de nacimiento"); return; }

        // Validación de >= 18 años (en dispositivos modernos con java.time)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate fechaNac = LocalDate.parse(birthday, fmt);
                LocalDate ahora    = LocalDate.now();
                if (Period.between(fechaNac, ahora).getYears() < 18) {
                    showSnack("Lo sentimos, debe ser mayor de 18 años para utilizar la App");
                    return;
                }
            } catch (Exception e) {
                toast("Fecha inválida");
                return;
            }
        } else {
            // Fallback simple para APIs viejas
            if (calcAgeLegacy(birthday) < 18) {
                showSnack("Lo sentimos, debe ser mayor de 18 años para utilizar la App");
                return;
            }
        }

        // Ejecutar alta
        doSignUp();
    }

    private void doSignUp() {
        progress.setMessage("Registrando...");
        progress.setCanceledOnTouchOutside(false);
        progress.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, (Task<AuthResult> task) -> {
                    if (!task.isSuccessful()) {
                        progress.hide();
                        toast("Introduzca un e-mail o password válidos");
                        updateUI(null);
                        return;
                    }

                    final FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        progress.hide();
                        toast("No se pudo obtener el usuario");
                        updateUI(null);
                        return;
                    }

                    // Asegurar que tenemos FID y FCM token
                    FirebaseInstallations.getInstance().getId()
                            .addOnCompleteListener(fidTask -> {
                                if (fidTask.isSuccessful()) myInstallId = fidTask.getResult();

                                FirebaseMessaging.getInstance().getToken()
                                        .addOnCompleteListener(fcmTask -> {
                                            if (fcmTask.isSuccessful()) myFcmToken = fcmTask.getResult();

                                            writeUserProfile(user);
                                        });
                            });
                });
    }

    private void writeUserProfile(@NonNull FirebaseUser user) {
        // Formato de fecha “dd/MM/yyyy HH:mm” para tu campo “date”
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String nowStr = dateFormat.format(Calendar.getInstance().getTime());

        // Cálculo de edad (para guardar en “age”)
        int age = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? calcAgeModern(birthday) : calcAgeLegacy(birthday);

        // Construimos el payload exacto que queremos en la BD
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getUid());
        data.put("nombre", name);
        data.put("birthDay", birthday);
        data.put("date", nowStr);
        data.put("age", age);
        data.put("mail", email);
        data.put("foto", getString(R.string.URL_PHOTO_DEF));
        data.put("estado", true);

        // NUEVO: identificadores claros
        data.put("installId", myInstallId);         // para sesión única
        data.put("fcmToken",  myFcmToken);          // para notificaciones

        // Compatibilidad (BORRAR cuando migres todo lo viejo):
        data.put("token", myInstallId);

        // Otros campos existentes
        data.put("distance", 0);
        data.put("descripcion", TextUtils.isEmpty(desc) ? "" : desc);
        data.put("latitud", 0);
        data.put("longitud", 0);

        // Alta en /Cuentas/<uid>
        DatabaseReference userRef = ref_cuentas.child(user.getUid());
        userRef.setValue(data).addOnCompleteListener(setTask -> {
            if (!setTask.isSuccessful()) {
                progress.hide();
                toast("Error guardando datos");
                updateUI(null);
                return;
            }

            // Actualizar perfil público de Firebase Auth
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(Uri.parse(getString(R.string.URL_PHOTO_DEF)))
                    .build();

            user.updateProfile(profileUpdates).addOnCompleteListener(updTask -> {
                progress.hide();
                if (updTask.isSuccessful()) {
                    // Pedimos ubicación después del alta
                    ActivityCompat.requestPermissions(
                            SignUpActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_LOCATION
                    );
                } else {
                    // Incluso si falla el update de perfil, continuamos
                    ActivityCompat.requestPermissions(
                            SignUpActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_LOCATION
                    );
                }
            });
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(SignUpActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            if (progress.isShowing()) progress.dismiss();
            finish();
        } else {
            FirebaseAuth.getInstance().signOut();
            com.facebook.login.LoginManager.getInstance().logOut();
        }
    }

    private void toast(String msg) {
        Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showSnack(String msg) {
        final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_INDEFINITE);
        snack.setAction("OK", v -> snack.dismiss());
        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }

    // ==== utilidades de edad ====
    private int calcAgeModern(String ddMMyyyy) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birth = LocalDate.parse(ddMMyyyy, fmt);
            return Period.between(birth, LocalDate.now()).getYears();
        } catch (Exception e) {
            return 0;
        }
    }

    private int calcAgeLegacy(String ddMMyyyy) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(ddMMyyyy);
            if (birthDate == null) return 0;
            Calendar dob = Calendar.getInstance(); dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
            return Math.max(age, 0);
        } catch (ParseException e) {
            return 0;
        }
    }
}
