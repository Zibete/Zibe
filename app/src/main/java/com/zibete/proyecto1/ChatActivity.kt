package com.zibete.proyecto1

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.canhub.cropper.CropImageContractOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.zibete.proyecto1.Adapters.AdapterChat
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.CropHelper
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserRepository
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment
import com.zibete.proyecto1.utils.FirebaseRefs.user

class ChatActivity : AppCompatActivity() {

    // --------- UI refs (solo las que usamos en este refactor) ----------
    private lateinit var imgUser: CircleImageView
    private lateinit var nameUser: TextView
    private lateinit var tvState: TextView
    private lateinit var msg: EditText
    private lateinit var btnCamera: ImageView
    private lateinit var btnSendMsg: ImageView
    private lateinit var btnMic: LottieAnimationView
    private lateinit var linearPhotoView: LinearLayout
    private lateinit var linearPhoto: LinearLayout
    private lateinit var loadingPhoto: ProgressBar
    private lateinit var photo: ImageView
    private lateinit var rvMsg: RecyclerView
    private lateinit var buttonScrollBack: FloatingActionButton
    private lateinit var linearTimer: LinearLayout
    private lateinit var timer: Chronometer
    private lateinit var linearDeleteMsg: LinearLayout
    private lateinit var countDeleteMsg: TextView
    private lateinit var trashAnimated: LottieAnimationView
    private lateinit var trashAnimated2: LottieAnimationView
    private lateinit var linearLottie: LinearLayout
    private lateinit var layoutChat: LinearLayout
    private lateinit var layoutBloq: LinearLayout
    private lateinit var iconConnected: ImageView
    private lateinit var iconDisconnected: ImageView

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

    // --------- media / storage ---------
    private var imageUriCamera: Uri? = null
    private var currentAudioUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var nameAudio: String? = null
    private var msgType = Constants.MSG
    private var stringMsg: String? = null

    private lateinit var refActual: DatabaseReference
    private var startedByMe: DatabaseReference? = null
    private var startedByHim: DatabaseReference? = null
    private var refYourReceiverData: StorageReference? = null
    private var refMyReceiverData: StorageReference? = null
    private var listenerChatUnknown: ValueEventListener? = null

    private lateinit var iconVibrator: android.os.Vibrator

    // --------- Crop / pickers / permisos ---------
    private lateinit var cropLauncher: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private val hasMicPermission = AtomicBoolean(false)
    private val hasCameraPermission = AtomicBoolean(false)

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

        btnCamera = findViewById(R.id.btnCamera)
        btnSendMsg = findViewById(R.id.btnSendMsg)
        btnMic = findViewById(R.id.btnMic)
        linearPhotoView = findViewById(R.id.linear_photo_view)
        linearPhoto = findViewById(R.id.linear_photo)
        loadingPhoto = findViewById(R.id.loadingPhoto)
        photo = findViewById(R.id.photo)
        linearTimer = findViewById(R.id.linear_timer)
        timer = findViewById(R.id.timer)
        linearDeleteMsg = findViewById(R.id.linearDeleteMsg)
        countDeleteMsg = findViewById(R.id.countDeleteMsg)
        trashAnimated = findViewById(R.id.trashAnimated)
        trashAnimated2 = findViewById(R.id.trashAnimated2)
        linearLottie = findViewById(R.id.linearLottie)
        layoutChat = findViewById(R.id.layoutChat)
        layoutBloq = findViewById(R.id.layoutBloq)
        iconConnected = findViewById(R.id.icon_conectado)
        iconDisconnected = findViewById(R.id.icon_desconectado)
        buttonScrollBack = findViewById(R.id.buttonScrollBack)

        iconVibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator

        // --- Recycler ---
        rvMsg = findViewById(R.id.rvMsg)
        mLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = false
            stackFromEnd = true
        }
        rvMsg.layoutManager = mLayoutManager
        adapter = AdapterChat(chatsArrayList as ArrayList<Chats>, Constants.maxChatSize, applicationContext)
        rvMsg.adapter = adapter

        // --- init launchers ---
        initActivityResultLaunchers()

        // --- estado inicial botones ---
        btnMic.visibility = View.VISIBLE
        btnCamera.visibility = View.VISIBLE
        btnSendMsg.visibility = View.GONE
        linearPhotoView.visibility = View.GONE
        loadingPhoto.visibility = View.GONE

        // --- extras ---
        idUser = intent.extras?.getString("id_user")
        unknownName = intent.extras?.getString("unknownName")
        idUserUnknown = intent.extras?.getString("idUserUnknown")

        // ===== Config chat (igual que tu lógica, sólo ordenado) =====
        setupChatHeaderAndRefs()

        // ===== CropHelper: registramos el launcher apuntando a mi carpeta de envío =====
        cropLauncher = CropHelper.registerLauncher(
            caller = this,
            ctx = this,
            refSendImages = refMyReceiverData!!,
            linearPhotoView = linearPhotoView,
            linearPhoto = linearPhoto,
            photo = photo,
            loadingPhoto = loadingPhoto,
            msg = msg,
            btnCamera = btnCamera,
            btnSendMsg = btnSendMsg,
            onCroppedUri = { /* opcional */ }
        )

        // ===== UI listeners =====
        btnCamera.setOnClickListener { sendPhoto() }
        btnSendMsg.setOnClickListener { sendMessage(null) }

        msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (msg.text.isEmpty()) {
                    btnCamera.visibility = View.VISIBLE
                    btnMic.visibility = View.VISIBLE
                    btnSendMsg.visibility = View.GONE
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (msg.text.isNotEmpty()) {
                    btnCamera.visibility = View.GONE
                    btnMic.visibility = View.GONE
                    btnSendMsg.visibility = View.VISIBLE
                    FirebaseRefs.refDatos.child(user!!.uid).child("Estado").child("estado")
                        .setValue(getString(R.string.escribiendo))
                    FirebaseRefs.refCuentas.child(user.uid).child("estado").setValue(true)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                if (msg.text.isEmpty()) {
                    btnCamera.visibility = View.VISIBLE
                    btnMic.visibility = View.VISIBLE
                    btnSendMsg.visibility = View.GONE
                    UserRepository.setUserOnline(applicationContext, user!!.uid)
                }
            }
        })

        // ===== Mic (mantengo tu gesto; sólo quité lo deprecado) =====
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
                    adapter.getDate(first)
                } else {
                    findViewById<View>(R.id.linearBack).visibility = View.GONE
                    findViewById<View>(R.id.linearDate).visibility = View.GONE
                }
            }
        })
        buttonScrollBack.setOnClickListener { setScrollbar() }

        // ===== Observadores de mensajes (igual que tenías) =====
        hookChatListeners()

        // ===== Estados / block =====
        hookBlockAndPresence()

        hookChatListeners()

        hookBlockAndPresence()

        visto()

    }// ===== ON CREATE


    // ---------------- Ciclo de vida ----------------
    override fun onPause() {
        super.onPause()
        // Estado offline cuando salís del chat
        user?.let {
            UserRepository.setUserOffline(applicationContext, it.uid)
            refActual.setValue("") // Limpia referencia de chat activo
        }
    }

    override fun onResume() {
        super.onResume()
        // Estado online al volver
        user?.let {
            UserRepository.setUserOnline(applicationContext, it.uid)
            refActual.setValue("${idUserFinal}${refChatWith}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Detener audio si hay reproducción activa
        AdapterChat.mediaPlayer?.let { player ->
            player.stop()
            AdapterChat.mediaPlayer = null
        }

        // Quitar listener si era chat “unknown”
        if (refChatWith == Constants.chatWithUnknown && listenerChatUnknown != null) {
            FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName)
                .child(idUserFinal!!)
                .removeEventListener(listenerChatUnknown!!)
        }

        // Limpia “chat actual” en Firebase
        refActual.setValue("")
    }


    // ---------------- Menú ----------------
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        return true
    }

    /** Menú superior: estados de acción según 'estado' */
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

    /** Acciones del menú */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val view = findViewById<View>(android.R.id.content)
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_silent -> {
                UserRepository.Silent(nameUserFinal, idUserFinal, refChatWith)
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_notif -> {
                UserRepository.Silent(nameUserFinal, idUserFinal, refChatWith)
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_bloq -> {
                UserRepository.setBlockUser(this, nameUserFinal, idUserFinal, view, refChatWith)
            }
            R.id.action_desbloq -> {
                UserRepository.setUnBlockUser(this, idUserFinal, nameUserFinal, view, refChatWith)
            }
            R.id.action_delete -> {
                Constants().DeleteChat(this, idUserFinal, nameUserFinal, view, refChatWith)
            }
        }
        return super.onOptionsItemSelected(item)
    }




    // ----------------------------- Activity Result Launchers -----------------------------
    private fun initActivityResultLaunchers() {
        // Galería moderna
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // lanzamos crop directamente
                CropHelper.launchCrop(cropLauncher, uri)
            }
        }

        // Cámara moderna
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUriCamera != null) {
                CropHelper.launchCrop(cropLauncher, imageUriCamera!!)
            } else {
                imageUriCamera = null
            }
        }

        // Permisos múltiples
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            hasCameraPermission.set(result[Manifest.permission.CAMERA] == true)
            hasMicPermission.set(result[Manifest.permission.RECORD_AUDIO] == true)
        }
    }

    // ----------------------------- Cámara / Galería / Crop -----------------------------
    private fun sendPhoto() {
        if (estadoUser == "bloq") {
            snackCenter("Mensaje no enviado: Estás bloqueado por el usuario")
            return
        }

        val viewFilter = layoutInflater.inflate(R.layout.profile_photo_layout, null)
        val imgCancel = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
        val cameraOpt = viewFilter.findViewById<ImageView>(R.id.cameraSelected)
        val storageOpt = viewFilter.findViewById<ImageView>(R.id.storageSelected)
        val title = viewFilter.findViewById<TextView>(R.id.tv_title)
        viewFilter.findViewById<LinearLayout>(R.id.linear_edit_delete).visibility = View.GONE
        title.text = getString(R.string.enviar_desde)

        val dialog = AlertDialog.Builder(this, R.style.AlertDialogApp)
            .setView(viewFilter)
            .create()
        dialog.show()

        cameraOpt.setOnClickListener {
            ensurePermissions(arrayOf(Manifest.permission.CAMERA)) {
                startCameraModern()
            }
            dialog.dismiss()
        }

        storageOpt.setOnClickListener {
            // No hace falta permiso para leer en Android 13+ con el picker
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )

            dialog.dismiss()
        }

        imgCancel.setOnClickListener { dialog.dismiss() }
    }

    private fun startCameraModern() {
        // Insertamos un placeholder en MediaStore y disparamos TakePicture()
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
        takePictureLauncher.launch(imageUriCamera)
    }

    // ----------------------------- Audio (MediaStore, sin rutas directas) -----------------------------
    private fun startRecordAudio() {
        if (!hasMicPermission.get()) {
            ensurePermissions(arrayOf(Manifest.permission.RECORD_AUDIO)) { startRecordAudio() }
            return
        }
        if (mediaRecorder != null) return

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        nameAudio = "AUD_${sdf.format(Date())}.m4a"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, nameAudio)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Zibe")
            }
        }
        currentAudioUri = contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        )

        val pfd = currentAudioUri?.let { contentResolver.openFileDescriptor(it, "w") }
        if (pfd == null) {
            snackCenter("No se pudo iniciar la grabación")
            return
        }

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(pfd.fileDescriptor)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                release()
                mediaRecorder = null
                snackCenter("Error al grabar audio")
                return
            }
        }

        // UI grabando
        setMicAnimated()
        msg.visibility = View.GONE
        btnCamera.visibility = View.GONE
        linearTimer.visibility = View.VISIBLE
        timer.base = SystemClock.elapsedRealtime()
        timer.start()

        FirebaseRefs.refDatos.child(user!!.uid).child("Estado").child("estado").setValue(getString(R.string.grabando))
        FirebaseRefs.refCuentas.child(user!!.uid).child("estado").setValue(true)
    }

    private fun stopRecordAudio() {
        val elapsed = timer.text.toString()
        if (elapsed == "00:00") {
            cancelRecordAudio()
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { /* noop */ }
        mediaRecorder = null

        // Subir el audio a Firebase (como hacías)
        val name = nameAudio ?: "AUD_${System.currentTimeMillis()}.m4a"
        val localUri = currentAudioUri
        if (localUri != null) {
            val stream = contentResolver.openInputStream(localUri)
            if (stream != null) {
                refYourReceiverData!!.child(name).putStream(stream)
                    .addOnSuccessListener { task: UploadTask.TaskSnapshot ->
                        task.storage.downloadUrl.addOnSuccessListener { uri ->
                            stringMsg = uri.toString()
                            msgType = Constants.AUDIO
                            sendMessage(elapsed)
                        }
                    }
            } else {
                snackCenter("No se pudo leer el audio grabado")
            }
        }

        vibrate(80)
        timer.stop()
        linearTimer.visibility = View.GONE
        btnMic.cancelAnimation()
        btnMic.clearAnimation()
        setMicButton()

        msg.visibility = View.VISIBLE
        btnCamera.visibility = View.VISIBLE
        UserRepository.setUserOnline(applicationContext, user!!.uid)
    }

    private fun cancelRecordAudio() {
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        vibrate(80)
        btnMic.cancelAnimation()
        btnMic.clearAnimation()
        trashAnimated2.visibility = View.VISIBLE
        trashAnimated2.playAnimation()
        timer.stop()
        linearTimer.visibility = View.GONE
        msg.visibility = View.VISIBLE
        btnCamera.visibility = View.VISIBLE
        UserRepository.setUserOnline(applicationContext, user!!.uid)
        currentAudioUri = null
    }

    // ----------------------------- Mic Gesture (idéntico a tu UX, limpiado) -----------------------------
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
                    stopRecordAudio()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (linearTimer.isVisible) {
                        if (event.rawX > halfWidth) {
                            val move = event.rawX + dX
                            btnMic.animate().x(move).setDuration(0).start()
                        } else {
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

    // ----------------------------- Envío de mensajes (tu misma lógica) -----------------------------
    private fun sendMessage(timerText: String?) {
        val (textMiChatw, textSuChatw) = when (msgType) {
            Constants.PHOTO -> {
                // reset UI foto
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

        val visto = if (suActual != user!!.uid + refChatWith || suActual == null) 1 else 3
        val dateNow = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault()).format(Date())
        val finalDate = if (timerText == null) dateNow else "$dateNow $timerText"

        val chatmsg = Chats(stringMsg!!, finalDate, user!!.uid, msgType, visto)

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
            textMiChatw!!, finalDate, null, user!!.uid,
            idUserFinal!!, nameUserFinal!!, yourPhoto!!, estadoYo!!, miNoVisto, 0
        )
        FirebaseRefs.refDatos.child(user!!.uid).child(refChatWith!!).child(idUserFinal!!).setValue(miChat)

        // Card en receptor + notificación (igual que tenías; limpieza menor)
        if (suActual != user!!.uid + refChatWith || suActual == null) {
            val count = noVisto + 1
            val suChat = ChatWith(
                textSuChatw!!, finalDate, null, user!!.uid,
                user!!.uid, myName!!, myPhoto!!, estadoUser!!, count, 1
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
                        "authorization" to "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_..." // tu key
                    )
                }
                Volley.newRequestQueue(applicationContext).add(req)
            }
        } else {
            val suChat = ChatWith(
                textSuChatw!!, finalDate, null, user!!.uid,
                user!!.uid, myName!!, myPhoto!!, estadoUser!!, 0, 3
            )
            FirebaseRefs.refDatos.child(idUserFinal!!).child(refChatWith!!).child(user!!.uid)
                .setValue(suChat)
        }

        msg.setText("")
        stringMsg = null
        msgType = Constants.MSG
    }

    // ----------------------------- Helpers varios -----------------------------
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
        if (need) requestPermissionsLauncher.launch(perms) else onGranted()
    }

