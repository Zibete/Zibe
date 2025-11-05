package com.zibete.proyecto1;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.util.Arrays;

import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;

public class AuthActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private LoginManager loginManager;
    private EditText edtEmail, edtPassword;
    private MaterialButton btnGoogle;
    private com.google.android.material.button.MaterialButton btnFacebook;
    private FirebaseAuth mAuth;
    private ProgressDialog progress;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final int RC_SIGN_IN = 1;

    private LinearLayout linearLogin, linearDontDelete;
    private TextView tvLogin;
    private boolean deleteUser, deleteFirebaseAccount;
    private GoogleSignInClient googleSignInClient;

    private final FacebookCallback<LoginResult> facebookCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            handleFacebookAccessToken(loginResult.getAccessToken());
        }

        @Override
        public void onCancel() {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
            updateUI(null);
        }

        @Override
        public void onError(FacebookException error) {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
            updateUI(null);
        }
    };

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        progress.dismiss();
                        Toast.makeText(this, "Error de Google: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                        updateUI(null);
                    }
                } else {
                    progress.dismiss();
                    Toast.makeText(this, "Inicio cancelado", Toast.LENGTH_LONG).show();
                    updateUI(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        SharedPreferences prefs = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean onBoarding = prefs.getBoolean("onBoarding", false);
        deleteUser = prefs.getBoolean("deleteUser", false);
        deleteFirebaseAccount = prefs.getBoolean("deleteFirebaseAccount", false);

        linearLogin = findViewById(R.id.linearLogin);
        linearDontDelete = findViewById(R.id.linearDontDelete);
//        tvLogin = findViewById(R.id.tvLogin);
        edtEmail = findViewById(R.id.edt_mail);
        edtPassword = findViewById(R.id.edt_pass);
        btnGoogle = findViewById(R.id.btn_google_custom);
        btnFacebook = findViewById(R.id.btn_facebook_custom);
        btnGoogle.setIconTint(null);
        btnFacebook.setIconTint(null);


        mAuth = FirebaseAuth.getInstance();
        progress = new ProgressDialog(this, R.style.AlertDialogApp);
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, facebookCallback);

        if (!onBoarding) {
            startActivity(new Intent(this, OnBoardingActivity.class));
            editor.putBoolean("onBoarding", true).apply();
        }

        linearLogin.setVisibility(deleteUser ? View.GONE : View.VISIBLE);
        linearDontDelete.setVisibility(deleteUser ? View.VISIBLE : View.GONE);

//        tvLogin.setText(deleteUser ? "Seleccione la cuenta que desea eliminar..." : "");

        if (deleteFirebaseAccount) {
            showDeleteAccountMessage();
            editor.putBoolean("deleteFirebaseAccount", false).apply();
        }

        setupFacebookLogin();
        setupAuthListener();
        setupGoogleSignIn();

        btnGoogle.setOnClickListener(v -> {
            progress.setMessage("Espere...");
            progress.show();
            googleSignInClient.signOut().addOnCompleteListener(task ->
                    googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
            );
        });
    }

    private void setupFacebookLogin() {
        // 1) Click del botón dispara el flujo de login reutilizando LoginManager
        btnFacebook.setOnClickListener(v -> {
            loginManager.setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);
            loginManager.logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
        });
    }

    private void setupAuthListener() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                if (deleteUser) {
                    refDatos.child(user.getUid()).removeValue();
                    refCuentas.child(user.getUid()).removeValue();
                    FirebaseStorage.getInstance().getReference()
                            .child("Users/imgPerfil/" + user.getUid() + ".jpg").delete();
                    user.delete();
                    dontDelete(null);
                    showDeleteAccountMessage();
                } else {
                    updateUI(user);
                }
            }
        };
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        showProgress();
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progress.dismiss();
                    if (task.isSuccessful()) {
                        // authStateListener llamará a updateUI(user)
                        return;
                    }
                    Exception e = task.getException();
                    String msg = "Error autenticando con Facebook.";
                    if (e != null) {
                        msg += " " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    updateUI(null);
                });
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                progress.dismiss();
                updateUI(null);
            }
        });
    }

    public void registro(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void entrar(View view) {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            progress.dismiss();
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_LONG).show();
                updateUI(null);
            }
        });
    }

    public void resetPassword(View view) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_setemail, null);
        EditText edtEmailReset = dialogView.findViewById(R.id.edt_email);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AuthTheme_Zibe))
                .setTitle("Reestablecimiento de contraseña")
                .setView(dialogView)
                .setPositiveButton("Enviar e-mail", (dialog, id) -> {
                    showProgress();
                    mAuth.sendPasswordResetEmail(edtEmailReset.getText().toString())
                            .addOnCompleteListener(task -> {
                                progress.dismiss();
                                String msg = task.isSuccessful() ?
                                        "Instrucciones enviadas a " + edtEmailReset.getText() :
                                        edtEmailReset.getText() + " no existe";
                                showSnackbar(msg);
                            });
                })
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        edtEmailReset.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean enable = s.length() > 0;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enable);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(enable ? getColor(R.color.colorG) : Color.GRAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void dontDelete(View view) {
        SharedPreferences prefs = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("deleteUser", false).apply();
        deleteUser = false;
        linearLogin.setVisibility(View.VISIBLE);
        linearDontDelete.setVisibility(View.GONE);
        tvLogin.setText("Iniciar sesión con...");
    }

    private void showProgress() {
        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);
    }

    private void showSnackbar(String message) {
        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", v -> {});
        snack.setBackgroundTint(getColor(R.color.colorC));
        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }

    private void showDeleteAccountMessage() {
        progress.dismiss();
        showSnackbar("La cuenta ha sido eliminada");
    }

    public void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            FirebaseAuth.getInstance().signOut();
            loginManager.logOut();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.removeAuthStateListener(mAuthListener);
    }
}
