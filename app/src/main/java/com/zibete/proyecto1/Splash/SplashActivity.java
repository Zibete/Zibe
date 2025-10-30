package com.zibete.proyecto1.Splash;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging; // (API nueva de FCM)
import com.zibete.proyecto1.AuthActivity;
import com.zibete.proyecto1.CustomPermission;
import com.zibete.proyecto1.MainActivity;
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_cuentas;

public class SplashActivity extends AppCompatActivity {
    private String userToken;
    private FirebaseAuth mAuth;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    userToken = task.getResult();
                });

    }

    public void onStart(){
        super.onStart();

        final FirebaseUser user = mAuth.getCurrentUser();

        progressBar.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable(){

            public void run(){

                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    // Si hay conexión a Internet en este momento

                    // Chequeamos si hay alguien logueado.
                    if(user != null) {

                        if (ActivityCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {

                            Intent intent = new Intent(SplashActivity.this, CustomPermission.class);
                            startActivity(intent);
                            finish();

                        }else{
                            QueryToken(user);
                        }

                    }else{
                        updateUI(user);
                    }

                } else {

                    progressBar.setVisibility(View.INVISIBLE);

                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay conexión a Internet en este momento", Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("Reintentar", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                            onStart();
                        }
                    });

                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                    // No hay conexión a Internet en este momento
                }
            }
        }, 1000);
    }

    public void QueryToken(final FirebaseUser user){

        ref_cuentas.orderByChild("token").equalTo(userToken).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){ //Ya hay alguien con el token---> 1-Puedo ser yo, 2-puede ser otro, 3-puedo ser yo y otro

                    long count = dataSnapshot.getChildrenCount();

                    if (count == 1){ //Si hay uno solo, soy yo u otro

                        for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            if(snapshot.getRef().getKey().equals(user.getUid())) { //soy
                                updateUI(user);

                            }else{ //agarro el que no soy


                                DialogToken(snapshot, user, 1);

                            }
                        }
                    }else{ //Si hay más de uno, son dos, agarro el que no soy

                        for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            if(!snapshot.getRef().getKey().equals(user.getUid())) {

                                DialogToken(snapshot, user, 2);

                            }
                        }
                    }
                }else {

                    ref_cuentas.child(user.getUid()).child("token").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                            if (dataSnapshot1.exists()) {
                                dataSnapshot1.getRef().setValue(userToken);
                            }
                            updateUI(user);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void updateUI(final FirebaseUser user){

        final SharedPreferences prefs = getSharedPreferences("flag_Splash", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        final boolean bandActivity = prefs.getBoolean("flag_Splash", false);

        if(user != null) {

            ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (!dataSnapshot.exists()) {

                        final FirebaseUser user = mAuth.getCurrentUser();
                        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                        Users newUser = new Users(
                                user.getUid(),
                                user.getDisplayName(),
                                "",
                                dateFormat.format(Calendar.getInstance().getTime()),
                                0,
                                user.getEmail(),
                                user.getPhotoUrl().toString(),
                                true,
                                userToken,
                                0,
                                "",
                                0,
                                0);
                        dataSnapshot.getRef().setValue(newUser);

                        IntentEditProfile();

                    }else{

                        String edad = dataSnapshot.child("birthDay").getValue(String.class);

                        if (!bandActivity) {

                            editor.putBoolean("flag_Splash", true);
                            editor.apply();

                            IntentEditProfile();

                        }else {

                            assert edad != null;
                            if (edad.equals("")){
                                IntentEditProfile();
                            }else{
                                IntentMain();
                            }
                        }
                        finish();
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                }
            });

        }else{

            IntentAuth(editor);

        }
    }

    public void IntentAuth(SharedPreferences.Editor editor) {
        Intent intent;
        editor.putBoolean("flag_Splash", false);
        editor.apply();
        FirebaseAuth.getInstance().signOut();
        com.facebook.login.LoginManager.getInstance().logOut();
        intent = new Intent(SplashActivity.this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    public void IntentEditProfile() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("flagIntent",0);
        startActivity(intent);
    }


    public void IntentMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("flagIntent",1);
        startActivity(intent);
    }



    public void DialogToken(final DataSnapshot snapshot, final FirebaseUser user, final int flag) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(SplashActivity.this, R.style.AlertDialogApp));
        builder.setTitle("Un momento...");
        builder.setMessage("Ya hay una cuenta asociada a este dispositivo, si continúa, se desvinculará a " + snapshot.child("mail").getValue(String.class) + ". ¿Desea continuar?");
        builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int id) {

                ref_cuentas.child(user.getUid()).child("token").setValue(userToken);
                snapshot.getRef().child("token").setValue("");
                updateUI(user);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int id) {

                if (flag == 2){

                    ref_cuentas.child(user.getUid()).child("token").setValue("");
                }

                updateUI(null);
                return;

            }
        });
        builder.setCancelable(false);
        builder.show();
    }

}