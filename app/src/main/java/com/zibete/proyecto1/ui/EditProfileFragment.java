package com.zibete.proyecto1.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
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
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.Splash.SplashActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.firebase.ui.auth.AuthUI.getApplicationContext;
import static com.zibete.proyecto1.Constants.CAMERA_SELECTED;
import static com.zibete.proyecto1.Constants.PERMISSIONS_EDIT_PROFILE;
import static com.zibete.proyecto1.Constants.PHOTO_SELECTED;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;

public class EditProfileFragment extends Fragment {

    ImageView imageProfile, deleteSelected, cameraSelected, storageSelected, img_cancel_dialog;
    EditText edtNameUser, edtDesc, edtDate;
    TextView tvAge, completeProfile, tv_title;
    String birthDay;
    ProgressBar loadingPhoto;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ProgressDialog progress;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    StorageReference refImgUser;

    ImageButton btSave, bt_edit;
    TextView btn_ok, btn_done;
    LinearLayout linearImageActivity, linearButtonsEdit, linearOnBoardingProfile, linear_edit_delete;
    View view;

    ArrayList<String> photoList;
    private ContentValues values;
    private Uri imageUri;
    private Bitmap thumbnail;
    String imageurl;
    byte[] thumb_byte;

    // Tokens actuales del dispositivo
    private String myInstallId = null;
    private String myFcmToken = null;

    public EditProfileFragment() { }

