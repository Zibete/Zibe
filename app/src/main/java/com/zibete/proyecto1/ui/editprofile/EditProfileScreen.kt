package com.zibete.proyecto1.ui.editprofile

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_SCREEN
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.core.utils.copyToTempFile
import com.zibete.proyecto1.ui.components.PhotoHeader
import com.zibete.proyecto1.ui.components.PhotoSourceBottomSheet
import com.zibete.proyecto1.ui.components.ZibeAboutField
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeBirthDatePickerDialog
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeCollapsingFabStack
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputBirthdate
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeMenuItem
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.media.rememberZibePhotoCropper
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@Composable
fun EditProfileRoute(
    editProfileViewModel: EditProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val state by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        editProfileViewModel.load()
    }

    LaunchedEffect(editProfileViewModel) {
        editProfileViewModel.events.collectLatest { event ->
            when (event) {
                is EditProfileUiEvent.NavigateBack -> onNavigateBack()
                is EditProfileUiEvent.NavigateToSettings -> onOpenSettings()
            }
        }
    }

    EditProfileWelcomeBottomSheet(
        isOpen = state.showWelcomeSheet,
        onDismiss = {
            editProfileViewModel.onWelcomeSheetDismissed()
        }
    )

    EditProfileScreen(
        state = state,
        onNameChange = editProfileViewModel::onNameChanged,
        onDescriptionChange = editProfileViewModel::onDescriptionChanged,
        onBirthDateChange = editProfileViewModel::onBirthDateChanged,
        onPhotoSelected = { uri ->
            val localUri = uri.copyToTempFile(context)
            if (localUri != null) {
                editProfileViewModel.onPhotoSelected(localUri)
            } else {
                editProfileViewModel.showSnack(
                    UiText.StringRes(R.string.err_zibe),
                    ZibeSnackType.ERROR
                )
            }
        },
        onPhotoDeleted = editProfileViewModel::onPhotoDeletedSetDefault,
        onSave = editProfileViewModel::onSaveClicked,
        onBackRequest = editProfileViewModel::onBackRequest,
        onDiscardDismiss = editProfileViewModel::onDiscardDialogDismiss,
        onDiscardConfirmExit = editProfileViewModel::onDiscardDialogConfirmExit,
        onSettingsRequest = editProfileViewModel::onSettingsRequest,
        photoModel = editProfileViewModel.resolveProfilePhotoToLoad(),
        onShowSnack = { uiText, snackType ->
            editProfileViewModel.showSnack(uiText, snackType)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onPhotoDeleted: () -> Unit,
    onSave: () -> Unit,
    onBackRequest: () -> Unit,
    onDiscardDismiss: () -> Unit,
    onDiscardConfirmExit: () -> Unit,
    onSettingsRequest: () -> Unit,
    photoModel: Any?,
    onShowSnack: (UiText, ZibeSnackType) -> Unit = { _, _ -> }
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current
    val context = LocalContext.current
    val resolvedPhoto = photoModel?.toString().orEmpty()

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeVisible = imeBottomDp > 0.dp
    val collapseThresholdPx = with(density) { 56.dp.toPx() }
    val isFabCollapsed = isImeVisible || scrollState.value.toFloat() > collapseThresholdPx

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }

    var fabHeightPx by remember { mutableIntStateOf(0) }
    val fabHeightDp = with(density) { fabHeightPx.toDp() }
    val bottomSpacerTarget = fabHeightDp + dimensionResource(R.dimen.element_spacing_medium)
    val bottomSpacer by animateDpAsState(bottomSpacerTarget, label = "fabSpacer")

    val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val cropper = rememberZibePhotoCropper(
        onCropped = { croppedUri ->
            onPhotoSelected(croppedUri)
        },
        onError = { e ->
            onShowSnack(
                e.message.toUiText(R.string.err_zibe_prefix, R.string.err_zibe),
                ZibeSnackType.ERROR
            )
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { cropper.launch(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUriString?.let { cropper.launch(it.toUri()) }
        } else {
            pendingCameraUriString?.let { uriString ->
                context.contentResolver.delete(uriString.toUri(), null, null)
            }
        }
        pendingCameraUriString = null
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCameraImageUri(context)
            if (uri == null) {
                onShowSnack(
                    UiText.StringRes(R.string.err_zibe),
                    ZibeSnackType.ERROR
                )
                return@rememberLauncherForActivityResult
            }
            pendingCameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            onShowSnack(
                UiText.StringRes(R.string.err_camera_permission_denied),
                ZibeSnackType.ERROR
            )
        }
    }

    val launchCamera = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                val uri = createCameraImageUri(context)
                if (uri == null) {
                    onShowSnack(
                        UiText.StringRes(R.string.err_zibe),
                        ZibeSnackType.ERROR
                    )
                } else {
                    pendingCameraUriString = uri.toString()
                    cameraLauncher.launch(uri)
                }
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val menuSettings = stringResource(R.string.menu_settings)
    val menuDeletePhoto = stringResource(R.string.action_delete_photo)

    val menuItems = remember(state.photoUrl, state.photoPreviewUri, state.deletePhoto) {
        val items = mutableListOf<ZibeMenuItem>()
        items.add(
            ZibeMenuItem(
                label = menuSettings,
                onClick = {
                    onSettingsRequest()
                },
                icon = Icons.Rounded.Settings
            )
        )

        val hasPhoto =
            state.photoPreviewUri != null || (state.photoUrl != null && !state.deletePhoto)
        if (hasPhoto) {
            items.add(
                ZibeMenuItem(
                    label = menuDeletePhoto,
                    onClick = onPhotoDeleted,
                    icon = Icons.Rounded.Delete
                )
            )
        }
        items
    }

    val saveEnabled =
        state.hasPendingChanges &&
                !state.isLoading &&
                !state.isSaving &&
                !state.isValidating &&
                state.nameError == null &&
                state.birthDateError == null

    BackHandler { onBackRequest() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(zibeColors.gradientZibe)
            .testTag(EDIT_PROFILE_SCREEN)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ZibeToolbar(
                    title = stringResource(R.string.menu_edit_profile),
                    onBack = onBackRequest,
                    menuItems = menuItems,
                    showSkipButton = state.showSkipButton
                )
            },
            floatingActionButton = {
                ZibeCollapsingFabStack(
                    collapsed = isFabCollapsed,
                    primaryText = {
                        Text(
                            text = stringResource(id = R.string.action_save),
                            style = zibeTypography.label
                        )
                    },
                    primaryIcon = { Icon(Icons.Rounded.Check, null) },
                    primaryEnabled = saveEnabled,
                    primaryLoading = state.isSaving,
                    onPrimaryClick = onSave,
                    secondaryText = {
                        Text(
                            stringResource(id = R.string.edit_picture),
                            style = zibeTypography.label
                        )
                    },
                    secondaryIcon = { Icon(Icons.Rounded.AddAPhoto, null) },
                    secondaryEnabled = !state.isLoading && !state.isSaving,
                    onSecondaryClick = { showPhotoSourceSheet = true },
                    onHeightPxChanged = { fabHeightPx = it }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_padding),
                        vertical = dimensionResource(R.dimen.screen_padding)
                    )
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    )
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_medium))
            ) {
                PhotoHeader(
                    photoUrl = resolvedPhoto,
                    isLoading = state.isLoading,
                    onClick = {
                        if (resolvedPhoto.isNotBlank()) {
                            PhotoViewerActivity.startSingle(context, resolvedPhoto)
                        }
                    }
                )

                ZibeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_xs))
                    ) {
                        ZibeInputField(
                            value = state.name,
                            onValueChange = onNameChange,
                            label = stringResource(id = R.string.name),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null
                                )
                            },
                            enabled = !state.isSaving && !state.isLoading,
                            error = state.nameError?.asString(context),
                            singleLine = true
                        )

                        ZibeInputBirthdate(
                            value = state.birthDate.takeIf { it.isNotBlank() }
                                ?.let { isoToUiDate(it) }.orEmpty(),
                            age = state.age?.toString().orEmpty(),
                            onClick = { if (!state.isLoading) showDatePicker = true },
                            error = state.birthDateError?.asString(context),
                            enabled = !state.isLoading
                        )

                        ZibeAboutField(
                            value = state.description,
                            onValueChange = onDescriptionChange,
                            label = stringResource(id = R.string.description),
                            enabled = !state.isSaving && !state.isLoading,
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                            },
                            containerColor = zibeColors.contentLightBg
                        )
                    }
                }

                ZibeAnimatedQuotesCard(
                    strings = listOf(
                        stringResource(id = R.string.edit_profile_hints_text),
                        stringResource(id = R.string.signup_animated_message_1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(bottomSpacer))
            }
        }
    }

    PhotoSourceBottomSheet(
        isOpen = showPhotoSourceSheet,
        onDismiss = { showPhotoSourceSheet = false },
        onCameraClick = { launchCamera() },
        onGalleryClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onDeleteClick = if (state.photoUrl != null || state.photoPreviewUri != null) onPhotoDeleted else null,
        sheetState = photoSheetState
    )

    ZibeBirthDatePickerDialog(
        isOpen = showDatePicker,
        initialIso = state.birthDate.takeIf { it.isNotBlank() },
        onDismiss = { showDatePicker = false },
        onConfirmIso = { iso ->
            onBirthDateChange(iso)
        },
        showModeToggle = false
    )

    if (state.showDiscardDialog) {
        ZibeDialog(
            title = stringResource(id = R.string.discard_changes_title),
            content = { Text(stringResource(id = R.string.discard_changes_message)) },
            confirmText = stringResource(id = R.string.action_exit),
            cancelText = stringResource(id = R.string.action_dont_discard),
            onConfirm = onDiscardConfirmExit,
            onCancel = onDiscardDismiss
        )
    }
}

private fun createCameraImageUri(context: Context): Uri? {
    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
    if (!picturesDir.exists() && !picturesDir.mkdirs()) return null
    return runCatching {
        val file = File.createTempFile("profile_camera_", ".jpg", picturesDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    ZibeTheme {
        EditProfileScreen(
            state = EditProfileUiState(
                isLoading = false,
                name = "John Doe",
                description = "This is a sample description",
                age = 25,
                hasPendingChanges = true
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onBirthDateChange = {},
            onPhotoSelected = {},
            onPhotoDeleted = {},
        onSave = {},
        photoModel = null,
        onShowSnack = { _, _ -> },
        onBackRequest = {},
            onDiscardDismiss = {},
            onDiscardConfirmExit = {},
            onSettingsRequest = {},
        )
    }
}
