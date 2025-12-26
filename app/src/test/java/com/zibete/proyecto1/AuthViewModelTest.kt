package com.zibete.proyecto1

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.domain.session.DeleteAccountResult
import com.zibete.proyecto1.ui.auth.AuthUiEvent
import com.zibete.proyecto1.ui.auth.AuthViewModel
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.DELETE_ACCOUNT_SUCCESS
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEmailLogin con email vacio emite warning y no carga loading`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false, deleteUser = false)
        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferencesProvider(prefsState),
            userPreferencesActions = FakeUserPreferencesActions(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val events = mutableListOf<AuthUiEvent>()
        val job = launch { vm.events.toList(events) }

        // When
        vm.onEmailLogin(email = "", password = "123")
        advanceUntilIdle()

        // Then
        assertTrue(events.first() is AuthUiEvent.ShowSnack)
        val snack = events.first() as AuthUiEvent.ShowSnack
        assertEquals(ERR_EMAIL_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `onEmailLogin con password vacio emite warning y no carga loading`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferencesProvider(prefsState),
            userPreferencesActions = FakeUserPreferencesActions(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val events = mutableListOf<AuthUiEvent>()
        val job = launch { vm.events.toList(events) }

        // When
        vm.onEmailLogin(email = "a@a.com", password = "")
        advanceUntilIdle()

        // Then
        val snack = events.first() as AuthUiEvent.ShowSnack
        assertEquals(ERR_PASSWORD_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `onEmailLogin success con usuario no nulo navega a splash`() = runTest {
        // Given
        val sessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }
        val sessionActions = FakeUserSessionActions().apply {
            // simulamos que el signIn no falla
            signInShouldFail = false
        }

        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val vm = AuthViewModel(
            userSessionProvider = sessionProvider,
            userSessionActions = sessionActions,
            userPreferencesProvider = FakeUserPreferencesProvider(prefsState),
            userPreferencesActions = FakeUserPreferencesActions(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val events = mutableListOf<AuthUiEvent>()
        val job = launch { vm.events.toList(events) }

        // When
        vm.onEmailLogin(email = "a@a.com", password = "123456")
        advanceUntilIdle()

        // Then
        assertTrue(events.contains(AuthUiEvent.NavigateToSplash))
        assertFalse(vm.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `onDeleteAccountClicked success muestra mensaje y setea deleteUser false`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false).apply {
            deleteUser = true
        }

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferencesProvider(prefsState),
            userPreferencesActions = FakeUserPreferencesActions(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val events = mutableListOf<AuthUiEvent>()
        val job = launch { vm.events.toList(events) }

        // When
        vm.onDeleteAccountClicked()
        advanceUntilIdle()

        // Then
        assertTrue(deleteUseCase.called)
        assertFalse(vm.uiState.value.deleteUser)
        assertFalse(vm.uiState.value.isLoading)

        val snackEvents = events.filterIsInstance<AuthUiEvent.ShowSnack>()
        assertTrue(snackEvents.any { it.message == DELETE_ACCOUNT_SUCCESS && it.type == ZibeSnackType.INFO })

        job.cancel()
    }
}
