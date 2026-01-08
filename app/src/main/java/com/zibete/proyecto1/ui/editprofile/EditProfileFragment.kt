package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
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
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.Constants.TestTags.BIRTHDATE_PICKER
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_WELCOME_SHEET
import com.zibete.proyecto1.core.constants.MSG_CAMERA_ERROR
import com.zibete.proyecto1.core.constants.MSG_CAMERA_PERMISSION_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_PROFILE_MESSAGE
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.main.MainNavEvent
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.core.utils.SimpleWatcher
import com.zibete.proyecto1.core.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.ui.extensions.setTextIfChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class EditProfileFragment : Fragment(), EditProfileWelcomeSheet.Listener {

    private val editProfileViewModel: EditProfileViewModel by viewModels()
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var editPhotoDialog: Dialog? = null
    private var pendingCameraUri: Uri? = null

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

        binding.buttonSave.isEnabled = false

        binding.profilePhoto.setOnClickListener {
            val photoUrl = editProfileViewModel.uiState.value.photoUrl
                ?: DEFAULT_PROFILE_PHOTO_URL
            PhotoViewerActivity.startSingle(requireContext(), photoUrl)
        }

        binding.actionButtonEditPhoto.setOnClickListener { showEditPhotoDialogInternal() }
        binding.datePickerBirthDay.setOnClickListener { showMaterialDatePicker() }
        binding.buttonSave.setOnClickListener { editProfileViewModel.onSaveClicked() }
        binding.buttonDone.setOnClickListener { editProfileViewModel.onBackToMain() }

        // Watchers -> VM
        binding.inputUserName.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onNameChanged(it)
        })
        binding.inputDescription.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onDescriptionChanged(it)
        })

        editProfileViewModel.load()

        collectUi()

        showWelcomeIfNeeded()
    }

    private fun collectUi() {
        binding.buttonDone.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    editProfileViewModel.uiState.collect { state ->

                        binding.buttonSave.isEnabled = state.saveEnabled && !state.isSaving

                        binding.inputUserName.setTextIfChanged(state.displayName)
                        binding.datePickerBirthDay.setTextIfChanged(editProfileViewModel.birthDateUi)
                        binding.inputDescription.setTextIfChanged(state.description)

                        binding.tvEdad.text = state.age?.toString().orEmpty()

                        binding.completeProfile.text = SIGNUP_PROFILE_MESSAGE

                        binding.buttonDone.isVisible = state.hasBirthDate

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

    private fun showWelcomeIfNeeded() {
        viewLifecycleOwner.lifecycleScope.launch {
            val shown = editProfileViewModel.isEditProfileWelcomeShown()
            if (shown) return@launch

            // Evita duplicados si rota pantalla o se re-crea el fragment
            if (parentFragmentManager.findFragmentByTag(EDIT_PROFILE_WELCOME_SHEET) != null) return@launch

            EditProfileWelcomeSheet()
                .show(parentFragmentManager, EDIT_PROFILE_WELCOME_SHEET)
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
                editProfileViewModel.onError(requireContext().getString(R.string.msg_camera_error))
                return
            }

            takePictureLauncher.launch(uri)
        }.onFailure {
            editProfileViewModel.onError(MSG_CAMERA_ERROR)
        }
    }

    private fun showEditPhotoDialogInternal() {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.Zibe_Dialog)
        val dialogBinding = SelectSourcePicBinding.inflate(LayoutInflater.from(themedContext))

        dialogBinding.cardEditDelete.isVisible = true
        dialogBinding.tvTitle.text = getString(R.string.editar_foto_de_perfil)

        editPhotoDialog = Dialog(themedContext).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            window?.setBackgroundDrawable(getDrawable(requireContext(),R.drawable.badge_round))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            show()
        }

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

        picker.show(parentFragmentManager, BIRTHDATE_PICKER)
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

    override fun onDestroyView() {
        editPhotoDialog?.dismiss()
        editPhotoDialog = null
        pendingCameraUri = null
        _binding = null
        super.onDestroyView()
    }

    override fun onEditProfileWelcomeDismissed() {
        editProfileViewModel.markEditProfileWelcomeShown()
    }
}