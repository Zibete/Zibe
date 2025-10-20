package com.zibete.proyecto1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.util.Arrays;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;
import static com.zibete.proyecto1.MainActivity.REQUEST_LOCATION;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;
import static com.zibete.proyecto1.MainActivity.ref_datos;


public class AuthActivity extends AppCompatActivity {


    private CallbackManager callbackManager;
    private EditText edt1, edt_pass;
    private SignInButton btn_google;
    private LoginButton btn_facebook;

    private FirebaseAuth mAuth;

    private ProgressDialog progress;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleApiClient;
    LinearLayout linearLogin, linearDontDelete;
    TextView tvLogin;
    boolean deleteUser, deleteFirebaseAccount;


    String email;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        SharedPreferences authPreferences = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor authEditor = authPreferences.edit();
        boolean onBoarding = authPreferences.getBoolean("onBoarding", false);
        deleteUser = authPreferences.getBoolean("deleteUser", false);
        deleteFirebaseAccount = authPreferences.getBoolean("deleteFirebaseAccount", false);

        linearLogin = findViewById(R.id.linearLogin);
        linearDontDelete = findViewById(R.id.linearDontDelete);
        tvLogin = findViewById(R.id.tvLogin);
        mAuth = FirebaseAuth.getInstance();
        progress = new ProgressDialog(this, R.style.AlertDialogApp);
        callbackManager = CallbackManager.Factory.create();
        btn_google = findViewById(R.id.btn_google);
        btn_facebook = findViewById(R.id.btn_facebook);
        edt1 = findViewById(R.id.edt_mail);
        edt_pass = findViewById(R.id.edt_pass);



        if (!onBoarding) {
            Intent intent = new Intent(AuthActivity.this, OnBoardingActivity.class);
            startActivity(intent);
            authEditor.putBoolean("onBoarding", true);
            authEditor.apply();
        }



        if (deleteUser) {
            linearLogin.setVisibility(View.GONE);
            linearDontDelete.setVisibility(View.VISIBLE);
            tvLogin.setText("Seleccione la cuenta que desea eliminar...");
        }else{
            linearLogin.setVisibility(View.VISIBLE);
            linearDontDelete.setVisibility(View.GONE);
            tvLogin.setText("Iniciar sesión con...");
        }


        if (deleteFirebaseAccount) {

            MessageDeleteAccount();

            deleteFirebaseAccount = false;
            authEditor.putBoolean("deleteFirebaseAccount", false);
            authEditor.apply();

        }

        btn_facebook.setReadPermissions(Arrays.asList("public_profile", "email"));

        btn_facebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(final LoginResult loginResult) {

                progress.setMessage("Espere...");
                progress.show();
                progress.setCanceledOnTouchOutside(false);

                handleFacebookAccessToken(loginResult.getAccessToken());


            }

            @Override
            public void onCancel() {

                updateUI(null);
                //Toast.makeText(AuthActivity.this, "facebook onCancel ", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(FacebookException error) {

               // Toast.makeText(AuthActivity.this, "facebook onError ", Toast.LENGTH_SHORT).show();
                updateUI(null);
            }
        });//FIN FACEBOOK LOGIN




        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = mAuth.getCurrentUser();

                if(user!=null){
                    if (deleteUser) {

                        ref_datos.child(user.getUid()).removeValue();
                        ref_cuentas.child(user.getUid()).removeValue();
                        FirebaseStorage.getInstance().getReference().child("Users/imgPerfil/" + user.getUid() + ".jpg").delete();
                        user.delete();

                        dontDelete(null);

                        MessageDeleteAccount();


                    }else {
                        updateUI(user);
                    }
                }
            }
        };


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
            .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                }
            }).addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();
            btn_google.setOnClickListener(new View.OnClickListener() {
                @Override
            public void onClick(View v) {
                    progress.setMessage("Espere...");
                    progress.show();
                    progress.setCanceledOnTouchOutside(false);

                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent,RC_SIGN_IN);
            }
        });









    }//FIN OnCreate

    public void MessageDeleteAccount() {
        progress.dismiss();
        final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "La cuenta ha sido eliminada", Snackbar.LENGTH_INDEFINITE);
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
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,resultCode,data);
        if (requestCode == RC_SIGN_IN) {

            final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                if (result.isSuccess()) {


                    firebaseAuthWithGoogle(result.getSignInAccount());


                } else {

                    progress.hide();
                    updateUI(null);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    updateUI(null);
                    progress.hide();
                }
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        showProgressBar();
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()){

                    updateUI(null);
                    progress.hide();
                }
            }
        });

    }




    public void registro (View view){

        Intent intent = new Intent (AuthActivity.this, SignUpActivity.class);

        startActivity(intent);

    }




    public void updateUI(final FirebaseUser user) {
        if(user != null) {

            Intent intent = new Intent(AuthActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            progress.hide();
            finish();

        }else{

            FirebaseAuth.getInstance().signOut();
            com.facebook.login.LoginManager.getInstance().logOut();
        }
    }


    public void entrar (View view){
        email = edt1.getText().toString().trim();
        password = edt_pass.getText().toString().trim();


        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Introduzca un e-mail",Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)){
            Toast.makeText(this, "Introduzca una contraseña",Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {


                    Toast.makeText(AuthActivity.this, "Alguno de los datos ingresados no es correcto",Toast.LENGTH_LONG).show();
                    progress.hide();
                    updateUI(null);
                }
            }
        });


    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.removeAuthStateListener(mAuthListener);

    }

    private void showProgressBar() {
        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

    }


    public void resetPassword(View view) {


        LayoutInflater inflater = LayoutInflater.from(AuthActivity.this);

        View dialog_setemail = inflater.inflate(R.layout.dialog_setemail,null);

        final EditText edt_email = dialog_setemail.findViewById(R.id.edt_email);
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(AuthActivity.this, R.style.AuthTheme_NoActionBar));



        mBuilder.setView(dialog_setemail);
        mBuilder.setCancelable(true);
        mBuilder.setTitle("Reestablecimiento de contraseña");





        mBuilder.setPositiveButton("Enviar e-mail", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int id) {


                progress.setMessage("Espere...");
                progress.show();
                progress.setCanceledOnTouchOutside(false);

                mAuth.sendPasswordResetEmail(edt_email.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    progress.dismiss();
                                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Las instrucciones de reestablecimiento fueron enviadas a " + edt_email.getText().toString(), Snackbar.LENGTH_INDEFINITE);
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

                                } else {
                                    progress.dismiss();
                                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), edt_email.getText().toString() + " no existe", Snackbar.LENGTH_INDEFINITE);
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
                                }
                            }
                        });



            }
        });
        mBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int id) {
                return;
            }
        });



        final AlertDialog dialog = mBuilder.create();


        edt_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_email.length() == 0) {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);

                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edt_email.length() != 0) {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(AuthActivity.this.getResources().getColor(R.color.colorG));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (edt_email.length() == 0) {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                }
            }
        });

        dialog.show();
        ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);








    }


    public void dontDelete(View view) {

        SharedPreferences authPreferences = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor authEditor = authPreferences.edit();
        authEditor.putBoolean("deleteUser", false);
        deleteUser = false;
        authEditor.apply();

        linearLogin.setVisibility(View.VISIBLE);
        linearDontDelete.setVisibility(View.GONE);
        tvLogin.setText("Iniciar sesión con...");


    }
}