package com.zibete.proyecto1.ui.chat.media

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.ui.chat.ChatViewModel
import com.zibete.proyecto1.ui.media.buildZibeUcropIntent
import java.io.File

class ChatPhotoController(
    private val activity: ComponentActivity,
    private val viewModel: ChatViewModel
) {
    private var cameraUri: Uri? = null
    private var pendingFileName: String? = null

    private val cropLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleCropResult(result.resultCode, pendingFileName, result.data)
        pendingFileName = null
    }

    private val galleryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            pendingFileName = null
        } else {
            viewModel.onRemovePendingPhoto()
            launchCrop(uri)
        }
    }

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val source = cameraUri
        if (success && source != null) {
            viewModel.onRemovePendingPhoto()
            launchCrop(source)
        } else if (source != null) {
            activity.contentResolver.delete(source, null, null)
            pendingFileName = null
        }
        cameraUri = null
    }

    private val cameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else viewModel.onError(UiText.StringRes(R.string.err_camera_permission_denied))
    }

    fun launchCamera() {
        if (
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        pendingFileName = createPhotoFileName()
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun openCamera() {
        pendingFileName = createPhotoFileName()
        val uri = createCameraImageUri()
        if (uri == null) {
            viewModel.onError(UiText.StringRes(R.string.msg_camera_error))
            return
        }
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    private fun launchCrop(sourceUri: Uri) {
        cropLauncher.launch(buildZibeUcropIntent(activity, sourceUri))
    }

    private fun handleCropResult(resultCode: Int, fileName: String?, data: Intent?) {
        when {
            resultCode == Activity.RESULT_OK && data != null -> {
                val resultUri = UCrop.getOutput(data)
                if (resultUri == null || fileName == null) {
                    viewModel.onRemovePendingPhoto()
                    viewModel.onError(UiText.StringRes(R.string.chat_error_get_cropped_image))
                } else {
                    viewModel.onCroppedPhotoReady(fileName, resultUri.toString())
                }
            }

            resultCode == UCrop.RESULT_ERROR -> {
                viewModel.onRemovePendingPhoto()
                viewModel.onError(UiText.StringRes(R.string.chat_error_crop_image))
            }

            else -> viewModel.onRemovePendingPhoto()
        }
    }

    private fun createPhotoFileName(): String = "chat_photo_${System.currentTimeMillis()}.jpg"

    private fun createCameraImageUri(): Uri? {
        val directory = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
        if (!directory.exists() && !directory.mkdirs()) return null
        val file = File.createTempFile("chat_camera_", ".jpg", directory)
        return FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
    }
}
