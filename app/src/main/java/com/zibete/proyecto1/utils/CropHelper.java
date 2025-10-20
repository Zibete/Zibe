// com/zibete/proyecto1/utils/CropHelper.java
package com.zibete.proyecto1.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class CropHelper {

    // ====== Fragment ======
    public static ActivityResultLauncher<CropImageContractOptions> registerLauncher(
            Fragment fragment,
            StorageReference refSendImages,
            LinearLayout linear_photo_view,
            LinearLayout linear_photo,
            ImageView photo,
            ProgressBar loadingPhoto,
            android.widget.EditText msg,
            ImageView btnCamera,
            ImageView btnSendMsg,
            java.util.function.Consumer<Uri> onCroppedUri // callback si querés usar la Uri luego
    ) {
        return fragment.registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri resultUri = result.getUriContent();
                applyUiAndUpload(fragment.requireContext(), (AppCompatActivity) fragment.requireActivity(),
                        resultUri, refSendImages, linear_photo_view, linear_photo,
                        photo, loadingPhoto, msg, btnCamera, btnSendMsg);
                if (onCroppedUri != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onCroppedUri.accept(resultUri);
                }
            } else {
                // manejar error si querés: result.getError()
            }
        });
    }

    // ====== Activity ======
    public static ActivityResultLauncher<CropImageContractOptions> registerLauncherForActivity(
            AppCompatActivity activity,
            StorageReference refSendImages,
            LinearLayout linear_photo_view,
            LinearLayout linear_photo,
            ImageView photo,
            ProgressBar loadingPhoto,
            android.widget.EditText msg,
            ImageView btnCamera,
            ImageView btnSendMsg,
            java.util.function.Consumer<Uri> onCroppedUri
    ) {
        return activity.registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri resultUri = result.getUriContent();
                applyUiAndUpload(activity, activity,
                        resultUri, refSendImages, linear_photo_view, linear_photo,
                        photo, loadingPhoto, msg, btnCamera, btnSendMsg);
                if (onCroppedUri != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onCroppedUri.accept(resultUri);
                }
            } else {
                // manejar error si querés
            }
        });
    }

    // === común a ambos ===
    private static void applyUiAndUpload(
            Context ctx,
            AppCompatActivity activity,
            Uri resultUri,
            StorageReference refSendImages,
            LinearLayout linear_photo_view,
            LinearLayout linear_photo,
            ImageView photo,
            ProgressBar loadingPhoto,
            android.widget.EditText msg,
            ImageView btnCamera,
            ImageView btnSendMsg
    ) {
        // mostrar UI de “foto lista para enviar”
        linear_photo_view.setVisibility(android.view.View.VISIBLE);
        msg.setVisibility(android.view.View.GONE);
        btnCamera.setVisibility(android.view.View.GONE);
        btnSendMsg.setVisibility(android.view.View.VISIBLE);

        // tamaño del preview
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthPixels / 3, widthPixels / 3);
        linear_photo.setLayoutParams(params);

        // subir a Firebase
        String childName = resultUri.getLastPathSegment() != null
                ? resultUri.getLastPathSegment()
                : ("img_" + System.currentTimeMillis());

        StorageReference refPic = refSendImages.child(childName);

        refPic.putFile(resultUri).addOnSuccessListener((UploadTask.TaskSnapshot snap) -> {
            Task<Uri> urlTask = snap.getStorage().getDownloadUrl();
            urlTask.addOnSuccessListener(downloadUri -> {
                loadingPhoto.setVisibility(android.view.View.VISIBLE);
                Glide.with(ctx)
                        .load(downloadUri.toString())
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(35)))
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                loadingPhoto.setVisibility(android.view.View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loadingPhoto.setVisibility(android.view.View.GONE);
                                return false;
                            }
                        })
                        .into(photo);
            });
        });
    }

    // Lanzar el cropper con opciones comunes
    public static void launchCrop(ActivityResultLauncher<CropImageContractOptions> launcher, @Nullable Uri imageUri) {
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.outputRequestWidth = 1920;
        options.outputRequestHeight = 1080;
        CropImageContractOptions contract = new CropImageContractOptions(imageUri, options);
        launcher.launch(contract);
    }
}
