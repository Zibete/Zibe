package com.zibete.proyecto1.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.constants.DELETE_ACCOUNT_SUCCESS
import com.zibete.proyecto1.core.constants.DO_NOT_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.core.constants.resetPasswordError
import com.zibete.proyecto1.core.constants.resetPasswordSuccess
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeGoogleSignInUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.UnitScenario
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onEmailLogin with empty email emit snack and set isLoading false`() = runTest {
        // Given
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(
            email = "",
            password = TestData.PASSWORD
        )
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack

        assertEquals(ERR_EMAIL_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onEmailLogin with empty password emit snack and set isLoading false`() = runTest {
        // Given
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(
            email = TestData.EMAIL,
            password = ""
        )
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack

        assertEquals(ERR_PASSWORD_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onEmailLogin success with user not null navigate to splash`() = runTest {
        // Given
        val scenario = UnitScenario(
            currentUserUid = TestData.UID,
            shouldFail = false
        )

        val vm = buildVm(scenario = scenario)

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(
            email = TestData.EMAIL,
            password = TestData.PASSWORD
        )
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(AuthUiEvent.NavigateToSplash, event)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onEmailLogin failure emit snack`() = runTest {
        // Given
        val scenario = UnitScenario(shouldFail = true)

        val vm = buildVm(scenario = scenario)

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onEmailLogin(
            TestData.EMAIL,
            TestData.PASSWORD
        )
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(ZibeSnackType.ERROR, (event as AuthUiEvent.ShowSnack).type)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onDeleteAccountClicked success emit snack and set deleteUser false and set isLoading false`() = runTest {
        // Given
        val scenario = UnitScenario(
            deleteUser = true,
            shouldFail = false
        )

        val deleteAccountUseCase = FakeDeleteAccountUseCase { scenario }

        val vm = buildVm(
            scenario = scenario,
            deleteAccountUseCase = deleteAccountUseCase,
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onDeleteAccountClicked()
        advanceUntilIdle()

        // Then
        assertTrue(deleteAccountUseCase.execute() is ZibeResult.Success<Unit>)

        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack
        assertEquals(DELETE_ACCOUNT_SUCCESS, snack.message)
        assertEquals(ZibeSnackType.INFO, snack.type)

        assertFalse(vm.uiState.value.deleteUser)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onDoNotDeleteAccountClicked emit snack and set deleteUser and isLoading false`() = runTest {
        // Given
        val scenario = UnitScenario(deleteUser = true)

        val vm = buildVm(scenario = scenario)

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
    fun `reset password with email failure emit snack and set isLoading false`() = runTest {
        // Given
        val scenario = UnitScenario(shouldFail = true)

        val userSessionActions = FakeUserSessionActions { scenario }

        val vm = buildVm(
            scenario = scenario,
            userSessionActions = userSessionActions
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack

        assertFalse(userSessionActions.sendPasswordResetEmail(TestData.EMAIL) is ZibeResult.Success<Unit>)
        assertEquals(resetPasswordError(TestData.EMAIL), snack.message)
        assertEquals(ZibeSnackType.ERROR, snack.type)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `reset password with email success emit snack and set isLoading false`() = runTest {
        // Given
        val scenario = UnitScenario(shouldFail = false)

        val userSessionActions = FakeUserSessionActions { scenario }

        val vm = buildVm(
            scenario = scenario,
            userSessionActions = userSessionActions
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = event as AuthUiEvent.ShowSnack

        assertTrue(userSessionActions.sendPasswordResetEmail(TestData.EMAIL) is ZibeResult.Success<Unit>)
        assertEquals(resetPasswordSuccess(TestData.EMAIL), snack.message)
        assertEquals(ZibeSnackType.SUCCESS, snack.type)
    }

    @Test
    fun `onNavigateToSignUpClicked navigate to signup and set isLoading false`() = runTest {
        // Given
        val vm = buildVm()

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

    private fun buildVm(
        scenario: UnitScenario = UnitScenario(),
        userSessionProvider: UserSessionProvider = FakeUserSessionProvider(
            currentUser = scenario.currentUserUid?.let { uid ->
                mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
            }
        ),
        userSessionActions: UserSessionActions = FakeUserSessionActions { scenario },
        userPreferencesProvider: UserPreferencesProvider = FakeUserPreferencesProvider { scenario },
        userPreferencesActions: UserPreferencesActions = FakeUserPreferencesActions { scenario },
        deleteAccountUseCase: DeleteAccountUseCase = FakeDeleteAccountUseCase { scenario },
        googleSignInUseCase: GoogleSignInUseCase = FakeGoogleSignInUseCase { scenario }
    ): AuthViewModel =
        AuthViewModel(
            userSessionProvider = userSessionProvider,
            userSessionActions = userSessionActions,
            userPreferencesProvider = userPreferencesProvider,
            userPreferencesActions = userPreferencesActions,
            deleteAccountUseCase = deleteAccountUseCase,
            googleSignInUseCase = googleSignInUseCase
        )
}