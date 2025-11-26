package com.zibete.proyecto1

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.canhub.cropper.CropImageContractOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.zibete.proyecto1.adapters.AdapterChatGroup
import com.zibete.proyecto1.databinding.FragmentGroupBinding
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.CropHelper
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupChat
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

class ChatGroupFragment : Fragment() {

    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private val chatsArrayList: ArrayList<ChatsGroup> = ArrayList()
    private var adapter: AdapterChatGroup? = null
    private var mLayoutManager: LinearLayoutManager? = null

    private var progress: ProgressDialog? = null
    private var msgType: Int = Constants.MSG
    private var stringMsg: String? = null

    private var values: ContentValues? = null
    private var imageUri: Uri? = null

    private val photoList: ArrayList<String?> = ArrayList()

    // Camera / gallery
    private var imageUriCamera: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsGranted: (() -> Unit)? = null


    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference
    private val user get() = currentUser!!

    private val refSendImages: StorageReference by lazy {
        storageReference.child("Chats/${user.uid}/")
    }

    // Launcher CropActivity
    private lateinit var cropImageLauncher: ActivityResultLauncher<CropImageContractOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initActivityResultLaunchers()
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        // Ocultar badge de grupos al entrar
        MainActivity.badgeDrawableGroup?.isVisible = false

        // Marcar mensajes leídos del grupo activo
        refGroupChat.child(UsuariosFragment.groupName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (this@ChatGroupFragment.isResumed &&
                        UsuariosFragment.groupName.isNotEmpty()
                    ) {
                        val count = snapshot.childrenCount.toInt()
                        refDatos.child(user.uid)
                            .child("ChatList")
                            .child("msgReadGroup")
                            .setValue(count)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })


        progress = ProgressDialog(requireContext(), R.style.AlertDialogApp)

        initRecycler()
        initSendUi()
        initOnBoardingObserver()
        initMessagesListener()
        initCropLauncher()

        // Cancelar notificaciones al abrir el chat de grupo
        (requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        return view
    }

    // ================== INIT UI ==================

    private fun initRecycler() = with(binding) {
        mLayoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = false
            stackFromEnd = true
        }

        rvMsg.layoutManager = mLayoutManager
        adapter = AdapterChatGroup(chatsArrayList, Constants.MAXCHATSIZE, requireContext())
        rvMsg.adapter = adapter

        rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val totalItemCount = adapter?.itemCount ?: 0
                val lastVisible = mLayoutManager?.findLastVisibleItemPosition() ?: 0
                val endHasBeenReached = lastVisible + 1 >= totalItemCount
                linearBack.isVisible = totalItemCount > 0 && !endHasBeenReached
            }
        })

        buttonScrollBack.setOnClickListener { setScrollbar() }

        adapter?.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
            }
        })

        // Para compatibilidad con Adapter (borrado múltiple)
