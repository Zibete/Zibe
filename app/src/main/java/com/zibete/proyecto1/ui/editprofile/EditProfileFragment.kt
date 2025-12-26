package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.FragmentEditProfileBinding
import com.zibete.proyecto1.databinding.SelectSourcePicBinding
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.MSG_CAMERA_ERROR
import com.zibete.proyecto1.ui.constants.MSG_CAMERA_PERMISSION_REQUIRED
import com.zibete.proyecto1.ui.constants.SIGNUP_PROFILE_MESSAGE
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.main.MainNavEvent
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.utils.SimpleWatcher
import com.zibete.proyecto1.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.utils.UserMessageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private val editProfileViewModel: EditProfileViewModel by viewModels()
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var editPhotoDialog: Dialog? = null
    private var pendingCameraUri: Uri? = null
    private val photoList: ArrayList<String> = arrayListOf()

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) pendingCameraUri?.let { editProfileViewModel.onPhotoSelected(it) }
        }

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { editProfileViewModel.onPhotoSelected(it) }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else editProfileViewModel.onError(MSG_CAMERA_PERMISSION_REQUIRED)
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { editProfileViewModel.onPhotoSelected(it) }
        }

    private fun onGalleryClicked() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    private fun onCameraClicked() {
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        if (granted) startCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            if (url.isNotBlank()) photoList.add(url) else return@setOnClickListener

            startActivity(
                Intent(requireContext(), PhotoViewerActivity::class.java).apply {
                    putExtra("photoList", photoList)
                }
            )
        }

        binding.btnEditPhoto.setOnClickListener { showEditPhotoDialogInternal() }
        binding.datePickerBirthDay.setOnClickListener { showMaterialDatePicker() }
        binding.btnSave.setOnClickListener { editProfileViewModel.onSaveClicked() }
        binding.btnDone.setOnClickListener { editProfileViewModel.onBackToMain() }

        // Watchers -> VM
        binding.edtNameUser.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onNameChanged(it)
        })
        binding.edtDesc.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onDescriptionChanged(it)
        })

        editProfileViewModel.load()

        collectUi()

        setupOnboarding()
    }

    private fun collectUi() {

        binding.btnDone.isVisible = false



        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    editProfileViewModel.uiState.collect { state ->

                        binding.btnSave.isEnabled = state.saveEnabled && !state.isSaving

                        binding.edtNameUser.setTextIfChanged(state.displayName)
                        binding.datePickerBirthDay.setTextIfChanged(editProfileViewModel.birthDateUi)
                        binding.edtDesc.setTextIfChanged(state.description)

                        binding.tvEdad.text = state.age?.toString().orEmpty()

                        binding.completeProfile.text = SIGNUP_PROFILE_MESSAGE

                        binding.btnDone.isVisible = state.hasBirthDate

                        val toLoad: Any = state.photoPreviewUri
                            ?: state.photoUrl
                            ?: DEFAULT_PROFILE_PHOTO_URL

                        loadProfilePhoto(toLoad)
                    }
                }

                launch {
                    editProfileViewModel.events.collect { event ->
                        when (event) {
                            is EditProfileUiEvent.ShowMessage -> {
                                UserMessageUtils.showSnack(
                                    root = binding.root,
                                    message = event.message,
                                    type = event.type
                                )
                            }

                            is EditProfileUiEvent.OnBackToMain -> {
                                (activity as? MainActivity)?.mainViewModel?.emit(
                                    MainNavEvent.BackFromEditProfile(event.message))
                            }
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

        viewLifecycleOwner.lifecycleScope.launch {

            val isFirstLoginDone = editProfileViewModel.isFirstLoginDone()

            if (!isFirstLoginDone) {
                binding.linearOnBoardingProfile.isVisible = true
                editProfileViewModel.markFirstLoginAsDone()
            }
        }

        binding.btnOk.setOnClickListener {
            binding.linearOnBoardingProfile.isVisible = false
        }

        binding.linearOnBoardingProfile.setOnClickListener {
            binding.linearOnBoardingProfile.isVisible = false
        }
    }


    private fun startCamera() {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis().toString())
            }

            pendingCameraUri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            val uri = pendingCameraUri
            if (uri == null) {
                editProfileViewModel.onError("No se pudo preparar la captura")
                return
            }

            takePictureLauncher.launch(uri)
        }.onFailure {
            editProfileViewModel.onError(MSG_CAMERA_ERROR)
        }
    }

    private fun showEditPhotoDialogInternal() {
        val dialogBinding = SelectSourcePicBinding.inflate(layoutInflater)

        dialogBinding.cardEditDelete.isVisible = true
        dialogBinding.tvTitle.text = getString(R.string.editar_foto_de_perfil)

        editPhotoDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.deleteSelected.setOnClickListener {
            editProfileViewModel.onPhotoDeletedSetDefault()
            editPhotoDialog?.dismiss()
        }

        dialogBinding.cameraSelection.setOnClickListener {
            onCameraClicked()
            editPhotoDialog?.dismiss()
        }

        dialogBinding.gallerySelection.setOnClickListener {
            onGalleryClicked()
            editPhotoDialog?.dismiss()
        }

        dialogBinding.imgCancelDialog.setOnClickListener { editPhotoDialog?.dismiss() }

        editPhotoDialog?.show()
    }

    private fun showMaterialDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.birthDate))
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ZibeDatePickerOverlay)
            .setSelection(
                editProfileViewModel.uiState.value.birthDate
                    .let { iso -> isoToMillis(iso) }
                    ?: MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val isoDate = millisToIso(millis)
            editProfileViewModel.onBirthDateChanged(isoDate)
        }

        picker.show(parentFragmentManager, "ZIBE_BIRTHDATE_PICKER")
    }

    private fun setupOptionMenu() {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: android.view.MenuInflater) =
                    Unit

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

    private fun EditText.setTextIfChanged(newValue: String) {
        val current = text?.toString().orEmpty()
        if (current == newValue) return
        if (hasFocus()) return
        setText(newValue)
    }


    override fun onDestroyView() {
        editPhotoDialog?.dismiss()
        editPhotoDialog = null
        pendingCameraUri = null
        _binding = null
        super.onDestroyView()
    }
}