    @SuppressLint("RestrictedApi")
    public static SharedPreferences prefs = getApplicationContext().getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE);
    public static SharedPreferences.Editor editor = prefs.edit();
    public boolean flagProfile = prefs.getBoolean("flag_OnBoardingProfile", false);

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        setHasOptionsMenu(true);

        if (user == null) return view;
        refImgUser = storageReference.child("Users/imgPerfil/" + user.getUid() + ".jpg");

        // Obtener tokens (no bloqueante)
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myInstallId = t.getResult(); });
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(t -> { if (t.isSuccessful()) myFcmToken = t.getResult(); });

        photoList = new ArrayList<>();
        linearOnBoardingProfile = view.findViewById(R.id.linearOnBoardingProfile);
        linearButtonsEdit = view.findViewById(R.id.linearButtonsEdit);
        linearImageActivity = view.findViewById(R.id.linearImageActivity);
        progress = new ProgressDialog(getContext(), R.style.AlertDialogApp);
        imageProfile = view.findViewById(R.id.ftPerfil);
        edtNameUser = view.findViewById(R.id.edtNameUser);
        edtDesc = view.findViewById(R.id.edtDesc);
        tvAge = view.findViewById(R.id.tvEdad);
        completeProfile = view.findViewById(R.id.completeProfile);
        edtDate = view.findViewById(R.id.edtFecha);
        btSave = view.findViewById(R.id.bt_save);
        bt_edit = view.findViewById(R.id.bt_edit);
        btn_ok = view.findViewById(R.id.btn_ok);
        loadingPhoto = view.findViewById(R.id.loadingPhoto);
        btn_done = view.findViewById(R.id.btn_done);

        btSave.setEnabled(false);

        if (!flagProfile) {
            linearOnBoardingProfile.setVisibility(View.VISIBLE);
            btn_done.setVisibility(View.VISIBLE);
            editor.putBoolean("flag_OnBoardingProfile", true);
            editor.apply();
        } else {
            linearOnBoardingProfile.setVisibility(View.GONE);
            btn_done.setVisibility(View.GONE);
        }

        btn_done.setOnClickListener(v -> ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String bd = String.valueOf(dataSnapshot.child("birthDay").getValue(String.class));
                    if (bd == null || bd.isEmpty()) {
                        final Snackbar snack = Snackbar.make(requireActivity().findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
                        snack.setAction("OK", vv -> snack.dismiss());
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();
                    } else {
                        Intent intent = new Intent(getContext(), SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        }));

        btn_ok.setOnClickListener(v -> linearOnBoardingProfile.setVisibility(View.GONE));
        linearOnBoardingProfile.setOnClickListener(v -> linearOnBoardingProfile.setVisibility(View.GONE));

        // Cargar datos actuales
        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                String foto = dataSnapshot.child("foto").getValue(String.class);
                String bd = dataSnapshot.child("birthDay").getValue(String.class);
                String desc = dataSnapshot.child("descripcion").getValue(String.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (bd == null || bd.equals("")) {
                        completeProfile.setText("Te pediremos que completes tu fecha de nacimiento. También podrás agregar información sobre vos o cambiar tu foto de perfil");
                    } else {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        LocalDate fechaNac = LocalDate.parse(bd, fmt);
                        String edad3 = String.valueOf(Period.between(fechaNac, LocalDate.now()).getYears());
                        tvAge.setText(edad3);
                        edtDate.setText(bd);
                        completeProfile.setText("Actualizá tu perfil con una foto y tus datos personales");
                    }
                }

                edtDesc.setText(desc);

                loadingPhoto.setVisibility(View.VISIBLE);
                Glide.with(getContext())
                        .load(foto)
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(35)))
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
                        .into(imageProfile);

                if (foto != null) photoList.add(foto);
                btSave.setEnabled(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        edtNameUser.setText(user.getDisplayName());
        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String desc = dataSnapshot.child("descripcion").getValue(String.class);
                    edtDesc.setText(desc);
                    btSave.setEnabled(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        DisplayMetrics dimension = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int height = dimension.heightPixels;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height - (height / 4));
        linearImageActivity.setLayoutParams(layoutParams);
        linearImageActivity.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SlidePhotoActivity.class);
            intent.putExtra("photoList", photoList);
            intent.putExtra("position", 0);
            intent.putExtra("rotation", 180);
            startActivity(intent);
        });

        bt_edit.setOnClickListener(v -> EditProfilePhoto());

        edtDate.setOnClickListener(view -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    new ContextThemeWrapper(getContext(), R.style.AlertDialogApp),
                    (DatePicker dp, int year, int month, int dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        birthDay = simpleDateFormat.format(calendar.getTime());
                        edtDate.setText(birthDay);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate fechaNac = LocalDate.parse(birthDay, fmt);
                            String edad3 = String.valueOf(Period.between(fechaNac, LocalDate.now()).getYears());
                            tvAge.setText(edad3);
                            btSave.setEnabled(true);
                        }
                    },
                    calendar.get(Calendar.YEAR) - 18,
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        btSave.setOnClickListener(v -> save());

        TextWatcher enableSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { btSave.setEnabled(false); }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { btSave.setEnabled(true); }
            @Override public void afterTextChanged(Editable s) { }
        };
        edtNameUser.addTextChangedListener(enableSaveWatcher);
        edtDesc.addTextChangedListener(enableSaveWatcher);

        return view;
    }

    public void EditProfilePhoto() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        int read = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int write = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cam = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);

        if (read != PackageManager.PERMISSION_GRANTED || write != PackageManager.PERMISSION_GRANTED || cam != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, PERMISSIONS_EDIT_PROFILE);
        } else {
            final View viewFilter = getLayoutInflater().inflate(R.layout.profile_photo_layout, null);
            deleteSelected = viewFilter.findViewById(R.id.deleteSelected);
            cameraSelected = viewFilter.findViewById(R.id.cameraSelected);
            storageSelected = viewFilter.findViewById(R.id.storageSelected);
            tv_title = viewFilter.findViewById(R.id.tv_title);
            linear_edit_delete = viewFilter.findViewById(R.id.linear_edit_delete);
            img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);
            linear_edit_delete.setVisibility(View.VISIBLE);
            tv_title.setText(getResources().getString(R.string.editar_foto_de_perfil));

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogApp));
            builder.setView(viewFilter);
            builder.setCancelable(true);
            final AlertDialog dialog = builder.create();
            dialog.show();

            deleteSelected.setOnClickListener(v -> {
                btSave.setEnabled(true);
                imageUri = Uri.parse(getContext().getString(R.string.URL_PHOTO_DEF));
                imageurl = getContext().getString(R.string.URL_PHOTO_DEF);

                loadingPhoto.setVisibility(View.VISIBLE);
                Glide.with(getContext())
                        .load(imageUri)
                        .apply(new RequestOptions().transform(new RoundedCorners(35)))
                        .listener(new RequestListener<Drawable>() {
                            @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }
                            @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }
                        }).into(imageProfile);

                thumb_byte = null;
                dialog.dismiss();
            });

            cameraSelected.setOnClickListener(v -> {
                startCamera();
                dialog.dismiss();
            });

            img_cancel_dialog.setOnClickListener(v -> dialog.dismiss());

            storageSelected.setOnClickListener(v -> {
                Intent gallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PHOTO_SELECTED);
                dialog.dismiss();
            });
        }
    }

    public void startCamera() {
        values = new ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());
        imageUri = requireContext().getContentResolver().insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA_SELECTED);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

        // Tu flujo de crop está comentado. Dejamos solo el cierre del progress.
        progress.dismiss();
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {android.provider.MediaStore.Images.Media.DATA};
        Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_EDIT_PROFILE) {
            EditProfilePhoto();
        }
    }

    private void save() {
        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

        String name = edtNameUser.getText().toString().trim();
        String fecha = edtDate.getText().toString().trim();
        String desc = edtDesc.getText().toString().trim();

        if (fecha.isEmpty()) {
            progress.dismiss();
            snack("Debe ingresar su fecha de nacimiento para continuar");
            return;
        }

        // Validación + cálculo de edad
        int edad = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birth = LocalDate.parse(fecha, fmt);
            edad = Period.between(birth, LocalDate.now()).getYears();
            if (edad < 18) {
                progress.dismiss();
                snack("Lo sentimos, debe ser mayor de 18 años para utilizar la App");
                return;
            }
        } else {
            // Fallback simple si alguna vez soportás <26
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar b = Calendar.getInstance();
                b.setTime(sdf.parse(fecha));
                Calendar now = Calendar.getInstance();
                edad = now.get(Calendar.YEAR) - b.get(Calendar.YEAR);
                if (now.get(Calendar.DAY_OF_YEAR) < b.get(Calendar.DAY_OF_YEAR)) edad--;
                if (edad < 18) {
                    progress.dismiss();
                    snack("Lo sentimos, debe ser mayor de 18 años para utilizar la App");
                    return;
                }
            } catch (Exception e) {
                progress.dismiss();
                snack("Fecha de nacimiento inválida");
                return;
            }
        }

        // Subida de foto (si corresponde)
        if (thumb_byte != null) {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zibe");
            if (!folder.mkdirs()) {
                Log.e("TAG", "No se pudo crear el directorio (puede existir)");
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
                    ref_cuentas.child(user.getUid()).child("foto").setValue(downloadUri.toString());
                    imageUri = downloadUri;
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build();
                    user.updateProfile(profileUpdates);
                }
            });
        }

        // Actualizar nombre/foto en Auth
        UserProfileChangeRequest profileUpdates;
        if (imageurl != null && imageurl.equals(getContext().getString(R.string.URL_PHOTO_DEF))) {
            // si marcó foto por defecto
            refImgUser.delete();
            ref_cuentas.child(user.getUid()).child("foto").setValue(imageurl);
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

        // Timestamp
        SimpleDateFormat stamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String nowStr = stamp.format(Calendar.getInstance().getTime());

        // Escribir campos de perfil + tokens
        ref_cuentas.child(user.getUid()).child("nombre").setValue(name);
        ref_cuentas.child(user.getUid()).child("birthDay").setValue(fecha);
        ref_cuentas.child(user.getUid()).child("age").setValue(edad);
        ref_cuentas.child(user.getUid()).child("descripcion").setValue(desc);
        ref_cuentas.child(user.getUid()).child("date").setValue(nowStr);

        if (myInstallId != null && !myInstallId.isEmpty()) {
            ref_cuentas.child(user.getUid()).child("installId").setValue(myInstallId);
            // compat temporal (mismo valor que installId)
            ref_cuentas.child(user.getUid()).child("token").setValue(myInstallId);
        }
        if (myFcmToken != null && !myFcmToken.isEmpty()) {
            ref_cuentas.child(user.getUid()).child("fcmToken").setValue(myFcmToken);
        }

        updateUI(user);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(getContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            progress.dismiss();
            Intent intent = new Intent(getContext(), SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public static void DeleteProfilePreferences() {
        editor.putBoolean("flag_OnBoardingProfile", false);
        editor.apply();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_favorites).setVisible(false);
    }

    private void snack(String msg) {
        if (getView() == null) return;
        final Snackbar snack = Snackbar.make(getView(), msg, Snackbar.LENGTH_INDEFINITE);
        snack.setAction("OK", v -> snack.dismiss());
        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }
}
