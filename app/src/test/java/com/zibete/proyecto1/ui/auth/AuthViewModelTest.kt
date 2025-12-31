package com.zibete.proyecto1.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.domain.session.DeleteAccountResult
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferences
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.DELETE_ACCOUNT_SUCCESS
import com.zibete.proyecto1.ui.constants.DO_NOT_DELETE_ACCOUNT
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.ui.constants.RESET_PASSWORD_EMAIL_INSTRUCTION
import com.zibete.proyecto1.ui.constants.resetPasswordError
import com.zibete.proyecto1.ui.constants.resetPasswordSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEmailLogin con email vacio emite warning y no carga loading`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false,
            deleteUser = false
        )
        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(email = "", password = TestData.PASSWORD)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(ERR_EMAIL_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onEmailLogin con password vacio emite warning y no carga loading`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(email = TestData.EMAIL, password = "")
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(ERR_PASSWORD_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onEmailLogin success con usuario no nulo navega a splash`() = runTest {
        val sessionProvider = mock<UserSessionProvider>()
        val sessionActions = mock<UserSessionActions>()

        val firebaseUser = mock<FirebaseUser>()
        whenever(firebaseUser.uid).thenReturn(TestData.UID)
        whenever(sessionProvider.currentUser).thenReturn(firebaseUser)

        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val vm = AuthViewModel(
            userSessionProvider = sessionProvider,
            userSessionActions = sessionActions,
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val deferred = backgroundScope.async { awaitEvent(vm) }
        runCurrent()

        vm.onEmailLogin(email = TestData.EMAIL, password = TestData.PASSWORD)
        advanceUntilIdle()

        val event = deferred.await()
        assertEquals(AuthUiEvent.NavigateToSplash, event)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `login falla emite snack`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)

        val sessionActions = FakeUserSessionActions().apply {
            signInShouldFail = true
            signInFailure = RuntimeException("boom")
        }

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = sessionActions,
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(TestData.EMAIL, TestData.PASSWORD)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        assertEquals(ZibeSnackType.ERROR, (event as AuthUiEvent.ShowSnack).type)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onDeleteAccountClicked success muestra mensaje y setea deleteUser false`() = runTest {
        // Given
        val prefsState =
            FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false).apply {
                deleteUser = true
            }

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onDeleteAccountClicked()
        advanceUntilIdle()

        // Then
        assertTrue(deleteUseCase.called)

        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(DELETE_ACCOUNT_SUCCESS, snack.message)
        assertEquals(ZibeSnackType.INFO, snack.type)

        assertFalse(vm.uiState.value.deleteUser)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onDoNotDeleteAccountClicked muestra mensaje y setea deleteUser false`() = runTest {
        // Given
        val prefsState =
            FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false).apply {
                deleteUser = true
            }

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onDoNotDeleteAccountClicked()
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(DO_NOT_DELETE_ACCOUNT, snack.message)
        assertEquals(ZibeSnackType.INFO, snack.type)

        assertFalse(vm.uiState.value.deleteUser)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `reset password con email vacio emite snack`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword("")
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(RESET_PASSWORD_EMAIL_INSTRUCTION, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
    }

    @Test
    fun `reset password con email OK reslut failure emite snack`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val sessionActions = FakeUserSessionActions().apply {
            resetShouldFail = true
            resetFailure = RuntimeException("boom")
        }

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = sessionActions,
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(resetPasswordError(TestData.EMAIL), snack.message)
        assertEquals(ZibeSnackType.ERROR, snack.type)
    }

    @Test
    fun `reset password con email OK reslut success emite snack`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)

        val deleteUseCase = FakeDeleteAccountUseCase(
            result = DeleteAccountResult.Success
        )

        val sessionActions = FakeUserSessionActions().apply {
            resetShouldFail = false
        }

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = sessionActions,
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = deleteUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(resetPasswordSuccess(TestData.EMAIL), snack.message)
        assertEquals(ZibeSnackType.SUCCESS, snack.type)
    }

    @Test
    fun `onNavigateToSignUpClicked navega a signup`() = runTest {
        // Given
        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)

        val vm = AuthViewModel(
            userSessionProvider = FakeUserSessionProvider(),
            userSessionActions = FakeUserSessionActions(),
            userPreferencesProvider = FakeUserPreferences(prefsState),
            userPreferencesActions = FakeUserPreferences(prefsState),
            deleteAccountUseCase = FakeDeleteAccountUseCase()
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onNavigateToSignUpClicked()
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        assertEquals(AuthUiEvent.NavigateToSignUp, event)
        assertFalse(vm.uiState.value.isLoading)
    }

    private suspend fun awaitEvent(vm: AuthViewModel): AuthUiEvent {
        return withTimeout(2_000) { vm.events.first() }
    }

}