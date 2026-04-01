package com.zibete.proyecto1.ui.chat

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.ui.chat.media.ChatAudioPlayer
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatActivity : BaseChatSessionActivity() {

    private companion object {
        const val MIN_AUDIO_DURATION_MS = 1_000L
    }

    override val enableComposeSnackHost: Boolean = false

    private val chatViewModel: ChatViewModel by viewModels()
    private var imageUriCamera: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioUri: Uri? = null
    private var currentAudioFile: File? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var pendingAudioName: String? = null
    private var pendingImageName: String? = null
    private var recordStartElapsed: Long = 0L
    private lateinit var iconVibrator: Vibrator
    private lateinit var uCropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: (() -> Unit)? = null
    private val pendingAudioUrl = mutableStateOf<String?>(null)
    private val pendingAudioDurationMs = mutableLongStateOf(0L)
    private val recordingElapsedMs = mutableLongStateOf(0L)
    private val isRecordingCanceled = mutableStateOf(false)
    private var recordingJob: Job? = null

    private val isAudioUploading = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()

        chatViewModel.init()
        observeChatSessionEvents(chatViewModel.events)

        iconVibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        initActivityResultLaunchers()
        installBackHandler()

        setContent {
            ZibeTheme {
                val mediaUiState = ChatMediaUiState(
                    recordingElapsedMs = recordingElapsedMs.longValue,
                    isRecordingCanceled = isRecordingCanceled.value,
                    isAudioUploading = isAudioUploading.value
                )
                val callbacks = ChatCallbacks(
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onProfileClick = { openProfile() },
                    onToggleNotifications = chatViewModel::onToggleNotificationsClicked,
                    onToggleBlock = chatViewModel::onToggleBlockClicked,
                    onDeleteChoiceMode = chatViewModel::onDeleteChoiceMode,
                    onConfirmHide = chatViewModel::onConfirmHide,
                    onDeleteSelected = chatViewModel::onDeleteSelectedMessages,
                    onClearSelection = chatViewModel::clearSelection,
                    onPhotoSourceClick = chatViewModel::onSendPhotoClicked,
                    onLaunchCamera = { launchCamera() },
                    onLaunchGallery = { launchGallery() },
                    onRemovePendingPhoto = chatViewModel::onRemovePendingPhoto,
                    onTextChanged = chatViewModel::onTextChanged,
                    onSendText = chatViewModel::onSendMessage,
                    onSendPhoto = { url -> lifecycleScope.launch { chatViewModel.onSendPhoto(url) } },
                    onSendAudio = { url, duration -> sendAudio(url, duration) },
                    onSelectionChanged = chatViewModel::onMessageSelectionChanged,
                    onMicPressed = { handleMicPressed() },
                    onMicMoved = { x, y, width -> handleMicMoved(x, y, width) },
                    onMicReleased = { handleMicReleased() }
                )
                ChatRoute(
                    viewModel = chatViewModel,
                    mediaUiState = mediaUiState,
                    callbacks = callbacks,
                    snackBarManager = snackBarManager
                )
            }
        }

        clearNotifications()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(DsR.color.zibe_dark_bg)
        window.navigationBarColor = getColor(DsR.color.zibe_dark_bg)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    override fun onStart() {
        super.onStart()
        chatViewModel.onThreadScreenStarted()
    }

    override fun onStop() {
        super.onStop()
        chatViewModel.onThreadScreenStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseRecorder()
        ChatAudioPlayer.release()
    }

    private fun installBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (chatViewModel.chatState.value.selectedIds.isNotEmpty()) {
                    chatViewModel.clearSelection()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initActivityResultLaunchers() {
        uCropResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                chatViewModel.handleCroppedImageResult(
                    result.resultCode,
                    pendingImageName,
                    result.data
                )
                pendingImageName = null
            }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    chatViewModel.onRemovePendingPhoto()
                    chatViewModel.startUCropFlow(uri, this, uCropResultLauncher)
                } else {
                    pendingImageName = null
                }
            }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    imageUriCamera?.let { uri ->
                        chatViewModel.onRemovePendingPhoto()
                        chatViewModel.startUCropFlow(uri, this, uCropResultLauncher)
                    }
                } else {
                    imageUriCamera?.let { contentResolver.delete(it, null, null) }
                    pendingImageName = null
                }
                imageUriCamera = null
            }

        requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val granted = result.values.all { it }
                if (granted) {
                    onPermissionsGranted?.invoke()
                } else {
                    onPermissionsDenied?.invoke()
                }
                onPermissionsGranted = null
                onPermissionsDenied = null
            }
    }

    private fun launchCamera() {
        requestPermissions(
            permissions = arrayOf(Manifest.permission.CAMERA),
            onGranted = {
                pendingImageName = createPhotoFileName()
                val cameraUri = createCameraImageUri()
                if (cameraUri == null) {
                    lifecycleScope.launch {
                        chatViewModel.onError(UiText.StringRes(R.string.msg_camera_error))
                    }
                    return@requestPermissions
                }
                imageUriCamera = cameraUri
                takePictureLauncher.launch(cameraUri)
            },
            onDenied = {
                lifecycleScope.launch {
                    chatViewModel.onError(UiText.StringRes(R.string.err_camera_permission_denied))
                }
            }
        )
    }

    private fun launchGallery() {
        pendingImageName = createPhotoFileName()
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun createPhotoFileName(): String = "chat_photo_${System.currentTimeMillis()}.jpg"

    private fun createCameraImageUri(): Uri? {
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
        if (!picturesDir.exists() && !picturesDir.mkdirs()) return null
        val file = File.createTempFile("chat_camera_", ".jpg", picturesDir)
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }

    private fun handleMicPressed() {
        requestPermissions(
            permissions = arrayOf(Manifest.permission.RECORD_AUDIO),
            onGranted = { startRecording() },
            onDenied = {
                lifecycleScope.launch {
                    chatViewModel.onError(UiText.StringRes(R.string.err_zibe))
                }
            }
        )
    }

    private fun handleMicMoved(x: Float, y: Float, width: Float) {
        chatViewModel.onMicMoved(x, y, width)
        if (mediaRecorder == null) return
        val threshold = -width * 0.5f
        val canceled = x < threshold
        if (isRecordingCanceled.value != canceled) {
            isRecordingCanceled.value = canceled
        }
    }

    private fun handleMicReleased() {
        if (mediaRecorder == null) {
            chatViewModel.onMicReleased()
            return
        }
        finishRecording(send = !isRecordingCanceled.value)
        chatViewModel.onMicReleased()
    }

    private fun startRecording() {
        if (mediaRecorder != null) return

        val fileName = createAudioFileName()
        val output = prepareAudioOutput(fileName)
        if (output == null) {
            lifecycleScope.launch {
                chatViewModel.onError(UiText.StringRes(R.string.chat_error_create_audio))
            }
            return
        }

        pendingAudioName = fileName
        currentAudioUri = output.first
        currentPfd = output.second

        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(output.second.fileDescriptor)
        }

        try {
            recorder.prepare()
            recorder.start()
        } catch (_: Exception) {
            lifecycleScope.launch {
                chatViewModel.onError(UiText.StringRes(R.string.chat_error_open_audio))
            }
            recorder.release()
            releaseRecorder()
            return
        }

        mediaRecorder = recorder
        recordStartElapsed = SystemClock.elapsedRealtime()
        isRecordingCanceled.value = false
        startRecordingTicker()
        chatViewModel.onMicPressed()
        vibrateShort()
    }

    private fun finishRecording(send: Boolean) {
        val elapsed = SystemClock.elapsedRealtime() - recordStartElapsed
        val audioUri = currentAudioUri
        val audioFile = currentAudioFile
        val audioName = pendingAudioName

        releaseRecorder()
        stopRecordingTicker()
        recordStartElapsed = 0L
        isRecordingCanceled.value = false
        pendingAudioName = null
        currentAudioUri = null
        currentAudioFile = null

        if (!send) {
            deleteAudioOutput(audioUri, audioFile)
            return
        }

        if (elapsed < MIN_AUDIO_DURATION_MS) {
            deleteAudioOutput(audioUri, audioFile)
            snackBarManager.show(
                uiText = UiText.StringRes(R.string.chat_recording_too_short),
                type = ZibeSnackType.WARNING
            )
            return
        }

        if (audioUri == null || audioName == null) return

        isAudioUploading.value = true

        lifecycleScope.launch {
            val url = chatViewModel.uploadMedia(audioName, audioUri, PATH_AUDIOS)
            isAudioUploading.value = false
            if (url == null) {
                chatViewModel.onError(UiText.StringRes(R.string.chat_error_upload_audio))
            } else {
                sendAudio(url, elapsed)
            }
        }
    }

    private fun createAudioFileName(): String = "audio_${System.currentTimeMillis()}.m4a"

    private fun prepareAudioOutput(fileName: String): Pair<Uri, ParcelFileDescriptor>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = createAudioMediaUri(fileName) ?: return null
            val pfd = contentResolver.openFileDescriptor(uri, "w") ?: return null
            currentAudioFile = null
            Pair(uri, pfd)
        } else {
            val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return null
            if (!musicDir.exists() && !musicDir.mkdirs()) return null
            val file = File(musicDir, fileName)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            currentAudioFile = file
            Pair(Uri.fromFile(file), pfd)
        }
    }

    private fun createAudioMediaUri(fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/m4a")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + File.separator + "Zibe"
                )
            }
        }
        return contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun deleteAudioOutput(uri: Uri?, file: File?) {
        if (uri != null) {
            contentResolver.delete(uri, null, null)
        }
        file?.delete()
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        mediaRecorder?.release()
        mediaRecorder = null

        try {
            currentPfd?.close()
        } catch (_: Exception) {
        }
        currentPfd = null
    }

    private fun startRecordingTicker() {
        recordingJob?.cancel()
        recordingJob = lifecycleScope.launch {
            while (mediaRecorder != null) {
                recordingElapsedMs.longValue = SystemClock.elapsedRealtime() - recordStartElapsed
                delay(100)
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingJob?.cancel()
        recordingJob = null
        recordingElapsedMs.longValue = 0L
    }

    private fun vibrateShort() {
        if (::iconVibrator.isInitialized) {
            iconVibrator.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun sendAudio(url: String, duration: Long) {
        lifecycleScope.launch {
            chatViewModel.onSendAudio(url, duration)
        }
    }

    private fun openProfile() {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra(EXTRA_USER_ID, chatViewModel.otherUid)
        }
        startActivity(intent)
    }

    private fun requestPermissions(
        permissions: Array<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            onGranted()
            return
        }

        onPermissionsGranted = onGranted
        onPermissionsDenied = onDenied
        requestPermissionsLauncher.launch(missing.toTypedArray())
    }

    private fun clearNotifications() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }
}


