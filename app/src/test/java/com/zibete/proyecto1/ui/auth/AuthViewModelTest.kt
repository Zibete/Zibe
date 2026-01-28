package com.zibete.proyecto1.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.validation.EmailValidator
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.data.auth.GoogleSignInUseCase
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeAuthSessionActions
import com.zibete.proyecto1.fakes.FakeAuthSessionProvider
import com.zibete.proyecto1.fakes.FakeDeleteAccountUseCase
import com.zibete.proyecto1.fakes.FakeEmailValidator
import com.zibete.proyecto1.fakes.FakeGoogleSignInUseCase
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var snackBarManager: SnackBarManager
    private lateinit var appNavigator: AppNavigator
    private val testConfig = SettingsConfig(navigationDelay = 0, validationDebounce = 0)
    private val emailValidator = FakeEmailValidator()

    @Before
    fun setup() {
        snackBarManager = SnackBarManager()
        appNavigator = AppNavigator()
        emailValidator.result = true
    }

    @Test
    fun `onEmailLogin con email vacio muestra snack de advertencia`() = runTest {
        val scenario = TestScenario()
        val vm = buildVm(scenario = scenario)

        vm.onEmailLogin(email = "", password = TestData.PASSWORD)

        val snack = snackBarManager.events.first()
        assertEquals(UiText.StringRes(R.string.err_email_required), snack.uiText)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoadingLogin)
    }

    @Test
    fun `onEmailLogin con email invalido muestra snack de advertencia`() = runTest {
        emailValidator.result = false
        val scenario = TestScenario()
        val vm = buildVm(scenario = scenario)

        vm.onEmailLogin(email = "invalid-email", password = TestData.PASSWORD)

        val snack = snackBarManager.events.first()
        assertEquals(UiText.StringRes(R.string.err_invalid_format_email), snack.uiText)
        assertEquals(ZibeSnackType.WARNING, snack.type)
    }

    @Test
    fun `onEmailLogin con password invalida muestra snack de advertencia`() = runTest {
        val scenario = TestScenario()
        val vm = buildVm(scenario = scenario)

        // Password demasiado corta según CredentialValidators
        vm.onEmailLogin(email = TestData.EMAIL, password = "123")

        val snack = snackBarManager.events.first()
        assertIs<UiText.StringRes>(snack.uiText)
        assertEquals(ZibeSnackType.WARNING, snack.type)
    }

    @Test
    fun `onEmailLogin exitoso navega a Splash`() = runTest {
        val scenario = TestScenario(currentUserUid = TestData.UID, shouldFail = false)
        val vm = buildVm(scenario = scenario)

        vm.onEmailLogin(email = TestData.EMAIL, password = TestData.PASSWORD)

        // Usamos first() para obtener el evento, lo cual suspende hasta que llegue.
        // Como estamos en runTest, esto debería funcionar bien si el evento se emite.
        val navEvent = appNavigator.events.first()
        assertIs<NavAppEvent.FinishFlowNavigateToSplash>(navEvent)
    }

    @Test
    fun `onDeleteAccountClicked exitoso limpia estado y muestra snack`() = runTest {
        val scenario = TestScenario(deleteUser = true, shouldFail = false)
        val deleteUseCase = FakeDeleteAccountUseCase { scenario }
        val vm = buildVm(scenario = scenario, deleteAccountUseCase = deleteUseCase)

        vm.onDeleteAccountClicked()

        val snack = snackBarManager.events.first()
        assertTrue(deleteUseCase.wasCalled)
        assertEquals(ZibeSnackType.INFO, snack.type)
        assertFalse(vm.uiState.value.deleteAccount)
    }

    @Test
    fun `onResetPassword fallido emite snack de error con email en args`() = runTest {
        val scenario = TestScenario(shouldFail = true)
        val vm = buildVm(scenario = scenario)

        vm.onResetPassword(TestData.EMAIL)

        val snack = snackBarManager.events.first()
        val expectedUiText = UiText.StringRes(R.string.reset_password_error, listOf(TestData.EMAIL))

        assertEquals(expectedUiText, snack.uiText)
        assertEquals(ZibeSnackType.ERROR, snack.type)
    }

    @Test
    fun `onNavigateToSignUp emite evento de navegacion interno`() = runTest {
        val vm = buildVm()

        vm.onNavigateToSignUp()

        val event = vm.events.first()
        assertEquals(AuthUiEvent.NavigateToSignUp, event)
    }

    private fun buildVm(
        scenario: TestScenario = TestScenario(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(
            currentUser = scenario.currentUserUid?.let { uid ->
                mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
            }
        ),
        authSessionActions: AuthSessionActions = FakeAuthSessionActions { scenario },
        deleteAccountUseCase: DeleteAccountUseCase = FakeDeleteAccountUseCase { scenario },
        googleSignInUseCase: GoogleSignInUseCase = FakeGoogleSignInUseCase { scenario }
    ): AuthViewModel = AuthViewModel(
        authSessionProvider = authSessionProvider,
        authSessionActions = authSessionActions,
        deleteAccountUseCase = deleteAccountUseCase,
        googleSignInUseCase = googleSignInUseCase,
        snackBarManager = snackBarManager,
        appNavigator = appNavigator,
        config = testConfig,
        emailValidator = emailValidator
    )
}
