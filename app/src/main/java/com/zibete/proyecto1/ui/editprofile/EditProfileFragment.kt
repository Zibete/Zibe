package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private var lastLoadedPhotoModel: Any? = null

    // Insets/FABs
    private var fabsHeightPx: Int = 0
    private var fabsBaseBottomPadding: Int = 0
    private var lastScrollY: Int = 0
    private var scrollListenerInstalled = false

    // --------------------------------------------
    // Activity Result Launchers
    // --------------------------------------------

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                pendingCameraUri?.let { startCrop(it) }
            }
        }

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else editProfileViewModel.onError(UiText.StringRes(R.string.msg_camera_permission_required))
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult
            val outputUri = com.yalantis.ucrop.UCrop.getOutput(data)

            if (outputUri != null) {
                editProfileViewModel.onPhotoSelected(outputUri)
            } else {
                val error = com.yalantis.ucrop.UCrop.getError(data)
                editProfileViewModel.onError(
                    UiText.StringRes(
                        R.string.err_zibe_prefix,
                        args = listOf(error?.message.orEmpty())
                    )
                )
            }
        }

    // --------------------------------------------
    // Lifecycle
    // --------------------------------------------

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

        setToolbarForEditProfile()
        setupInsets(view)
        setupListeners(savedInstanceState)
        installScrollBehavior()
        collectUi()
        showWelcomeIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        setToolbarForEditProfile()
    }

    override fun onDestroyView() {
        editPhotoDialog?.dismiss()
        editPhotoDialog = null
        pendingCameraUri = null
        _binding = null
        super.onDestroyView()
    }

    // --------------------------------------------
    // UI setup
    // --------------------------------------------

    private fun setupListeners(savedInstanceState: Bundle?) {
        binding.profilePhoto.setOnClickListener {
            val state = editProfileViewModel.uiState.value
            val model: Any = state.photoPreviewUri
                ?: state.photoUrl
                ?: DEFAULT_PROFILE_PHOTO_URL

            // Si PhotoViewerActivity hoy acepta String:
            PhotoViewerActivity.startSingle(requireContext(), model.toString())
        }


        binding.fabEditPhoto.setOnClickListener { showEditPhotoDialogInternal() }
        binding.pickerBirthDate.setOnClickListener { showMaterialDatePicker() }

        binding.fabSave.isEnabled = false
        binding.fabSave.setOnClickListener { editProfileViewModel.onSaveClicked() }

        binding.inputUserName.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onNameChanged(
                it
            )
        })
        binding.inputDescription.addTextChangedListener(SimpleWatcher {
            editProfileViewModel.onDescriptionChanged(
                it
            )
        })

        if (savedInstanceState == null) {
            editProfileViewModel.load()
        }
    }

    private fun installScrollBehavior() {
        if (scrollListenerInstalled) return
        scrollListenerInstalled = true

        binding.scrollable.setOnScrollChangeListener { _, _, scrollY, _, _ ->
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

    private fun setupInsets(view: View) {
        val extraSpacingPx = view.resources.getDimensionPixelSize(R.dimen.element_spacing_medium)

        fabsBaseBottomPadding = binding.linearFabs.paddingBottom

        binding.linearFabs.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val newHeight = v.height
            if (newHeight != fabsHeightPx) {
                fabsHeightPx = newHeight
                ViewCompat.requestApplyInsets(view)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(sys.bottom, ime.bottom)

            binding.linearFabs.updatePadding(bottom = fabsBaseBottomPadding + bottomInset)
            binding.scrollable.updatePadding(bottom = bottomInset + fabsHeightPx + extraSpacingPx)

            insets
        }

        ViewCompat.requestApplyInsets(view)
    }

    // --------------------------------------------
    // Collectors
    // --------------------------------------------

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    editProfileViewModel.uiState.collect { state ->
                        binding.fabSave.isEnabled = state.saveEnabled && !state.isSaving

                        binding.inputUserName.setTextIfChanged(state.name)
                        binding.pickerBirthDate.setTextIfChanged(editProfileViewModel.birthDateUi)
                        binding.inputDescription.setTextIfChanged(state.description)
                        binding.inputAge.setTextIfChanged(state.age?.toString().orEmpty())

                        val toLoad: Any = state.photoPreviewUri
                            ?: state.photoUrl
                            ?: DEFAULT_PROFILE_PHOTO_URL

                        maybeLoadProfilePhoto(toLoad)
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
                                (activity as? MainActivity)?.mainViewModel?.emit(MainUiEvent.BackFromEditProfile)
                            }
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------
    // Image loading (Glide)
    // --------------------------------------------

    private fun maybeLoadProfilePhoto(model: Any) {
        if (lastLoadedPhotoModel == model) return
        lastLoadedPhotoModel = model
        loadProfilePhoto(model)
    }

    private fun loadProfilePhoto(model: Any) {
        binding.circularLoading.isVisible = true

        Glide.with(this)
            .load(model)
            .centerCrop()
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.circularLoading.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.circularLoading.isVisible = false
                    return false
                }
            })
            .into(binding.profilePhoto)
    }

    // --------------------------------------------
    // Dialog / Pickers
    // --------------------------------------------

    private fun showEditPhotoDialogInternal() {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_Zibe_AlertDialog)
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

    private fun onGalleryClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
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

    private fun startCrop(source: Uri) {
        val destFile = java.io.File(
            requireContext().cacheDir,
            "profile_crop_${System.currentTimeMillis()}.jpg"
        )
        val destUri = Uri.fromFile(destFile)

        val uCrop = com.yalantis.ucrop.UCrop.of(source, destUri)
            // Elegí lo que quieras:
            .withAspectRatio(3f, 4f) // consistente con tu ratio 3:4
            .withMaxResultSize(1440, 1920) // evita bitmaps gigantes

        val intent = uCrop.getIntent(requireContext()).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        cropLauncher.launch(intent)
    }

    private fun showMaterialDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.birth_date))
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ThemeOverlay_Zibe_MaterialCalendar)
            .setSelection(
                editProfileViewModel.uiState.value.birthDate
                    .let { iso -> isoToMillis(iso) }
                    ?: MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            editProfileViewModel.onBirthDateChanged(millisToIso(millis))
        }

        picker.show(parentFragmentManager, BIRTHDATE_PICKER)
    }

    // --------------------------------------------
    // Welcome + toolbar
    // --------------------------------------------

    private fun showWelcomeIfNeeded() {
        viewLifecycleOwner.lifecycleScope.launch {
            val shown = editProfileViewModel.isEditProfileWelcomeShown()
            if (shown) return@launch
            if (parentFragmentManager.findFragmentByTag(EDIT_PROFILE_WELCOME_SHEET) != null) return@launch

            EditProfileWelcomeSheet().show(parentFragmentManager, EDIT_PROFILE_WELCOME_SHEET)
        }
    }

    fun hasPendingChanges(): Boolean = editProfileViewModel.uiState.value.saveEnabled

    private fun setToolbarForEditProfile() {
        (activity as? MainActivity)?.mainViewModel?.setToolbarState(
            showToolbar = true,
            showBack = true,
            showUsersFragmentSettings = false,
            showBottomNav = false,
            currentScreen = CurrentScreen.EDIT_PROFILE
        )
    }

    // --------------------------------------------
    // State restore (camera URI)
    // --------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PENDING_CAMERA_URI, pendingCameraUri)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        pendingCameraUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState?.getParcelable(KEY_PENDING_CAMERA_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState?.getParcelable(KEY_PENDING_CAMERA_URI)
            }
    }

    override fun onEditProfileWelcomeDismissed() {
        editProfileViewModel.markEditProfileWelcomeShown()
    }

    private companion object {
        const val KEY_PENDING_CAMERA_URI = "pending_camera_uri"
    }
}