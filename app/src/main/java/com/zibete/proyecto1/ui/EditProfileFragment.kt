package com.zibete.proyecto1.ui

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.Objects
import androidx.core.content.edit
import com.zibete.proyecto1.utils.Constants.CAMERA_SELECTED
import com.zibete.proyecto1.utils.Constants.PERMISSIONS_EDIT_PROFILE
import com.zibete.proyecto1.utils.Constants.PHOTO_SELECTED

class EditProfileFragment : Fragment() {

    // UI
    private var ftPerfil: ResizableImageViewProfile? = null
    private var loadingPhoto: ProgressBar? = null
    private var edtNameUser: TextInputEditText? = null
    private var edtDesc: TextInputEditText? = null
    private var edtDate: TextInputEditText? = null
    private var tvAge: TextView? = null
    private var completeProfile: TextView? = null
    private var btnDone: TextView? = null
    private var btnOk: TextView? = null

    private var btSave: ExtendedFloatingActionButton? = null
    private var btEdit: ExtendedFloatingActionButton? = null
    private var linearButtonsEdit: LinearLayout? = null
    private var linearOnBoardingProfile: LinearLayout? = null

    // State
    private var birthDay: String? = null
    private var progress: ProgressDialog? = null
    private var photoList: ArrayList<String?> = arrayListOf()

    // Firebase
    private val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference
    private var refImgUser: StorageReference? = null

    // Media
    private var values: ContentValues? = null
    private var imageUri: Uri? = null
    private var imageurl: String? = null
    private var thumb_byte: ByteArray? = null
    private val thumbnail: Bitmap? = null // se rellena donde corresponda en tu flujo actual

    // Tokens
    private var myInstallId: String? = null
    private var myFcmToken: String? = null

    // Prefs
    private var prefs: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var flagProfile: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        setHasOptionsMenu(true)

        val user = currentUser ?: return view

