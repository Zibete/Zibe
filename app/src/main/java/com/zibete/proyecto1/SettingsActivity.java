package com.zibete.proyecto1;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.zibete.proyecto1.model.ChatsGroup;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.ui.EditProfileFragment;
import com.zibete.proyecto1.ui.GruposFragment;
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment;
import com.zibete.proyecto1.utils.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import static com.zibete.proyecto1.ChatGroupFragment.listenerGroupChat;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.MainActivity.layoutSettings;
import static com.zibete.proyecto1.Constants.listenerGroupBadge;
import static com.zibete.proyecto1.Constants.listenerMsgUnreadBadge;
import static com.zibete.proyecto1.Constants.listenerToken;
import static com.zibete.proyecto1.utils.FirebaseRefs.refChatUnknown;
import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupChat;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.PageAdapterGroup.valueEventListenerTitle;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.editor;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupNotifications;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.inGroup;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.individualNotifications;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.readGroupMsg;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userDate;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userType;


public class SettingsActivity extends AppCompatActivity {

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Toolbar toolbar;
    TextView my_email;
    LinearLayout btn_change_email, btn_change_password;

    LinearLayout linear_notif_grupales, linear_notif_individuales;

    ImageView arrow_down_change_pass, arrow_up_change_pass, arrow_down_change_email, arrow_up_change_email;
    Button btn_logout, btn_report, btn_delete_account;
    ImageButton btn_save_pass,btn_save_email ;

//    ShapeableImageView btn_save_email;
    LinearLayout linearChangeEmail, linearChangePassword;
    EditText edt_password_email, edt_password_pass, edt_new_mail, edt_new_pass;
    String newEmail, newPassword, password;
    ProgressDialog progress;
    String provider;
    SwitchMaterial switch_individualNotifications, switch_groupNotifications;

    private CallbackManager callbackManager;
    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        arrow_down_change_pass = findViewById(R.id.arrow_down_change_pass);
        arrow_up_change_pass = findViewById(R.id.arrow_up_change_pass);
        arrow_down_change_email = findViewById(R.id.arrow_down_change_email);
        arrow_up_change_email = findViewById(R.id.arrow_up_change_email);

        toolbar = findViewById(R.id.toolbar_ajustes);
        my_email = findViewById(R.id.my_email);
        btn_change_email = findViewById(R.id.btn_change_email);
        btn_change_password = findViewById(R.id.btn_change_password);
        btn_save_pass = findViewById(R.id.btn_save_pass);
        btn_save_email = findViewById(R.id.btn_save_email);
        btn_logout = findViewById(R.id.btn_logout);
        btn_report = findViewById(R.id.btn_report);
        btn_delete_account = findViewById(R.id.btn_delete_account);
        linearChangeEmail = findViewById(R.id.linearChangeEmail);
        linearChangePassword = findViewById(R.id.linearChangePassword);
        edt_password_email = findViewById(R.id.edt_password_email);
        edt_password_pass = findViewById(R.id.edt_password_pass);
        edt_new_mail = findViewById(R.id.edt_new_mail);
        edt_new_pass = findViewById(R.id.edt_new_pass);
        switch_groupNotifications = findViewById(R.id.switch_groupNotifications);
        switch_individualNotifications = findViewById(R.id.switch_individualNotifications);
        linear_notif_grupales = findViewById(R.id.linear_notif_grupales);
        linear_notif_individuales = findViewById(R.id.linear_notif_individuales);


        linear_notif_grupales.setOnClickListener(v -> switch_groupNotifications.performClick());

        linear_notif_individuales.setOnClickListener(v -> switch_individualNotifications.performClick());


        progress = new ProgressDialog(SettingsActivity.this, R.style.AlertDialogApp);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        switch_groupNotifications.setChecked(groupNotifications);
        switch_individualNotifications.setChecked(individualNotifications);


/*
        ref_datos.child(user.getUid()).child("Settings").child("groupNotifications").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    groupNotifications = dataSnapshot.getValue(boolean.class);
                }else{
                    groupNotifications = true;
                    ref_datos.child(user.getUid()).child("Settings").child("groupNotifications").setValue(true);
                }
                switch_groupNotifications.setChecked(groupNotifications);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        ref_datos.child(user.getUid()).child("Settings").child("individualNotifications").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    individualNotifications = dataSnapshot.getValue(boolean.class);
                }else{
                    ref_datos.child(user.getUid()).child("Settings").child("individualNotifications").setValue(true);
                    individualNotifications = true;
                }
                switch_individualNotifications.setChecked(individualNotifications);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

 */

