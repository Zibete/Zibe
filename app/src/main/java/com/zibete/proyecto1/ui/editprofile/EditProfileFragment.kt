package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.databinding.FragmentEditProfileBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.SIGNUP_PROFILE_MESSAGE
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class EditProfileFragment : BaseChatSessionFragment() {

    private val editProfileViewModel: EditProfileViewModel by viewModels()

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var editPhotoDialog: AlertDialog? = null
//    private var prefs: SharedPreferences? = null

    private var pendingCameraUri: Uri? = null
    private val photoList: ArrayList<String> = arrayListOf()

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            if (allGranted) showEditPhotoDialogInternal() else editProfileViewModel.onError("Necesitás otorgar permisos para cambiar la foto.")
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { editProfileViewModel.onPhotoSelected(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                pendingCameraUri?.let { editProfileViewModel.onPhotoSelected(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOptionMenu()

        binding.btnSave.isEnabled = false

        binding.profilePhoto.setOnClickListener {
            val url = editProfileViewModel.uiState.value.photoUrl
                ?: DEFAULT_PROFILE_PHOTO_URL

            photoList.clear()
            if (url.isNotBlank()) photoList.add(url)

            startActivity(
                Intent(requireContext(), SlidePhotoActivity::class.java).apply {
                    putExtra("photoList", photoList)
                    putExtra("position", 0)
                    putExtra("rotation", 0)
                }
            )
        }

        binding.btnEditPhoto.setOnClickListener { requestPermsAndOpenPhotoDialog() }
        binding.datePickerBirthDay.setOnClickListener { showMaterialDatePicker() }
        binding.btnSave.setOnClickListener { editProfileViewModel.onSaveClicked() }

        // Watchers -> VM
        binding.edtNameUser.addTextChangedListener(SimpleWatcher { editProfileViewModel.onNameChanged(it) })
        binding.edtDesc.addTextChangedListener(SimpleWatcher { editProfileViewModel.onDescriptionChanged(it) })

        editProfileViewModel.load()

        collectUi()

        setupOnboarding()
    }

    private fun collectUi() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    editProfileViewModel.uiState.collect { state ->

                        binding.btnSave.isEnabled = state.saveEnabled // && !state.isSaving

                        binding.edtNameUser.setText(state.displayName)
                        binding.datePickerBirthDay.setText(state.birthDate)
                        binding.edtDesc.setText(state.description)

                        binding.tvEdad.text = state.age?.toString().orEmpty()

                        binding.completeProfile.text = SIGNUP_PROFILE_MESSAGE

                        val toLoad: Any = state.photoPreviewUri
                            ?: state.photoUrl
                            ?: DEFAULT_PROFILE_PHOTO_URL

                        loadProfilePhoto(toLoad)
                    }
                }

                launch {
                    editProfileViewModel.events.collect { ev ->
                        when (ev) {
                            is EditProfileUiEvent.ShowMessage -> editProfileViewModel.onError(ev.message)
                        }
                    }
                }
            }
        }
    }

    private fun loadProfilePhoto(model: Any) {
        binding.loadingPhoto.isVisible = true

        Glide.with(requireContext())
            .load(model)
            .apply(RequestOptions().dontTransform())
            .listener(object : RequestListener<android.graphics.drawable.Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingPhoto.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingPhoto.isVisible = false
                    return false
                }
            })
            .into(binding.profilePhoto)
    }

    private fun setupOnboarding() {

        binding.linearOnBoardingProfile.isVisible = !editProfileViewModel.isOnboardingProfileDone()

        if (!editProfileViewModel.isOnboardingProfileDone()) editProfileViewModel.onboardingProfileDone()

        binding.btnOk.setOnClickListener { binding.linearOnBoardingProfile.isVisible = false }
        binding.linearOnBoardingProfile.setOnClickListener { binding.linearOnBoardingProfile.isVisible = false }

        binding.btnDone.isVisible = isDateOfBirthSet()
        binding.btnDone.setOnClickListener {
            (activity as? MainActivity)?.mainViewModel?.onBackPressed()
        }
    }

    private fun requestPermsAndOpenPhotoDialog() {
        val perms = requiredPhotoPermissions()
        val needRequest = perms.any {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            permissionsLauncher.launch(perms.associateWith { true }.keys.toTypedArray())
        } else {
            showEditPhotoDialogInternal()
        }
    }

    private fun requiredPhotoPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun showEditPhotoDialogInternal() {
        val viewFilter = layoutInflater.inflate(R.layout.select_source_pic, null)

        val deleteSelected = viewFilter.findViewById<ImageView>(R.id.deleteSelected)
        val cameraSelection = viewFilter.findViewById<MaterialCardView>(R.id.cameraSelection)
        val gallerySelection = viewFilter.findViewById<MaterialCardView>(R.id.gallerySelection)
        val tvTitle = viewFilter.findViewById<TextView>(R.id.tv_title)
        val cardEditDelete = viewFilter.findViewById<MaterialCardView>(R.id.card_edit_delete)
        val imgCancel = viewFilter.findViewById<ImageView>(R.id.img_cancel_dialog)

        cardEditDelete.isVisible = true
        tvTitle.text = getString(R.string.editar_foto_de_perfil)

        editPhotoDialog = AlertDialog.Builder(
            ContextThemeWrapper(requireContext(), R.style.AlertDialogApp)
        )
            .setView(viewFilter)
            .setCancelable(true)
            .create()

        deleteSelected.setOnClickListener {
            editProfileViewModel.onPhotoDeletedSetDefault()
            editPhotoDialog?.dismiss()
        }

        cameraSelection.setOnClickListener {
            startCamera()
            editPhotoDialog?.dismiss()
        }

        gallerySelection.setOnClickListener {
            galleryLauncher.launch("image/*")
            editPhotoDialog?.dismiss()
        }

        imgCancel.setOnClickListener { editPhotoDialog?.dismiss() }
        editPhotoDialog?.show()
    }

    private fun startCamera() {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis())
            }
            pendingCameraUri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri)
            }
            cameraLauncher.launch(intent)
        } catch (_: Exception) {
            editProfileViewModel.onError("No se pudo abrir la cámara")
        }
    }

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

        picker.addOnPositiveButtonClickListener { selection ->
            val formatted = epochToDate(selection)
            binding.datePickerBirthDay.setText(formatted)
            editProfileViewModel.onBirthDateChanged(formatted)
        }

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

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: android.view.MenuInflater) = Unit
                override fun onPrepareMenu(menu: Menu) {
                    menu.findItem(R.id.action_settings)?.isVisible = true
                }
                override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean = false
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    fun hasPendingChanges(): Boolean = editProfileViewModel.uiState.value.saveEnabled

    fun isDateOfBirthSet(): Boolean = editProfileViewModel.uiState.value.birthDate.isNotBlank()

    override fun onDestroyView() {
        editPhotoDialog?.dismiss()
        editPhotoDialog = null
        pendingCameraUri = null
        _binding = null
        super.onDestroyView()
    }
}

private class SimpleWatcher(
    private val onChanged: (String) -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun afterTextChanged(s: android.text.Editable?) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onChanged(s?.toString().orEmpty())
    }
}
