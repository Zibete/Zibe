package com.zibete.proyecto1.ui.chat

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
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
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterChat
import com.zibete.proyecto1.databinding.ActivityChatBinding
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.core.constants.ERR_ZIBE
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ChatActivity : BaseChatSessionActivity() {

    // ===== VM / Binding =====
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var binding: ActivityChatBinding
    private val hideDateRunnable = Runnable { binding.linearDate.isVisible = false }

    // ===== Recycler =====
    private lateinit var adapter: AdapterChat
    private lateinit var layoutManager: LinearLayoutManager

    // ===== Media / Storage =====
    private var imageUriCamera: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioUri: Uri? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var pendingAudioName: String? = null
    private var pendingImageName: String? = null
    private var recordStartElapsed: Long = 0L
    private lateinit var iconVibrator: Vibrator

    // ===== Launchers / Permisos =====
    private lateinit var uCropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsGranted: (() -> Unit)? = null

    // ==================================== onCreate ====================================
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeChatSessionEvents(chatViewModel.events)

        setupToolbar()
        setupRecycler()
        initActivityResultLaunchers()
        setupUiListeners()
        setupTextWatcher()
        setupMicGesture()
        setupScrollHelpers()

        resetUiState()


        collectUiState()
        configureChatTitle()
        startAudioCancelBlink()
        clearNotifications()
    }

    // ==================================== Setup ====================================

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecycler() {
        layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = false
            stackFromEnd = true
        }
        binding.rvMsg.layoutManager = layoutManager

        adapter = AdapterChat(
            maxSize = MAX_CHAT_SIZE,
            context = this,
            hasSelection = { chatViewModel.chatState.value.selectedIds.isNotEmpty() },
            isSelected = { item -> chatViewModel.chatState.value.selectedIds.contains(item.id) },
            onSelectionChanged = { item, selected ->
                chatViewModel.onMessageSelectionChanged(item, selected)
            },
            myAudioAvatarUrl = chatViewModel.myIdentity.userPhotoUrl,
            otherAudioAvatarUrl = chatViewModel.otherProfile.value?.photoUrl,
            myUid = chatViewModel.myUid
        )


        binding.rvMsg.adapter = adapter
    }

    private fun configureChatTitle() {
        if (chatViewModel.nodeType == NODE_DM) {
            binding.cardviewTitle.isVisible = false
        } else {
            binding.tvChatTitle.text =
                getString(R.string.chat_private_in_group, chatViewModel.groupName)
            binding.cardviewTitle.isVisible = true
        }
    }

    private fun clearNotifications() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    private fun startAudioCancelBlink() {
        ObjectAnimator.ofFloat(binding.tvCancelAudio, "alpha", 1f, 0f, 1f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        currentAudioUri = null
        iconVibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    // ==================================== Collectors ====================================

    private fun collectUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    chatViewModel.headerState.collect { state ->
                        when (state) {
                            ChatHeaderState.Loading -> {
                                binding.nameUser.text = getString(R.string.loading)
                                binding.tvStatus.text = getString(R.string.loading)
                            }

                            is ChatHeaderState.Loaded -> {
                                binding.nameUser.text = state.name
                                binding.tvStatus.text = state.status
                                Glide.with(this@ChatActivity)
                                    .load(state.photoUrl)
                                    .into(binding.userImage)

                                binding.layoutChat.isVisible = !state.isBlocked
                                binding.layoutBloq.isVisible = state.isBlocked
                                binding.linearLottie.isVisible = !state.isBlocked
                            }
                        }
                    }
                }

                launch {
                    chatViewModel.chatState.collect { state ->

                        adapter.submitList(state.messages) {
                            if (adapter.itemCount > 0) {
                                binding.rvMsg.scrollToPosition(adapter.itemCount - 1)
                            }
                        }

                        if (state.photoReady || state.textReady) updateSendUiState() else resetUiState()

                        if (state.showPhotoPicker) {
                            openMediaSourcePicker()
                            chatViewModel.onPhotoPickerHandled()
                        }

                        val selectionCount = state.selectedIds.size
                        binding.linearDeleteMsg.isVisible = selectionCount > 1
                        binding.countDeleteMsg.text = selectionCount.toString()
                    }
                }


                launch {
                    chatViewModel.userStatus.collect { status ->
                        when (status) {
                            is UserStatus.Online -> {
                                binding.iconConnected.isVisible = true
                                binding.iconDisconnected.isVisible = false
                                binding.tvStatus.text = getString(R.string.online)
                                binding.tvStatus.setTypeface(null, Typeface.NORMAL)
                            }

                            is UserStatus.TypingOrRecording -> {
                                binding.iconConnected.isVisible = true
                                binding.iconDisconnected.isVisible = false
                                binding.tvStatus.text = status.text
                                binding.tvStatus.setTypeface(null, Typeface.ITALIC)
                            }

                            is UserStatus.LastSeen -> {
                                binding.iconConnected.isVisible = false
                                binding.iconDisconnected.isVisible = true
                                binding.tvStatus.text = status.text
                            }

                            is UserStatus.Offline -> {
                                binding.iconDisconnected.isVisible = true
                                binding.iconConnected.isVisible = false
                                binding.tvStatus.text = getString(R.string.offline)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================================== Listeners ====================================

    private fun setupUiListeners() {

        binding.btnCamera.setOnClickListener {
            chatViewModel.onSendPhotoClicked()
        }

        binding.btnSendMsg.setOnClickListener {
            val textMessage = binding.msg.text.toString()
            lifecycleScope.launch { chatViewModel.onSendMessage(textMessage) }
            binding.msg.setText("")
        }

        binding.linearDeleteMsg.setOnClickListener {
            binding.trashAnimated.playAnimation()
            chatViewModel.onDeleteSelectedMessages()
        }

        binding.cancelAction.setOnClickListener {
            resetUiState()
        }

        binding.photo.setOnClickListener {
            val photoUrl = chatViewModel.otherProfile.value?.photoUrl ?: return@setOnClickListener
            PhotoViewerActivity.startSingle(this, photoUrl)
        }

        binding.buttonUnblockUser.setOnClickListener {
            chatViewModel.onUnblockClicked()
        }

        binding.linearNameUser.setOnClickListener { v ->
            startActivity(
                Intent(v.context, ProfileActivity::class.java)
                    .putExtra(EXTRA_USER_ID, chatViewModel.otherUid)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    private fun setupTextWatcher() {
        binding.msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (binding.msg.text.isEmpty()) {
                    chatViewModel.onTextReady(false)
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.msg.text.isNotEmpty()) {
                    chatViewModel.onTextReady(true)
                    chatViewModel.setUserActivityStatus(getString(R.string.typing))
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.msg.text.isEmpty()) {
                    chatViewModel.onTextReady(false)
                    chatViewModel.setUserActivityStatus(getString(R.string.online))
                }
            }
        })
    }

    private fun setupScrollHelpers() {
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val total = adapter.itemCount
                val last = layoutManager.findLastVisibleItemPosition()
                val first = layoutManager.findFirstVisibleItemPosition()
                val atEnd = last + 1 >= total

                if (total > 0 && !atEnd) {
                    binding.linearBack.isVisible = true
                    binding.linearDate.isVisible = true
                    binding.linearDate.isVisible = true
                    binding.rvMsg.removeCallbacks(hideDateRunnable)
                    binding.rvMsg.postDelayed(hideDateRunnable, 2000)
                    binding.tvDate.text = adapter.getDateChat(first)
                } else {
                    binding.linearBack.isVisible = false
                    binding.linearDate.isVisible = false
                }
            }
        })

        binding.buttonScrollBack.setOnClickListener {
            if (adapter.itemCount > 0) binding.rvMsg.scrollToPosition(adapter.itemCount - 1)
        }

        setMicButton()
    }

    // ==================================== Lifecycle ====================================

    // ChatActivity.kt (ONSTART / ONSTOP) - DM y group_dm

    override fun onStart() {
        super.onStart()
        chatViewModel.onThreadScreenStarted()
    }

    override fun onStop() {
        chatViewModel.onThreadScreenStopped()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdapterChat.mediaPlayer?.let { player ->
            try { player.stop() } catch (_: Exception) {}
            AdapterChat.mediaPlayer = null
        }
        chatViewModel.onThreadScreenStopped()
    }

    // ==================================== Menu ====================================

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val state = chatViewModel.headerState.value
        if (state is ChatHeaderState.Loaded) {
            menu.findItem(R.id.action_delete_chat).isVisible = true
            menu.findItem(R.id.action_notifications_off).isVisible = state.notificationsEnabled
            menu.findItem(R.id.action_notifications_on).isVisible = !state.notificationsEnabled
            menu.findItem(R.id.action_block).isVisible = !state.isBlocked
            menu.findItem(R.id.action_unblock).isVisible = state.isBlocked
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_notifications_off, R.id.action_notifications_on -> {
                chatViewModel.onToggleNotificationsClicked()
                return true
            }
            R.id.action_bloq -> {
                chatViewModel.onBlockClicked()
                return true
            }
            R.id.action_desbloq -> {
                chatViewModel.onUnblockClicked()
                return true
            }
            R.id.action_delete_chat -> {
                chatViewModel.onDeleteChatClicked()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ==================================== Launchers / Permissions ====================================

    private fun initActivityResultLaunchers() {

        uCropResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            chatViewModel.handleCroppedImageResult(result.resultCode, pendingImageName, result.data)
        }

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                chatViewModel.startUCropFlow(uri, this, uCropResultLauncher)
            }
        }

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUriCamera != null) {
                chatViewModel.startUCropFlow(imageUriCamera!!, this, uCropResultLauncher)
            } else {
                imageUriCamera = null
                pendingImageName = null
            }
        }

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = result.values.all { it }
            if (allGranted) onPermissionsGranted?.invoke()
            onPermissionsGranted = null
        }
    }

    private fun ensurePermissions(perms: Array<String>, onGranted: () -> Unit) {
        val need = perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need) {
            onPermissionsGranted = onGranted
            requestPermissionsLauncher.launch(perms)
        } else {
            onGranted()
        }
    }

    // ==================================== Media Picker ====================================

    private fun openMediaSourcePicker() {
        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)
        val imgCancel = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val title = viewFilter.findViewById<TextView>(R.id.tv_title)

        viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete).isVisible = false
        title.text = getString(R.string.enviar_desde)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogApp)
            .setView(viewFilter)
            .create()

        dialog.show()

        cameraSelection.setOnClickListener {
            ensurePermissions(arrayOf(Manifest.permission.CAMERA)) {
                lifecycleScope.launch { startCameraModern() }
            }
            dialog.dismiss()
        }

        gallerySelection.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            dialog.dismiss()
        }

        imgCancel.setOnClickListener { dialog.dismiss() }
    }

    private suspend fun startCameraModern() {
        pendingImageName = "IMG_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, pendingImageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zibe")
            }
        }

        imageUriCamera = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (imageUriCamera == null) {
            chatViewModel.onError("No se pudo abrir la cámara")
            return
        }

        takePictureLauncher.launch(imageUriCamera!!)
    }

    // ==================================== Audio Record ====================================

    private suspend fun startRecordAudio() {
        val perms = arrayOf(Manifest.permission.RECORD_AUDIO)

        val anyDenied = perms.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (anyDenied) {
            ensurePermissions(perms) { lifecycleScope.launch { startRecordAudio() } }
            return
        }

        if (mediaRecorder != null) return

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        pendingAudioName = "AUD_${sdf.format(Date())}.m4a"

        currentPfd = null
        currentAudioUri = null

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, pendingAudioName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Zibe")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                currentAudioUri = contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return chatViewModel.onError("No se pudo crear el archivo de audio")

                currentPfd = contentResolver.openFileDescriptor(currentAudioUri!!, "w")
                    ?: return chatViewModel.onError("No se pudo abrir el archivo de audio")

            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: return chatViewModel.onError("No hay directorio de música disponible")

                val outFile = File(dir, pendingAudioName!!)
                currentAudioUri = FileProvider.getUriForFile(
                    this,
                    "$packageName.provider",
                    outFile
                )

                currentPfd = ParcelFileDescriptor.open(
                    outFile,
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                )
            }
        } catch (e: Exception) {
            chatViewModel.onError(e.message ?: ERR_ZIBE)
            currentPfd?.closeQuietly()
            currentPfd = null
            currentAudioUri = null
            return
        }

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentPfd!!.fileDescriptor)
                prepare()
                start()
            }
        } catch (e: Exception) {
            currentPfd?.closeQuietly()
            currentPfd = null

            if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
                runCatching {
                    val cv = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(currentAudioUri!!, cv, null, null)
                    contentResolver.delete(currentAudioUri!!, null, null)
                }
            }

            currentAudioUri = null
            mediaRecorder = null
            chatViewModel.onError(e.message ?: ERR_ZIBE)
            return
        }

        recordStartElapsed = SystemClock.elapsedRealtime()
        setMicAnimated()

        binding.msg.visibility = View.GONE
        binding.btnCamera.visibility = View.GONE
        binding.linearTimer.visibility = View.VISIBLE
        binding.timer.base = recordStartElapsed
        binding.timer.start()

        chatViewModel.setUserActivityStatus(getString(R.string.recording))
    }

    private fun stopRecordAudio() {
        if (mediaRecorder == null) {
            cancelRecordAudio()
            return
        }

        val audioDurationMs = SystemClock.elapsedRealtime() - recordStartElapsed
        val tooShort = audioDurationMs < 1000L

        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } finally {
            mediaRecorder = null
            currentPfd?.closeQuietly()
            currentPfd = null
        }

        if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
            runCatching {
                val cv = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                contentResolver.update(currentAudioUri!!, cv, null, null)
            }
        }

        if (tooShort || currentAudioUri == null) {
            runCatching {
                if (currentAudioUri != null) {
                    contentResolver.delete(currentAudioUri!!, null, null)
                }
            }
            cancelRecordAudio()
            return
        }

        val fileName = pendingAudioName ?: "AUD_${System.currentTimeMillis()}.m4a"
        val localUri = currentAudioUri!!

        lifecycleScope.launch {
            val url = chatViewModel.uploadMedia(
                fileName = fileName,
                uri = localUri,
                path = PATH_AUDIOS
            )

            if (url == null) {
                chatViewModel.onError("No se pudo subir el audio")
                return@launch
            }

            chatViewModel.onSendAudio(url, audioDurationMs)

            runCatching { contentResolver.delete(localUri, null, null) }
        }

        normalizeUiCancelRecordAudio()
        chatViewModel.setUserActivityStatus(getString(R.string.online))
    }

    private fun cancelRecordAudio() {
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null

        currentPfd?.closeQuietly()
        currentPfd = null

        if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
            runCatching {
                val cv = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                contentResolver.update(currentAudioUri!!, cv, null, null)
            }
        }

        binding.trashAnimated2.apply {
            visibility = View.VISIBLE
            playAnimation()
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    visibility = View.GONE
                    removeAnimatorListener(this)
                }
            })
        }

        normalizeUiCancelRecordAudio()
        chatViewModel.setUserActivityStatus(getString(R.string.online))

        runCatching {
            if (currentAudioUri != null) {
                contentResolver.delete(currentAudioUri!!, null, null)
            }
        }

        currentAudioUri = null
        pendingAudioName = null
    }

    // ==================================== UI Helpers ====================================

    private fun normalizeUiCancelRecordAudio() {
        vibrate(80)
        binding.btnMic.cancelAnimation()
        binding.btnMic.clearAnimation()
        binding.timer.stop()
        resetUiState()
    }

    private fun resetUiState() {
        binding.msg.isVisible = true
        binding.btnMic.isVisible = true
        binding.btnCamera.isVisible = true
        binding.loadingPhoto.isVisible = true
        binding.loadingButton.isVisible = true

        binding.linearTimer.isVisible = false
        binding.frameSendMsg.isVisible = false
        binding.btnSendMsg.isVisible = false
        binding.linearPhotoView.isVisible = false
    }

    private fun updateSendUiState() {
        binding.btnMic.isVisible = false
        binding.loadingPhoto.isVisible = false
        binding.loadingButton.isVisible = false
        binding.frameSendMsg.isVisible = true
        binding.btnSendMsg.isVisible = true
    }

    private fun ParcelFileDescriptor?.closeQuietly() {
        runCatching { this?.close() }
    }

    private fun vibrate(ms: Long) {
        iconVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ==================================== Mic Gesture ====================================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicGesture() {
        var firstTouchX = 0f
        var dX = 0f
        val scale = resources.displayMetrics.density

        binding.btnMic.setOnTouchListener { v, event ->
            if (binding.linearPhotoView.isVisible) return@setOnTouchListener true
            if (firstTouchX == 0f) firstTouchX = binding.btnMic.x

            val xAnim = SpringAnimation(binding.btnMic, SpringAnimation.X).apply {
                spring = SpringForce().apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                    stiffness = SpringForce.STIFFNESS_HIGH
                    finalPosition = firstTouchX
                }
            }

            val halfWidth = resources.displayMetrics.widthPixels / 2
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lifecycleScope.launch { startRecordAudio() }
                    xAnim.cancel()
                    vibrate(80)
                    dX = v.x - event.rawX - (75 * scale + 0.5f).toInt()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    xAnim.start()
                    setMicButton()
                    stopRecordAudio()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (binding.linearTimer.isVisible) {
                        if (event.rawX > halfWidth) {
                            val move = event.rawX + dX
                            binding.btnMic.animate().x(move).setDuration(0).start()
                        } else {
                            setMicButton()
                            cancelRecordAudio()
                            xAnim.start()
                        }
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun setMicAnimated() {
        val scale = resources.displayMetrics.density
        val dp200 = (200 * scale + 0.5f).toInt()
        val dp65 = (65 * scale + 0.5f).toInt()

        binding.btnMic.setImageResource(android.R.color.transparent)
        binding.btnMic.setBackgroundResource(R.color.transparent)
        binding.btnMic.setAnimation(R.raw.lf30_editor_24iqgref)
        binding.btnMic.layoutParams = LinearLayout.LayoutParams(dp200, dp200).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        binding.btnMic.playAnimation()

        binding.linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(0, 0, -dp65, -dp65)
        }
    }

    private fun setMicButton() {
        binding.btnMic.setBackgroundResource(R.drawable.badge_round)
        binding.btnMic.setImageResource(R.drawable.ic_baseline_mic_24)

        val scale = resources.displayMetrics.density
        val dp10 = (10 * scale + 0.5f).toInt()
        val dp13 = (13 * scale + 0.5f).toInt()
        val dp50 = (50 * scale + 0.5f).toInt()

        binding.linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.btnMic.layoutParams = LinearLayout.LayoutParams(dp50, dp50).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(dp10, dp10, dp10, dp10)
        }
        binding.btnMic.setPadding(dp13, dp13, dp13, dp13)
    }
}
