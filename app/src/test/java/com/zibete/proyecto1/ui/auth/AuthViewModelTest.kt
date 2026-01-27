package com.zibete.proyecto1.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.ZibeSnackEvent
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeAuthSessionActions
import com.zibete.proyecto1.fakes.FakeAuthSessionProvider
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeGoogleSignInUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun flow_onEmailLogin_emptyEmail_emitsSnack_setsIsLoading_false_noSideEffects() = runTest {
        // Given
        val scenario = TestScenario()
        val authSessionActions = FakeAuthSessionActions { scenario }
        val vm = buildVm(
            scenario = scenario,
            authSessionActions = authSessionActions
        )

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
        val snack = assertIs<ZibeSnackEvent>(event)

        assertEquals(UiText.StringRes(R.string.err_email_required), snack.uiText)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertEquals(null, authSessionActions.lastEmail)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun flow_onEmailLogin_emptyPassword_emitsSnack_setsIsLoading_false_noSideEffects() = runTest {
        // Given
        val scenario = TestScenario()
        val authSessionActions = FakeAuthSessionActions { scenario }
        val vm = buildVm(
            scenario = scenario,
            authSessionActions = authSessionActions
        )

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
        val snack = assertIs<ZibeSnackEvent>(event)

        assertEquals(UiText.StringRes(R.string.err_password_required), snack.uiText)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertEquals(null, authSessionActions.lastEmail)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun flow_onEmailLogin_success_withUser_navigatesToSplash() = runTest {
        // Given
        val scenario = TestScenario(
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

        assertEquals(NavAppEvent.FinishFlowNavigateToSplash(), event)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun flow_onEmailLogin_failure_emitsSnack_setsIsLoading_false() = runTest {
        // Given
        val scenario = TestScenario(shouldFail = true)

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
        val snack = assertIs<ZibeSnackEvent>(event)

        assertEquals(ZibeSnackType.ERROR, snack.uiText)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun flow_onDeleteAccountClicked_success_emitsSnack_setsDeleteUser_false_setsIsLoading_false() =
        runTest {
            // Given
            val scenario = TestScenario(
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
            val event = deferred.await()
            val snack = assertIs<ZibeSnackEvent>(event)

            assertTrue(deleteAccountUseCase.wasCalled)
            assertEquals(UiText.StringRes(R.string.account_delete_success), snack.uiText)
            assertEquals(ZibeSnackType.INFO, snack.type)
            assertFalse(vm.uiState.value.deleteAccount)
            assertFalse(vm.uiState.value.isLoadingLogin)
        }

    @Test
    fun flow_onDoNotDeleteAccountClicked_emitsSnack_setsDeleteUser_false_setsIsLoading_false() =
        runTest {
            // Given
            val scenario = TestScenario(deleteUser = true)
            val vm = buildVm(scenario = scenario)
            val deferred = async { awaitEvent(vm) }
            runCurrent()

            // When
            vm.onDoNotDeleteAccountClicked()
            advanceUntilIdle()

            // Then
            val event = deferred.await()
            val snack = assertIs<ZibeSnackEvent>(event)
            assertEquals(UiText.StringRes(R.string.account_delete_cancelled), snack.uiText)
            assertEquals(ZibeSnackType.INFO, snack.type)
            assertFalse(vm.uiState.value.deleteAccount)
            assertFalse(vm.uiState.value.isLoadingLogin)
        }

    @Test
    fun flow_onResetPassword_failure_emitsSnack_setsIsLoading_false() = runTest {
        // Given
        val scenario = TestScenario(shouldFail = true)
        val authSessionActions = FakeAuthSessionActions { scenario }
        val vm = buildVm(
            scenario = scenario,
            authSessionActions = authSessionActions
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = assertIs<ZibeSnackEvent>(event)

        assertEquals(TestData.EMAIL, authSessionActions.lastEmail)
        assertEquals(
            UiText.StringRes(R.string.reset_password_error, listOf(TestData.EMAIL)),
            snack.uiText
        )
        assertEquals(ZibeSnackType.ERROR, snack.type)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun flow_onResetPassword_success_emitsSnack_setsIsLoading_false() = runTest {
        // Given
        val scenario = TestScenario(shouldFail = false)
        val authSessionActions = FakeAuthSessionActions { scenario }
        val vm = buildVm(
            scenario = scenario,
            authSessionActions = authSessionActions
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onResetPassword(TestData.EMAIL)
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        val snack = assertIs<ZibeSnackEvent>(event)

        assertEquals(TestData.EMAIL, authSessionActions.lastEmail)
        assertEquals(
            UiText.StringRes(R.string.reset_password_success, listOf(TestData.EMAIL)),
            snack.uiText
        )
        assertEquals(ZibeSnackType.SUCCESS, snack.type)
    }

    @Test
    fun flow_onNavigateToSignUpClicked_navigatesToSignUp_setsIsLoading_false() = runTest {
        // Given
        val vm = buildVm()
        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onNavigateToSignUp()
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        assertEquals(AuthUiEvent.NavigateToSignUp, event)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    private suspend fun awaitEvent(vm: AuthViewModel): AuthUiEvent {
        return withTimeout(2_000) { vm.events.first() }
    }

    private fun buildVm(
        scenario: TestScenario = TestScenario(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(
            currentUser = scenario.currentUserUid?.let { uid ->
                mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
            }
        ),
        authSessionActions: AuthSessionActions = FakeAuthSessionActions { scenario },
        userPreferencesProvider: UserPreferencesProvider = FakeUserPreferencesProvider { scenario },
        userPreferencesActions: UserPreferencesActions = FakeUserPreferencesActions { scenario },
        deleteAccountUseCase: DeleteAccountUseCase = FakeDeleteAccountUseCase { scenario },
        googleSignInUseCase: GoogleSignInUseCase = FakeGoogleSignInUseCase { scenario }
    ): AuthViewModel =
        AuthViewModel(
            authSessionProvider = authSessionProvider,
            authSessionActions = authSessionActions,
            userPreferencesProvider = userPreferencesProvider,
            userPreferencesActions = userPreferencesActions,
            deleteAccountUseCase = deleteAccountUseCase,
            googleSignInUseCase = googleSignInUseCase,
            snackBarManager = {},
            appNavigator = {},
            config = {}
        )
}