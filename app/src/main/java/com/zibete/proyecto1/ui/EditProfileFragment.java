package com.zibete.proyecto1.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zibete.proyecto1.FixedSwipeRefreshLayout;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.adapters.AdapterUsers;
import com.zibete.proyecto1.model.Users;
import com.zibete.proyecto1.utils.DateUtils;
import com.zibete.proyecto1.utils.ProfileUiBinder;
import com.zibete.proyecto1.utils.UserRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;
import static com.zibete.proyecto1.utils.Constants.CAMERA_SELECTED;
import static com.zibete.proyecto1.utils.Constants.PERMISSIONS_EDIT_PROFILE;
import static com.zibete.proyecto1.utils.Constants.PHOTO_SELECTED;
import static com.zibete.proyecto1.MainActivity.filter;
import static com.zibete.proyecto1.MainActivity.refresh;
import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;

public class EditProfileFragment extends Fragment {

    // UI
    private ResizableImageViewProfile ftPerfil;
    private ProgressBar loadingPhoto;
    private TextInputEditText edtNameUser, edtDesc, edtDate;
    private TextView tvAge, completeProfile, btnDone, btnOk;

    private ExtendedFloatingActionButton btSave, btEdit;
    private LinearLayout linearButtonsEdit, linearOnBoardingProfile;

    // State
    private String birthDay;
    private ProgressDialog progress;
    private ArrayList<String> photoList;

    // Firebase
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageReference = storage.getReference();
    private StorageReference refImgUser;

    // Media
    private ContentValues values;
    private Uri imageUri;
    private Bitmap thumbnail;
    private String imageurl;
    private byte[] thumb_byte;

    // Tokens
    private String myInstallId = null;
    private String myFcmToken = null;

    // Prefs
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean flagProfile;

    public EditProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        setHasOptionsMenu(true);

        if (user == null) return view;

        // Prefs
        Context ctx = requireContext();
        prefs = ctx.getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE);
        editor = prefs.edit();
        flagProfile = prefs.getBoolean("flag_OnBoardingProfile", false);

        // Firebase storage ref
        refImgUser = storageReference.child("Users/imgPerfil/" + user.getUid() + ".jpg");

        // Tokens
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myInstallId = t.getResult(); });
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myFcmToken = t.getResult(); });

        // Bind UI
        ftPerfil = view.findViewById(R.id.ftPerfil);
        loadingPhoto = view.findViewById(R.id.loadingPhoto);
        edtNameUser = view.findViewById(R.id.edtNameUser);
        edtDesc = view.findViewById(R.id.edtDesc);
        edtDate = view.findViewById(R.id.edtFecha);
        tvAge = view.findViewById(R.id.tvEdad);
        linearButtonsEdit = view.findViewById(R.id.linearButtonsEdit);
        btEdit = view.findViewById(R.id.bt_edit);
        btSave = view.findViewById(R.id.bt_save);
        btnDone = view.findViewById(R.id.btn_done);
        linearOnBoardingProfile = view.findViewById(R.id.linearOnBoardingProfile);
        completeProfile = view.findViewById(R.id.completeProfile);
        btnOk = view.findViewById(R.id.btn_ok);

        progress = new ProgressDialog(getContext(), R.style.AlertDialogApp);
        photoList = new ArrayList<>();

