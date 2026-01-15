package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.constants.Constants.TestTags.BIRTHDATE_PICKER
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_WELCOME_SHEET
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.SimpleWatcher
import com.zibete.proyecto1.core.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.databinding.FragmentEditProfileBinding
import com.zibete.proyecto1.databinding.SelectSourcePicBinding
import com.zibete.proyecto1.ui.extensions.setTextIfChanged
import com.zibete.proyecto1.ui.main.CurrentScreen
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
            else editProfileViewModel.onError(
                UiText.StringRes(R.string.msg_camera_permission_required)
            )
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { editProfileViewModel.onPhotoSelected(it) }
        }

    private fun onGalleryClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    private fun onCameraClicked() {
        val granted =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
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

        setupInsets(view)

        binding.profilePhoto.setOnClickListener {
            val photoUrl = editProfileViewModel.uiState.value.photoUrl
                ?: DEFAULT_PROFILE_PHOTO_URL
            PhotoViewerActivity.startSingle(requireContext(), photoUrl)
        }

        binding.fabEditPhoto.setOnClickListener { showEditPhotoDialogInternal() }
        binding.pickerBirthDate.setOnClickListener { showMaterialDatePicker() }

        binding.fabSave.isEnabled = false
        binding.fabSave.setOnClickListener { editProfileViewModel.onSaveClicked() }

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

        setupOptionMenu()
    }

    private fun setupInsets(view: View) {
        fun apply(insets: WindowInsetsCompat) {
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, ime.bottom)

            // 1) FABs: que suban siempre por arriba de nav/gestos/teclado
            binding.linearFabs.updatePadding(bottom = bottomInset)

            // 2) Scroll: aseguramos que el contenido no quede debajo de los FABs
            // Necesitamos la altura real de los FABs, por eso usamos doOnLayout.
            binding.linearFabs.doOnLayout {
                val extraSpacing =
                    view.resources.getDimensionPixelSize(R.dimen.element_spacing_medium)
                binding.scrollable.updatePadding(
                    bottom = it.height + extraSpacing
                )
            }
        }

        // Listener de insets
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            apply(insets)
            insets
        }

        // Forzamos primer pase
        ViewCompat.requestApplyInsets(view)
    }

    private fun collectUi() {
//        binding.fabSkip.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    editProfileViewModel.uiState.collect { state ->

                        binding.fabSave.isEnabled = state.saveEnabled && !state.isSaving

                        binding.inputUserName.setTextIfChanged(state.name)
                        binding.pickerBirthDate.setTextIfChanged(editProfileViewModel.birthDateUi)
                        binding.inputDescription.setTextIfChanged(state.description)

                        binding.inputAge.setText(state.age?.toString().orEmpty())


//                        binding.fabSkip.isVisible = state.hasBirthDate

                        val toLoad: Any = state.photoPreviewUri
                            ?: state.photoUrl
                            ?: DEFAULT_PROFILE_PHOTO_URL

                        loadProfilePhoto(toLoad)

                        var lastScrollY = 0

                        binding.scrollable.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                            // Si la posición actual es mayor a la anterior, el usuario baja
                            if (scrollY > lastScrollY) {
                                binding.fabSave.shrink()
                                binding.fabEditPhoto.shrink()
                            } else {
                                binding.fabSave.extend()
                                binding.fabEditPhoto.extend()
                            }
                            lastScrollY = scrollY
                        }
                    }
                }

                launch {
                    editProfileViewModel.events.collect { event ->
                        when (event) {
                            is EditProfileUiEvent.ShowSnack -> {
                                UserMessageUtils.showSnack(
                                    root = binding.root,
                                    message = event.uiText.asString(requireContext()),
                                    type = event.type
                                )
                            }

                            is EditProfileUiEvent.OnBackToMain -> {
                                (activity as? MainActivity)?.mainViewModel?.emit(
                                    MainUiEvent.BackFromEditProfile
                                )
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
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingPhoto.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
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
                editProfileViewModel.onError(UiText.StringRes(R.string.msg_camera_error))
                return
            }

            takePictureLauncher.launch(uri)
        }.onFailure {
            editProfileViewModel.onError(UiText.StringRes(R.string.msg_camera_error))
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
            window?.setBackgroundDrawable(getDrawable(requireContext(), R.drawable.badge_round))
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
            .setTitleText(getString(R.string.birth_date))
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
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                    Unit

                override fun onPrepareMenu(menu: Menu) {
                    menu.findItem(R.id.action_settings)?.isVisible = true
                    menu.findItem(R.id.action_skip)?.isVisible = !editProfileViewModel.showSkip()
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
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

    val mainViewModel: MainViewModel by viewModels()

    fun setToolbarForEditProfile() {
        mainViewModel.setToolbarState(
            showBack = true,
            showSettings = true,
            currentScreen = CurrentScreen.EDIT_PROFILE
        )
    }

    override fun onResume() {
        super.onResume()
        setToolbarForEditProfile()
    }

    override fun onEditProfileWelcomeDismissed() {
        editProfileViewModel.markEditProfileWelcomeShown()
    }
}