//        countDeleteMsg = binding.countDeleteMsg
    }

    private fun initSendUi() = with(binding) {
        // Estado inicial
        btnCamera.isVisible = true
        btnSendMsg.isVisible = false
        frameSendMsg.isVisible = false
        loadingButton.isVisible = false
        linearPhotoView.isVisible = false
        linearBack.isVisible = false

        // Ver foto a pantalla completa
        photo.setOnClickListener {
            if (!stringMsg.isNullOrEmpty()) {
                photoList.clear()
                photoList.add(stringMsg)
                val intent = Intent(requireContext(), SlidePhotoActivity::class.java).apply {
                    putExtra("photoList", photoList)
                    putExtra("position", 0)
                    putExtra("rotation", 180)
                }
                startActivity(intent)
            }
        }

        // Cancelar preview
        cancelAction.setOnClickListener {
            cancelPreviewPhoto()
        }

        layoutChatGroup.setOnClickListener {
            // reservado (mantener por compatibilidad si se usaba para ocultar teclado etc)
        }

        // Toggle cámara / enviar como en ChatActivity
        msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (msg.text.isEmpty()) {
                    btnCamera.isVisible = true
                    btnSendMsg.isVisible = false
                    frameSendMsg.isVisible = false
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (msg.text.isNotEmpty()) {
                    btnCamera.isVisible = false
                    btnSendMsg.isVisible = true
                    frameSendMsg.isVisible = true
                } else {
                    btnCamera.isVisible = true
                    btnSendMsg.isVisible = false
                    frameSendMsg.isVisible = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (msg.text.isEmpty()) {
                    btnCamera.isVisible = true
                    btnSendMsg.isVisible = false
                    frameSendMsg.isVisible = false
                }
            }
        })

        btnCamera.setOnClickListener { sendPhoto() }
        btnSendMsg.setOnClickListener { sendMessage() }
    }

    private fun initOnBoardingObserver() = with(binding) {
        refGroupChat.child(UsuariosFragment.groupName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    progressbar2.isVisible = false
                    if (dataSnapshot.exists()) {
                        linearOnBoardingGroupChat.isVisible = false
                        rvMsg.isVisible = true
                    } else {
                        linearOnBoardingGroupChat.isVisible = true
                        rvMsg.isVisible = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun initMessagesListener() {
        listenerGroupChat = object : ChildEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onChildAdded(
                dataSnapshot: DataSnapshot,
                previousChildName: String?
            ) {
                if (!dataSnapshot.exists() || !UsuariosFragment.inGroup) return

                val chat = dataSnapshot.getValue(ChatsGroup::class.java) ?: return
                val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")

                val dateChat = try {
                    fmt.parse(chat.dateTime)
                } catch (e: ParseException) {
                    null
                }

                val dateUser = try {
                    fmt.parse(UsuariosFragment.userDate)
                } catch (e: ParseException) {
                    null
                }

                if (dateChat != null && dateUser != null && dateChat.after(dateUser)) {
                    adapter?.addChat(chat)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        refGroupChat.child(UsuariosFragment.groupName)
            .addChildEventListener(listenerGroupChat as ChildEventListener)
    }

    private fun initCropLauncher() = with(binding) {
        // El CropHelper ya se encarga de mostrar / ocultar preview y loaders.
        // El callback lo dejamos por si en el futuro querés lógica extra.
        cropImageLauncher = CropHelper.registerLauncherForFragment(
            this@ChatGroupFragment,
            refSendImages,
            linearPhotoView,
            linearPhoto,
            photo,
            loadingPhoto,
            loadingButton,
            frameSendMsg,
            msg,
            btnCamera,
            btnSendMsg
        ) { uri: Uri? ->
            if (uri != null) {
                msgType = Constants.PHOTO
                stringMsg = uri.toString()
                loadingPhoto.isVisible = false
                loadingButton.isVisible = false
                frameSendMsg.isVisible = true
                btnSendMsg.isVisible = true
            } else {
                cancelPreviewPhoto()
            }
        }
    }

    // ================== FOTO ==================

    fun sendPhoto() {
        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)

        val imgCancelDialog = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val tvTitle = viewFilter.findViewById<TextView>(R.id.tv_title)
        val cardEditDelete = viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete)

        tvTitle.text = getString(R.string.enviar_desde)
        cardEditDelete.isVisible = false

        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(requireContext(), R.style.AlertDialogApp)
        )
            .setView(viewFilter)
            .create()

        dialog.show()

        // Cámara
        cameraSelection.setOnClickListener {
            ensurePermissions(arrayOf(Manifest.permission.CAMERA)) {
                startCameraModern()
            }
            dialog.dismiss()
        }

        // Galería
        gallerySelection.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            dialog.dismiss()
        }

        imgCancelDialog.setOnClickListener { dialog.dismiss() }
    }

    private fun cancelPreviewPhoto() = with(binding) {
        linearPhotoView.isVisible = false
        msg.isVisible = true
        btnCamera.isVisible = true
        btnSendMsg.isVisible = false
        frameSendMsg.isVisible = false
        loadingPhoto.isVisible = false
        loadingButton.isVisible = false
        msgType = Constants.MSG
        stringMsg = null
    }

    // ================== MENSAJES ==================

    @SuppressLint("SimpleDateFormat")
    fun sendMessage() = with(binding) {
        val textForNotification: String

        if (msgType == Constants.PHOTO) {
            // Para el receptor mostramos "Foto recibida"
            textForNotification = getString(R.string.photo_received)
            // La URL real ya la puso CropHelper/stringMsg
        } else {
            msgType = Constants.MSG
            stringMsg = msg.text.toString().trim()
            textForNotification = stringMsg.orEmpty()
        }

        if (stringMsg.isNullOrEmpty()) return

        // UI loading en botón
        btnSendMsg.isVisible = false
        loadingButton.isVisible = true

        val c = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")

        val chatMsg = ChatsGroup(
            stringMsg!!,
            dateFormat.format(c.time),
            UsuariosFragment.userName,
            user.uid,
            msgType,
            UsuariosFragment.userType
        )

        // Enviar a Firebase
        refGroupChat.child(UsuariosFragment.groupName)
            .push()
            .setValue(chatMsg)
            .addOnCompleteListener {
                // Reset UI siempre
                msg.setText("")
                msgType = Constants.MSG
                stringMsg = null
                cancelPreviewPhoto()
                loadingButton.isVisible = false
            }

        // Notificaciones a los demás miembros del grupo
        refGroupUsers.child(UsuariosFragment.groupName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val userId =
                            snapshot.child("user_id").getValue(String::class.java) ?: continue
                        if (userId == user.uid) continue

                        refCuentas.child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val token =
                                        snapshot.child("token")
                                            .getValue(String::class.java)
                                            .orEmpty()
                                    if (token.isNotEmpty()) {
                                        sendGroupPushNotification(token, textForNotification)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendGroupPushNotification(token: String, msg: String) {
        try {
            val data = JSONObject().apply {
                put("novistos", "")
                put("user", UsuariosFragment.userName)
                put("msg", msg)
                put("id_user", user.uid)
                put("type", UsuariosFragment.groupName)
            }

            val json = JSONObject().apply {
                put("to", token)
                put("priority", "high")
                put("data", data)
            }

            val url = "https://fcm.googleapis.com/fcm/send"
            val request = object : JsonObjectRequest(
                Method.POST, url, json, null, null
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val header = HashMap<String, String>()
                    header["content-type"] = "application/json"
                    header["authorization"] =
                        "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"
                    return header
                }
            }

            Volley.newRequestQueue(requireContext()).add(request)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // ================== SCROLL ==================

    private fun setScrollbar() {
        val b = _binding ?: return              // Si la vista ya no existe, no hacemos nada
        val count = adapter?.itemCount ?: return
        if (count > 0) {
            b.rvMsg.scrollToPosition(count - 1)
        }
    }

    // ================== MENU ==================

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unlock)
        val actionFavorites = menu.findItem(R.id.action_favorites)
        val actionExit = menu.findItem(R.id.action_exit)

        actionExit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        actionExit.isVisible = true
        actionSearch.isVisible = false
        actionUnlock.isVisible = true
        actionFavorites.isVisible = true
    }

    // ================== CAMERA / GALLERY (LEGACY) ==================

    private fun initActivityResultLaunchers() {
        // Galería (Photo Picker)
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                // Mismo flujo que cuando viene de cámara
                CropHelper.launchCrop(
                    cropImageLauncher,
                    uri
                )
            }
        }

        // Cámara moderna
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUriCamera != null) {
                CropHelper.launchCrop(
                    cropImageLauncher,
                    imageUriCamera!!
                )
            } else {
                imageUriCamera = null
            }
        }

        // Permisos múltiples
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

        imageUriCamera = ctx.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        if (imageUriCamera == null) {
            // si querés, usa un Toast o similar
            // Toast.makeText(ctx, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
            return
        }

        takePictureLauncher.launch(imageUriCamera!!)
    }

