package com.zibete.proyecto1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.zibete.proyecto1.Splash.SplashActivity;

import static com.zibete.proyecto1.Constants.REQUEST_LOCATION;

public class CustomPermission extends AppCompatActivity {

    Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_permission);

        start = findViewById(R.id.btnStart);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(CustomPermission.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Zibe necesita acceso a su ubicación para poder funcionar", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();

                }


                ActivityCompat.requestPermissions(
                        CustomPermission.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);

            }
        });



    }




    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(CustomPermission.this, SplashActivity.class);
                startActivity(intent);
                finish();

            }else{

                final Snackbar snack = Snackbar.make(CustomPermission.this.findViewById(android.R.id.content), "Se cerrará su sesión", Snackbar.LENGTH_INDEFINITE);
                snack.setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        snack.dismiss();
                        FirebaseAuth.getInstance().signOut();
                        com.facebook.login.LoginManager.getInstance().logOut();
                        Intent intent = new Intent(CustomPermission.this, SplashActivity.class);
                        startActivity(intent);
                        finish();

                    }
                });
                snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();



            }
        }
    }


}