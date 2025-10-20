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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.canhub.cropper.CropImageContractOptions;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import androidx.activity.result.ActivityResultLauncher;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;


import com.zibete.proyecto1.CustomPermission;
import com.zibete.proyecto1.MainActivity;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.utils.CropHelper;

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
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.firebase.ui.auth.AuthUI.getApplicationContext;
import static com.zibete.proyecto1.Constants.CAMERA_SELECTED;
import static com.zibete.proyecto1.Constants.PERMISSIONS_EDIT_PROFILE;
import static com.zibete.proyecto1.Constants.PHOTO_SELECTED;
import static com.zibete.proyecto1.MainActivity.mBottomNavigation;
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

    StorageReference refImgUser = storageReference.child("Users/imgPerfil/" + user.getUid() + ".jpg");
    ImageButton btSave, bt_edit;
    TextView btn_ok, btn_done;
    LinearLayout linearImageActivity, linearButtonsEdit, linearOnBoardingProfile, linear_edit_delete;
    View view;

    ArrayList<String> photoList;
    private ContentValues values;
    private Uri imageUri;
    private Bitmap thumbnail;
    String imageurl;

    byte [] thumb_byte;


//
//    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        cropImageLauncher = CropHelper.registerLauncher(
//                this,
//                refSendImages,
//                linear_photo_view,
//                linear_photo,
//                photo,
//                loadingPhoto,
//                msg,
//                btnCamera,
//                btnSendMsg,
//                uri -> { /* opcional */ }
//        );
//





    public EditProfileFragment(){
        //Constructor vacío
    }


    @SuppressLint("RestrictedApi")
    public static SharedPreferences prefs = getApplicationContext().getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE);
    public static SharedPreferences.Editor editor = prefs.edit();
    public boolean flagProfile = prefs.getBoolean("flag_OnBoardingProfile", false);


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){

        view = inflater.inflate(R.layout.fragment_edit_profile,container,false);

        setHasOptionsMenu(true);

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



        // Toolbar toolbar = view.findViewById(R.id.toolbar_edit_profile);
        //((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        //((AppCompatActivity)getActivity()).setTitle("");
        //((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //((AppCompatActivity)getActivity()).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getContext().getResources().getColor(R.color.fui_transparent)));



            btSave.setEnabled(false);



        if (!flagProfile) {

            linearOnBoardingProfile.setVisibility(View.VISIBLE);
            btn_done.setVisibility(View.VISIBLE);
            editor.putBoolean("flag_OnBoardingProfile", true);
            editor.apply();

        }else{

            linearOnBoardingProfile.setVisibility(View.GONE);
            btn_done.setVisibility(View.GONE);
        }




        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            String age = dataSnapshot.child("birthDay").getValue(String.class);

                            if (age.isEmpty()) {

                                final Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
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

                            }else {

                                Intent intent = new Intent (getContext(),SplashActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

            }
        });



        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearOnBoardingProfile.setVisibility(View.GONE);
            }
        });


        linearOnBoardingProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearOnBoardingProfile.setVisibility(View.GONE);
            }
        });



        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    String foto = dataSnapshot.child("foto").getValue(String.class);
                    String edad = dataSnapshot.child("birthDay").getValue(String.class);
                    String desc = dataSnapshot.child("descripcion").getValue(String.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        assert edad != null;
                        if (edad.equals("")){

                            completeProfile.setText("Te pediremos que completes tu fecha de nacimiento. También podrás agregar información sobre vos o cambiar tu foto de perfil");

                        }else{

                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate fechaNac = LocalDate.parse(edad, fmt);
                            LocalDate ahora = LocalDate.now();

                            Period period = Period.between(fechaNac, ahora);

                            String edad3 = String.valueOf(period.getYears());
                            tvAge.setText(edad3);
                            edtDate.setText(edad);

                            completeProfile.setText("A continuación podrás agregar información sobre vos o cambiar tu foto de perfil");

                        }

                    }

                    edtDesc.setText(desc);


                    loadingPhoto.setVisibility(View.VISIBLE);
                    Glide.with(getContext())
                            .load(foto)
                            .apply(new RequestOptions().transform( new CenterCrop(), new RoundedCorners(35)))
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    loadingPhoto.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    loadingPhoto.setVisibility(View.GONE);
                                    return false;
                                }

                            })
                            .into(imageProfile);



                    photoList.add(foto);

                    btSave.setEnabled(false);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        edtNameUser.setText(user.getDisplayName());
        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                if (dataSnapshot.exists()){

                    String desc = dataSnapshot.child("descripcion").getValue(String.class);
                    edtDesc.setText(desc);
                    btSave.setEnabled(false);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });






        DisplayMetrics dimension = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dimension);

        int height = dimension.heightPixels;




        //linearScrollEditProfile.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height-(height/4));

        //layoutParams.setMargins(30, height-(height/4), 30, 0);

        linearImageActivity.setLayoutParams(layoutParams);
        linearImageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(getContext(), SlidePhotoActivity.class);
                intent.putExtra("photoList",photoList);
                intent.putExtra("position",0);
                intent.putExtra("rotation",180);
                startActivity(intent);

            }
        });



        bt_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditProfilePhoto();

            }
        });



        edtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Calendar calendar = Calendar.getInstance();


                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        calendar.set(Calendar.YEAR,year);
                        calendar.set(Calendar.MONTH,month);
                        calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Date date = calendar.getTime();
                        birthDay = simpleDateFormat.format(date);
                        edtDate.setText(birthDay);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate fechaNac = LocalDate.parse(birthDay, fmt);
                            LocalDate ahora = LocalDate.now();

                            Period periodo = Period.between(fechaNac, ahora);

                            String edad3 = String.valueOf(periodo.getYears());
                            tvAge.setText(edad3);
                            btSave.setEnabled(true);
                        }

                    }
                },calendar.get(Calendar.YEAR)-18,calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });


        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });



        edtNameUser.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                btSave.setEnabled(false);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btSave.setEnabled(true);
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        edtDesc.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                btSave.setEnabled(false);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btSave.setEnabled(true);
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });



        return view;
    }

    public void EditProfilePhoto() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        int ReadStoragePermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.READ_EXTERNAL_STORAGE);
        int WriteStoragePermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.CAMERA);

        if (ReadStoragePermission != PackageManager.PERMISSION_GRANTED || WriteStoragePermission != PackageManager.PERMISSION_GRANTED ||
                cameraPermission!= PackageManager.PERMISSION_GRANTED) {

            requestPermissions(permissions, PERMISSIONS_EDIT_PROFILE);

            }else {


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

            deleteSelected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    btSave.setEnabled(true);
                    imageUri = Uri.parse(getContext().getString(R.string.URL_PHOTO_DEF));
                    imageurl = getContext().getString(R.string.URL_PHOTO_DEF);

                    //Picasso.with(getContext()).load(imageUri).into(imageProfile);

                    loadingPhoto.setVisibility(View.VISIBLE);

                    Glide.with(getContext())
                            .load(imageUri)
                            .apply(new RequestOptions().transform(new RoundedCorners(35)))
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    loadingPhoto.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    loadingPhoto.setVisibility(View.GONE);
                                    return false;
                                }

                            })
                            .into(imageProfile);








                    thumb_byte = null;

                    dialog.dismiss();
                }
            });


            cameraSelected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {



                        startCamera();



                    dialog.dismiss();
                }
            });


            img_cancel_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });



            storageSelected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
