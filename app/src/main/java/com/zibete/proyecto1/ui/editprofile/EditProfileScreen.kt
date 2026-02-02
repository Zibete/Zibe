package com.zibete.proyecto1.ui.editprofile

import LocalZibeExtendedColors
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_SCREEN
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.ui.components.ZibeAboutField
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeInputBirthdate
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibeMenuItem
import com.zibete.proyecto1.ui.components.ZibePrimaryFAB
import com.zibete.proyecto1.ui.components.ZibeSecondaryFAB
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun EditProfileRoute(
    editProfileViewModel: EditProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val snackHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showWelcomeSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        editProfileViewModel.load()
        if (!editProfileViewModel.isEditProfileWelcomeShown()) {
            showWelcomeSheet = true
        }
    }

    LaunchedEffect(Unit) {
        editProfileViewModel.snackBarEvents.collectLatest { event ->
            snackHostState.showZibeMessage(
                message = event.uiText.asString(context),
                type = event.type
            )
        }
    }

    LaunchedEffect(editProfileViewModel) {
        editProfileViewModel.events.collectLatest { event ->
            when (event) {
                is EditProfileUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    EditProfileWelcomeBottomSheet(
        isOpen = showWelcomeSheet,
        onDismiss = {
            showWelcomeSheet = false
            editProfileViewModel.markEditProfileWelcomeShown()
        }
    )

    val photoModel = editProfileViewModel.resolveProfilePhotoToLoad()

    EditProfileScreen(
        state = uiState,
        onNameChange = editProfileViewModel::onNameChanged,
        onDescriptionChange = editProfileViewModel::onDescriptionChanged,
        onBirthDateChange = editProfileViewModel::onBirthDateChanged,
        onPhotoSelected = editProfileViewModel::onPhotoSelected,
        onPhotoDeleted = editProfileViewModel::onPhotoDeletedSetDefault,
        onSave = editProfileViewModel::onSaveClicked,
        onNavigateBack = onNavigateBack,
        onBackRequest = editProfileViewModel::onBackRequest,
        onOpenSettings = onOpenSettings,
        snackBarHostState = snackHostState,
        photoModel = photoModel,
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
    onNavigateBack: () -> Unit,
    onBackRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    snackBarHostState: SnackbarHostState,
    photoModel: Any?,
    onShowSnack: (UiText, ZibeSnackType) -> Unit = { _, _ -> }
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current
    val context = LocalContext.current

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }

    var fabHeightPx by remember { mutableIntStateOf(0) }
    val fabHeightDp = with(LocalDensity.current) { fabHeightPx.toDp() }
    val extraBottomMargin = dimensionResource(R.dimen.element_spacing_xl)

    val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUriString?.let { onPhotoSelected(it.toUri()) }
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
            pendingCameraUriString = uri?.toString()
            uri?.let { cameraLauncher.launch(it) }
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
                pendingCameraUriString = uri?.toString()
                uri?.let { cameraLauncher.launch(it) }
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val birthDateMillis = remember(state.birthDate) {
        runCatching {
            Instant.parse(state.birthDate).toEpochMilli()
        }.getOrNull()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = birthDateMillis ?: Instant.now().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Instant.now().toEpochMilli()
            }
        }
    )

    fun millisToIsoString(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)


    BackHandler { onBackRequest() }

    val menuSettings = stringResource(R.string.menu_settings)
    val menuDeletePhoto = stringResource(R.string.action_delete_photo)

    val menuItems = remember(state.photoUrl, state.photoPreviewUri, state.deletePhoto) {
        val items = mutableListOf<ZibeMenuItem>()

        items.add(
            ZibeMenuItem(
                label = menuSettings,
                onClick = {
                    onOpenSettings()
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

    val saveEnabled by remember(
        state.isLoading,
        state.isSaving,
        state.hasPendingChanges,
        state.birthDateError,
        state.nameError
    ) {
        derivedStateOf {
            state.hasPendingChanges &&
                    !state.isLoading &&
                    !state.isSaving &&
                    state.nameError == null &&
                    state.birthDateError == null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            ZibeToolbar(
                title = stringResource(R.string.menu_edit_profile),
                onBack = onNavigateBack,
                menuItems = menuItems
            )
        },
        snackbarHost = { ZibeSnackbar(hostState = snackBarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.onSizeChanged { fabHeightPx = it.height }
            ) {
                ZibeSecondaryFAB(
                    text = {
                        Text(
                            stringResource(id = R.string.edit_picture),
                            style = zibeTypography.label
                        )
                    },
                    icon = { Icon(Icons.Rounded.AddAPhoto, null) },
                    onClick = { showPhotoSourceSheet = true },
                    enabled = !state.isLoading && !state.isSaving
                )
                Spacer(modifier = Modifier.size(16.dp))
                ZibePrimaryFAB(
                    text = {
                        Text(
                            text = stringResource(id = R.string.action_save),
                            style = zibeTypography.label,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Rounded.Check, null) },
                    onClick = onSave,
                    enabled = saveEnabled,
                    isLoading = state.isSaving
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(zibeColors.gradientZibe)
                .testTag(EDIT_PROFILE_SCREEN)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_padding),
                        vertical = dimensionResource(R.dimen.screen_padding)
                    )
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_medium))
            ) {
                ZibeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        val painter = rememberAsyncImagePainter(model = photoModel)
                        Image(
                            painter = painter,
                            contentDescription = stringResource(id = R.string.content_description_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    photoModel?.let {
                                        PhotoViewerActivity.startSingle(context, it.toString())
                                    }
                                }
                        )
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }

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
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            enabled = !state.isSaving && !state.isLoading,
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    val today = Instant.now().toEpochMilli()
                    if (selectedDate != null && selectedDate > today) {
                        onShowSnack(
                            UiText.StringRes(R.string.err_future_date),
                            ZibeSnackType.WARNING
                        )
                    } else {
                        selectedDate?.let { millis ->
                            onBirthDateChange(millisToIsoString(millis))
                        }
                        showDatePicker = false
                    }
                }) {
                    Text(
                        stringResource(id = R.string.action_accept),
                        style = zibeTypography.label,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.action_cancel), style = zibeTypography.label)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(stringResource(id = R.string.discard_changes_title), style = zibeTypography.h2)
            },
            text = {
                Text(
                    stringResource(id = R.string.discard_changes_message),
                    style = zibeTypography.body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text(
                        stringResource(id = R.string.action_exit),
                        color = zibeColors.snackRed,
                        style = zibeTypography.label,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(
                        stringResource(id = R.string.action_dont_discard),
                        style = zibeTypography.label
                    )
                }
            }
        )
    }
}

private fun createCameraImageUri(context: Context): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis().toString())
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )
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
            onNavigateBack = {},
            snackBarHostState = SnackbarHostState(),
            photoModel = null,
            onShowSnack = { _, _ -> },
            onBackRequest = {},
            onOpenSettings = {},
        )
    }
}
