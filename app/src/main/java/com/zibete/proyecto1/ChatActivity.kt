package com.zibete.proyecto1

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
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.adapters.AdapterChat
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.ChatViewModel
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.utils.ChatUtils
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.user
import com.zibete.proyecto1.utils.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    @Inject
    lateinit var repo: UserPreferencesRepository

    // --------- ViewModel ---------
    private val viewModel: ChatViewModel by viewModels()

    // --------- UI refs ----------
    private lateinit var imgUser: CircleImageView
    private lateinit var nameUser: TextView
    private lateinit var tvCancelAudio: TextView
    private lateinit var tvState: TextView
    private lateinit var msg: EditText
    private lateinit var btnCamera: ImageView
    private lateinit var btnSendMsg: ImageView
    private lateinit var btnMic: LottieAnimationView
    private lateinit var linearPhotoView: LinearLayout
    private lateinit var linearPhoto: LinearLayout
    private lateinit var loadingPhoto: ProgressBar
    private lateinit var loadingButton: ProgressBar
    private lateinit var photo: ImageView
    private lateinit var rvMsg: RecyclerView
    private lateinit var buttonScrollBack: FloatingActionButton
    private lateinit var buttonUnlockUser: Button
    private lateinit var timer: Chronometer
    private lateinit var trashAnimated: LottieAnimationView
    private lateinit var trashAnimated2: LottieAnimationView
    private lateinit var linearLottie: LinearLayout
    private lateinit var layoutChat: LinearLayout
    private lateinit var layoutBloq: LinearLayout

    private lateinit var frameSendMsg: FrameLayout
    private lateinit var iconConnected: ImageView
    private lateinit var iconDisconnected: ImageView
    private lateinit var cancelAction: ImageView
    private lateinit var tvDate: TextView

    // --------- Estado / datos ---------
    private var idUser: String? = null
    private var idUserUnknown: String? = null
    private var unknownName: String? = null
    private var idUserFinal: String? = null
    private var nameUserFinal: String? = null
    private var refChatWith: String? = null
    private var refChat: String? = null
    private var estadoUser: String? = null
    private var estadoYo: String? = null
    private var suActual: String? = null
    private var token: String? = null
    private var noVisto: Int = 0
    private var miNoVisto: Int = 0
    private var myName: String? = null

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


    private lateinit var refActual: DatabaseReference
    private var startedByMe: DatabaseReference? = null
    private var startedByHim: DatabaseReference? = null
    private var refYourReceiverData: StorageReference? = null
    private var refMyReceiverData: StorageReference? = null
    private var listenerChatUnknown: ValueEventListener? = null

    private lateinit var iconVibrator: android.os.Vibrator

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
        setContentView(R.layout.activity_chat)

        // --- toolbar y notifs ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        // --- bindViews ---
        imgUser = findViewById(R.id.image_user)
        nameUser = findViewById(R.id.nameUser)
        tvState = findViewById(R.id.tv_estado)
        msg = findViewById(R.id.msg)
        tvDate = findViewById(R.id.tv_date)
        btnCamera = findViewById(R.id.btnCamera)
        btnSendMsg = findViewById(R.id.btnSendMsg)
        btnMic = findViewById(R.id.btnMic)
        linearPhotoView = findViewById(R.id.linear_photo_view)
        cancelAction = findViewById(R.id.cancel_action)
        linearPhoto = findViewById(R.id.linear_photo)
        loadingPhoto = findViewById(R.id.loadingPhoto)
        loadingButton = findViewById(R.id.loadingButton)
        photo = findViewById(R.id.photo)
        linearTimer = findViewById(R.id.linear_timer)
        timer = findViewById(R.id.timer)
        linearDeleteMsg = findViewById(R.id.linearDeleteMsg)
        countDeleteMsg = findViewById(R.id.countDeleteMsg)
        trashAnimated = findViewById(R.id.trashAnimated)
        trashAnimated2 = findViewById(R.id.trashAnimated2)
        linearLottie = findViewById(R.id.linearLottie)
        linearNameUser = findViewById(R.id.linearNameUser)
        layoutChat = findViewById(R.id.layoutChat)
        layoutBloq = findViewById(R.id.layoutBloq)
        frameSendMsg = findViewById(R.id.frameSendMsg)
        iconConnected = findViewById(R.id.icon_conectado)
        iconDisconnected = findViewById(R.id.icon_desconectado)
        buttonScrollBack = findViewById(R.id.buttonScrollBack)
        buttonUnlockUser = findViewById(R.id.btnDesbloq)
        tvCancelAudio = findViewById(R.id.tv_cancel_audio)

        ObjectAnimator.ofFloat(tvCancelAudio, "alpha", 1f, 0f, 1f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE; start()
        }

        currentAudioUri = null
        iconVibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator

        // --- Recycler ---
        rvMsg = findViewById(R.id.rvMsg)
        mLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = false
            stackFromEnd = true
        }
        rvMsg.layoutManager = mLayoutManager
        adapter = AdapterChat(chatsArrayList as ArrayList<Chats>, Constants.MAXCHATSIZE, applicationContext)
        rvMsg.adapter = adapter

        // --- init launchers ---
        initActivityResultLaunchers()

        // --- estado inicial botones ---
        msg.isVisible = true
        btnMic.isVisible = true
        btnCamera.isVisible = true
        loadingButton.isVisible = false
        frameSendMsg.isVisible = false
        btnSendMsg.isVisible = false
        linearPhotoView.isVisible = false
        loadingPhoto.isVisible = false

        // --- extras ---
        UserRepository.setUserOnline(applicationContext, user.uid)

        idUser = intent.extras?.getString("id_user")
        unknownName = intent.extras?.getString("unknownName")
        idUserUnknown = intent.extras?.getString("idUserUnknown")

        // ===== Config chat =====
        setupChatHeaderAndRefs()