/*
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PHOTO_SELECTED);

*/


                        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(gallery, PHOTO_SELECTED);

                        dialog.dismiss();

                }
            });


        }
    }

    public void startCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());

        imageUri = getContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA_SELECTED);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);

//
//        if(requestCode == CAMERA_SELECTED && resultCode == RESULT_OK) {
//
//
//            CropImage.activity(imageUri)
//                    .setGuidelines(CropImageView.Guidelines.ON)
//                    .setRequestedSize(1920, 1080)
//                    .start(getContext(), EditProfileFragment.this);
//            }
//
//        if(requestCode == PHOTO_SELECTED && resultCode == RESULT_OK) {
//
//                Uri imgUri = CropImage.getPickImageResultUri(getContext(), data);
//
//                CropImage.activity(imgUri)
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .setRequestedSize(1920, 1080)
//                        .start(getContext(), EditProfileFragment.this);
//        }
//
//        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//
//            if (resultCode == RESULT_OK) {
//
//                btSave.setEnabled(true);
//
//                Uri resultUri = result.getUri();
//
//
//                //Picasso.with(getContext()).load(resultUri).into(imageProfile);
//                loadingPhoto.setVisibility(View.VISIBLE);
//
//                Glide.with(getContext())
//                        .load(resultUri)
//                        .apply(new RequestOptions().transform(new RoundedCorners(35)))
//                        .listener(new RequestListener<Drawable>() {
//                            @Override
//                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                                loadingPhoto.setVisibility(View.GONE);
//                                return false;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                                loadingPhoto.setVisibility(View.GONE);
//                                return false;
//                            }
//
//                        })
//                        .into(imageProfile);
//
//                try {
//                    thumbnail = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), resultUri);
//                    imageurl = getRealPathFromURI(resultUri);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//
//
//
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//                thumb_byte = byteArrayOutputStream.toByteArray();
//
//
//            }
//        }

        progress.dismiss();
    }




    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_EDIT_PROFILE){

            EditProfilePhoto();

        }
    }



    private void save(){

        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);
        String birthday = edtDate.getText().toString().trim();

        if (!edtDate.getText().toString().isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate fechaNac = LocalDate.parse(birthday, fmt);
                LocalDate ahora = LocalDate.now();
                Period periodo = Period.between(fechaNac, ahora);
                int edad = periodo.getYears();
                if (edad < 18) {
                    progress.dismiss();
                    final Snackbar snack = Snackbar.make(getView(), "Lo sentimos, debe ser mayor de 18 años para utilizar la App", Snackbar.LENGTH_INDEFINITE);
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
        }else{
            progress.dismiss();
            final Snackbar snack = Snackbar.make(getView(), "Debe ingresar su fecha de nacimiento para continuar", Snackbar.LENGTH_INDEFINITE);
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
            return;

        }

        if (thumb_byte != null) {



            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zibe");

            if(!folder.mkdirs()){
                Log.e("TAG", "Error: No se creo el directorio privado");
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyHHmm");
            String name = "IMG_" + simpleDateFormat.format(Calendar.getInstance().getTime());


            File file = new File(folder, name + ".jpg");

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }



            UploadTask uploadTask = refImgUser.putBytes(thumb_byte);


            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());


                    }
                    return refImgUser.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {

                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    Uri downloadUri = task.getResult();
                    ref_cuentas.child(user.getUid()).child("foto").setValue(downloadUri.toString());
                    imageUri = Uri.parse(downloadUri.toString());
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(downloadUri.toString()))
                            .build();
                    user.updateProfile(profileUpdates);


                }
            });

        }


        String name = edtNameUser.getText().toString().trim();
        String fecha = edtDate.getText().toString().trim();
        String desc = edtDesc.getText().toString().trim();

        ref_cuentas.child(user.getUid()).child("nombre").setValue(name);
        ref_cuentas.child(user.getUid()).child("birthDay").setValue(fecha);
        ref_cuentas.child(user.getUid()).child("descripcion").setValue(desc);

        UserProfileChangeRequest profileUpdates;

        if (imageurl == getContext().getString(R.string.URL_PHOTO_DEF)){

            refImgUser.delete();
            ref_cuentas.child(user.getUid()).child("foto").setValue(imageurl);
            profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(edtNameUser.getText().toString().trim())
                    .setPhotoUri(imageUri)
                    .build();

        }else {

            profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(edtNameUser.getText().toString().trim())
                    .build();

        }

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateUI(user);
                        }
                    }
                });


    }


    private void updateUI(FirebaseUser user) {
        if(user != null){


            Toast.makeText(getContext(),"Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            progress.dismiss();

            Intent intent = new Intent (getContext(),SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        }
    }




    public static void DeleteProfilePreferences(){

        editor.putBoolean("flag_OnBoardingProfile", false);
        editor.apply();

    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_favorites).setVisible(false);


    }

}
