package com.zibete.proyecto1.ui.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import java.io.File

fun buildZibeUcropIntent(context: Context, source: Uri): Intent {
    val destFile = File(
        context.cacheDir,
        "profile_crop_${System.currentTimeMillis()}.jpg"
    )
    val destUri = Uri.fromFile(destFile)

    val options = UCrop.Options().apply {
        setFreeStyleCropEnabled(true)
        setAspectRatioOptions(
            0,
            AspectRatio("Original", 0f, 0f),
            AspectRatio("3:4", 3f, 4f),
            AspectRatio("9:16", 9f, 16f)
        )
    }

    val uCrop = UCrop.of(source, destUri)
        .withOptions(options)
        .useSourceImageAspectRatio()
        .withMaxResultSize(1440, 1920)

    @Suppress("UNCHECKED_CAST")
    return (uCrop.getIntent(context) as Intent).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

sealed class ZibeCropResult {
    data class Success(val uri: Uri) : ZibeCropResult()
    data class Error(val throwable: Throwable) : ZibeCropResult()
    data object Cancelled : ZibeCropResult()
}

class ZibeUcropContract : ActivityResultContract<Uri, ZibeCropResult>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return buildZibeUcropIntent(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ZibeCropResult {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                val output = intent?.let { UCrop.getOutput(it) }
                if (output != null) ZibeCropResult.Success(output)
                else ZibeCropResult.Error(IllegalStateException("UCrop output is null"))
            }
            UCrop.RESULT_ERROR -> {
                val error = intent?.let { UCrop.getError(it) }
                ZibeCropResult.Error(error ?: IllegalStateException("UCrop failed"))
            }
            else -> ZibeCropResult.Cancelled
        }
    }
}

@Composable
fun rememberZibePhotoCropper(
    onCropped: (Uri) -> Unit,
    onError: (Throwable) -> Unit = {}
): ZibePhotoCropper {
    val currentOnCropped by rememberUpdatedState(onCropped)
    val currentOnError by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(
        contract = ZibeUcropContract()
    ) { result ->
        when (result) {
            is ZibeCropResult.Success -> currentOnCropped(result.uri)
            is ZibeCropResult.Error -> currentOnError(result.throwable)
            ZibeCropResult.Cancelled -> Unit
        }
    }

    return remember(launcher) {
        ZibePhotoCropper { source ->
            launcher.launch(source)
        }
    }
}

class ZibePhotoCropper internal constructor(
    private val launchInternal: (Uri) -> Unit
) {
    fun launch(source: Uri) {
        launchInternal(source)
    }
}