//        // ===== CropHelper =====
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
        btnCamera.setOnClickListener {
            sendPhoto()
        }
        btnSendMsg.setOnClickListener {
            sendMessage(null)
        }
        linearDeleteMsg.setOnClickListener {
            trashAnimated.playAnimation()
            DeleteMsgs()
        }
        cancelAction.setOnClickListener {
            cancelSendPhoto()
        }
        photo.setOnClickListener {
            photoList.add(stringMsg!!)
            val intent = Intent(this@ChatActivity, SlidePhotoActivity::class.java).apply {
                putExtra("photoList", photoList)
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            startActivity(intent)
        }
        buttonUnlockUser.setOnClickListener {
            val view = findViewById<View>(android.R.id.content)
            UserRepository.setUnBlockUser(this@ChatActivity, idUserFinal!!, nameUserFinal!!, view, refChatWith!!)
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



        msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (msg.text.isEmpty()) {
                    btnCamera.isVisible = true
                    btnMic.isVisible = true
                    btnSendMsg.isVisible = false
                    frameSendMsg.isVisible = false
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (msg.text.isNotEmpty()) {
                    btnCamera.isVisible = false
                    btnMic.isVisible = false
                    btnSendMsg.isVisible = true
                    frameSendMsg.isVisible = true
                    FirebaseRefs.refDatos.child(user.uid).child("Estado").child("estado")
                        .setValue(getString(R.string.escribiendo))
                    FirebaseRefs.refCuentas.child(user.uid).child("estado").setValue(true)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                if (msg.text.isEmpty()) {
                    btnCamera.isVisible = true
                    btnMic.isVisible = true
                    btnSendMsg.isVisible = false
                    frameSendMsg.isVisible = false
                    UserRepository.setUserOnline(applicationContext, user.uid)
                }
            }
        })

        // ===== Mic gesture =====
        setupMicGesture()

        // ===== Scroll / fecha =====
        rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                    tvDate.text = date
                } else {
                    findViewById<View>(R.id.linearBack).visibility = View.GONE
                    findViewById<View>(R.id.linearDate).visibility = View.GONE
                }
            }
        })
        buttonScrollBack.setOnClickListener { setScrollbar() }

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
        user.let {
            UserRepository.setUserOffline(applicationContext, it.uid)
            refActual.setValue("") // Limpia referencia de chat activo
        }
    }

    override fun onResume() {
        super.onResume()
        user.let {
            UserRepository.setUserOnline(applicationContext, it.uid)
            // Alineado con la comparación suActual != user.uid + refChatWith
            refActual.setValue(it.uid + (refChatWith ?: ""))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdapterChat.mediaPlayer?.let { player ->
            try { player.stop() } catch (_: Exception) {}
            AdapterChat.mediaPlayer = null
        }

        if (refChatWith == Constants.CHATWITHUNKNOWN && listenerChatUnknown != null && idUserFinal != null) {
            FirebaseRefs.refGroupUsers.child(repo.groupName)
                .child(idUserFinal!!)
                .removeEventListener(listenerChatUnknown!!)
        }

        refActual.setValue("")
    }

    // ---------------- Menú ----------------
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val actionSilent = menu.findItem(R.id.action_silent)
        val actionNotif = menu.findItem(R.id.action_notif)
        val actionBloq = menu.findItem(R.id.action_bloq)
        val actionDesbloq = menu.findItem(R.id.action_desbloq)
        val actionDelete = menu.findItem(R.id.action_delete)

        actionDelete.isVisible = true

        FirebaseRefs.refDatos.child(user!!.uid).child(refChatWith!!).child(idUserFinal!!).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    val state = ds.getValue(String::class.java)
                    when (state) {
                        "silent" -> {
                            actionSilent.isVisible = false
                            actionNotif.isVisible = true
                            actionDesbloq.isVisible = false
                            actionBloq.isVisible = true
                        }
                        refChatWith -> {
                            actionSilent.isVisible = true
                            actionNotif.isVisible = false
                            actionDesbloq.isVisible = false
                            actionBloq.isVisible = true
                        }
                        "bloq" -> {
                            actionSilent.isVisible = false
                            actionNotif.isVisible = false
                            actionDesbloq.isVisible = true
                            actionBloq.isVisible = false
                        }
                        "delete" -> {
                            actionSilent.isVisible = true
                            actionNotif.isVisible = false
                            actionDesbloq.isVisible = false
                            actionBloq.isVisible = true
                        }
                        else -> {
                            actionSilent.isVisible = true
                            actionNotif.isVisible = false
                            actionDesbloq.isVisible = false
                            actionBloq.isVisible = true
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val view = findViewById<View>(android.R.id.content)
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_silent -> {
                UserRepository.silent(nameUserFinal, idUserFinal, refChatWith)
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_notif -> {
                UserRepository.silent(nameUserFinal, idUserFinal, refChatWith)
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_bloq -> {
                UserRepository.setBlockUser(this, nameUserFinal, idUserFinal, view, refChatWith)
            }
            R.id.action_desbloq -> {
                UserRepository.setUnBlockUser(this, idUserFinal, nameUserFinal, view, refChatWith)
            }
            R.id.action_delete -> {
                ChatUtils.deleteChat(this, idUserFinal!!, nameUserFinal, view, refChatWith!!)
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
            viewModel.handleCroppedImageResult(result.resultCode, result.data, this)
        }

        // 2. Galería moderna
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // Delegamos la lógica de uCrop al VM, que nos dará las URIs
                viewModel.startUCropFlow(uri, this, uCropResultLauncher)
            }
        }

        // 3. Cámara moderna
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUriCamera != null) {
                viewModel.startUCropFlow(imageUriCamera!!, this, uCropResultLauncher)
            } else {
                imageUriCamera = null
            }
        }

        // 4. Permisos múltiples
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = result.values.all { it == true }
            if (allGranted) {
                onPermissionsGranted?.invoke()
            }
            onPermissionsGranted = null
        }
    }

    // ----------------------------- Cámara / Galería / Crop -----------------------------
    private fun sendPhoto() {

        if (estadoUser == "bloq") {
            snackCenter("Mensaje no enviado: Estás bloqueado por el usuario")
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
        if (needsLegacyWrite) perms += Manifest.permission.WRITE_EXTERNAL_STORAGE

        val anyDenied = perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (anyDenied) {
            ensurePermissions(perms.toTypedArray()) { startRecordAudio() }
            return
        }
        if (mediaRecorder != null) return

        // 2) Preparar destino
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        nameAudio = "AUD_${sdf.format(java.util.Date())}.m4a"

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
        msg.visibility = View.GONE
        btnCamera.visibility = View.GONE
        linearTimer.visibility = View.VISIBLE
        timer.base = recordStartElapsed
        timer.start()

        FirebaseRefs.refDatos.child(user.uid).child("Estado").child("estado")
            .setValue(getString(R.string.grabando))
        FirebaseRefs.refCuentas.child(user.uid).child("estado").setValue(true)
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
        UserRepository.setUserOnline(applicationContext, user!!.uid)
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
        trashAnimated2.apply {
            visibility = View.VISIBLE
            playAnimation()
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { visibility = View.GONE }
            })
        }

        normalizeUiCancelRecordAudio()
        UserRepository.setUserOnline(applicationContext, user.uid)

        // Borrado del archivo (si existe)
        runCatching { if (currentAudioUri != null) contentResolver.delete(currentAudioUri!!, null, null) }
        currentAudioUri = null
        nameAudio = null
    }

    private fun normalizeUiCancelRecordAudio() {
        vibrate(80)
        btnMic.cancelAnimation()
        btnMic.clearAnimation()
        timer.stop()
        linearTimer.visibility = View.GONE
        msg.visibility = View.VISIBLE
        btnCamera.visibility = View.VISIBLE
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

        btnMic.setOnTouchListener { v, event ->
            if (linearPhotoView.isVisible) return@setOnTouchListener true
            if (firstTouchX == 0f) firstTouchX = btnMic.x

            val xAnim = SpringAnimation(btnMic, SpringAnimation.X).apply {
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
                            btnMic.animate().x(move).setDuration(0).start()
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
        val (textMiChatw, textSuChatw) = when (msgType) {
            Constants.PHOTO -> {
                linearPhotoView.visibility = View.GONE
                msg.visibility = View.VISIBLE
                btnCamera.visibility = View.VISIBLE
                btnMic.visibility = View.VISIBLE
                getString(R.string.photo_send) to getString(R.string.photo_received)
            }
            Constants.AUDIO -> getString(R.string.audio_send) to getString(R.string.audio_received)
            else -> {
                msgType = Constants.MSG
                stringMsg = msg.text.toString()
                stringMsg to stringMsg
            }
        }

        if (stringMsg.isNullOrEmpty()) return

        if (estadoUser == "bloq") {
            snackCenter("Mensaje no enviado: Estás bloqueado por el usuario")
            return
        }

        val visto = if (suActual != user.uid + refChatWith || suActual == null) 1 else 3
        val dateNow = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault()).format(Date())
        val finalDate = if (timerText == null) dateNow else "$dateNow $timerText"

        val chatmsg = Chats(
            stringMsg!!,
            finalDate,
            user.uid,
            msgType,
            visto)

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
            textMiChatw!!, finalDate, null, user.uid,
            idUserFinal!!, nameUserFinal!!, yourPhoto, estadoYo!!, miNoVisto, 0
        )
        FirebaseRefs.refDatos.child(user.uid).child(refChatWith!!).child(idUserFinal!!).setValue(miChat)

        if (suActual != user.uid + refChatWith || suActual == null) {
            val count = noVisto + 1
            val suChat = ChatWith(
                textSuChatw!!, finalDate, null, user.uid,
                user.uid, myName!!, myPhoto, estadoUser!!, count, 1
            )
            FirebaseRefs.refDatos.child(idUserFinal!!).child(refChatWith!!).child(user!!.uid)
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
                textSuChatw!!, finalDate, null, user.uid,
                user.uid, myName!!, myPhoto, estadoUser!!, 0, 3
            )
            FirebaseRefs.refDatos.child(idUserFinal!!).child(refChatWith!!).child(user.uid)
                .setValue(suChat)
        }

        msg.setText("")
        stringMsg = null
        msgType = Constants.MSG
    }

    // ----------------------------- Helpers -----------------------------
    private fun setScrollbar() = rvMsg.scrollToPosition(adapter.itemCount - 1)

    private fun snackCenter(text: String) {
        val snack = Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT)
        snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            .textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }

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

    private fun setupChatHeaderAndRefs() {
        val me = user ?: return

        if (idUser != null) {
            // 1 a 1
            findViewById<View>(R.id.cardview_title)?.visibility = View.GONE
            idUserFinal = idUser
            refChat = Constants.CHAT
            refChatWith = Constants.CHATWITH
            myPhoto = me.photoUrl?.toString() ?: ""
            myName = me.displayName ?: ""

            FirebaseRefs.refCuentas.child(idUserFinal!!).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (ds.exists()) {
                        val thisUser = ds.getValue(Users::class.java)
                        extraUserList.add(thisUser)
                        yourPhoto = ds.child("foto").getValue(String::class.java).orEmpty()
                        nameUserFinal = ds.child("nombre").getValue(String::class.java)
                        nameUser.text = nameUserFinal
                        Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                    } else {
                        FirebaseRefs.refDatos.child(me.uid).child(Constants.CHATWITH).child(idUserFinal!!).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snap: DataSnapshot) {
                                yourPhoto = snap.child("wUserPhoto").getValue(String::class.java).orEmpty()
                                nameUserFinal = snap.child("wUserName").getValue(String::class.java)
                                nameUser.text = nameUserFinal
                                Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            // Chat UNKNOWN
            findViewById<View>(R.id.cardview_title)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_chat_title)?.text = "Chat privado en ${repo.groupName}"

            idUserFinal = idUserUnknown
            nameUserFinal = unknownName
            refChat = Constants.UNKNOWN
            refChatWith = Constants.CHATWITHUNKNOWN
            myName = repo.userName
            nameUser.text = nameUserFinal

            FirebaseRefs.refGroupUsers.child(repo.groupName).child(idUserUnknown!!).child("type")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) {
                        if (ds.exists()) {
                            val type = ds.getValue(Int::class.java)
                            if (type == 0) {
                                yourPhoto = getString(R.string.URL_PHOTO_DEF)
                                Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                            } else {
                                FirebaseRefs.refCuentas.child(idUserUnknown!!).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(d2: DataSnapshot) {
                                        if (d2.exists()) {
                                            yourPhoto = d2.child("foto").getValue(String::class.java).orEmpty()
                                            Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            myPhoto = if (repo.userType == 0) {
                getString(R.string.URL_PHOTO_DEF)
            } else {
                me.photoUrl?.toString() ?: ""
            }

            listenerChatUnknown = object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    val userName = ds.child("user_name").getValue(String::class.java)
                    if (!ds.exists() || userName != nameUserFinal) {
                        AlertDialog.Builder(this@ChatActivity, R.style.AlertDialogApp)
                            .setMessage("Lo sentimos, $nameUserFinal ya no está disponible")
                            .setCancelable(false)
                            .setPositiveButton(DIALOG_ACCEPT) { _, _ -> finish() }
                            .show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            FirebaseRefs.refGroupUsers.child(repo.groupName).child(idUserFinal!!)
                .addValueEventListener(listenerChatUnknown!!)
        }

        // Token del receptor
        if (idUserFinal != null) {
            FirebaseRefs.refCuentas.child(idUserFinal!!).child("token")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) { token = ds.getValue(String::class.java) }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // Storage refs
        refYourReceiverData = Constants.storageReference.child("${refChatWith}/${idUserFinal}/")
        refMyReceiverData = Constants.storageReference.child("${refChatWith}/${me.uid}/")

        refActual = FirebaseRefs.refDatos.child(user.uid).child("ChatList").child("Actual")

        // Ramas de mensajes
        startedByMe  = FirebaseRefs.refChats.child(refChat!!).child("${me.uid} <---> $idUserFinal").child("Mensajes")
        startedByHim = FirebaseRefs.refChats.child(refChat!!).child("$idUserFinal <---> ${me.uid}").child("Mensajes")
    }

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
        val me = user ?: return

        FirebaseRefs.refDatos.child(idUserFinal!!).child("ChatList").child("Actual")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) { suActual = ds.getValue(String::class.java) ?: "" }
                override fun onCancelled(error: DatabaseError) {}
            })

        UserRepository.stateUser(
            applicationContext,
            idUserFinal!!,
            iconConnected,
            iconDisconnected,
            tvState,
            refChatWith
        )

        FirebaseRefs.refDatos.child(me.uid).child(refChatWith!!).child(idUserFinal!!).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val state = snapshot.getValue(String::class.java)
                    if (state == "bloq") {
                        layoutBloq.visibility = View.VISIBLE
                        layoutChat.visibility = View.GONE
                        linearLottie.visibility = View.GONE
                    } else {
                        layoutBloq.visibility = View.GONE
                        layoutChat.visibility = View.VISIBLE
                        linearLottie.visibility = View.VISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        FirebaseRefs.refDatos.child(idUserFinal!!).child(refChatWith!!).child(me.uid).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    estadoUser = ds.getValue(String::class.java) ?: refChatWith
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        FirebaseRefs.refDatos.child(me.uid).child(refChatWith!!).child(idUserFinal!!).child("estado")
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

        btnMic.setImageResource(android.R.color.transparent)
        btnMic.setBackgroundResource(R.color.transparent)
        btnMic.setAnimation(R.raw.lf30_editor_24iqgref)
        btnMic.layoutParams = LinearLayout.LayoutParams(dp200, dp200).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        btnMic.playAnimation()

        linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(0, 0, -dp65, -dp65)
        }
    }

    private fun setMicButton() {
        btnMic.setBackgroundResource(R.drawable.marco_color_b_round)
        btnMic.setImageResource(R.drawable.ic_baseline_mic_24)

        val scale = resources.displayMetrics.density
        val dp10 = (10 * scale + 0.5f).toInt()
        val dp13 = (13 * scale + 0.5f).toInt()
        val dp50 = (50 * scale + 0.5f).toInt()

        linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        btnMic.layoutParams = LinearLayout.LayoutParams(dp50, dp50).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(dp10, dp10, dp10, dp10)
        }
        btnMic.setPadding(dp13, dp13, dp13, dp13)
    }

    private fun visto() {
        val me = user ?: return
        FirebaseRefs.refDatos.child(me.uid).child(refChatWith!!).child(idUserFinal!!)
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
        RemoveChatWith(count)
        msgSelected.clear()
        countDeleteMsg.text = msgSelected.size.toString()
    }

    private fun iterateDelete(ds: DataSnapshot, chat: Chats) {
        for (snap in ds.children) {
            val type = snap.child("type").getValue(Int::class.java)
            val sender = snap.child("envia").getValue(String::class.java)

            if (sender == user.uid) {
                when (type) {
                    Constants.MSG -> snap.child("type").ref.setValue(Constants.MSG_SENDER_DLT)
                    Constants.MSG_RECEIVER_DLT -> snap.ref.removeValue()
                    Constants.PHOTO -> snap.child("type").ref.setValue(Constants.PHOTO_SENDER_DLT)
                    Constants.PHOTO_RECEIVER_DLT -> {
                        val start = chat.message.indexOf(idUserFinal!!) + idUserFinal!!.length + 3
                        val end = chat.message.indexOf(".jpg") + 4
                        refYourReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                    Constants.AUDIO -> snap.child("type").ref.setValue(Constants.AUDIO_SENDER_DLT)
                    Constants.AUDIO_RECEIVER_DLT -> {
                        val start = chat.message.indexOf(idUserFinal!!) + idUserFinal!!.length + 3
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
                        val start = chat.message.indexOf(user.uid) + user.uid.length + 3
                        val end = chat.message.indexOf(".jpg") + 4
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                    Constants.AUDIO -> snap.child("type").ref.setValue(Constants.AUDIO_RECEIVER_DLT)
                    Constants.AUDIO_SENDER_DLT -> {
                        val start = chat.message.indexOf(user.uid) + user.uid.length + 3
                        val end = chat.message.indexOf(".m4a") + 4 // FIX extensión
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                }
            }
        }
    }

    fun RemoveChatWith(countList: Int) {
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
                    if (chat.sender == user.uid) {
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
                    FirebaseRefs.refDatos.child(user.uid).child(refChatWith!!).child(idUserFinal!!).removeValue()
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
        linearPhotoView.isVisible = false
        msg.isVisible = true
        btnCamera.isVisible = true
        btnMic.isVisible = true
        btnSendMsg.isVisible = false
        frameSendMsg.isVisible = false

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
