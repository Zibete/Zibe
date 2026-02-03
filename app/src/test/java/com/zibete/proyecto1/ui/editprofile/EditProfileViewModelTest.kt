package com.zibete.proyecto1.ui.editprofile

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.testing.TestScenario
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val scenario = TestScenario()
    private val userPreferencesProvider = FakeUserPreferencesProvider { scenario }
    private val userPreferencesActions = FakeUserPreferencesActions { scenario }
    private val userRepositoryProvider = FakeUserRepositoryProvider { scenario }
    private val updateProfileUseCase = mockk<UpdateProfileUseCase>(relaxed = true)
    private val snackBarManager = SnackBarManager()
    private val config = SettingsConfig(navigationDelay = 0L, validationDebounce = 0L)

    @Test
    fun backAfterPhotoSelected_opensDiscardDialog() = runTest {
        val vm = buildVm()

        vm.load()
        advanceUntilIdle()
        vm.onPhotoSelected(mockk(relaxed = true))
        assertTrue(vm.uiState.value.hasPendingChanges)

        vm.onBackRequest()

        assertTrue(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun backAfterPhotoDeleted_opensDiscardDialog() = runTest {
        val vm = buildVm()

        vm.load()
        advanceUntilIdle()
        vm.onPhotoDeletedSetDefault()
        assertTrue(vm.uiState.value.hasPendingChanges)

        vm.onBackRequest()

        assertTrue(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun deleteAfterPhotoSelected_discardsPreviewAndKeepsRemote() = runTest {
        val vm = buildVm()

        vm.load()
        advanceUntilIdle()

        vm.onPhotoSelected(mockk(relaxed = true))
        vm.onPhotoDeletedSetDefault()

        assertFalse(vm.uiState.value.hasPendingChanges)
        assertFalse(vm.uiState.value.deletePhoto)
        assertTrue(vm.uiState.value.photoPreviewUri == null)
    }

    private fun buildVm(): EditProfileViewModel = EditProfileViewModel(
        userPreferencesProvider = userPreferencesProvider,
        userPreferencesActions = userPreferencesActions,
        userRepositoryProvider = userRepositoryProvider,
        updateProfileUseCase = updateProfileUseCase,
        snackBarManager = snackBarManager,
        config = config
    )
}