        switch_groupNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switch_groupNotifications.isChecked()){

                    groupNotifications = true;
                    editor.putBoolean("groupNotifications", true);
                    editor.apply();

                    //ref_datos.child(user.getUid()).child("Settings").child("groupNotifications").setValue(true);
                    Toast.makeText(SettingsActivity.this, "Notificaciones grupales encendidas", Toast.LENGTH_SHORT).show();
                }else{

                    groupNotifications = false;
                    editor.putBoolean("groupNotifications", false);
                    editor.apply();

                    //ref_datos.child(user.getUid()).child("Settings").child("groupNotifications").setValue(false);
                    Toast.makeText(SettingsActivity.this, "Notificaciones grupales apagadas", Toast.LENGTH_SHORT).show();

                }
            }
        });
        switch_individualNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switch_individualNotifications.isChecked()){
                    individualNotifications = true;
                    editor.putBoolean("individualNotifications", true);
                    editor.apply();

                    //ref_datos.child(user.getUid()).child("Settings").child("individualNotifications").setValue(true);
                    Toast.makeText(SettingsActivity.this, "Notificaciones individuales encendidas", Toast.LENGTH_SHORT).show();

                }else{
                    individualNotifications = false;
                    editor.putBoolean("individualNotifications", false);
                    editor.apply();
                    //ref_datos.child(user.getUid()).child("Settings").child("individualNotifications").setValue(false);
                    Toast.makeText(SettingsActivity.this, "Notificaciones individuales apagadas", Toast.LENGTH_SHORT).show();

                }
            }
        });

        btn_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(SettingsActivity.this, ReportActivity.class);
                startActivity(intent);


            }
        });


        for (UserInfo user: Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getProviderData()) {
            if (user.getProviderId().equals("facebook.com")) {
                provider = "Facebook";
            } else if (user.getProviderId().equals("google.com")) {
                provider = "Google";
            }
        }

        String email = user.getEmail();
        if (provider == null) {
            my_email.setText(email);
        }else{
            my_email.setText(email + " (" + provider + ")");
        }




        btn_save_email.setEnabled(false);
        btn_save_pass.setEnabled(false);

        edt_password_pass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_password_pass.length() == 0) {
                    btn_save_pass.setEnabled(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(edt_new_pass.length() == 0)) {
                    if (!(edt_password_pass.length() == 0)) {
                        btn_save_pass.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_password_pass.length() == 0) {
                    btn_save_pass.setEnabled(false);
                }
            }
        });
        edt_new_pass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_new_pass.length() == 0) {
                    btn_save_pass.setEnabled(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(edt_new_pass.length() == 0)) {
                    if (!(edt_password_pass.length() == 0)) {
                        btn_save_pass.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_new_pass.length() == 0) {
                    btn_save_pass.setEnabled(false);
                }
            }
        });

        edt_password_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_password_email.length() == 0) {
                    btn_save_email.setEnabled(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(edt_password_email.length() == 0)) {
                    if (!(edt_new_mail.length() == 0)) {
                        btn_save_email.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_password_email.length() == 0) {
                    btn_save_email.setEnabled(false);
                }
            }
        });
        edt_new_mail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_new_mail.length() == 0) {
                    btn_save_email.setEnabled(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!(edt_new_mail.length() == 0)) {
                    if (!(edt_password_email.length() == 0)) {
                        btn_save_email.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_new_mail.length() == 0) {
                    btn_save_email.setEnabled(false);
                }
            }
        });


        btn_change_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (provider == null) {

                    if (linearChangeEmail.getVisibility() == View.GONE) {
                        linearChangeEmail.setVisibility(View.VISIBLE);
                        arrow_down_change_email.setVisibility(View.GONE);
                        arrow_up_change_email.setVisibility(View.VISIBLE);

                    } else {
                        linearChangeEmail.setVisibility(View.GONE);
                        arrow_down_change_email.setVisibility(View.VISIBLE);
                        arrow_up_change_email.setVisibility(View.GONE);
                    }
                } else {
                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No disponible para usuarios autenticados con " + provider, Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                        }
                    });
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();                  }
            }
        });

        btn_change_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (provider == null) {

                    if (linearChangePassword.getVisibility() == View.GONE) {
                        linearChangePassword.setVisibility(View.VISIBLE);
                        arrow_down_change_pass.setVisibility(View.GONE);
                        arrow_up_change_pass.setVisibility(View.VISIBLE);
                    } else {
                        linearChangePassword.setVisibility(View.GONE);
                        arrow_down_change_pass.setVisibility(View.VISIBLE);
                        arrow_up_change_pass.setVisibility(View.GONE);
                    }
                }else{
                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No disponible para usuarios autenticados con " + provider, Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                        }
                    });
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                }
            }
        });


        btn_save_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                password = edt_password_email.getText().toString().trim();
                newEmail = edt_new_mail.getText().toString().trim();
                newPassword = null;

                checkData(password, newEmail, newPassword);
            }
        });


        btn_save_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                password = edt_password_pass.getText().toString().trim();
                newEmail = null;
                newPassword = edt_new_pass.getText().toString().trim();

                checkData(password, newEmail, newPassword);
            }
        });





        btn_delete_account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogApp))
                        .setTitle("Eliminar Cuenta")
                        .setMessage("¿Está seguro de eliminar su cuenta?")
                        .setCancelable(false)
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {


                                new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogApp))
                                        .setTitle("Atención")
                                        .setMessage("Si continúa se eliminarán todos sus datos personales, también las fotos y las conversaciones. ¿Desea continuar?")
                                        .setCancelable(false)
                                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface builder, int id) {

                                                if (provider == null) {

                                                    LayoutInflater inflater = LayoutInflater.from(SettingsActivity.this);

                                                    View dialog_setpassword = inflater.inflate(R.layout.dialog_setpassword, null);

                                                    final EditText edt_password = dialog_setpassword.findViewById(R.id.edt_password);
                                                    final AlertDialog.Builder mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogApp));


                                                    mBuilder.setView(dialog_setpassword);
                                                    mBuilder.setCancelable(true);
                                                    mBuilder.setTitle("Ingrese su contraseña para finalizar");


                                                    mBuilder.setPositiveButton("Eliminar cuenta", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface builder, int id) {


                                                            progress.setMessage("Espere...");
                                                            progress.show();
                                                            progress.setCanceledOnTouchOutside(false);

                                                            getCredential(edt_password.getText().toString(), null, null, edt_password.getText().toString());


                                                        }
                                                    });
                                                    mBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface builder, int id) {
                                                            return;
                                                        }
                                                    });


                                                    final AlertDialog dialog = mBuilder.create();

                                                    edt_password.addTextChangedListener(new TextWatcher() {
                                                        @Override
                                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                            if (edt_password.length() == 0) {
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);

                                                            }
                                                        }

                                                        @Override
                                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                            if (edt_password.length() != 0) {
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                                                            }
                                                        }

                                                        @Override
                                                        public void afterTextChanged(Editable s) {
                                                            if (edt_password.length() == 0) {
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                                                            }
                                                        }
                                                    });

                                                    dialog.show();
                                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                                                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);


                                                }else{

                                                    progress.setMessage("Espere...");
                                                    progress.show();
                                                    progress.setCanceledOnTouchOutside(false);
                                                    getCredential(null, null, null, provider);

                                                }

                                            }
                                        })
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface builder, int id) {
                                                return;
                                            }
                                        })
                                        .show();




                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {
                                return;
                            }
                        })
                        .show();




            }
        });




        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogApp))
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Está seguro de cerrar su sesión?")
                        .setCancelable(false)
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {


                                logOut(null);


                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {
                                return;
                            }
                        })
                        .show();




            }
        });


    }

    public void exitGroup() {
        inGroup = false;

        if (listenerGroupBadge != null) {
            refGroupChat.child(groupName).removeEventListener(listenerGroupBadge);
        }
        if (listenerMsgUnreadBadge != null) {
            final Query query = refDatos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
            query.removeEventListener(listenerMsgUnreadBadge);
        }
        if (valueEventListenerTitle != null) {
            refGroupUsers.child(groupName).removeEventListener(valueEventListenerTitle); //Elimino el usuario
        }
        if (listenerGroupChat != null) {
            refGroupChat.child(groupName).removeEventListener(listenerGroupChat);
        }


        refDatos.child(user.getUid()).child(chatWithUnknown).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String key = snapshot.getKey();
                    refChatUnknown.child(user.getUid() + " <---> " + key).removeValue(); //Elimino mi chat con él
                    refChatUnknown.child(key + " <---> " + user.getUid()).removeValue(); //Elimino su chat conmigo
                    refDatos.child(key).child(chatWithUnknown).child(user.getUid()).removeValue(); //Elimino su chat lista
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        refDatos.child(user.getUid()).child(chatWithUnknown).removeValue(); //Elimino mis chat lista


        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");

        final ChatsGroup chatmsg = new ChatsGroup(
                "abandonó la sala",
                dateFormat3.format(Calendar.getInstance().getTime()),
                userName,
                user.getUid(),
                0,
                userType);
        refGroupChat.child(groupName).push().setValue(chatmsg);


        //badgeDrawableGroup.setVisible(false);
        refGroupUsers.child(groupName).child(user.getUid()).removeValue();

        inGroup = false;
        userName = "";
        groupName = "";
        userType = 2;
        readGroupMsg = 0;
        userDate = "";

        editor.putBoolean("inGroup", false);
        editor.putString("userName", "");
        editor.putString("groupName", "");
        editor.putInt("userType", 2);
        editor.putInt("readGroupMsg", 0);
        editor.putString("userDate", "");

        editor.apply();

        layoutSettings.setVisibility(View.GONE);
        invalidateOptionsMenu();

        Fragment newFragment = new GruposFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, newFragment);
        toolbar.setTitle(R.string.menu_grupos);
        transaction.commit();
    }

    public void logOut(String deleteUser) {

        if (deleteUser == null) {

            UserRepository.setUserOffline(getApplicationContext(), user.getUid());
        }

        if(inGroup) {
            exitGroup();
        }

        if (listenerToken != null) {
            refCuentas.child(user.getUid()).child("token").removeEventListener(listenerToken);
        }


        UsuariosFragment.DeletePreferences();
        EditProfileFragment.DeleteProfilePreferences(this);

        FirebaseAuth.getInstance().signOut();
        com.facebook.login.LoginManager.getInstance().logOut();



        finish();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);



    }


    private void getCredential(String password, final String newEmail, final String newPassword, final String deleteUser) {

        AuthCredential credential;
        if (provider == null) {

            credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            reAuthenticate(newEmail, newPassword, deleteUser, credential);

        } else if (provider.equals("Facebook")) {

            AccessToken token = AccessToken.getCurrentAccessToken();
            credential = FacebookAuthProvider.getCredential(token.getToken());
            reAuthenticate(newEmail, newPassword, deleteUser, credential);

        } else if (provider.equals("Google")) {

            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {

                credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                reAuthenticate(newEmail, newPassword, deleteUser, credential);

            }

        }

    }


    private void reAuthenticate(final String newEmail, final String newPassword, final String deleteUser, AuthCredential credential) {

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (!task.isSuccessful()) {
                    progress.dismiss();
                    if (provider == null){

                        Toast.makeText(SettingsActivity.this, "La contraseña es incorrecta", Toast.LENGTH_SHORT).show();

                    }else{

                        new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogApp))
                                .setTitle("Error")
                                .setMessage("Se necesita un inicio de sesión reciente para eliminar la cuenta")
                                .setCancelable(false)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface builder, int id) {

                                        SharedPreferences authPreferences = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor authEditor = authPreferences.edit();
                                        authEditor.putBoolean("deleteUser", true);
                                        authEditor.apply();


                                        logOut(null);

                                    }
                                })
                                .show();



                    }

                } else if (newEmail != null) {

                    updateEmail(newEmail);

                } else if (newPassword != null) {

                    updatePassword(newPassword);

                } else if (deleteUser != null){

                    deleteFirebaseUser(deleteUser);

                }
            }
        });
    }



    private void deleteFirebaseUser(String deleteUser) {

        SharedPreferences authPreferences = getSharedPreferences("AuthPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor authEditor = authPreferences.edit();
        authEditor.putBoolean("deleteFirebaseAccount", true);
        authEditor.apply();


        refDatos.child(user.getUid()).removeValue();

        refCuentas.child(user.getUid()).removeValue();

        FirebaseStorage.getInstance().getReference().child("Users/imgPerfil/" + user.getUid() + ".jpg").delete();

        logOut(deleteUser);

        user.delete();


    }

    private void updatePassword(String newPassword) {

        user.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progress.dismiss();
                        if (task.isSuccessful()) {

                            Toast.makeText(SettingsActivity.this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                            startActivity(intent);
                            finish();

                        }else{

                            Toast.makeText(SettingsActivity.this, "La contraseña debe tener al menos seis caracteres", Toast.LENGTH_SHORT).show();

                        }
                    }
                });



    }



    private void updateEmail(String newEmail) {

        user.updateEmail(newEmail)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progress.dismiss();
                        if (task.isSuccessful()) {

                            Toast.makeText(SettingsActivity.this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                            startActivity(intent);
                            finish();

                        }else{

                            Toast.makeText(SettingsActivity.this, "Introduzca un e-mail válido", Toast.LENGTH_SHORT).show();

                        }
                    }
                });

        }

    private void checkData(String password, String newEmail, String newPassword) {

        if (TextUtils.isEmpty(password)){
            Toast.makeText(this, "Introduzca una contraseña",Toast.LENGTH_SHORT).show();
            return;
        }

        if (newEmail !=null) {
            if (TextUtils.isEmpty(newEmail)) {
                Toast.makeText(this, "Introduzca un e-mail", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (newPassword !=null) {
            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "Introduzca la nueva contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

        getCredential(password,newEmail,newPassword, null);

    }






    @Override
    protected void onPause() {
        super.onPause();
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserRepository.setUserOnline(getApplicationContext(), user.getUid());
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