//        // === Ajustes para que la imagen respete ancho completo y alto auto ===
//        ViewGroup.LayoutParams lp = ftPerfil.getLayoutParams();
//        if (lp != null) {
//            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
//            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//            ftPerfil.setLayoutParams(lp);
//        }
//        ftPerfil.setAdjustViewBounds(true);
//        ftPerfil.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Onboarding overlay
        btSave.setEnabled(false);
        if (!flagProfile) {
            linearOnBoardingProfile.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.VISIBLE);
            editor.putBoolean("flag_OnBoardingProfile", true).apply();
        } else {
            linearOnBoardingProfile.setVisibility(View.GONE);
            btnDone.setVisibility(View.GONE);
        }

        btnOk.setOnClickListener(v1 -> linearOnBoardingProfile.setVisibility(View.GONE));
        linearOnBoardingProfile.setOnClickListener(v12 -> linearOnBoardingProfile.setVisibility(View.GONE));

        btnDone.setOnClickListener(v -> refCuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;
                String bd = dataSnapshot.child("birthDay").getValue(String.class);
                if (bd == null || bd.isEmpty()) {
                    snack("Complete su fecha de nacimiento");
                } else {
                    Intent intent = new Intent(getContext(), SplashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        }));

        // Cargar datos actuales
        bindCurrentProfile();

        // Nombre y desc desde Auth / DB
        if (edtNameUser != null) edtNameUser.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
        refCuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;
                String desc = dataSnapshot.child("descripcion").getValue(String.class);
                if (edtDesc != null) edtDesc.setText(desc != null ? desc : "");
                btSave.setEnabled(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Click foto -> SlidePhotoActivity
        ftPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SlidePhotoActivity.class);
            intent.putExtra("photoList", photoList);
            intent.putExtra("position", 0);
            intent.putExtra("rotation", 0);
            startActivity(intent);
        });

        // Editar foto (dialog)
        btEdit.setOnClickListener(v -> EditProfilePhoto());

        // Date picker Material
        edtDate.setOnClickListener(v -> showMaterialDatePicker());

        // Guardar
        btSave.setOnClickListener(v -> save());

        // Habilitar guardar al modificar texto
        TextWatcher enableSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { btSave.setEnabled(true); }
            @Override public void afterTextChanged(Editable s) { }
        };
        edtNameUser.addTextChangedListener(enableSaveWatcher);
        edtDesc.addTextChangedListener(enableSaveWatcher);

        return view;
    }

    private void bindCurrentProfile() {
        refCuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                String foto = dataSnapshot.child("foto").getValue(String.class);
                String bd = dataSnapshot.child("birthDay").getValue(String.class);
                String desc = dataSnapshot.child("descripcion").getValue(String.class);

                // Edad + textos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (bd == null || bd.isEmpty()) {
                        completeProfile.setText("Te pediremos que completes tu fecha de nacimiento. También podrás agregar información sobre vos o cambiar tu foto de perfil");
                    } else {
                        try {
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate fechaNac = LocalDate.parse(bd, fmt);
                            String edad = String.valueOf(Period.between(fechaNac, LocalDate.now()).getYears());
                            tvAge.setText(edad);
                            edtDate.setText(bd);
                            completeProfile.setText("Actualizá tu perfil con una foto y tus datos personales");
                        } catch (Exception ignored) { }
                    }
                }

                if (edtDesc != null) edtDesc.setText(desc != null ? desc : "");

                loadingPhoto.setVisibility(View.VISIBLE);

                // IMPORTANTE: sin transformaciones para respetar FIT_CENTER + adjustViewBounds
                RequestOptions opts = new RequestOptions().dontTransform();

                Glide.with(requireContext())
                        .load(foto != null && !foto.isEmpty() ? foto : getString(R.string.URL_PHOTO_DEF))
                        .apply(opts)
                        .listener(new RequestListener<Drawable>() {
                            @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }
                            @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ftPerfil);

                if (foto != null && !foto.isEmpty()) photoList.add(foto);
                btSave.setEnabled(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ======== DATE PICKER (Material) ========
    private void showMaterialDatePicker() {
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        MaterialDatePicker<Long> picker = builder
                .setTitleText(getString(R.string.fecha_nacimiento))
                .setCalendarConstraints(constraints.build())
                .setTheme(R.style.ZibeDatePickerOverlay)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            String formatted = epochToDate(selection);
            birthDay = formatted;
            edtDate.setText(formatted);

            try {
                int edad = computeAgeFromString(formatted);
                tvAge.setText(String.valueOf(edad));
            } catch (Exception ignored) { }

            btSave.setEnabled(true);
        });

        picker.show(getParentFragmentManager(), "ZIBE_BIRTHDATE_PICKER");
    }

    private String epochToDate(Long epochMillis) {
        if (epochMillis == null) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate ld = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
            return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(epochMillis);
        }
    }

    private int computeAgeFromString(String ddMMyyyy) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birth = LocalDate.parse(ddMMyyyy, fmt);
            return Period.between(birth, LocalDate.now()).getYears();
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar b = Calendar.getInstance();
            b.setTime(Objects.requireNonNull(sdf.parse(ddMMyyyy)));
            Calendar now = Calendar.getInstance();
            int age = now.get(Calendar.YEAR) - b.get(Calendar.YEAR);
            if (now.get(Calendar.DAY_OF_YEAR) < b.get(Calendar.DAY_OF_YEAR)) age--;
            return age;
        }
    }

    // ======== FOTO DE PERFIL ========
    public void EditProfilePhoto() {
        ArrayList<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.CAMERA);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            perms.add(Manifest.permission.CAMERA);
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        boolean needRequest = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true; break;
            }
        }
        if (needRequest) {
            requestPermissions(perms.toArray(new String[0]), PERMISSIONS_EDIT_PROFILE);
            return;
        }

        final View viewFilter = getLayoutInflater().inflate(R.layout.select_source_pic, null);
        ImageView deleteSelected = viewFilter.findViewById(R.id.deleteSelected);
        MaterialCardView cameraSelection = viewFilter.findViewById(R.id.cameraSelection);
        MaterialCardView gallerySelection = viewFilter.findViewById(R.id.gallerySelection);
        TextView tv_title = viewFilter.findViewById(R.id.tv_title);
        MaterialCardView card_edit_delete = viewFilter.findViewById(R.id.card_edit_delete);
        ImageView img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);

        card_edit_delete.setVisibility(View.VISIBLE);
        tv_title.setText(getResources().getString(R.string.editar_foto_de_perfil));

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogApp));
        builder.setView(viewFilter);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();
        dialog.show();

        deleteSelected.setOnClickListener(v -> {
            btSave.setEnabled(true);
            imageUri = Uri.parse(getString(R.string.URL_PHOTO_DEF));
            imageurl = getString(R.string.URL_PHOTO_DEF);

            loadingPhoto.setVisibility(View.VISIBLE);

            // Respetar FIT_CENTER + alto auto
            Glide.with(requireContext())
                    .load(imageUri)
                    .apply(new RequestOptions().dontTransform())
                    .listener(new RequestListener<Drawable>() {
                        @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            loadingPhoto.setVisibility(View.GONE);
                            return false;
                        }
                        @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            loadingPhoto.setVisibility(View.GONE);
                            return false;
                        }
                    }).into(ftPerfil);

            thumb_byte = null;
            dialog.dismiss();
        });

        cameraSelection.setOnClickListener(v -> {
            startCamera();
            dialog.dismiss();
        });

        img_cancel_dialog.setOnClickListener(v -> dialog.dismiss());

        gallerySelection.setOnClickListener(v -> {
            Intent gallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            try {
                startActivityForResult(gallery, PHOTO_SELECTED);
            } catch (Exception e) {
                snack("No se pudo abrir la galería");
            }
            dialog.dismiss();
        });
    }

    public void startCamera() {
        try {
            values = new ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());
            imageUri = requireContext().getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA_SELECTED);
        } catch (Exception e) {
            snack("No se pudo abrir la cámara");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isAdded()) return;

        if (progress == null) {
            progress = new ProgressDialog(getContext(), R.style.AlertDialogApp);
        }
        progress.setMessage("Espere...");
        progress.setCanceledOnTouchOutside(false);

        try {
            progress.show();
            // Aquí cargarías el preview de lo tomado/seleccionado si lo procesás.
        } catch (Throwable ignored) {
        } finally {
            if (progress != null && progress.isShowing()) progress.dismiss();
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        try {
            String[] proj = {android.provider.MediaStore.Images.Media.DATA};
            Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String res = cursor.getString(column_index);
            cursor.close();
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_EDIT_PROFILE) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            }
            if (allGranted) {
                EditProfilePhoto();
            } else {
                snack("Necesitás otorgar permisos para cambiar la foto.");
            }
        }
    }

    private void save() {
        if (progress == null) {
            progress = new ProgressDialog(getContext(), R.style.AlertDialogApp);
        }
        progress.setMessage("Espere...");
        progress.setCanceledOnTouchOutside(false);
        progress.show();

        try {
            String name = edtNameUser.getText() == null ? "" : edtNameUser.getText().toString().trim();
            String fecha = edtDate.getText() == null ? "" : edtDate.getText().toString().trim();
            String desc = edtDesc.getText() == null ? "" : edtDesc.getText().toString().trim();

            if (fecha.isEmpty()) {
                snack("Debe ingresar su fecha de nacimiento para continuar");
                return;
            }

            int edad;
            try {
                edad = computeAgeFromString(fecha);
                if (edad < 18) {
                    snack("Lo sentimos, debe ser mayor de 18 años para utilizar la App");
                    return;
                }
            } catch (Exception e) {
                snack("Fecha de nacimiento inválida");
                return;
            }

            if (thumb_byte != null) {
                try {
                    File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zibe");
                    if (!folder.exists() && !folder.mkdirs()) {
                        Log.e("EditProfile", "No se pudo crear el directorio (o ya existe)");
                    }

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyHHmm", Locale.getDefault());
                    String fname = "IMG_" + simpleDateFormat.format(Calendar.getInstance().getTime());
                    File file = new File(folder, fname + ".jpg");
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    UploadTask uploadTask = refImgUser.putBytes(thumb_byte);
                    uploadTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                        return refImgUser.getDownloadUrl();
                    }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            refCuentas.child(user.getUid()).child("foto").setValue(downloadUri.toString());
                            imageUri = downloadUri;
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(downloadUri)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                    });
                } catch (Throwable t) {
                    Log.e("EditProfile", "Error subiendo foto", t);
                }
            }

            UserProfileChangeRequest profileUpdates;
            if (imageurl != null && imageurl.equals(getString(R.string.URL_PHOTO_DEF))) {
                try { refImgUser.delete(); } catch (Throwable ignored) { }
                refCuentas.child(user.getUid()).child("foto").setValue(imageurl);
                profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .setPhotoUri(imageUri)
                        .build();
            } else {
                profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();
            }
            user.updateProfile(profileUpdates);

            SimpleDateFormat stamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String nowStr = stamp.format(Calendar.getInstance().getTime());

            refCuentas.child(user.getUid()).child("nombre").setValue(name);
            refCuentas.child(user.getUid()).child("birthDay").setValue(fecha);
            refCuentas.child(user.getUid()).child("age").setValue(edad);
            refCuentas.child(user.getUid()).child("descripcion").setValue(desc);
            refCuentas.child(user.getUid()).child("date").setValue(nowStr);

            if (myInstallId != null && !myInstallId.isEmpty()) {
                refCuentas.child(user.getUid()).child("installId").setValue(myInstallId);
                refCuentas.child(user.getUid()).child("token").setValue(myInstallId);
            }
            if (myFcmToken != null && !myFcmToken.isEmpty()) {
                refCuentas.child(user.getUid()).child("fcmToken").setValue(myFcmToken);
            }

            updateUI(user);
        } finally {
            if (progress != null && progress.isShowing()) {
                try { progress.dismiss(); } catch (Throwable ignored) {}
            }
        }
    }

    private void updateUI(FirebaseUser user) {
        if (!isAdded()) return;
        if (user != null) {
            try {
                Toast.makeText(getContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) { }
            if (progress != null && progress.isShowing()) progress.dismiss();
            Intent intent = new Intent(getContext(), SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            if (progress != null && progress.isShowing()) progress.dismiss();
        }
    }

    public static void DeleteProfilePreferences(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("flag_OnBoardingProfile", false);
        editor.apply();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        try {
            if (menu.findItem(R.id.action_search) != null) {
                menu.findItem(R.id.action_search).setVisible(false);
            }
            if (menu.findItem(R.id.action_favorites) != null) {
                menu.findItem(R.id.action_favorites).setVisible(false);
            }
        } catch (Throwable ignored) { }
    }

    private void snack(String msg) {
        if (!isAdded() || getView() == null) return;
        final Snackbar snack = Snackbar.make(getView(), msg, Snackbar.LENGTH_INDEFINITE);
        snack.setAction("OK", v -> snack.dismiss());
        try {
            snack.setBackgroundTint(getResources().getColor(R.color.zibe_pink));
            TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } catch (Throwable ignored) { }
        snack.show();
    }

    public static class UsuariosViewModel extends ViewModel {

        private MutableLiveData<String> mText;

        public UsuariosViewModel() {
            mText = new MutableLiveData<>();
            mText.setValue("This is Usuarios fragment");
        }

        public LiveData<String> getText() {
            return mText;
        }
    }

    public static class UsuariosFragment extends Fragment implements SearchView.OnQueryTextListener{

        View view;
        ProgressBar progressbar;
        ImageButton goChat;
        static RecyclerView rv;
        static AdapterUsers adapterUsers;
        LinearLayoutManager mLayoutManager;
        FixedSwipeRefreshLayout swipe_refresh;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();



        @SuppressLint("RestrictedApi")
        public static SharedPreferences preferences = getApplicationContext().getSharedPreferences("FilterUsers", Context.MODE_PRIVATE);
        public static SharedPreferences.Editor editor = preferences.edit();

        public static boolean filterPrefs = preferences.getBoolean("filterPrefs", false);
        public static boolean checkPref = preferences.getBoolean("checkPref", false);
        public static boolean edadPref = preferences.getBoolean("edadPref", false);
        public static int desdePref = preferences.getInt("desdePref",0);
        public static int hastaPref = preferences.getInt("hastaPref",0);

        public static boolean inGroup = preferences.getBoolean("inGroup",false);
        public static String groupName = preferences.getString("groupName","");
        public static String userName = preferences.getString("userName","");
        public static int userType = preferences.getInt("userType",2);
        public static int readGroupMsg = preferences.getInt("readGroupMsg",0);
        public static String userDate = preferences.getString("userDate","");
        public static int countGroupBadge;

        public static boolean individualNotifications = preferences.getBoolean("individualNotifications",true);
        public static boolean groupNotifications = preferences.getBoolean("groupNotifications",true);


        public final ArrayList<Users>usersArrayList = new ArrayList<>();
        public final ArrayList<Users>originalUsersList = new ArrayList<>();
        public final ArrayList<Users>usersArrayList2 = new ArrayList<>();


        public UsuariosFragment() {
            //Constructor vacío
        }


        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){

            view = inflater.inflate(R.layout.fragment_usuarios,container,false);

            setHasOptionsMenu(true);

            progressbar = view.findViewById(R.id.progressbar);
            goChat = view.findViewById(R.id.goChat);
            mLayoutManager = new LinearLayoutManager(getContext());
            mLayoutManager.setReverseLayout(true);
            mLayoutManager.setStackFromEnd(true);
            rv = view.findViewById(R.id.rv);
            rv.setLayoutManager(mLayoutManager);
            // Sugerencias de performance para listas con blur
            rv.setHasFixedSize(true);
            rv.setItemViewCacheSize(20);
            rv.setDrawingCacheEnabled(true);
            rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);



            //Verificar si esta en True o False:
            if (!filterPrefs) {

                editor.putBoolean("checkPref", false);
                editor.putBoolean("edadPref", false);
                editor.putInt("desdePref", 0);
                editor.putInt("hastaPref", 0);

                editor.apply();
                filter.setColorFilter(getContext().getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);

            }else{
                filter.setColorFilter(getContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);

            }


            adapterUsers = new AdapterUsers(usersArrayList, originalUsersList, getContext());
            rv.setAdapter(adapterUsers);

            swipe_refresh = view.findViewById(R.id.swipe_refresh);
            swipe_refresh.setRecyclerView(rv);




            swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {


                    loadUsers(checkPref, desdePref, hastaPref, "refresh");


                    swipe_refresh.setRefreshing(false);
                }
            });




    //BOTON Refresh
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    loadUsers(checkPref, desdePref, hastaPref, "refresh");


                }
            });




    //BOTON Filtro
            filter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    final View viewFilter = getLayoutInflater().inflate(R.layout.filter_layout,null);
                    final SwitchCompat switch_edad = viewFilter.findViewById(R.id.switch_edad);
                    final SwitchCompat switch_online = viewFilter.findViewById(R.id.switch_online);
                    final Spinner spinnerAge = viewFilter.findViewById(R.id.spinner_age);
                    final Spinner spinnerAge2 = viewFilter.findViewById(R.id.spinner_age2);

                    final Integer[] edades = new Integer[]  {18,19,20,21,22,23,24,25,26,27,28,29,30,31,
                            32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,
                            57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,
                            82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99};


                    ArrayAdapter<Integer> adapter1 = new ArrayAdapter<>(getContext(),
                            R.layout.tv_spinner_selected,edades);
                    adapter1.setDropDownViewResource(R.layout.tv_spinner_lista);
                    spinnerAge.setAdapter(adapter1);
                    spinnerAge2.setAdapter(adapter1);

                    if (filterPrefs){
                        spinnerAge.setSelection(desdePref-18);
                        spinnerAge2.setSelection(hastaPref-18);
                        switch_online.setChecked(checkPref);
                        switch_edad.setChecked(edadPref);
                    }


                    if (switch_edad.isChecked()) {
                        spinnerAge.setEnabled(true);
                        spinnerAge2.setEnabled(true);
                    }else{
                        spinnerAge.setEnabled(false);
                        spinnerAge2.setEnabled(false);
                    }





                    spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if(spinnerAge2.getSelectedItemPosition() < spinnerAge.getSelectedItemPosition()) {
                                spinnerAge2.setSelection(position);
                            }
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) { }
                    });
                    spinnerAge2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if(spinnerAge.getSelectedItemPosition() > spinnerAge2.getSelectedItemPosition()) {
                                spinnerAge.setSelection(position);
                            }
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) { }
                    });


                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogApp));

                    builder.setView(viewFilter);
                    builder.setPositiveButton("Filtrar", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface builder, int selectedIndex) {

                            filterPrefs = true;
                            editor.putBoolean("filterPrefs", filterPrefs);

                            checkPref = switch_online.isChecked();
                            editor.putBoolean("checkPref", switch_online.isChecked());

                            edadPref = switch_edad.isChecked();
                            editor.putBoolean("edadPref", switch_edad.isChecked());

                            if (switch_edad.isChecked()){

                                desdePref = edades[spinnerAge.getSelectedItemPosition()];
                                hastaPref = edades[spinnerAge2.getSelectedItemPosition()];
                                editor.putInt("desdePref", edades[spinnerAge.getSelectedItemPosition()]);
                                editor.putInt("hastaPref", edades[spinnerAge2.getSelectedItemPosition()]);

                            }else{
                                desdePref = 0;
                                hastaPref = 0;
                                editor.putInt("desdePref", 0);
                                editor.putInt("hastaPref", 0);
                            }


                            editor.apply();

                            loadUsers(checkPref, desdePref, hastaPref, "filter");
                            filter.setColorFilter(getContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);

                        }
                    });

                    builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface builder, int id) {

                                    return;

                                }
                            });


                    builder.setNeutralButton("Quitar filtro", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface builder, int id) {

                                    DeletePreferences();

                                    loadUsers(checkPref, desdePref, hastaPref, "filter");

                                    return;

                                }
                            });


                    builder.setCancelable(true);
                    final AlertDialog dialog = builder.create();


                    switch_online.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                            if (isChecked) {
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));
                            }else{
                                if (!switch_edad.isChecked()){
                                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                                }
                            }
                        }
                    });



                    switch_edad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                            if (isChecked) {
                                spinnerAge.setEnabled(true);
                                spinnerAge2.setEnabled(true);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));

                            }else{
                                spinnerAge.setEnabled(false);
                                spinnerAge2.setEnabled(false);
                                if (!switch_online.isChecked()){
                                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);

                                }
                            }
                        }
                    });



                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            if(!filterPrefs){
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.GRAY);
                            }
                            if(!switch_online.isChecked() & !switch_edad.isChecked()){
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                            }else{
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));

                            }

                        }
                    });

                    dialog.show();
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getContext().getResources().getColor(R.color.blanco));
                }
            });

















            loadUsers(checkPref, desdePref, hastaPref, "load");


            return view;
        }

        @SuppressLint("RestrictedApi")
        public static void DeletePreferences() {
            filterPrefs = false;
            checkPref = false;
            edadPref = false;
            desdePref = 0;
            hastaPref = 0;

            inGroup = false;
            groupName = "";
            userName = "";
            userType = 2;

            editor.putBoolean("inGroup", false);
            editor.putString("groupName", "");
            editor.putString("userName", "");
            editor.putInt("userType", 2);

            editor.putBoolean("filterPrefs", false);
            editor.putBoolean("checkPref", false);
            editor.putBoolean("edadPref", false);
            editor.putInt("desdePref", 0);
            editor.putInt("hastaPref", 0);

            editor.apply();

            filter.setColorFilter(getApplicationContext().getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
        }






        public void loadUsers(final boolean check, final Integer desde, final Integer hasta, final String flag) {

            progressbar.setVisibility(View.VISIBLE);

            refCuentas.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                        if (flag.equals("load")){
                            usersArrayList.removeAll(usersArrayList);
                        }else{
                            usersArrayList2.removeAll(usersArrayList2);
                        }

                        originalUsersList.removeAll(originalUsersList);


                        if (edadPref){

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                String key = snapshot.getKey();
                                String usuario = user.getUid();

                                if (!key.equals(usuario)){

                                    Users users = snapshot.getValue(Users.class);
                                    int edad = DateUtils.calcularEdad(users.getBirthDay());
                                    users.setAge(edad);

                                    double distanceMeters = ProfileUiBinder.getDistanceMeters(
                                            UserRepository.latitude,
                                            UserRepository.longitude,
                                            users.getLatitude(),
                                            users.getLongitude());
                                    users.setDistance(distanceMeters);

                                    if (users.getAge() >= desde & users.getAge() <= hasta) {

                                        if (check) {

                                            if (users.getState()) {

                                                if (flag.equals("load")) {

                                                    adapterUsers.addUser(users);
                                                } else {
                                                    usersArrayList2.add(users);
                                                }

                                            }
                                        } else {

                                            if (flag.equals("load")) {
                                                adapterUsers.addUser(users);
                                            } else {
                                                usersArrayList2.add(users);
                                            }
                                        }
                                    }
                                }
                            }
                        }else{


                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                String key = snapshot.getKey();
                                String usuario = user.getUid();

                                if (!key.equals(usuario)){

                                    Users users = snapshot.getValue(Users.class);

                                    double distanceMeters = ProfileUiBinder.getDistanceMeters(
                                            UserRepository.latitude,
                                            UserRepository.longitude,
                                            users.getLatitude(),
                                            users.getLongitude());
                                    users.setDistance(distanceMeters);

                                    if (check) {

                                        if (users.getState()) {
                                            if (flag.equals("load")) {
                                                adapterUsers.addUser(users);
                                            } else {
                                                usersArrayList2.add(users);
                                            }
                                        }
                                    } else {

                                        if (flag.equals("load")) {
                                            adapterUsers.addUser(users);
                                        } else {
                                            usersArrayList2.add(users);
                                        }
                                    }

                                }
                            }
                        }

                        Collections.sort(usersArrayList2);
                        Collections.sort(usersArrayList);
                        Collections.sort(originalUsersList);


                        if (flag.equals("load")){

                            adapterUsers.notifyDataSetChanged();

                        }else {

                            adapterUsers.updateDataUsers(usersArrayList2);
                            //adapter.updateDataUsers(originalUsersList);
                        }
                        setScrollbar();

                    } else {
                        progressbar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No existen usuarios", Toast.LENGTH_SHORT).show();
                    }

                    progressbar.setVisibility(View.GONE);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);

            final MenuItem action_search = menu.findItem(R.id.action_search);
            final MenuItem action_settings2 = menu.findItem(R.id.action_unlock);
            final MenuItem action_exit = menu.findItem(R.id.action_exit);

            action_exit.setVisible(false);
            action_search.setVisible(true);
            action_settings2.setVisible(true);

            SearchView searchView = (SearchView) action_search.getActionView();

            searchView.setOnQueryTextListener(this);

        }


        public static void setScrollbar(){
            rv.scrollToPosition(adapterUsers.getItemCount()-1);
        }

        @Override
        public boolean onQueryTextSubmit(String query) {

            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {

            adapterUsers.getFilter().filter(newText);

            return false;
        }
    }
}