// ========================= Bloques que mantengo (sin cambios de lógica) =========================

    /** Construye id_user_final, refChatWith, myName/fotos y referencias Firebase */
    private fun setupChatHeaderAndRefs() {
        val me = user ?: return

        if (idUser != null) {
            // Chat 1 a 1
            findViewById<View>(R.id.cardview_title)?.visibility = View.GONE
            idUserFinal = idUser
            refChat = Constants.chat
            refChatWith = Constants.chatWith
            myPhoto = me.photoUrl?.toString()
            myName = me.displayName

            // Nombre y foto del otro usuario
            FirebaseRefs.refCuentas.child(idUserFinal!!).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (ds.exists()) {
                        val thisUser = ds.getValue(Users::class.java)
                        extraUserList.add(thisUser)
                        yourPhoto = ds.child("foto").getValue(String::class.java)
                        nameUserFinal = ds.child("nombre").getValue(String::class.java)
                        nameUser.text = nameUserFinal
                        Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                    } else {
                        FirebaseRefs.refDatos.child(me.uid).child(Constants.chatWith).child(idUserFinal!!).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snap: DataSnapshot) {
                                yourPhoto = snap.child("wUserPhoto").getValue(String::class.java)
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
            // Chat UNKNOWN (grupal / privado temporal)
            findViewById<View>(R.id.cardview_title)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_chat_title)?.text = "Chat privado en ${UsuariosFragment.groupName}"

            idUserFinal = idUserUnknown
            nameUserFinal = unknownName
            refChat = Constants.unknown
            refChatWith = Constants.chatWithUnknown
            myName = UsuariosFragment.userName
            nameUser.text = nameUserFinal

            FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName).child(idUserUnknown!!).child("type")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) {
                        if (ds.exists()) {
                            val type = ds.getValue(Int::class.java) ?: 0
                            if (type == 0) {
                                yourPhoto = getString(R.string.URL_PHOTO_DEF)
                                Glide.with(this@ChatActivity).load(yourPhoto).into(imgUser)
                            } else {
                                FirebaseRefs.refCuentas.child(idUserUnknown!!).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(d2: DataSnapshot) {
                                        if (d2.exists()) {
                                            yourPhoto = d2.child("foto").getValue(String::class.java)
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

            myPhoto = if (UsuariosFragment.userType == 0) {
                getString(R.string.URL_PHOTO_DEF)
            } else {
                me.photoUrl?.toString()
            }

            listenerChatUnknown = object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    val userName = ds.child("user_name").getValue(String::class.java)
                    if (!ds.exists() || userName != nameUserFinal) {
                        AlertDialog.Builder(this@ChatActivity, R.style.AlertDialogApp)
                            .setMessage("Lo sentimos, $nameUserFinal ya no está disponible")
                            .setCancelable(false)
                            .setPositiveButton("Aceptar") { _, _ -> finish() }
                            .show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName).child(idUserFinal!!)
                .addValueEventListener(listenerChatUnknown!!)
        }

        // Token del receptor
        FirebaseRefs.refCuentas.child(idUserFinal!!).child("token").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(ds: DataSnapshot) { token = ds.getValue(String::class.java) }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Storage refs (carpetas por conversación)
        refYourReceiverData = Constants.storageReference.child("$refChatWith/$idUserFinal/")
        refMyReceiverData = Constants.storageReference.child("$refChatWith/${me.uid}/")

        refActual = FirebaseRefs.refDatos.child(user.uid).child("ChatList").child("Actual")

        // Ramas de mensajes (iniciados por mí / por él)
        startedByMe  = FirebaseRefs.refChats.child(refChat!!).child("${me.uid} <---> $idUserFinal").child("Mensajes")
        startedByHim = FirebaseRefs.refChats.child(refChat!!).child("$idUserFinal <---> ${me.uid}").child("Mensajes")
    }

    /** Listeners de mensajes para ambos sentidos */
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

    /** Bloqueos / presencia / estado de la conversación */
    private fun hookBlockAndPresence() {
        val me = user ?: return

        // Su "Actual"
        FirebaseRefs.refDatos.child(idUserFinal!!).child("ChatList").child("Actual")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) { suActual = ds.getValue(String::class.java) ?: "" }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Estado online/typing/iconos
        UserRepository.stateUser(
            applicationContext,
            idUserFinal,
            iconConnected,
            iconDisconnected,
            tvState,
            refChatWith
        )

        // Si lo bloqueaste
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

        // Cómo me tiene él
        FirebaseRefs.refDatos.child(idUserFinal!!).child(refChatWith!!).child(me.uid).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    estadoUser = ds.getValue(String::class.java) ?: refChatWith
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Mi estado respecto a él
        FirebaseRefs.refDatos.child(me.uid).child(refChatWith!!).child(idUserFinal!!).child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    estadoYo = ds.getValue(String::class.java) ?: refChatWith
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /** UI del mic animado (igual que tu UX, sin deprecados) */
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

    /** Botón mic por defecto */
    private fun setMicButton() {
        val scale = resources.displayMetrics.density
        val dp10 = (10 * scale + 0.5f).toInt()
        val dp13 = (13 * scale + 0.5f).toInt()
        val dp50 = (50 * scale + 0.5f).toInt()

        btnMic.setBackgroundResource(R.drawable.marco_color_b_round)
        btnMic.setImageResource(R.drawable.ic_baseline_mic_24)

        linearLottie.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        btnMic.layoutParams = LinearLayout.LayoutParams(dp50, dp50).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            setMargins(dp10, dp10, dp10, dp10)
        }
        btnMic.setPadding(dp13, dp13, dp13, dp13)
    }

    /** Vistos / doble check / reseteo de contadores */
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

    /** Eliminar mensajes seleccionados (misma lógica) */
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

            if (sender == user!!.uid) {
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
                        val end = chat.message.indexOf(".mp3") + 4
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
                        val start = chat.message.indexOf(user!!.uid) + user!!.uid.length + 3
                        val end = chat.message.indexOf(".jpg") + 4
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                    Constants.AUDIO -> snap.child("type").ref.setValue(Constants.AUDIO_RECEIVER_DLT)
                    Constants.AUDIO_SENDER_DLT -> {
                        val start = chat.message.indexOf(user!!.uid) + user!!.uid.length + 3
                        val end = chat.message.indexOf(".mp3") + 4
                        refMyReceiverData!!.child(chat.message.substring(start, end)).delete()
                        snap.ref.removeValue()
                    }
                }
            }
        }
    }

    /** Remueve la card de ChatWith si quedó sin mensajes */
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
                    if (chat.sender == user!!.uid) {
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
                    FirebaseRefs.refDatos.child(user!!.uid).child(refChatWith!!).child(idUserFinal!!).removeValue()
                    onBackPressedDispatcher.onBackPressed()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /** Cuando Firebase agrega un mensaje (onChildAdded) */
    private fun addChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.addChat(chat)
    }

    /** Cuando Firebase actualiza un mensaje (onChildChanged) */
    private fun actualizeChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.actualizeMsg(chat)
    }

    /** Cuando Firebase borra un mensaje (onChildRemoved) */
    private fun deleteChat(ds: DataSnapshot) {
        val chat = ds.getValue(Chats::class.java) ?: return
        adapter.deleteMsg(chat)
    }

    companion object {
        // Campos “globales” que usás desde el Adapter u otros lugares
        var myPhoto: String? = null
        var yourPhoto: String? = null

        // Selección múltiple (lo usa AdapterChat)
        val msgSelected: ArrayList<Chats> = ArrayList()

        // Referencias UI compartidas (si las manipulás desde Adapter)
        var countDeleteMsg: TextView? = null
        var tv_date: TextView? = null
        var linearDeleteMsg: LinearLayout? = null
        var linear_timer: LinearLayout? = null

        // Utilidades para selección (idénticas a tus firmas actuales)
        fun selectedDeleteMsg(chats: Chats?) {
            if (chats == null) return
            msgSelected.add(chats)
            if (msgSelected.size > 1) {
                linearDeleteMsg?.visibility = View.VISIBLE
                countDeleteMsg?.text = msgSelected.size.toString()
            }
        }

        fun notSelectedDeleteMsg(chats: Chats?) {
            if (chats == null) return
            val idx = msgSelected.indexOf(chats)
            if (idx != -1) {
                msgSelected.removeAt(idx)
                if (msgSelected.isEmpty()) {
                    linearDeleteMsg?.visibility = View.GONE
                }
                countDeleteMsg?.text = msgSelected.size.toString()
            }
        }

        fun setDate(date: String?) {
            tv_date?.text = date ?: ""
        }
    }



}


