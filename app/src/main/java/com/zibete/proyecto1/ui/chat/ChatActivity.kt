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
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.SlideProfileActivity
import com.zibete.proyecto1.adapters.AdapterChat
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.ActivityChatBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.ui.constants.Constants.AUDIO
import com.zibete.proyecto1.ui.constants.Constants.AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.NODE_ACTIVE_CHAT_UID
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.ui.constants.MESSAGE_NOT_SENT_USER_BLOCKED
import com.zibete.proyecto1.utils.Utils.now
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : BaseChatSessionActivity() {

    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository

    val myUid  = userRepository.myUid
    val user  = userRepository.user


    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var binding: ActivityChatBinding

    // --------- Estado / datos ---------


    private var refChatWith: String? = null
    private var refChat: String? = null
    private var estadoUser: String? = null
    private var estadoYo: String? = null
    private var suActual: String? = null
    private var token: String? = null
    private var noVisto: Int = 0
    private var miNoVisto: Int = 0
    private var myName: String? = null


    private val userId = intent.extras?.getString("userId")?: ""
    private val nodeType = intent.extras?.getString("nodeType")?: ""
    private val userName = intent.extras?.getString("userName")?: ""

    // --------- listas / adaptador ---------
    private val chatsArrayList = ArrayList<Chats?>()
    private val extraUserList = ArrayList<Users?>()
    private lateinit var adapter: AdapterChat
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var linearNameUser: LinearLayout

    // --------- media / storage ---------
    private var imageUriCamera: Uri? = null
    private var msgType = Constants.MSG
    private var stringMsg: String? = null

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioUri: Uri? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var nameAudio: String? = null
    private var recordStartElapsed: Long = 0L



    private var listenerChatUnknown: ValueEventListener? = null

    private lateinit var iconVibrator: Vibrator

    // --------- Crop / pickers / permisos ---------
    private lateinit var uCropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private val hasMicPermission = AtomicBoolean(false)
    private val hasCameraPermission = AtomicBoolean(false)

    // callback pendiente para ejecutar luego de otorgar permisos
    private var onPermissionsGranted: (() -> Unit)? = null

    // ==================================== onCreate ====================================
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeChatSessionEvents(chatViewModel.events)

        // Header (nombre, foto, bloqueo, etc.)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.headerState.collect { state ->
                        when (state) {
                            ChatHeaderState.Loading -> {
                                binding.nameUser.text = getString(R.string.cargando)
                                binding.tvStatus.text = getString(R.string.offline)
                            }
                            is ChatHeaderState.Loaded -> {
                                binding.nameUser.text = state.name
                                binding.tvStatus.text = state.status
                                Glide.with(this@ChatActivity)
                                    .load(state.photoUrl)
                                    .into(binding.userImage)
                                if (state.shouldCloseChat) finish()


                                if (state.isBlocked) {
                                    binding.layoutChat.isVisible = false
                                    binding.layoutBloq.isVisible = true
                                }


                            }
                        }
                    }
                }

                // Estado online/escribiendo (mismo repeatOnLifecycle)
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


        // --- toolbar y notifs ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        ObjectAnimator.ofFloat(binding.tvCancelAudio, "alpha", 1f, 0f, 1f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE; start()
        }

        currentAudioUri = null
        iconVibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // --- Recycler ---
//        rvMsg = findViewById(R.id.rvMsg)
        mLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = false
            stackFromEnd = true
        }
        binding.rvMsg.layoutManager = mLayoutManager
        adapter = AdapterChat(
            chatsArrayList as ArrayList<Chats>,
            Constants.MAXCHATSIZE,
            applicationContext
        )
        binding.rvMsg.adapter = adapter

        // --- init launchers ---
        initActivityResultLaunchers()

        //esto tiene sentido??
        // --- estado inicial botones ---
        binding.msg.isVisible = true
        binding.btnMic.isVisible = true
        binding.btnCamera.isVisible = true
        binding.loadingButton.isVisible = false
        binding.frameSendMsg.isVisible = false
        binding.btnSendMsg.isVisible = false
        binding.linearPhotoView.isVisible = false
        binding.loadingPhoto.isVisible = false

        //esto tiene sentido??
        // --- extras ---
        lifecycleScope.launch { userRepository.setUserOnline() }

        // ===== Config chat =====
