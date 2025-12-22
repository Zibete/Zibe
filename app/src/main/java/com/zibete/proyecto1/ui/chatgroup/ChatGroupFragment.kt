//package com.zibete.proyecto1.ui.chatgroup
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.app.NotificationManager
//import android.app.ProgressDialog
//import android.content.ContentValues
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.provider.MediaStore
//import android.text.Editable
//import android.text.TextWatcher
//import android.view.ContextThemeWrapper
//import android.view.LayoutInflater
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.android.volley.toolbox.JsonObjectRequest
//import com.android.volley.toolbox.Volley
//import com.google.android.material.card.MaterialCardView
//import com.google.android.material.snackbar.Snackbar
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.ChildEventListener
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.ValueEventListener
//import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.storage.StorageReference
//import com.zibete.proyecto1.ui.profile.ProfileActivity
//import com.zibete.proyecto1.R
//import com.zibete.proyecto1.ui.media.PhotoViewerActivity
//import com.zibete.proyecto1.adapters.AdapterChatGroup
//import com.zibete.proyecto1.data.UserPreferencesRepository
//import com.zibete.proyecto1.data.UserRepository
//import com.zibete.proyecto1.data.UserSessionManager
//import com.zibete.proyecto1.databinding.FragmentGroupBinding
//import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
//import com.zibete.proyecto1.model.ChatGroup
//import com.zibete.proyecto1.ui.chat.ChatActivity
//import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_ID
//import com.zibete.proyecto1.ui.constants.Constants.MAXCHATSIZE
//import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
//import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
//import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
//import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
//import com.zibete.proyecto1.utils.FirebaseRefs
//import dagger.hilt.android.AndroidEntryPoint
//import org.json.JSONException
//import org.json.JSONObject
//import java.text.ParseException
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class ChatGroupFragment : Fragment() {
//
//    @Inject
//    lateinit var firebaseRefsContainer: FirebaseRefsContainer
//    @Inject
//    lateinit var userSessionManager: UserSessionManager
//    @Inject
//    lateinit var userPreferencesRepository: UserPreferencesRepository
//    @Inject
//    lateinit var userRepository: UserRepository
//
//    private lateinit var _binding: FragmentGroupBinding
//    private val binding get() = _binding
//
//    private val chatsArrayList: ArrayList<ChatGroup> = ArrayList()
//    private var adapter: AdapterChatGroup? = null
//    private var mLayoutManager: LinearLayoutManager? = null
//
//    private var progress: ProgressDialog? = null
//    private var msgType: Int = MSG_TEXT
//    private var stringMsg: String? = null
//
//    private var values: ContentValues? = null
//    private var imageUri: Uri? = null
//
//    private val photoList: ArrayList<String?> = ArrayList()
//
//    // Camera / gallery
//    private var imageUriCamera: Uri? = null
//    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
//    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
//    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
//    private var onPermissionsGranted: (() -> Unit)? = null
//
//
//    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
//    private val storageReference: StorageReference = storage.reference
//    private val myUid: String get() = userRepository.myUid
//
//    private val refSendImages: StorageReference by lazy {
//        storageReference.child("ChatMessage/${myUid}/")
//    }
//
//    // Launcher CropActivity
//    private lateinit var uCropResultLauncher: ActivityResultLauncher<Intent>
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
////        initActivityResultLaunchers()
//    }
//
//    @SuppressLint("InflateParams")
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentGroupBinding.inflate(inflater, container, false)
//        val view = binding.root
//
//        // Ocultar badge de grupos al entrar
////        MainActivity.badgeDrawableGroup?.isVisible = false
//
//        // Marcar mensajes leídos del grupo activo
//        FirebaseRefs.refGroupChat.child(userPreferencesRepository.groupName)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (this@ChatGroupFragment.isResumed &&
//                        userPreferencesRepository.groupName.isNotEmpty()
//                    ) {
//                        val count = snapshot.childrenCount.toInt()
//                        FirebaseRefs.refDatos.child(myUid)
//                            .child(NODE_CHATLIST)
//                            .child("msgReadGroup")
//                            .setValue(count)
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//
//
//        progress = ProgressDialog(requireContext(), R.style.AlertDialogApp)
//
//        initRecycler()
//        initSendUi()
//        initOnBoardingObserver()
//        initMessagesListener()
//        initCropLauncher()
//
//        // Cancelar notificaciones al abrir el chat de grupo
//        (requireContext()
//            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
//
//        return view
//    }
//
//    // ================== INIT UI ==================
//
//    private fun initRecycler() = with(binding) {
//        mLayoutManager = LinearLayoutManager(requireContext()).apply {
//            reverseLayout = false
//            stackFromEnd = true
//        }
//
//        rvMsg.layoutManager = mLayoutManager
////        adapter = AdapterChatGroup(chatsArrayList, Constants.MAXCHATSIZE, requireContext())
//
//        adapter = AdapterChatGroup(
//            context = requireContext(),
//            maxSize = MAXCHATSIZE,
//            initialList = chatsArrayList, // Tu array actual
//            onImageClicked = { url -> PhotoViewerActivity.startSingle(requireContext(), url) },
//            onUserSingleTap = { chat, view -> handleUserSingleTap(chat, view) },
//            onUserDoubleTap = { chat, view -> handleUserDoubleTap(chat, view) }
//        )
//
//        rvMsg.adapter = adapter
//
//        rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                val totalItemCount = adapter?.itemCount ?: 0
//                val lastVisible = mLayoutManager?.findLastVisibleItemPosition() ?: 0
//                val endHasBeenReached = lastVisible + 1 >= totalItemCount
//                linearBack.isVisible = totalItemCount > 0 && !endHasBeenReached
//            }
//        })
//
//        buttonScrollBack.setOnClickListener { setScrollbar() }
//
//        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                super.onItemRangeInserted(positionStart, itemCount)
//                setScrollbar()
//            }
//        })
//
//        // Para compatibilidad con Adapter (borrado múltiple)
////        countDeleteMsg = binding.countDeleteMsg
//    }
//
//    private fun initSendUi() = with(binding) {
//        // Estado inicial
//        btnCamera.isVisible = true
//        btnSendMsg.isVisible = false
//        frameSendMsg.isVisible = false
//        loadingButton.isVisible = false
//        linearPhotoView.isVisible = false
//        linearBack.isVisible = false
//
//        // Ver foto a pantalla completa
//        photo.setOnClickListener {
//
//            if (!stringMsg.isNullOrEmpty()) {
//                photoList.clear()
//                photoList.add(stringMsg)
//
//                PhotoViewerActivity.startSingle(
//                    context = requireContext(),
//                    photoUrl = stringMsg ?: return@setOnClickListener
//                )
//            }
//        }
//
//        // Cancelar preview
//        cancelAction.setOnClickListener {
//            cancelPreviewPhoto()
//        }
//
//        layoutChatGroup.setOnClickListener {
//            // reservado (mantener por compatibilidad si se usaba para ocultar teclado etc)
//        }
//
//        // Toggle cámara / enviar como en ChatActivity
//        msg.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                if (msg.text.isEmpty()) {
//                    btnCamera.isVisible = true
//                    btnSendMsg.isVisible = false
//                    frameSendMsg.isVisible = false
//                }
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (msg.text.isNotEmpty()) {
//                    btnCamera.isVisible = false
//                    btnSendMsg.isVisible = true
//                    frameSendMsg.isVisible = true
//                } else {
//                    btnCamera.isVisible = true
//                    btnSendMsg.isVisible = false
//                    frameSendMsg.isVisible = false
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                if (msg.text.isEmpty()) {
//                    btnCamera.isVisible = true
//                    btnSendMsg.isVisible = false
//                    frameSendMsg.isVisible = false
//                }
//            }
//        })
//
//        btnCamera.setOnClickListener { sendPhoto() }
//        btnSendMsg.setOnClickListener { sendMessage() }
//    }
//
//    private fun initOnBoardingObserver() = with(binding) {
//        FirebaseRefs.refGroupChat.child(userPreferencesRepository.groupName)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    progressbar2.isVisible = false
//                    if (dataSnapshot.exists()) {
//                        linearOnBoardingGroupChat.isVisible = false
//                        rvMsg.isVisible = true
//                    } else {
//                        linearOnBoardingGroupChat.isVisible = true
//                        rvMsg.isVisible = false
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }
//
//    private fun initMessagesListener() {
//        listenerGroupChat = object : ChildEventListener {
//            @SuppressLint("SimpleDateFormat")
//            override fun onChildAdded(
//                dataSnapshot: DataSnapshot,
//                previousChildName: String?
//            ) {
//                if (!dataSnapshot.exists() || !userPreferencesRepository.inGroup) return
//
//                val chat = dataSnapshot.getValue(ChatGroup::class.java) ?: return
//                val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")
//
//                val dateChat = try {
//                    fmt.parse(chat.dateTime)
//                } catch (e: ParseException) {
//                    null
//                }
//
//                val dateUser = try {
//                    fmt.parse(userPreferencesRepository.userDate)
//                } catch (e: ParseException) {
//                    null
//                }
//
//                if (dateChat != null && dateUser != null && dateChat.after(dateUser)) {
//                    adapter?.addChat(chat)
//                }
//            }
//
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
//            override fun onChildRemoved(snapshot: DataSnapshot) {}
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
//            override fun onCancelled(error: DatabaseError) {}
//        }
//
//        FirebaseRefs.refGroupChat.child(userPreferencesRepository.groupName)
//            .addChildEventListener(listenerGroupChat as ChildEventListener)
//    }
//
//    private fun initCropLauncher() = with(binding) {
//        // El CropHelper ya se encarga de mostrar / ocultar preview y loaders.
//        // El callback lo dejamos por si en el futuro querés lógica extra.
////        cropImageLauncher = CropHelper.registerLauncherForFragment(
////            this@ChatGroupFragment,
////            refSendImages,
////            linearPhotoView,
////            linearPhoto,
////            photo,
////            loadingPhoto,
////            loadingButton,
////            frameSendMsg,
////            msg,
////            btnCamera,
////            btnSendMsg
////        ) { uri: Uri? ->
////            if (uri != null) {
////                msgType = Constants.PHOTO
////                stringMsg = uri.toString()
////                loadingPhoto.isVisible = false
////                loadingButton.isVisible = false
////                frameSendMsg.isVisible = true
////                btnSendMsg.isVisible = true
////            } else {
////                cancelPreviewPhoto()
////            }
////        }
//    }
//
//    // ================== FOTO ==================
//
//    fun sendPhoto() {
//        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)
//
//        val imgCancelDialog = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
//        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
//        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
//        val tvTitle = viewFilter.findViewById<TextView>(R.id.tv_title)
//        val cardEditDelete = viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete)
//
//        tvTitle.text = getString(R.string.enviar_desde)
//        cardEditDelete.isVisible = false
//
//        val dialog = AlertDialog.Builder(
//            ContextThemeWrapper(requireContext(), R.style.AlertDialogApp)
//        )
//            .setView(viewFilter)
//            .create()
//
//        dialog.show()
//
//        // Cámara
//        cameraSelection.setOnClickListener {
//            ensurePermissions(arrayOf(Manifest.permission.CAMERA)) {
//                startCameraModern()
//            }
//            dialog.dismiss()
//        }
//
//        // Galería
//        gallerySelection.setOnClickListener {
//            pickImageLauncher.launch(
//                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
//            )
//            dialog.dismiss()
//        }
//
//        imgCancelDialog.setOnClickListener { dialog.dismiss() }
//    }
//
//    private fun cancelPreviewPhoto() = with(binding) {
//        linearPhotoView.isVisible = false
//        msg.isVisible = true
//        btnCamera.isVisible = true
//        btnSendMsg.isVisible = false
//        frameSendMsg.isVisible = false
//        loadingPhoto.isVisible = false
//        loadingButton.isVisible = false
//        msgType = MSG_TEXT
//        stringMsg = null
//    }
//
//    // ================== MENSAJES ==================
//
//    @SuppressLint("SimpleDateFormat")
//    fun sendMessage() = with(binding) {
//        val textForNotification: String
//
//        if (msgType == MSG_PHOTO) {
//            // Para el receptor mostramos "Foto recibida"
//            textForNotification = getString(R.string.photo_received)
//            // La URL real ya la puso CropHelper/stringMsg
//        } else {
//            msgType = MSG_TEXT
//            stringMsg = msg.text.toString().trim()
//            textForNotification = stringMsg.orEmpty()
//        }
//
//        if (stringMsg.isNullOrEmpty()) return
//
//        // UI loading en botón
//        btnSendMsg.isVisible = false
//        loadingButton.isVisible = true
//
//        val c = Calendar.getInstance()
//        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")
//
//        val chatMsg = ChatGroup(
//            stringMsg!!,
//            dateFormat.format(c.time),
//            userPreferencesRepository.userNameGroup,
//            myUid,
//            msgType,
//            userPreferencesRepository.userType
//        )
//
//        // Enviar a Firebase
//        FirebaseRefs.refGroupChat.child(userPreferencesRepository.groupName)
//            .push()
//            .setValue(chatMsg)
//            .addOnCompleteListener {
//                // Reset UI siempre
//                msg.setText("")
//                msgType = MSG_TEXT
//                stringMsg = null
//                cancelPreviewPhoto()
//                loadingButton.isVisible = false
//            }
//
//        // Notificaciones a los demás miembros del grupo
//        FirebaseRefs.refGroupUsers.child(userPreferencesRepository.groupName)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    for (snapshot in dataSnapshot.children) {
//                        val userId =
//                            snapshot.child("user_id").getValue(String::class.java) ?: continue
//                        if (userId == myUid) continue
//
//                        FirebaseRefs.refCuentas.child(userId)
//                            .addListenerForSingleValueEvent(object : ValueEventListener {
//                                override fun onDataChange(snapshot: DataSnapshot) {
//                                    val token =
//                                        snapshot.child("token")
//                                            .getValue(String::class.java)
//                                            .orEmpty()
//                                    if (token.isNotEmpty()) {
//                                        sendGroupPushNotification(token, textForNotification)
//                                    }
//                                }
//
//                                override fun onCancelled(error: DatabaseError) {}
//                            })
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }
//
//    private fun sendGroupPushNotification(token: String, msg: String) {
//        try {
//            val data = JSONObject().apply {
//                put("novistos", "")
//                put("user", userPreferencesRepository.userNameGroup)
//                put("msg", msg)
//                put("id_user", myUid)
//                put("type", userPreferencesRepository.groupName)
//            }
//
//            val json = JSONObject().apply {
//                put("to", token)
//                put("priority", "high")
//                put("data", data)
//            }
//
//            val url = "https://fcm.googleapis.com/fcm/send"
//            val request = object : JsonObjectRequest(
//                Method.POST, url, json, null, null
//            ) {
//                override fun getHeaders(): MutableMap<String, String> {
//                    val header = HashMap<String, String>()
//                    header["content-type"] = "application/json"
//                    header["authorization"] =
//                        "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"
//                    return header
//                }
//            }
//
//            Volley.newRequestQueue(requireContext()).add(request)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
//    }
//
//    // ================== SCROLL ==================
//
//    private fun setScrollbar() {
//        val b = _binding ?: return              // Si la vista ya no existe, no hacemos nada
//        val count = adapter?.itemCount ?: return
//        if (count > 0) {
//            b.rvMsg.scrollToPosition(count - 1)
//        }
//    }
//
//    // ================== MENU ==================
//
//    override fun onPrepareOptionsMenu(menu: Menu) {
//        super.onPrepareOptionsMenu(menu)
//
//        val actionSearch = menu.findItem(R.id.action_search)
//        val actionUnlock = menu.findItem(R.id.action_unblock)
//        val actionFavorites = menu.findItem(R.id.action_favorites)
//        val actionExit = menu.findItem(R.id.action_exit_group)
//
//        actionExit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//        actionExit.isVisible = true
//        actionSearch.isVisible = false
//        actionUnlock.isVisible = true
//        actionFavorites.isVisible = true
//    }
//
//    // ================== CAMERA / GALLERY (LEGACY) ==================
//
////    private fun initActivityResultLaunchers() {
////        // Galería (Photo Picker)
////        pickImageLauncher = registerForActivityResult(
////            ActivityResultContracts.PickVisualMedia()
////        ) { uri ->
////            if (uri != null) {
////                // Mismo flujo que cuando viene de cámara
////                CropHelper.launchCrop(
////                    cropImageLauncher,
////                    uri
////                )
////            }
////        }
////
////        // Cámara moderna
////        takePictureLauncher = registerForActivityResult(
////            ActivityResultContracts.TakePicture()
////        ) { success ->
////            if (success && imageUriCamera != null) {
////                CropHelper.launchCrop(
////                    cropImageLauncher,
////                    imageUriCamera!!
////                )
////            } else {
////                imageUriCamera = null
////            }
////        }
////
////        // Permisos múltiples
////        requestPermissionsLauncher = registerForActivityResult(
////            ActivityResultContracts.RequestMultiplePermissions()
////        ) { result ->
////            val allGranted = result.values.all { it }
////            if (allGranted) {
////                onPermissionsGranted?.invoke()
////            }
////            onPermissionsGranted = null
////        }
////    }
//
//    private fun ensurePermissions(perms: Array<String>, onGranted: () -> Unit) {
//        val need = perms.any {
//            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
//        }
//        if (need) {
//            onPermissionsGranted = onGranted
//            requestPermissionsLauncher.launch(perms)
//        } else {
//            onGranted()
//        }
//    }
//
//    private fun startCameraModern() {
//        val ctx = requireContext()
//        val values = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT >= 29) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zibe")
//            }
//        }
//
//        imageUriCamera = ctx.contentResolver.insert(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            values
//        )
//
//        if (imageUriCamera == null) {
//            // si querés, usa un Toast o similar
//            // Toast.makeText(ctx, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        takePictureLauncher.launch(imageUriCamera!!)
//    }
//
//    // ================== GESTOS USUARIO ==================
//
//    // 2. Manejo de Single Tap (Ir a Perfil)
//    private fun handleUserSingleTap(chat: ChatGroup, view: View) {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//
//        if (chat.typeUser == 0) {
//            showSnack(view, getString(R.string.perfil_incognito))
//        } else if (chat.id != currentUserId) {
//            startActivity(
//                Intent(requireContext(), ProfileActivity::class.java)
//                    .putExtra(EXTRA_USER_ID, chat.id)
//                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
//            )
//        }
//    }
//
//    // 3. Manejo de Double Tap (Lógica compleja de usuario disponible)
//    private fun handleUserDoubleTap(chat: ChatGroup, view: View) {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//        if (chat.id == currentUserId) return
//
//        // Usamos el Repo (si ya migraste repo.groupName) o la variable global que tengas
//        val groupName = userPreferencesRepository.groupName // O UsersFragment.groupName si aún no migras todo
//
//        FirebaseRefs.refGroupUsers.child(groupName)
//            .child(chat.id)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    if (!dataSnapshot.exists()) {
//                        handleUserUnavailable(chat, view, wasRemovedFromGroup = true)
//                        return
//                    }
//
//                    val thisname = dataSnapshot.child("user_name").getValue(String::class.java)
//                    val type = dataSnapshot.child("type").getValue(Int::class.java) ?: 0
//
//                    val allowed = if (type == 0) {
//                        // Usuario incógnito, validamos por nombre
//                        thisname == chat.name
//                    } else {
//                        // Usuario registrado
//                        chat.typeUser == 1
//                    }
//
//                    if (allowed) {
//                        val intent = Intent(context, ChatActivity::class.java)
//                            .putExtra("unknownName", chat.name)
//                            .putExtra("idUserUnknown", chat.id)
//                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
//                        startActivity(intent)
//                    } else {
//                        handleUserUnavailable(chat, view, wasRemovedFromGroup = false)
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }
//
//    // 4. Manejo de Usuario No Disponible (Helper)
//    private fun handleUserUnavailable(chat: ChatGroup, view: View, wasRemovedFromGroup: Boolean) {
//        val text = if (chat.typeUser == 0) {
//            getString(R.string.user_not_available_fmt, chat.name)
//        } else {
//            if (wasRemovedFromGroup)
//                getString(R.string.user_not_in_chat_anymore_fmt, chat.name)
//            else
//                getString(R.string.user_not_available_fmt, chat.name)
//        }
//
//        showSnack(view, text)
//
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//        currentUserId?.let { uid ->
//            FirebaseRefs.refDatos.child(uid)
//                .child(NODE_GROUP_CHAT)
//                .child(chat.id)
//                .removeValue()
//        }
//    }
//
//    // 5. Mostrar Snackbar (Helper visual)
//    private fun showSnack(view: View, message: String) {
//        val snack = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
//        val bg = ContextCompat.getColor(requireContext(), R.color.colorC)
//        snack.setBackgroundTint(bg)
//        val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
//        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
//        snack.show()
//    }
//
////    private fun startCamera() {
////        values = ContentValues().apply {
////            put(
////                MediaStore.Images.Media.DESCRIPTION,
////                System.currentTimeMillis().toString()
////            )
////        }
////
////        imageUri = requireContext().contentResolver.insert(
////            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
////            values
////        )
////
////        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
////            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
////        }
////        startActivityForResult(intent, Constants.CAMERA_SELECTED)
////    }
//
////    @Deprecated("startActivityForResult legacy usage, mantenido por compatibilidad")
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////
////        if (resultCode != Activity.RESULT_OK) return
////
////        progress?.apply {
////            setMessage("Espere...")
////            setCanceledOnTouchOutside(false)
////            show()
////        }
////
////        when (requestCode) {
////            Constants.CAMERA_SELECTED -> {
////                imageUri?.let { uri ->
////                    CropHelper.launchCrop(cropImageLauncher, uri)
////                }
////            }
////            Constants.PHOTO_SELECTED -> {
////                val selected = data?.data
////                if (selected != null) {
////                    CropHelper.launchCrop(cropImageLauncher, selected)
////                }
////            }
////        }
////
////        progress?.dismiss()
////    }
////
////    @Deprecated("Deprecated in Java")
////    override fun onRequestPermissionsResult(
////        requestCode: Int,
////        permissions: Array<out String>,
////        grantResults: IntArray
////    ) {
////        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
////
////        if (requestCode == Constants.CAMERA_SELECTED &&
////            grantResults.isNotEmpty() &&
////            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
////        ) {
////            startCamera()
////        }
////
////        if (requestCode == Constants.PHOTO_SELECTED &&
////            grantResults.isNotEmpty() &&
////            grantResults[0] == PackageManager.PERMISSION_GRANTED
////        ) {
////            val gallery = Intent(
////                Intent.ACTION_PICK,
////                MediaStore.Images.Media.INTERNAL_CONTENT_URI
////            )
////            startActivityForResult(gallery, Constants.PHOTO_SELECTED)
////        }
////    }
//
//    // ================== LIFECYCLE ==================
//
//    override fun onDestroyView() {
//        super.onDestroyView()
////        _binding = null
//    }
//
//    companion object {
//        @JvmField
//        var listenerGroupChat: ChildEventListener? = null
//
//        @JvmField
//        var countDeleteMsg: TextView? = null
//    }
//}

