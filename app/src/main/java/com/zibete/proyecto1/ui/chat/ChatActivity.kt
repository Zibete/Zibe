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
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterChat
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.PATH_AUDIOS
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.databinding.ActivityChatBinding
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.ui.media.PhotoSourceSheet
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

    companion object {
        const val REQ_KEY_CHAT_SEND = "REQ_KEY_CHAT_SEND"
    }

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

        chatViewModel.init()

        observeChatSessionEvents(chatViewModel.events)

        setupToolbar()
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
//        val toolbar = findViewById<Toolbar>(R.id.toolbar_chat)
//        setSupportActionBar(toolbar)
//        supportActionBar?.title = ""
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupToolbar(
            toolbar = binding.toolbarChat
        )
    }

    private fun initActivityResultLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                chatViewModel.onPhotoSelected(uri)
            }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUriCamera?.let { chatViewModel.onPhotoSelected(it) }
            }
        }
    }

    private fun setupRecycler(myPhoto: String?, otherPhoto: String?) {
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
            myAudioAvatarUrl = myPhoto,
            otherAudioAvatarUrl = otherPhoto,
            myUid = chatViewModel.myUid
        )
        binding.rvMsg.adapter = adapter
    }

    private fun setupUiListeners() {
        binding.userImage.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, chatViewModel.partnerId)
            }
            startActivity(intent)
        }

        binding.nameUser.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, chatViewModel.partnerId)
            }
            startActivity(intent)
        }

        binding.btnSendMsg.setOnClickListener {
            chatViewModel.onSendMessage(binding.msg.text.toString())
        }

        binding.btnCamera.setOnClickListener {
            chatViewModel.onCameraClicked()
        }

        binding.cancelAction.setOnClickListener {
            chatViewModel.onRemovePendingPhoto()
        }

        binding.layoutBloq.setOnClickListener {
            // Unblock logic if needed
        }
    }

    private fun setupTextWatcher() {
        binding.msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                chatViewModel.onTextChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicGesture() {
        binding.btnMic.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    chatViewModel.onMicPressed()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    chatViewModel.onMicMoved(event.x, event.y, binding.btnMic.width.toFloat())
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    chatViewModel.onMicReleased()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupScrollHelpers() {
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Date logic
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible != RecyclerView.NO_POSITION && ::adapter.isInitialized) {
                    val item = adapter.currentList.getOrNull(firstVisible)
                    item?.message?.createdAt?.let { ts ->
                        binding.tvDate.text = formatHeaderDate(ts)
                        binding.linearDate.isVisible = true
                        binding.linearDate.handler?.removeCallbacks(hideDateRunnable)
                        binding.linearDate.postDelayed(hideDateRunnable, 2000)
                    }
                }
            }
        })
    }

    private fun formatHeaderDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun resetUiState() {
        binding.frameSendMsg.isVisible = false
        binding.btnMic.isVisible = true
        binding.btnCamera.isVisible = true
        binding.msg.hint = getString(R.string.escribe_un_mensaje)

        binding.linearPhotoView.isVisible = false
        binding.msg.text?.clear()
    }

    private fun updateSendUiState() {
        binding.frameSendMsg.isVisible = true
        binding.btnMic.isVisible = false
        binding.btnCamera.isVisible = false
    }

    private fun configureChatTitle() {
        // ...
    }

    private fun startAudioCancelBlink() {
        // ...
    }

    private fun clearNotifications() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    override fun onDestroy() {
        super.onDestroy()
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

                                // Initialize recycler once header is loaded (myIdentity will be ready)
                                if (binding.rvMsg.adapter == null) {
                                    setupRecycler(
                                        myPhoto = chatViewModel.myIdentity.userPhotoUrl,
                                        otherPhoto = state.photoUrl
                                    )
                                }
                            }
                        }
                    }
                }

                launch {
                    chatViewModel.chatState.collect { state ->
                        if (::adapter.isInitialized) {
                            adapter.submitList(state.messages) {
                                if (adapter.itemCount > 0) {
                                    binding.rvMsg.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                        }

                        if (state.photoReady || state.textReady) updateSendUiState() else resetUiState()

                        if (state.showPhotoPicker) {
                            PhotoSourceSheet.newInstance(
                                showDelete = false,
                                titleRes = R.string.send_photo,
                                requestKey = REQ_KEY_CHAT_SEND
                            ).show(supportFragmentManager, PhotoSourceSheet.TAG)
                            chatViewModel.onPhotoPickerShown()
                        }

                        if (state.pendingPhotoUri != null) {
                            binding.linearPhotoView.isVisible = true
                            Glide.with(this@ChatActivity)
                                .load(state.pendingPhotoUri)
                                .into(binding.photo)
                        } else {
                            binding.linearPhotoView.isVisible = false
                        }

                        if (state.isRecording) {
                            // Show recording UI
                        } else {
                            // Hide recording UI
                        }
                    }
                }
            }
        }
    }

    // ... Rest of the activity
}
