package com.zibete.proyecto1.ui.editprofile

import LocalZibeExtendedColors
import android.Manifest
import android.content.ContentValues
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.EDIT_PROFILE_SCREEN
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.TimeUtils.isoToUiDate
import com.zibete.proyecto1.ui.components.ZibeAboutField
import com.zibete.proyecto1.ui.components.ZibeAnimatedQuotesCard
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.components.ZibePrimaryFAB
import com.zibete.proyecto1.ui.components.ZibeSecondaryFAB
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.main.CurrentScreen
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri

@Composable
fun EditProfileRoute(
    editProfileViewModel: EditProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val snackHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val mainActivity = context as? MainActivity

    var showWelcomeSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        editProfileViewModel.load()
        if (!editProfileViewModel.isEditProfileWelcomeShown()) {
            showWelcomeSheet = true
        }
    }

    LaunchedEffect(uiState.hasBirthDate) {
        val showSkipButton = uiState.hasBirthDate && runCatching {
            !editProfileViewModel.isEditProfileWelcomeShown()
        }.getOrDefault(false)

//        mainActivity?.mainViewModel?.setToolbarState(
//            showToolbar = true,
//            showBack = true,
//            showUsersFragmentSettings = false,
//            showBottomNav = false,
//            currentScreen = CurrentScreen.EDIT_PROFILE,
//            showSkipButton = showSkipButton
//        )
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
        snackBarHostState = snackHostState,
        photoModel = photoModel,
        onShowSnack = { text, type ->
            editProfileViewModel.showSnack(text, type)
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
    snackBarHostState: SnackbarHostState,
    photoModel: Any?,
    onShowSnack: (UiText, ZibeSnackType) -> Unit = { _, _ -> }
) {
    val zibeColors = LocalZibeExtendedColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by rememberSaveable { mutableStateOf(false) }

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
            onShowSnack(UiText.StringRes(R.string.err_camera_permission_denied), ZibeSnackType.ERROR)
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

    BackHandler(enabled = state.saveEnabled && !state.isSaving) {
        showDiscardDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { ZibeSnackbar(hostState = snackBarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ZibeSecondaryFAB(
                    text = { Text(stringResource(id = R.string.edit_picture)) },
                    icon = { Icon(Icons.Rounded.AddAPhoto, null) },
                    onClick = { showPhotoSourceSheet = true },
                    enabled = !state.isLoading && !state.isSaving
                )
                Spacer(modifier = Modifier.size(16.dp))
                ZibePrimaryFAB(
                    text = { Text(text = stringResource(id = R.string.action_save)) },
                    icon = { Icon(Icons.Rounded.Check, null) },
                    onClick = onSave,
                    enabled = state.saveEnabled,
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
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = dimensionResource(R.dimen.screen_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_medium))
            ) {
                ZibeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_xs))
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                ZibeInputField(
                                    value = state.birthDate.takeIf { it.isNotBlank() }
                                        ?.let { isoToUiDate(it) }
                                        .orEmpty(),
                                    onValueChange = { },
                                    label = stringResource(R.string.birth_date),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.EditCalendar,
                                            contentDescription = stringResource(R.string.content_description_edit_birthdate)
                                        )
                                    },
                                    enabled = !state.isLoading,
                                    readOnly = true
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .testTag(TestTags.BIRTHDATE_PICKER)
                                        .clickable { showDatePicker = true }
                                ) {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.content_description_edit_birthdate),
                                        tint = zibeColors.lightText.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = dimensionResource(R.dimen.element_spacing_small))
                                    )
                                }
                            }

                            ZibeInputField(
                                value = state.age?.toString().orEmpty(),
                                onValueChange = {},
                                label = stringResource(id = R.string.age),
                                enabled = false,
                                singleLine = true,
                                modifier = Modifier
                                    .width(80.dp)
                            )
                        }

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
        onDeleteClick = onPhotoDeleted,
        sheetState = photoSheetState
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    val today = Instant.now().toEpochMilli()
                    if (selectedDate != null && selectedDate > today) {
                        onShowSnack(UiText.StringRes(R.string.err_future_date), ZibeSnackType.WARNING)
                    } else {
                        selectedDate?.let { millis ->
                            onBirthDateChange(millisToIsoString(millis))
                        }
                        showDatePicker = false
                    }
                }) {
                    Text(stringResource(id = R.string.action_accept))
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.action_cancel))
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
                Text(stringResource(id = R.string.discard_changes_title))
            },
            text = {
                Text(stringResource(id = R.string.discard_changes_message))
            },
            confirmButton = {
                Button(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(id = R.string.action_exit))
                }
            },
            dismissButton = {
                Button(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(id = R.string.action_dont_discard))
                }
            }
        )
    }
}

private fun createCameraImageUri(context: android.content.Context): Uri? {
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
                saveEnabled = true
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onBirthDateChange = {},
            onPhotoSelected = {},
            onPhotoDeleted = {},
            onSave = {},
            onNavigateBack = {},
            snackBarHostState = SnackbarHostState(),
            photoModel = null
        )
    }
}