//    private fun startCamera() {
//        values = ContentValues().apply {
//            put(
//                MediaStore.Images.Media.DESCRIPTION,
//                System.currentTimeMillis().toString()
//            )
//        }
//
//        imageUri = requireContext().contentResolver.insert(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            values
//        )
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
//        }
//        startActivityForResult(intent, Constants.CAMERA_SELECTED)
//    }

//    @Deprecated("startActivityForResult legacy usage, mantenido por compatibilidad")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (resultCode != Activity.RESULT_OK) return
//
//        progress?.apply {
//            setMessage("Espere...")
//            setCanceledOnTouchOutside(false)
//            show()
//        }
//
//        when (requestCode) {
//            Constants.CAMERA_SELECTED -> {
//                imageUri?.let { uri ->
//                    CropHelper.launchCrop(cropImageLauncher, uri)
//                }
//            }
//            Constants.PHOTO_SELECTED -> {
//                val selected = data?.data
//                if (selected != null) {
//                    CropHelper.launchCrop(cropImageLauncher, selected)
//                }
//            }
//        }
//
//        progress?.dismiss()
//    }
//
//    @Deprecated("Deprecated in Java")
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == Constants.CAMERA_SELECTED &&
//            grantResults.isNotEmpty() &&
//            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//        ) {
//            startCamera()
//        }
//
//        if (requestCode == Constants.PHOTO_SELECTED &&
//            grantResults.isNotEmpty() &&
//            grantResults[0] == PackageManager.PERMISSION_GRANTED
//        ) {
//            val gallery = Intent(
//                Intent.ACTION_PICK,
//                MediaStore.Images.Media.INTERNAL_CONTENT_URI
//            )
//            startActivityForResult(gallery, Constants.PHOTO_SELECTED)
//        }
//    }

    // ================== LIFECYCLE ==================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmField
        var listenerGroupChat: ChildEventListener? = null

        @JvmField
        var countDeleteMsg: TextView? = null
    }
}