//        setupChatHeaderAndRefs()

        if (nodeType == NODE_CURRENT_CHAT) {
            binding.cardviewTitle.isVisible = false
//            myName = user.displayName
        } else { //en grupo
            binding.tvChatTitle.text = "Chat privado en ${userPreferencesRepository.groupName}"
            binding.cardviewTitle.isVisible = true
//            myName = userPreferencesRepository.userNameGroup
//            myPhoto = if (userPreferencesRepository.userType == ANONYMOUS_USER) {
//                getString(R.string.URL_PHOTO_DEF)
//            } else {
//                user.photoUrl?.toString() ?: ""
//            }
        }

        // ===== CropHelper ===== comentado porque lo cambiamos por --> uCrop ??
//        cropLauncher = CropHelper.registerLauncher(
//            caller = this,
//            ctx = this,
//            refSendImages = refMyReceiverData!!,
//            linearPhotoView = linearPhotoView,
//            linearPhoto = linearPhoto,
//            photo = photo,
//            loadingPhoto = loadingPhoto,
//            loadingButton = loadingButton,
//            frameSendMsg = frameSendMsg,
//            msg = msg,
//            btnCamera = btnCamera,
//            btnSendMsg = btnSendMsg
//        ) { uri ->
//            // Hay foto lista para enviar
//            msgType = Constants.PHOTO
//            stringMsg = uri.toString()   // Si CropHelper te devuelve downloadUrl, usá ese
//            btnMic.isVisible = false
//            loadingPhoto.isVisible = false
//            loadingButton.isVisible = false
//            frameSendMsg.isVisible = true
//            btnSendMsg.isVisible = true
//
//        }

        // ===== UI listeners =====
        val photoList = ArrayList<String>()
        binding.btnCamera.setOnClickListener {
            sendPhoto()
        }
        binding.btnSendMsg.setOnClickListener {
            sendMessage(null)
        }
        linearDeleteMsg.setOnClickListener {
            binding.trashAnimated.playAnimation()
            DeleteMsgs()
        }
        binding.cancelAction.setOnClickListener {
            cancelSendPhoto()
        }
        binding.photo.setOnClickListener {
            photoList.add(stringMsg!!) // <-- porque stringMsg?
            val intent = Intent(this@ChatActivity, SlidePhotoActivity::class.java).apply {
                putExtra("photoList", photoList)
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            startActivity(intent)
        }
        binding.buttonUnblockUser.setOnClickListener {
            chatViewModel.onUnblockClicked(userId, userName, nodeType)
        }

        linearNameUser.setOnClickListener { v ->
            if (extraUserList.size == 1) {
                val intent = Intent(this, SlideProfileActivity::class.java)
                extraUserList.reverse()
                intent.putExtra("userList", extraUserList)
                intent.putExtra("position", 0)
                intent.putExtra("rotation", 0)
                v.context.startActivity(intent)
            }
        }



        binding.msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (binding.msg.text.isEmpty()) {
                    binding.btnCamera.isVisible = true
                    binding.btnMic.isVisible = true
                    binding.btnSendMsg.isVisible = false
                    binding.frameSendMsg.isVisible = false
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.msg.text.isNotEmpty()) {
                    binding.btnCamera.isVisible = false
                    binding.btnMic.isVisible = false
                    binding.btnSendMsg.isVisible = true
                    binding.frameSendMsg.isVisible = true
                    firebaseRefsContainer.refDatos.child(myUid).child("Estado").child("estado")
                        .setValue(getString(R.string.escribiendo))
                    firebaseRefsContainer.refCuentas.child(myUid).child("estado").setValue(true)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                if (binding.msg.text.isEmpty()) {
                    binding.btnCamera.isVisible = true
                    binding.btnMic.isVisible = true
                    binding.btnSendMsg.isVisible = false
                    binding.frameSendMsg.isVisible = false
                    lifecycleScope.launch { userRepository.setUserOnline() } // esta bien?
                }
            }
        })

        // ===== Mic gesture =====
        setupMicGesture()

        // ===== Scroll / fecha =====
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val total = adapter.itemCount
                val last = mLayoutManager.findLastVisibleItemPosition()
                val first = mLayoutManager.findFirstVisibleItemPosition()
                val atEnd = last + 1 >= total
                if (total > 0 && !atEnd) {
                    findViewById<View>(R.id.linearBack).visibility = View.VISIBLE
                    findViewById<View>(R.id.linearDate).visibility = View.VISIBLE
                    Handler(Looper.getMainLooper()).postDelayed({
                        findViewById<View>(R.id.linearDate).visibility = View.GONE
                    }, 2000)
                    val date = adapter.getDate(first)
                    binding.tvDate.text = date
                } else {
                    findViewById<View>(R.id.linearBack).visibility = View.GONE
                    findViewById<View>(R.id.linearDate).visibility = View.GONE
                }
            }
        })
        binding.buttonScrollBack.setOnClickListener { setScrollbar() }

        // ===== Observadores =====
        hookChatListeners()
        // ===== Estados / block =====
        hookBlockAndPresence()

        setMicButton()

        visto()
    }

    // ---------------- Ciclo de vida ----------------
    override fun onPause() {
        super.onPause()
        lifecycleScope.launch { userRepository.setUserLastSeen() }

        refActual.setValue("") // Limpia referencia de chat activo
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { userRepository.setUserOnline() }
        refActual.setValue(myUid + (refChatWith ?: ""))
    }

    override fun onDestroy() {
        super.onDestroy()
        AdapterChat.mediaPlayer?.let { player ->
            try { player.stop() } catch (_: Exception) {}
            AdapterChat.mediaPlayer = null
        }

        if (refChatWith == NODE_GROUP_CHAT && listenerChatUnknown != null) {
            firebaseRefsContainer.refGroupUsers.child(userPreferencesRepository.groupName)
                .child(userId)
                .removeEventListener(listenerChatUnknown!!)
        }

        refActual.setValue("")
    }

    // ---------------- Menú ----------------
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val state = chatViewModel.headerState.value

        if (state is ChatHeaderState.Loaded) {
            menu.findItem(R.id.action_delete_chat).isVisible = true
            menu.findItem(R.id.action_notifications_off).isVisible = state.notificationsEnabled
            menu.findItem(R.id.action_notifications_on).isVisible  = !state.notificationsEnabled
            menu.findItem(R.id.action_block).isVisible   = !state.isBlocked
            menu.findItem(R.id.action_unblock).isVisible = state.isBlocked
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.action_notifications_off, R.id.action_notifications_on -> {
                chatViewModel.onToggleNotificationsClicked(userId, userName, nodeType)
                return true
            }
            R.id.action_bloq -> {
                chatViewModel.onBlockClicked(userId, userName, nodeType)
                return true
            }
            R.id.action_desbloq -> {
                chatViewModel.onUnblockClicked(userId, userName, nodeType)
                return true
            }
            R.id.action_delete_chat -> {
                chatViewModel.onDeleteClicked(userId, userName, nodeType)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ----------------------------- Activity Result Launchers -----------------------------
    private fun initActivityResultLaunchers() {
        // ✅ 1. uCrop Launcher (REEMPLAZO DE LA DEPENDENCIA OBSOLETA)
        uCropResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            chatViewModel.handleCroppedImageResult(result.resultCode, result.data, this)
        }

        // 2. Galería moderna
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // Delegamos la lógica de uCrop al VM, que nos dará las URIs
                chatViewModel.startUCropFlow(uri, this, uCropResultLauncher)
            }
        }

        // 3. Cámara moderna
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUriCamera != null) {
//                chatViewModel.startUCropFlow(imageUriCamera!!, this, uCropResultLauncher)
            } else {
                imageUriCamera = null
            }
        }

        // 4. Permisos múltiples
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = result.values.all { it }
            if (allGranted) {
                onPermissionsGranted?.invoke()
            }
            onPermissionsGranted = null
        }
    }

    // ----------------------------- Cámara / Galería / Crop -----------------------------
    private fun sendPhoto() {

        if (estadoUser == CHAT_STATE_BLOQ) {
            snackCenter(MESSAGE_NOT_SENT_USER_BLOCKED)
            return
        }

        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)
        val imgCancel = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val title = viewFilter.findViewById<TextView>(R.id.tv_title)
        viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete).visibility = View.GONE
        title.text = getString(R.string.enviar_desde)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogApp)
            .setView(viewFilter)
            .create()
        dialog.show()

        cameraSelection.setOnClickListener {
            ensurePermissions(arrayOf(Manifest.permission.CAMERA)) {
                startCameraModern()
            }
            dialog.dismiss()
        }

        gallerySelection.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            dialog.dismiss()
        }

        imgCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun startCameraModern() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zibe")
            }
        }
        imageUriCamera = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
        if (imageUriCamera == null) {
            snackCenter("No se pudo abrir la cámara")
            return
        }
        takePictureLauncher.launch(imageUriCamera!!)
    }

    // ----------------------------- Audio (MediaStore, sin rutas directas) -----------------------------
    private fun startRecordAudio() {
        // 1) Permisos
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        val needsLegacyWrite = Build.VERSION.SDK_INT <= 28
//        if (needsLegacyWrite) perms plusAssign Manifest.permission.WRITE_EXTERNAL_STORAGE

        val anyDenied = perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (anyDenied) {
            ensurePermissions(perms.toTypedArray()) { startRecordAudio() }
            return
        }
        if (mediaRecorder != null) return

        // 2) Preparar destino
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        nameAudio = "AUD_${sdf.format(Date())}.m4a"

        currentPfd = null
        currentAudioUri = null

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                // MediaStore con scoped storage
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, nameAudio)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Zibe")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                currentAudioUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("No se pudo crear el URI de audio")
                currentPfd = contentResolver.openFileDescriptor(currentAudioUri!!, "w")
                    ?: throw IllegalStateException("No se pudo abrir el archivo de audio")
            } else {
                // ≤28: archivo app-specific + FileProvider
                val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: throw IllegalStateException("No hay directorio de música disponible")
                val outFile = File(dir, nameAudio!!)
                currentAudioUri = FileProvider.getUriForFile(this, "$packageName.provider", outFile)
                currentPfd = ParcelFileDescriptor.open(
                    outFile,
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                )
            }
        } catch (e: Exception) {
            snackCenter("No se pudo iniciar la grabación")
            currentPfd?.closeQuietly()
            currentPfd = null
            currentAudioUri = null
            return
        }

        // 3) Grabar
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
            // Si quedó pendiente en ≥29, cerramos el pending para que el sistema lo limpie
            if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
                runCatching {
                    val cv = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                    contentResolver.update(currentAudioUri!!, cv, null, null)
                    contentResolver.delete(currentAudioUri!!, null, null)
                }
            }
            currentAudioUri = null
            mediaRecorder = null
            snackCenter("Error al grabar audio")
            return
        }

        // 4) UI grabando
        recordStartElapsed = SystemClock.elapsedRealtime()
        setMicAnimated()
        binding.msg.visibility = View.GONE
        binding.btnCamera.visibility = View.GONE
        linearTimer.visibility = View.VISIBLE
        binding.timer.base = recordStartElapsed
        binding.timer.start()

        firebaseRefsContainer.refDatos.child(myUid).child("Estado").child("estado")
            .setValue(getString(R.string.grabando))
        firebaseRefsContainer.refCuentas.child(myUid).child("estado").setValue(true)
    }

    private fun stopRecordAudio() {
        // Si no hay recorder, normalizar UI
        if (mediaRecorder == null) {
            cancelRecordAudio()
            return
        }

        val elapsedMs = SystemClock.elapsedRealtime() - recordStartElapsed
        val tooShort = elapsedMs < 1000L // < 1s lo consideramos demasiado corto

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

        // Cerrar pending en ≥29
        if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
            runCatching {
                val cv = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                contentResolver.update(currentAudioUri!!, cv, null, null)
            }
        }

        if (tooShort || currentAudioUri == null) {
            // Borramos el archivo si existe
            runCatching { if (currentAudioUri != null) contentResolver.delete(currentAudioUri!!, null, null) }
            cancelRecordAudio()
            return
        }

        // ---------- Subir a Firebase con putFile(uri) (robusto para content://) ----------
        val name = nameAudio ?: "AUD_${System.currentTimeMillis()}.m4a"
        val localUri = currentAudioUri!!

        refYourReceiverData!!.child(name).putFile(localUri)
            .addOnSuccessListener { task ->
                task.storage.downloadUrl
                    .addOnSuccessListener { uri ->
                        stringMsg = uri.toString()
                        msgType = Constants.AUDIO
                        val mm = (elapsedMs / 1000 / 60).toInt().toString().padStart(2, '0')
                        val ss = ((elapsedMs / 1000) % 60).toInt().toString().padStart(2, '0')
                        val elapsedText = "$mm:$ss"
                        sendMessage(elapsedText)
                        // Limpieza local: opcional (si querés conservarlo, comentá la línea de abajo)
                        runCatching { contentResolver.delete(localUri, null, null) }
                    }
                    .addOnFailureListener {
                        snackCenter("No se pudo obtener URL del audio")
                    }
            }
            .addOnFailureListener {
                snackCenter("Fallo al subir el audio")
            }

        normalizeUiCancelRecordAudio()
        lifecycleScope.launch { userRepository.setUserOnline() }
        // No anulamos immediately el URI por si falla la subida; si preferís, podés setearlo en null acá.
    }

    private fun cancelRecordAudio() {
        // Detener/limpiar recorder si quedaba algo
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null

        // Cerrar PFD si quedó abierto
        currentPfd?.closeQuietly()
        currentPfd = null

        // Si fue ≥29 y quedó pendiente, cerramos y borramos
        if (Build.VERSION.SDK_INT >= 29 && currentAudioUri != null) {
            runCatching {
                val cv = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                contentResolver.update(currentAudioUri!!, cv, null, null)
            }
        }

        // Animación de trash y ocultar al terminar
        binding.trashAnimated2.apply {
            visibility = View.VISIBLE
            playAnimation()
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { visibility = View.GONE }
            })
        }

        normalizeUiCancelRecordAudio()
        lifecycleScope.launch { userRepository.setUserOnline() }

        // Borrado del archivo (si existe)
        runCatching { if (currentAudioUri != null) contentResolver.delete(currentAudioUri!!, null, null) }
        currentAudioUri = null
        nameAudio = null
    }

    private fun normalizeUiCancelRecordAudio() {
        vibrate(80)
        binding.btnMic.cancelAnimation()
        binding.btnMic.clearAnimation()
        binding.timer.stop()
        linearTimer.visibility = View.GONE
        binding.msg.visibility = View.VISIBLE
        binding.btnCamera.visibility = View.VISIBLE
    }

    private fun ParcelFileDescriptor.closeQuietly() {
        try { close() } catch (_: Exception) {}
    }

    // ----------------------------- Mic Gesture -----------------------------
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
                    startRecordAudio()
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
                    if (linearTimer.isVisible) {
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

    // ----------------------------- Envío de mensajes -----------------------------
    private fun sendMessage(timerText: String?) {

//        val user = user ?: return

        val (textMiChatw, textSuChatw) = when (msgType) {
            Constants.PHOTO -> {
                binding.linearPhotoView.visibility = View.GONE
                binding.msg.visibility = View.VISIBLE
                binding.btnCamera.visibility = View.VISIBLE
                binding.btnMic.visibility = View.VISIBLE
                getString(R.string.photo_send) to getString(R.string.photo_received)
            }
            Constants.AUDIO -> getString(R.string.audio_send) to getString(R.string.audio_received)
            else -> {
                msgType = Constants.MSG
                stringMsg = binding.msg.text.toString()
                stringMsg to stringMsg
            }
        }

        if (stringMsg.isNullOrEmpty()) return

        if (estadoUser == "bloq") {
            snackCenter("Mensaje no enviado: Estás bloqueado por el usuario")
            return
        }

        val visto = if (suActual != myUid + refChatWith || suActual == null) 1 else 3
        val dateNow = now()
        val finalDate = if (timerText == null) dateNow else "$dateNow $timerText"

        val chatmsg = Chats(
            stringMsg!!,
            finalDate,
            myUid,
            msgType,
            visto
        )

        startedByMe!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(sbMe: DataSnapshot) {
                if (!sbMe.exists()) {
                    startedByHim!!.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(sbHim: DataSnapshot) {
                            (if (!sbHim.exists()) startedByMe else startedByHim)!!
                                .push().setValue(chatmsg)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                } else {
                    sbMe.ref.push().setValue(chatmsg)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val miChat = ChatWith(
            textMiChatw!!, finalDate, null, myUid,
            userId, nameUserFinal!!, yourPhoto, estadoYo!!, miNoVisto, 0
        )
        firebaseRefsContainer.refDatos.child(myUid).child(refChatWith!!).child(userId).setValue(miChat)

        if (suActual != myUid + refChatWith || suActual == null) {
            val count = noVisto + 1
            val suChat = ChatWith(
                textSuChatw!!, finalDate, null, myUid,
                myUid, myName!!, myPhoto, estadoUser!!, count, 1
            )
            firebaseRefsContainer.refDatos.child(userId!!).child(refChatWith!!).child(myUid)
                .setValue(suChat)

            if (estadoUser != "silent") {
                val body = JSONObject().apply {
                    put("novistos", count)
                    put("user", myName)
                    put("msg", suChat.msg)
                    put("id_user", chatmsg.sender)
                    put("type", refChatWith)
                }
                val json = JSONObject().apply {
                    put("to", token)
                    put("priority", "high")
                    put("data", body)
                }
                val req = object : JsonObjectRequest(
                    Method.POST, "https://fcm.googleapis.com/fcm/send", json, null, null
                ) {
                    override fun getHeaders(): MutableMap<String, String> = hashMapOf(
                        "content-type" to "application/json",
                        "authorization" to "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"
                    )
                }
                Volley.newRequestQueue(applicationContext).add(req)
            }
        } else {
            val suChat = ChatWith(
                textSuChatw!!, finalDate, null, myUid,
                myUid, myName!!, myPhoto, estadoUser!!, 0, 3
            )
            firebaseRefsContainer.refDatos.child(userId).child(refChatWith!!).child(myUid)
                .setValue(suChat)
        }

        binding.msg.setText("")
        stringMsg = null
        msgType = Constants.MSG
    }

    // ----------------------------- Helpers -----------------------------
    private fun setScrollbar() = binding.rvMsg.scrollToPosition(adapter.itemCount - 1)

    private fun vibrate(ms: Long) {
        iconVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
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

    // ========================= Bloques mantenidos =========================



    private fun hookChatListeners() {
        val childListener = object : ChildEventListener {
            override fun onChildAdded(ds: DataSnapshot, prev: String?)    { addChat(ds) }
            override fun onChildChanged(ds: DataSnapshot, prev: String?)  { actualizeChat(ds) }
            override fun onChildRemoved(ds: DataSnapshot)                 { deleteChat(ds) }
            override fun onChildMoved(ds: DataSnapshot, prev: String?)    {}
            override fun onCancelled(error: DatabaseError)                {}
        }
        startedByMe?.addChildEventListener(childListener)
        startedByHim?.addChildEventListener(childListener)

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) { setScrollbar() }
        })
    }

    private fun hookBlockAndPresence() {

        firebaseRefsContainer.refDatos.child(userId).child(NODE_CHATLIST).child(NODE_ACTIVE_CHAT_UID)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    suActual = ds.getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {}
            })


        firebaseRefsContainer.refDatos.child(myUid).child(refChatWith!!).child(userId).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val state = snapshot.getValue(String::class.java)
                    if (state == "bloq") {
                        binding.layoutBloq.visibility = View.VISIBLE
                        binding.layoutChat.visibility = View.GONE
                        binding.linearLottie.visibility = View.GONE
                    } else {
                        binding.layoutBloq.visibility = View.GONE
                        binding.layoutChat.visibility = View.VISIBLE
                        binding.linearLottie.visibility = View.VISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        firebaseRefsContainer.refDatos.child(userId).child(refChatWith!!).child(myUid).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    estadoUser = ds.getValue(String::class.java) ?: refChatWith
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        firebaseRefsContainer.refDatos.child(myUid).child(refChatWith!!).child(userId).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    estadoYo = ds.getValue(String::class.java) ?: refChatWith
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(0, 0, -dp65, -dp65)
        }
    }

    private fun setMicButton() {
        binding.btnMic.setBackgroundResource(R.drawable.marco_color_b_round)
        binding.btnMic.setImageResource(R.drawable.ic_baseline_mic_24)

        val scale = resources.displayMetrics.density
        val dp10 = (10 * scale + 0.5f).toInt()
        val dp13 = (13 * scale + 0.5f).toInt()
        val dp50 = (50 * scale + 0.5f).toInt()

        binding.linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.btnMic.layoutParams = LinearLayout.LayoutParams(dp50, dp50).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(dp10, dp10, dp10, dp10)
        }
        binding.btnMic.setPadding(dp13, dp13, dp13, dp13)
    }

    private fun visto() {
//        val me = user ?: return
        firebaseRefsContainer.refDatos.child(myUid).child(refChatWith!!).child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dsMain: DataSnapshot) {
                    miNoVisto = 0
                    if (dsMain.exists()) {
                        val msgDescontar = dsMain.child("noVisto").getValue(Int::class.java) ?: 0
                        if (msgDescontar > 0) {
                            startedByMe!!.orderByChild("date").limitToLast(msgDescontar)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(d1: DataSnapshot) { setDoubleCheck(d1) }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            startedByHim!!.orderByChild("date").limitToLast(msgDescontar)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(d2: DataSnapshot) { setDoubleCheck(d2) }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                        dsMain.ref.child("wVisto").setValue(3)
                        dsMain.ref.child("noVisto").setValue(0)
                    }
                }
                private fun setDoubleCheck(ds: DataSnapshot) {
                    if (ds.exists()) {
                        for (snap in ds.children) {
                            if (snap.hasChild("visto")) {
                                snap.ref.child("visto").setValue(3)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun DeleteMsgs() {
        for (chat in msgSelected) {
            startedByMe!!.orderByChild("date").equalTo(chat.date)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) {
                        if (ds.exists()) iterateDelete(ds, chat) else {
                            startedByHim!!.orderByChild("date").equalTo(chat.date)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(ds2: DataSnapshot) { if (ds2.exists()) iterateDelete(ds2, chat) }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        val count = msgSelected.size
        Toast.makeText(this, if (count > 1) "$count mensajes eliminados" else "mensaje eliminado", Toast.LENGTH_SHORT).show()
        removeChatWith(count)
        msgSelected.clear()
        countDeleteMsg.text = msgSelected.size.toString()
    }

    private fun iterateDelete(ds: DataSnapshot, chat: Chats) {
        for (snap in ds.children) {
            val type = snap.child("type").getValue(Int::class.java)
            val sender = snap.child("envia").getValue(String::class.java)

            if (sender == myUid) {
                when (type) {
                    Constants.MSG -> snap.child("type").ref.setValue(Constants.MSG_SENDER_DLT)
                    Constants.MSG_RECEIVER_DLT -> snap.ref.removeValue()
                    Constants.PHOTO -> snap.child("type").ref.setValue(Constants.PHOTO_SENDER_DLT)
                    Constants.PHOTO_RECEIVER_DLT -> {
                        val start = chat.message.indexOf(userId) + userId.length + 3
                        val end = chat.message.indexOf(".jpg") + 4
                        refYourReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                    AUDIO -> snap.child("type").ref.setValue(Constants.AUDIO_SENDER_DLT)
                    AUDIO_RECEIVER_DLT -> {
                        val start = chat.message.indexOf(userId) + userId.length + 3
                        val end = chat.message.indexOf(".m4a") + 4 // FIX extensión
                        refYourReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                }
            } else {
                when (type) {
                    Constants.MSG -> snap.child("type").ref.setValue(Constants.MSG_RECEIVER_DLT)
                    Constants.MSG_SENDER_DLT -> snap.ref.removeValue()
                    Constants.PHOTO -> snap.child("type").ref.setValue(Constants.PHOTO_RECEIVER_DLT)
                    Constants.PHOTO_SENDER_DLT -> {
                        val start = chat.message.indexOf(myUid) + myUid.length + 3
                        val end = chat.message.indexOf(".jpg") + 4
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                    Constants.AUDIO -> snap.child("type").ref.setValue(Constants.AUDIO_RECEIVER_DLT)
                    Constants.AUDIO_SENDER_DLT -> {
                        val start = chat.message.indexOf(myUid) + myUid.length + 3
                        val end = chat.message.indexOf(".m4a") + 4 // FIX extensión
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                }
            }
        }
    }

    fun removeChatWith(countList: Int) {

        startedByMe!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(ds: DataSnapshot) {
                if (ds.exists()) deleteChatWith(ds) else {
                    startedByHim!!.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ds2: DataSnapshot) { if (ds2.exists()) deleteChatWith(ds2) }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            private fun deleteChatWith(data: DataSnapshot) {
                val messages = data.childrenCount
                val senderDelete = ArrayList<String>()
                val receiverDelete = ArrayList<String>()

                for (snap in data.children) {
                    val chat = snap.getValue(Chats::class.java) ?: continue
                    val key = snap.key ?: continue
                    if (chat.sender == myUid) {
                        when (chat.type) {
                            Constants.MSG_SENDER_DLT, Constants.PHOTO_SENDER_DLT, Constants.AUDIO_SENDER_DLT -> senderDelete.add(key)
                        }
                    } else {
                        when (chat.type) {
                            Constants.MSG_RECEIVER_DLT, Constants.PHOTO_RECEIVER_DLT, Constants.AUDIO_RECEIVER_DLT -> receiverDelete.add(key)
                        }
                    }
                }
                val count = messages - (senderDelete.size + receiverDelete.size + countList)
                if (count == 0L) {
                    firebaseRefsContainer.refDatos.child(myUid).child(refChatWith!!).child(userId).removeValue()
                    onBackPressedDispatcher.onBackPressed()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.addChat(chat)
    }

    private fun actualizeChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.actualizeMsg(chat)
    }

    private fun deleteChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.deleteMsg(chat)
    }

    private fun cancelSendPhoto(){
        binding.linearPhotoView.isVisible = false
        binding.msg.isVisible = true
        binding.btnCamera.isVisible = true
        binding.btnMic.isVisible = true
        binding.btnSendMsg.isVisible = false
        binding.frameSendMsg.isVisible = false

        msgType = Constants.MSG

    }

    // ---------------- Companion ----------------
    companion object {
        var msgSelected: ArrayList<Chats> = ArrayList()
        lateinit var linearTimer: LinearLayout
        lateinit var linearDeleteMsg: LinearLayout
        lateinit var countDeleteMsg: TextView
        lateinit var myPhoto: String
        lateinit var yourPhoto: String

        fun selectedDeleteMsg(chats: Chats?) {
            if (chats == null) return
            msgSelected.add(chats)
            if (msgSelected.size > 1) {
                linearDeleteMsg.visibility = View.VISIBLE
                countDeleteMsg.text = msgSelected.size.toString()
            }
        }

        fun notSelectedDeleteMsg(chats: Chats?) {
            if (chats == null) return
            val idx = msgSelected.indexOf(chats)
            if (idx != -1) {
                msgSelected.removeAt(idx)
                if (msgSelected.isEmpty()) {
                    linearDeleteMsg.visibility = View.GONE
                }
                countDeleteMsg.text = msgSelected.size.toString()
            }
        }
    }
}