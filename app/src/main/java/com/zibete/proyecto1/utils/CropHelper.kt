package com.zibete.proyecto1.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.firebase.storage.StorageReference

object CropHelper {

    /**
     * Registra el cropper (sirve para Fragment o Activity) y devuelve el launcher.
     *
     * @param caller Fragment/Activity que implementa ActivityResultCaller (p.ej. this)
     * @param ctx Context para recursos/Glide
     * @param refSendImages StorageReference a la carpeta de imágenes donde se sube el recorte
     * @param linearPhotoView Contenedor de la previsualización (se muestra al recortar)
     * @param linearPhoto Vista cuyo tamaño ajustamos para el preview cuadrado
     * @param photo ImageView donde se carga el resultado
     * @param loadingPhoto ProgressBar de carga del preview
     * @param msg EditText del mensaje (se oculta al mostrar foto)
     * @param btnCamera Botón de cámara (se oculta al mostrar foto)
     * @param btnSendMsg Botón de enviar (se muestra al recortar)
     * @param onCroppedUri Callback opcional con la URI recortada (después de subir y previsualizar)
     */
    fun registerLauncher(
        caller: ActivityResultCaller,
        ctx: Context,
        refSendImages: StorageReference,
        linearPhotoView: LinearLayout,
        linearPhoto: LinearLayout,
        photo: ImageView,
        loadingPhoto: ProgressBar,
        loadingButton: ProgressBar,
        frameSendMsg: FrameLayout,
        msg: EditText,
        btnCamera: ImageView,
        btnSendMsg: ImageView,
        onCroppedUri: ((Uri) -> Unit)? = null
    ): ActivityResultLauncher<CropImageContractOptions> {

        return caller.registerForActivityResult(
            CropImageContract()
        ) { result ->
            if (result == null || !result.isSuccessful) {
                // Si querés, log o UI de error con result?.error
                return@registerForActivityResult
            }

            val resultUri = result.uriContent ?: return@registerForActivityResult

            // 1) Mostrar UI de “foto lista para enviar”
            frameSendMsg.isVisible = true
            btnSendMsg.isVisible = false
            msg.isVisible = false
            btnCamera.isVisible = false

            linearPhotoView.isVisible = true
            loadingPhoto.isVisible = true
            loadingButton.isVisible = true

            // 2) Ajustar tamaño de preview (cuadrado = 1/3 del ancho)
            val width = ctx.resources.displayMetrics.widthPixels
            val params = LinearLayout.LayoutParams(width / 3, width / 3)
            linearPhoto.layoutParams = params

            // 3) Subir a Firebase y previsualizar con Glide
            val childName = resultUri.lastPathSegment?.ifBlank { null }
                ?: "img_${System.currentTimeMillis()}"
            val refPic = refSendImages.child(childName)

            refPic.putFile(resultUri)
                .addOnSuccessListener { snap ->
                    snap.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Mostrar spinner mientras carga Glide
                        Glide.with(ctx)
                            .load(downloadUri.toString())
                            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }
                            })
                            .into(photo)

                        onCroppedUri?.invoke(resultUri)
                    }
                }
        }
    }

    @JvmStatic
    fun launchCrop(
        launcher: ActivityResultLauncher<CropImageContractOptions>,
        imageUri: Uri
    ) {
        val options = CropImageOptions().apply {
            guidelines = CropImageView.Guidelines.ON
            outputRequestWidth = 1920
            outputRequestHeight = 1080
        }

        val contract = CropImageContractOptions(imageUri, options)
        launcher.launch(contract)
    }

    @JvmStatic
    fun registerLauncherForFragment(
        fragment: androidx.fragment.app.Fragment,
        refSendImages: StorageReference,
        linearPhotoView: LinearLayout,
        linearPhoto: LinearLayout,
        photo: ImageView,
        loadingPhoto: ProgressBar,
        loadingButton: ProgressBar,
        frameSendMsg : FrameLayout,
        msg: EditText,
        btnCamera: ImageView,
        btnSendMsg: ImageView,
        onCroppedUri: java.util.function.Consumer<Uri>
    ): ActivityResultLauncher<CropImageContractOptions> {
        return registerLauncher(
            caller = fragment,
            ctx = fragment.requireContext(),
            refSendImages = refSendImages,
            linearPhotoView = linearPhotoView,
            linearPhoto = linearPhoto,
            photo = photo,
            loadingPhoto = loadingPhoto,
            loadingButton = loadingButton,
            frameSendMsg = frameSendMsg,
            msg = msg,
            btnCamera = btnCamera,
            btnSendMsg = btnSendMsg
        ) { uri -> onCroppedUri.accept(uri) }
    }

}
