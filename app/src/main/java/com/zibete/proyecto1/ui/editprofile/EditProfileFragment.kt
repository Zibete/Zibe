package com.zibete.proyecto1.ui.editprofile

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags.BIRTHDATE_PICKER
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_WELCOME_SHEET
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.SimpleWatcher
import com.zibete.proyecto1.core.utils.TimeUtils.isoToMillis
import com.zibete.proyecto1.core.utils.TimeUtils.millisToIso
import com.zibete.proyecto1.databinding.FragmentEditProfileBinding
import com.zibete.proyecto1.ui.extensions.setTextIfChanged
import com.zibete.proyecto1.ui.media.PhotoSourceSheet
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Deprecated("Migrado a Compose. Usar EditProfileComposeFragment en su lugar.")
@AndroidEntryPoint
class EditProfileFragment : Fragment(), EditProfileWelcomeSheet.Listener {

    private val editProfileViewModel: EditProfileViewModel by viewModels()

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
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
            else editProfileViewModel.showSnack(UiText.StringRes(R.string.msg_camera_permission_required))
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult
            val outputUri = UCrop.getOutput(data)

            if (outputUri != null) {
                editProfileViewModel.onPhotoSelected(outputUri)
            } else {
                val e = UCrop.getError(data)
                editProfileViewModel.showSnack(
                    e?.message.toUiText(
                        R.string.err_zibe_prefix,
                        R.string.err_zibe
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

        setupListeners()

        if (savedInstanceState == null) editProfileViewModel.load()

        setupInsets(view)

        installScrollBehavior()

        collectUi()

        showWelcomeIfNeeded()
    }

    override fun onDestroyView() {
        pendingCameraUri = null
        _binding = null
        super.onDestroyView()
    }

    // --------------------------------------------
    // UI setup
    // --------------------------------------------

    private fun setupListeners() {
        binding.profilePhoto.setOnClickListener {
            val toLoad = editProfileViewModel.resolveProfilePhotoToLoad()
            PhotoViewerActivity.startSingle(requireContext(), toLoad.toString())
        }

        parentFragmentManager.setFragmentResultListener(
            REQ_KEY_EDIT_PROFILE,
            viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(PhotoSourceSheet.RES_ACTION)) {
                PhotoSourceSheet.ACTION_CAMERA -> onCameraClicked()
                PhotoSourceSheet.ACTION_GALLERY -> onGalleryClicked()
                PhotoSourceSheet.ACTION_DELETE -> editProfileViewModel.onPhotoDeletedSetDefault()
            }
        }

        binding.fabEditPhoto.setOnClickListener {
            PhotoSourceSheet.newInstance(
                showDelete = true,
                titleRes = R.string.edit_picture,
                requestKey = REQ_KEY_EDIT_PROFILE
            ).show(parentFragmentManager, PhotoSourceSheet.TAG)
        }
        binding.pickerBirthDate.setOnClickListener { showMaterialDatePicker() }
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

        binding.inputDescription.makeScrollableInsideScroll()
    }

    fun EditText.makeScrollableInsideScroll() {
        setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
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
                        val saving = state.isSaving
                        binding.fabSaveLoading.isVisible = saving
                        binding.fabSave.isEnabled = state.hasPendingChanges && !saving
                        binding.fabSave.text = if (saving) "" else getString(R.string.action_save)
                        binding.fabSave.icon = if (saving) null else ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_check_24
                        )


                        binding.fabEditPhoto.isEnabled = !state.isSaving && !state.isLoading
                        binding.pickerBirthDate.isEnabled = !state.isSaving && !state.isLoading
                        binding.inputUserName.isEnabled = !state.isSaving && !state.isLoading
                        binding.inputDescription.isEnabled = !state.isSaving && !state.isLoading

                        binding.inputUserName.setTextIfChanged(state.name)
                        binding.pickerBirthDate.setTextIfChanged(editProfileViewModel.uiState.value.birthDate)
                        binding.inputDescription.setTextIfChanged(state.description)
                        binding.inputAge.setTextIfChanged(state.age?.toString().orEmpty())

                        val toLoad = editProfileViewModel.resolveProfilePhotoToLoad()
                        maybeLoadProfilePhoto(toLoad)
                    }
                }

                launch {
                    editProfileViewModel.events.collect { event ->
                        when (event) {
                            is EditProfileUiEvent.NavigateBack -> {
                                (activity as? EditProfileExitHandler)?.onExitEditProfile()
                                    ?: requireActivity().onBackPressedDispatcher.onBackPressed()
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

    private fun maybeLoadProfilePhoto(model: Any?) {
        if (lastLoadedPhotoModel == model) return
        lastLoadedPhotoModel = model
        loadProfilePhoto(model)
    }

    private fun loadProfilePhoto(model: Any?) {
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
                editProfileViewModel.showSnack(UiText.StringRes(R.string.msg_camera_error))
                return
            }

            takePictureLauncher.launch(uri)
        }.onFailure {
            editProfileViewModel.showSnack(UiText.StringRes(R.string.msg_camera_error))
        }
    }

    private fun startCrop(source: Uri) {
        val destFile = java.io.File(
            requireContext().cacheDir,
            "profile_crop_${System.currentTimeMillis()}.jpg"
        )
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setAspectRatioOptions(
                0,
                AspectRatio("Original", 0f, 0f),
                AspectRatio("3:4", 3f, 4f),
                AspectRatio("9:16", 9f, 16f)
            )
        }

        val uCrop = UCrop.of(source, destUri)
            .withOptions(options)
            .useSourceImageAspectRatio()
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

        val iso = editProfileViewModel.uiState.value.birthDate.trim()
        val selection = iso.takeIf { it.isNotBlank() }?.let { isoToMillis(it) }
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.birth_date))
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ThemeOverlay_Zibe_MaterialCalendar)
            .setSelection(selection)
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

            EditProfileWelcomeSheet().show(childFragmentManager, EDIT_PROFILE_WELCOME_SHEET)
        }
    }

    fun hasPendingChanges(): Boolean = editProfileViewModel.uiState.value.hasPendingChanges

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
        const val REQ_KEY_EDIT_PROFILE = "photo_source_edit_profile"
    }
}