        // Prefs
        val ctx = requireContext()
        prefs = ctx.getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE)
        editor = prefs!!.edit()
        flagProfile = prefs!!.getBoolean("flag_OnBoardingProfile", false)

        // Firebase storage ref
        refImgUser = storageReference.child("Users/imgPerfil/${user.uid}.jpg")

        // Tokens
        FirebaseInstallations.getInstance().id
            .addOnCompleteListener { t ->
                if (t.isSuccessful) myInstallId = t.result
            }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { t ->
                if (t.isSuccessful) myFcmToken = t.result
            }

        // Bind UI
        ftPerfil = view.findViewById(R.id.ftPerfil)
        loadingPhoto = view.findViewById(R.id.loadingPhoto)
        edtNameUser = view.findViewById(R.id.edtNameUser)
        edtDesc = view.findViewById(R.id.edtDesc)
        edtDate = view.findViewById(R.id.edtFecha)
        tvAge = view.findViewById(R.id.tvEdad)
        linearButtonsEdit = view.findViewById(R.id.linearButtonsEdit)
        btEdit = view.findViewById(R.id.bt_edit)
        btSave = view.findViewById(R.id.bt_save)
        btnDone = view.findViewById(R.id.btn_done)
        linearOnBoardingProfile = view.findViewById(R.id.linearOnBoardingProfile)
        completeProfile = view.findViewById(R.id.completeProfile)
        btnOk = view.findViewById(R.id.btn_ok)

        progress = ProgressDialog(context, R.style.AlertDialogApp)

        // Onboarding overlay
        btSave?.isEnabled = false
        if (!flagProfile) {
            linearOnBoardingProfile?.visibility = View.VISIBLE
            btnDone?.visibility = View.VISIBLE
            editor?.putBoolean("flag_OnBoardingProfile", true)?.apply()
        } else {
            linearOnBoardingProfile?.visibility = View.GONE
            btnDone?.visibility = View.GONE
        }

        btnOk?.setOnClickListener {
            linearOnBoardingProfile?.visibility = View.GONE
        }
        linearOnBoardingProfile?.setOnClickListener {
            linearOnBoardingProfile?.visibility = View.GONE
        }

        btnDone?.setOnClickListener {
            refCuentas.child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return
                        val bd = snapshot.child("birthDay").getValue(String::class.java)
                        if (bd.isNullOrEmpty()) {
                            snack("Complete su fecha de nacimiento")
                        } else {
                            val intent = Intent(context, SplashActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            startActivity(intent)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // Cargar datos actuales
        bindCurrentProfile()

        // Nombre y desc inicial
        edtNameUser?.setText(user.displayName.orEmpty())
        refCuentas.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val desc = snapshot.child("descripcion").getValue(String::class.java)
                    edtDesc?.setText(desc.orEmpty())
                    btSave?.isEnabled = false
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Click foto -> SlidePhotoActivity
        ftPerfil?.setOnClickListener {
            val intent = Intent(context, SlidePhotoActivity::class.java).apply {
                putExtra("photoList", photoList)
                putExtra("position", 0)
                putExtra("rotation", 0)
            }
            startActivity(intent)
        }

        // Editar foto
        btEdit?.setOnClickListener { EditProfilePhoto() }

        // Date picker
        edtDate?.setOnClickListener { showMaterialDatePicker() }

        // Guardar
        btSave?.setOnClickListener { save() }

        // Habilitar guardar al modificar texto
        val enableSaveWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btSave?.isEnabled = true
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        edtNameUser?.addTextChangedListener(enableSaveWatcher)
        edtDesc?.addTextChangedListener(enableSaveWatcher)

        return view
    }

    // Carga de perfil actual
    private fun bindCurrentProfile() {
        val user = currentUser ?: return

        refCuentas.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val foto = snapshot.child("foto").getValue(String::class.java)
                    val bd = snapshot.child("birthDay").getValue(String::class.java)
                    val desc = snapshot.child("descripcion").getValue(String::class.java)

                    // Edad + textos (API 26+ con fallback)
                    if (bd.isNullOrEmpty()) {
                        completeProfile?.text =
                            "Te pediremos que completes tu fecha de nacimiento. También podrás agregar información sobre vos o cambiar tu foto de perfil"
                    } else {
                        try {
                            val edad = computeAgeFromString(bd)
                            tvAge?.text = edad.toString()
                            edtDate?.setText(bd)
                            completeProfile?.text =
                                "Actualizá tu perfil con una foto y tus datos personales"
                        } catch (_: Exception) {
                        }
                    }

                    edtDesc?.setText(desc.orEmpty())

                    loadingPhoto?.visibility = View.VISIBLE

                    val opts = RequestOptions().dontTransform()

                    Glide.with(requireContext())
                        .load(if (!foto.isNullOrEmpty()) foto else getString(R.string.URL_PHOTO_DEF))
                        .apply(opts)
                        .listener(object : RequestListener<Drawable?> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable?>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                loadingPhoto?.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable?>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                loadingPhoto?.visibility = View.GONE
                                return false
                            }
                        })
                        .into(ftPerfil!!)

                    if (!foto.isNullOrEmpty()) {
                        photoList.add(foto)
                    }
                    btSave?.isEnabled = false
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ======== DATE PICKER (Material) ========
    private fun showMaterialDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.fecha_nacimiento))
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ZibeDatePickerOverlay)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener(
            MaterialPickerOnPositiveButtonClickListener { selection ->
                val formatted = epochToDate(selection)
                birthDay = formatted
                edtDate?.setText(formatted)

                try {
                    val edad = computeAgeFromString(formatted)
                    tvAge?.text = edad.toString()
                } catch (_: Exception) {
                }
                btSave?.isEnabled = true
            }
        )

        picker.show(parentFragmentManager, "ZIBE_BIRTHDATE_PICKER")
    }

    private fun epochToDate(epochMillis: Long?): String {
        if (epochMillis == null) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ld = Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(epochMillis)
        }
    }

    @Throws(Exception::class)
    private fun computeAgeFromString(ddMMyyyy: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birth = LocalDate.parse(ddMMyyyy, fmt)
            Period.between(birth, LocalDate.now()).years
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val b = Calendar.getInstance()
            b.time = Objects.requireNonNull(sdf.parse(ddMMyyyy))
            val now = Calendar.getInstance()
            var age = now[Calendar.YEAR] - b[Calendar.YEAR]
            if (now[Calendar.DAY_OF_YEAR] < b[Calendar.DAY_OF_YEAR]) age--
            age
        }
    }

    // ======== FOTO DE PERFIL ========
    fun EditProfilePhoto() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.CAMERA
            perms += Manifest.permission.READ_MEDIA_IMAGES
        } else {
            perms += Manifest.permission.CAMERA
            perms += Manifest.permission.READ_EXTERNAL_STORAGE
            perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        val needRequest = perms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) !=
                    PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            requestPermissions(perms.toTypedArray(), PERMISSIONS_EDIT_PROFILE)
            return
        }

        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)
        val deleteSelected = viewFilter.findViewById<ImageView>(R.id.deleteSelected)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val tvTitle = viewFilter.findViewById<TextView>(R.id.tv_title)
        val cardEditDelete = viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete)
        val imgCancel = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)

        cardEditDelete.visibility = View.VISIBLE
        tvTitle.text = getString(R.string.editar_foto_de_perfil)

        val dialog = AlertDialog.Builder(
            ContextThemeWrapper(requireContext(), R.style.AlertDialogApp)
        )
            .setView(viewFilter)
            .setCancelable(true)
            .create()

        deleteSelected.setOnClickListener {
            btSave?.isEnabled = true
            imageUri = Uri.parse(getString(R.string.URL_PHOTO_DEF))
            imageurl = getString(R.string.URL_PHOTO_DEF)
            loadingPhoto?.visibility = View.VISIBLE

            Glide.with(requireContext())
                .load(imageUri)
                .apply(RequestOptions().dontTransform())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingPhoto?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingPhoto?.visibility = View.GONE
                        return false
                    }
                })
                .into(ftPerfil!!)

            thumb_byte = null
            dialog.dismiss()
        }

        cameraSelection.setOnClickListener {
            startCamera()
            dialog.dismiss()
        }

        gallerySelection.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            try {
                startActivityForResult(gallery, PHOTO_SELECTED)
            } catch (e: Exception) {
                snack("No se pudo abrir la galería")
            }
            dialog.dismiss()
        }

        imgCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun startCamera() {
        try {
            values = ContentValues().apply {
                put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis())
            }
            imageUri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            }
            startActivityForResult(intent, CAMERA_SELECTED)
        } catch (e: Exception) {
            snack("No se pudo abrir la cámara")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isAdded) return

        if (progress == null) {
            progress = ProgressDialog(context, R.style.AlertDialogApp)
        }
        progress?.apply {
            setMessage("Espere...")
            setCanceledOnTouchOutside(false)
        }

        try {
            progress?.show()
            // Aquí iría el procesamiento real de la imagen
        } catch (_: Throwable) {
        } finally {
            if (progress?.isShowing == true) {
                try {
                    progress?.dismiss()
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun getRealPathFromURI(contentUri: Uri): String? {
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = requireActivity().contentResolver
                .query(contentUri, proj, null, null, null) ?: return null
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val res = cursor.getString(columnIndex)
            cursor.close()
            res
        } catch (e: Exception) {
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_EDIT_PROFILE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                EditProfilePhoto()
            } else {
                snack("Necesitás otorgar permisos para cambiar la foto.")
            }
        }
    }

    private fun save() {
        val user = currentUser ?: return

        if (progress == null) {
            progress = ProgressDialog(context, R.style.AlertDialogApp)
        }
        progress?.apply {
            setMessage("Espere...")
            setCanceledOnTouchOutside(false)
            show()
        }

        try {
            val name = edtNameUser?.text?.toString()?.trim().orEmpty()
            val fecha = edtDate?.text?.toString()?.trim().orEmpty()
            val desc = edtDesc?.text?.toString()?.trim().orEmpty()

            if (fecha.isEmpty()) {
                snack("Debe ingresar su fecha de nacimiento para continuar")
                return
            }

            val edad = try {
                computeAgeFromString(fecha).also {
                    if (it < 18) {
                        snack("Lo sentimos, debe ser mayor de 18 años para utilizar la App")
                        return
                    }
                }
            } catch (e: Exception) {
                snack("Fecha de nacimiento inválida")
                return
            }

            // Subida de foto si hay thumb_byte
            thumb_byte?.let { bytes ->
                try {
                    val folder = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "Zibe"
                    )
                    if (!folder.exists() && !folder.mkdirs()) {
                        Log.e("EditProfile", "No se pudo crear directorio Zibe")
                    }

                    val sdf = SimpleDateFormat("ddMMyyyyHHmm", Locale.getDefault())
                    val fname = "IMG_${sdf.format(Calendar.getInstance().time)}"
                    val file = File(folder, "$fname.jpg")
                    try {
                        val fos = FileOutputStream(file)
                        thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }

                    val uploadTask = refImgUser!!.putBytes(bytes)
                    uploadTask
                        .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                            if (!task.isSuccessful) {
                                throw task.exception ?: Exception("Upload failed")
                            }
                            refImgUser!!.downloadUrl
                        })
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val downloadUri = task.result
                                refCuentas.child(user.uid).child("foto")
                                    .setValue(downloadUri.toString())
                                imageUri = downloadUri
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setPhotoUri(downloadUri)
                                    .build()
                                user.updateProfile(profileUpdates)
                            }
                        }
                } catch (t: Throwable) {
                    Log.e("EditProfile", "Error subiendo foto", t)
                }
            }

            val profileUpdates = if (imageurl != null &&
                imageurl == getString(R.string.URL_PHOTO_DEF)
            ) {
                try {
                    refImgUser?.delete()
                } catch (_: Throwable) {
                }
                refCuentas.child(user.uid).child("foto").setValue(imageurl)
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(imageUri)
                    .build()
            } else {
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            }

            user.updateProfile(profileUpdates)

            val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val nowStr = stamp.format(Calendar.getInstance().time)

            refCuentas.child(user.uid).child("nombre").setValue(name)
            refCuentas.child(user.uid).child("birthDay").setValue(fecha)
            refCuentas.child(user.uid).child("age").setValue(edad)
            refCuentas.child(user.uid).child("descripcion").setValue(desc)
            refCuentas.child(user.uid).child("date").setValue(nowStr)

            myInstallId?.takeIf { it.isNotEmpty() }?.let {
                refCuentas.child(user.uid).child("installId").setValue(it)
                refCuentas.child(user.uid).child("token").setValue(it)
            }

            myFcmToken?.takeIf { it.isNotEmpty() }?.let {
                refCuentas.child(user.uid).child("fcmToken").setValue(it)
            }

            updateUI(user)
        } finally {
            if (progress?.isShowing == true) {
                try {
                    progress?.dismiss()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (!isAdded) return
        if (user != null) {
            try {
                Toast.makeText(context, "Datos actualizados correctamente", Toast.LENGTH_SHORT)
                    .show()
            } catch (_: Throwable) {
            }
            if (progress?.isShowing == true) progress?.dismiss()

            val intent = Intent(context, SplashActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            startActivity(intent)
        } else {
            if (progress?.isShowing == true) progress?.dismiss()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        try {
            menu.findItem(R.id.action_search)?.isVisible = false
            menu.findItem(R.id.action_favorites)?.isVisible = false
        } catch (_: Throwable) {
        }
    }

    private fun snack(msg: String) {
        if (!isAdded || view == null) return
        val snack = Snackbar.make(requireView(), msg, Snackbar.LENGTH_INDEFINITE)
        snack.setAction("OK") { snack.dismiss() }
        try {
            snack.setBackgroundTint(resources.getColor(R.color.zibe_pink))
            val tv = snack.view.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text
            )
            tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        } catch (_: Throwable) {
        }
        snack.show()
    }

    // ===== ViewModel pequeño (se mantiene por compatibilidad) =====
    class UsuariosViewModel : ViewModel() {
        private val mText = MutableLiveData<String?>().apply {
            value = "This is Usuarios fragment"
        }
        val text: LiveData<String?> = mText
    }

    companion object {
        fun deleteProfilePreferences(context: Context?) {
            if (context == null) return
            val prefs =
                context.getSharedPreferences("OnBoardingProfile", Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean("flag_OnBoardingProfile", false)
            }
        }
    }


}