package com.zibete.proyecto1.ui.chatgroup

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterChatGroup
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.FragmentGroupBinding
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.ui.constants.Constants.MAX_CHAT_SIZE
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ChatGroupFragment : Fragment() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var groupRepository: GroupRepository


    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private val myUid: String get() = userRepository.myUid

    private val chats = ArrayList<ChatGroup>()
    private lateinit var layoutManager: LinearLayoutManager
    private var adapter: AdapterChatGroup? = null

    // Estado composer
    private var msgType: Int = MSG_TEXT
    private var pendingLocalPhotoUri: Uri? = null
    private var pendingRemotePhotoUrl: String? = null

    // Firebase refs/listeners (para remover correctamente)
    private var groupChatRef: DatabaseReference? = null
    private var onboardingListener: ValueEventListener? = null
    private var markReadListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null

    // Storage (si querés, luego lo movemos a repo)
//    private val storageReference: StorageReference by lazy { FirebaseStorage.getInstance().reference }
//    private val refSendImages: StorageReference by lazy {
//        storageReference.child("ChatMessage/$myUid/")
//    }

    // Launchers modernos
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsGranted: (() -> Unit)? = null
    private var cameraOutputUri: Uri? = null

//    private val fmt by lazy {
//        SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS", Locale.getDefault())
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) //fun setHasOptionsMenu(hasMenu: Boolean): Unit' is deprecated. Deprecated in Java
        initActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al entrar: cancelar notificaciones del grupo
        (requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        initRecycler()
        initComposerUi()
        initScrollBackUi()

        // loader inicial (lo apagamos cuando llegue data)
        binding.progressbar2.isVisible = true
        binding.rvMsg.isVisible = false
        binding.linearOnBoardingGroupChat.isVisible = false
    }

    override fun onStart() {
        super.onStart()
        attachFirebaseObservers()
    }

    override fun onStop() {
        super.onStop()
        detachFirebaseObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------------- UI ----------------

    private fun initRecycler() = with(binding) {
        layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = false
            stackFromEnd = true
        }

        adapter = AdapterChatGroup(
            context = requireContext(),
            maxSize = MAX_CHAT_SIZE,
            initialList = chats,
            onImageClicked = { url -> PhotoViewerActivity.startSingle(requireContext(), url) },
            onUserSingleTap = { chat, v -> handleUserSingleTap(chat, v) },
            onUserDoubleTap = { chat, v -> handleUserDoubleTap(chat, v) }
        )

        rvMsg.layoutManager = layoutManager
        rvMsg.adapter = adapter

        rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val total = adapter?.itemCount ?: 0
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val atBottom = lastVisible + 1 >= total
                linearBack.isVisible = total > 0 && !atBottom
            }
        })

        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                setScrollbar()
            }
        })
    }

    private fun initScrollBackUi() = with(binding) {
        linearBack.isVisible = false
        buttonScrollBack.setOnClickListener { setScrollbar() }
    }

    private fun initComposerUi() = with(binding) {
        btnCamera.isVisible = true
        frameSendMsg.isVisible = false
        btnSendMsg.isVisible = false
        loadingButton.isVisible = false
        linearPhotoView.isVisible = false
        loadingPhoto.isVisible = false

        // Watchers -> VM
        binding.msg.addTextChangedListener(Utils.SimpleWatcher {
            renderComposerState()
        })

        btnCamera.setOnClickListener { openPhotoSourceDialog() }
        btnSendMsg.setOnClickListener { onSendClicked() }

        cancelAction.setOnClickListener { clearPhotoPreview() }

        photo.setOnClickListener {
            val url = pendingRemotePhotoUrl
            if (!url.isNullOrEmpty()) {
                PhotoViewerActivity.startSingle(requireContext(), url)
            }
        }

        renderComposerState()
    }

    private fun renderComposerState() = with(binding) {
        val hasText = msg.text?.toString()?.trim().orEmpty().isNotEmpty()
        val hasPhoto = pendingLocalPhotoUri != null || msgType == MSG_PHOTO

        // si hay foto: forzamos “send”
        if (hasPhoto) {
            btnCamera.isVisible = false
            frameSendMsg.isVisible = true
            btnSendMsg.isVisible = !loadingButton.isVisible
            return@with
        }

        // si hay texto: send; sino: cámara
        if (hasText) {
            btnCamera.isVisible = false
            frameSendMsg.isVisible = true
            btnSendMsg.isVisible = !loadingButton.isVisible
        } else {
            btnCamera.isVisible = true
            frameSendMsg.isVisible = false
            btnSendMsg.isVisible = false
        }
    }

    // ---------------- Firebase attach/detach ----------------

    private fun attachFirebaseObservers() {
        val groupName = userPreferencesRepository.groupName
        if (groupName.isBlank() || !userPreferencesRepository.inGroup) return

        groupRepository.buildGroupRefs(groupName)

        groupChatRef = FirebaseRefs.refGroupChat.child(groupName)

        attachOnboardingObserver()
        attachMarkReadObserver()
        attachMessagesListener()
    }

    private fun detachFirebaseObservers() {
        val ref = groupChatRef ?: return

        onboardingListener?.let { ref.removeEventListener(it) }
        markReadListener?.let { ref.removeEventListener(it) }
        messagesListener?.let { ref.removeEventListener(it) }

        onboardingListener = null
        markReadListener = null
        messagesListener = null
        groupChatRef = null
    }

    private fun attachOnboardingObserver() {
        val ref = groupChatRef ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val b = _binding ?: return
                b.progressbar2.isVisible = false

                val hasMessages = snapshot.exists()
                b.linearOnBoardingGroupChat.isVisible = !hasMessages
                b.rvMsg.isVisible = hasMessages
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        onboardingListener = listener
        ref.addValueEventListener(listener)

        // TODO: mover a repo -> groupRepo.observeGroupHasMessages(groupName)
    }

    private fun attachMarkReadObserver() {
        val ref = groupChatRef ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isResumed) return
                val groupName = userPreferencesRepository.groupName
                if (groupName.isBlank()) return

                val count = snapshot.childrenCount.toInt()
                FirebaseRefs.refDatos.child(myUid)
                    .child(NODE_CHATLIST)
                    .child("msgReadGroup")
                    .setValue(count)

                // TODO: mover a repo -> groupRepo.setGroupReadCount(uid = myUid, count)
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        markReadListener = listener
        ref.addValueEventListener(listener)
    }

    private fun attachMessagesListener() {
        val ref = groupChatRef ?: return
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!snapshot.exists() || !userPreferencesRepository.inGroup) return

                val chat = snapshot.getValue(ChatGroup::class.java) ?: return

                val dateChat = try { fmt.parse(chat.timestamp) } catch (_: ParseException) { null }
                val dateUser = try { fmt.parse(userPreferencesRepository.userDate) } catch (_: ParseException) { null }

                if (dateChat != null && dateUser != null && dateChat.after(dateUser)) {
                    adapter?.addChat(chat)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }

        messagesListener = listener
        ref.addChildEventListener(listener)

        // TODO: mover a repo -> groupRepo.observeGroupMessages(groupName): Flow<ChatGroup>
    }

    // ---------------- Send (text/photo) ----------------

    private fun onSendClicked() {
        val groupName = userPreferencesRepository.groupName
        if (groupName.isBlank()) return

        binding.loadingButton.isVisible = true
        binding.btnSendMsg.isVisible = false
        renderComposerState()

        if (msgType == MSG_PHOTO) {
            val localUri = pendingLocalPhotoUri
            if (localUri == null) {
                binding.loadingButton.isVisible = false
                renderComposerState()
                return
            }

            // Subir y luego enviar URL
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    binding.loadingPhoto.isVisible = true
                    val remoteUrl = uploadPhotoAndGetUrl(localUri)
                    pendingRemotePhotoUrl = remoteUrl
                    sendMessageInternal(message = remoteUrl, type = MSG_PHOTO)
                }.onFailure {
                    showSnack(binding.root, "No se pudo enviar la foto.")
                }

                binding.loadingPhoto.isVisible = false
                binding.loadingButton.isVisible = false
                clearPhotoPreview()
                renderComposerState()
            }

        } else {
            val text = binding.msg.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                binding.loadingButton.isVisible = false
                renderComposerState()
                return
            }

            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    sendMessageInternal(message = text, type = MSG_TEXT)
                }.onFailure {
                    showSnack(binding.root, "No se pudo enviar el mensaje.")
                }

                binding.loadingButton.isVisible = false
                binding.msg.setText("")
                renderComposerState()
            }
        }

        // TODO: notificación -> backend/Cloud Function. NO en el cliente.
        // groupRepo.enqueueGroupNotification(groupName, fromUid = myUid, previewText = ...)
    }

    private suspend fun sendMessageInternal(message: String, type: Int) {
        val groupName = userPreferencesRepository.groupName
        val chatMsg = ChatGroup(
            message,
            fmt.format(Calendar.getInstance().time),
            userPreferencesRepository.userNameGroup,
            myUid,
            type,
            userPreferencesRepository.userType
        )

        groupChatRef
            ?: FirebaseRefs.refGroupChat.child(groupName)
        FirebaseRefs.refGroupChat.child(groupName)
            .push()
            .setValue(chatMsg)
            .await()

        // TODO: mover a repo -> groupRepo.sendGroupMessage(groupName, chatMsg)
    }

    private suspend fun uploadPhotoAndGetUrl(localUri: Uri): String {
        val fileRef = refSendImages.child("group_${userPreferencesRepository.groupName}_${System.currentTimeMillis()}.jpg")
        fileRef.putFile(localUri).await()
        return fileRef.downloadUrl.await().toString()

        // TODO: mover a repo -> mediaRepo.uploadGroupChatImage(groupName, uid, localUri): String
    }

    // ---------------- Photo flow ----------------

    private fun openPhotoSourceDialog() {
        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)
        val imgCancelDialog = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val tvTitle = viewFilter.findViewById<TextView>(R.id.tv_title)
        val cardEditDelete = viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete)

        tvTitle.text = getString(R.string.enviar_desde)
        cardEditDelete.isVisible = false

        val dialog = AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.AlertDialogApp))
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

        imgCancelDialog.setOnClickListener { dialog.dismiss() }
    }

    private fun showPhotoPreview(localUri: Uri) = with(binding) {
        pendingLocalPhotoUri = localUri
        pendingRemotePhotoUrl = null
        msgType = MSG_PHOTO

        linearPhotoView.isVisible = true
        msg.isVisible = false

        loadingPhoto.isVisible = false
        loadingButton.isVisible = false

        // preview local
        photo.setImageURI(localUri)

        frameSendMsg.isVisible = true
        btnSendMsg.isVisible = true
        btnCamera.isVisible = false

        renderComposerState()
    }

    private fun clearPhotoPreview() = with(binding) {
        pendingLocalPhotoUri = null
        pendingRemotePhotoUrl = null
        msgType = MSG_TEXT

        linearPhotoView.isVisible = false
        msg.isVisible = true
        loadingPhoto.isVisible = false

        renderComposerState()
    }

    private fun initActivityResultLaunchers() {
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) showPhotoPreview(uri)
        }

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val uri = cameraOutputUri
            if (success && uri != null) showPhotoPreview(uri)
            cameraOutputUri = null
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
        val need = perms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (need) {
            onPermissionsGranted = onGranted
            requestPermissionsLauncher.launch(perms)
        } else {
            onGranted()
        }
    }

    private fun startCameraModern() {
        val ctx = requireContext()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zibe")
            }
        }

        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            showSnack(binding.root, "No se pudo abrir la cámara.")
            return
        }

        cameraOutputUri = uri
        takePictureLauncher.launch(uri)
    }

    // ---------------- Scroll + Menu safe ----------------

    private fun setScrollbar() {
        val b = _binding ?: return
        val count = adapter?.itemCount ?: return
        if (count > 0) b.rvMsg.scrollToPosition(count - 1)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.action_exit_group)?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            isVisible = true
        }
        menu.findItem(R.id.action_search)?.isVisible = false
        menu.findItem(R.id.action_unblock)?.isVisible = true
        menu.findItem(R.id.action_favorites)?.isVisible = true
    }

    // ---------------- User gestures (igual a tu lógica, pero sin globals) ----------------

    private fun handleUserSingleTap(chat: ChatGroup, view: View) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (chat.userType == 0) {
            showSnack(view, getString(R.string.perfil_incognito))
        } else if (chat.senderUid != currentUserId) {
            startActivity(
                android.content.Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra(EXTRA_USER_ID, chat.senderUid)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    private fun handleUserDoubleTap(chat: ChatGroup, view: View) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (chat.senderUid == currentUserId) return

        val groupName = userPreferencesRepository.groupName
        FirebaseRefs.refGroupUsers.child(groupName)
            .child(chat.senderUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (!ds.exists()) {
                        handleUserUnavailable(chat, view)
                        return
                    }

                    val thisname = ds.child("user_name").getValue(String::class.java)
                    val type = ds.child("type").getValue(Int::class.java) ?: 0

                    val allowed = if (type == 0) thisname == chat.nameUser else chat.userType == 1
                    if (allowed) {
                        val intent = android.content.Intent(context, ChatActivity::class.java)
                            .putExtra("unknownName", chat.nameUser)
                            .putExtra("idUserUnknown", chat.senderUid)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    } else {
                        handleUserUnavailable(chat, view)
                    }
                }
                override fun onCancelled(error: DatabaseError) = Unit
            })

        // TODO mover a repo:
        // groupRepo.isGroupUserAvailable(groupName, chat.id) + validación allowed + navegación
    }

    private fun handleUserUnavailable(chat: ChatGroup, view: View) {
        showSnack(view, getString(R.string.user_not_available_fmt, chat.nameUser))

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseRefs.refDatos.child(currentUserId)
            .child(NODE_GROUP_DM)
            .child(chat.senderUid)
            .removeValue()

        // TODO mover a repo -> userRepo.removeGroupChatWithUnknown(uid, chat.id)
    }

    private fun showSnack(view: View, message: String) {
        val snack = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        snack.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorC))
        snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).textAlignment =
            View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }
}
