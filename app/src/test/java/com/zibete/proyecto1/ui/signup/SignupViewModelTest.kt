package com.zibete.proyecto1.ui.signup

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.di.SettingsConfig
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.data.auth.AuthSessionActions
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.fakes.FakeAuthSessionActions
import com.zibete.proyecto1.fakes.FakeEmailValidator
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionBootstrapper: SessionBootstrapper
    private lateinit var authSessionActions: AuthSessionActions
    private lateinit var snackBarManager: SnackBarManager
    private lateinit var updateProfileUseCase: UpdateProfileUseCase
    private lateinit var appNavigator: AppNavigator
    private val config = SettingsConfig(validationDebounce = 0L)
    private val emailValidator = FakeEmailValidator()

    @Before
    fun setup() {
        sessionBootstrapper = mockk<SessionBootstrapper>(relaxed = true)
        authSessionActions = mockk<AuthSessionActions>(relaxed = true)
        snackBarManager = mockk<SnackBarManager>(relaxed = true)
        updateProfileUseCase = mockk<UpdateProfileUseCase>(relaxed = true)
        appNavigator = mockk<AppNavigator>(relaxed = true)
        emailValidator.result = true
    }

    @Test
    fun flow_onRegister_blankEmail_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        // When
        vm.onRegister(
            email = "",
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.err_email_required),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)

        coVerify { authSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_invalidEmail_emitsSnack_noSideEffects() = runTest {
        // Given
        emailValidator.result = false
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        // When
        vm.onRegister(
            email = "invalid-email",
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.err_invalid_format_email),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun flow_onRegister_blankPassword_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        // When
        vm.onRegister(
            email = TestData.EMAIL,
            password = "",
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.err_password_required),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)

        coVerify { authSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_blankName_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        // When
        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = "",
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.signup_err_name_required),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)

        coVerify { authSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_blankBirthdate_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "",
            description = TestData.DESC
        )
        advanceUntilIdle()

        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.signup_err_birthdate_required),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)

        coVerify { authSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_underAge_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "2015-01-01",
            description = TestData.DESC
        )
        advanceUntilIdle()

        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.err_under_age),
                ZibeSnackType.WARNING
            )
        }
        assertFalse(vm.uiState.value.isLoading)

        coVerify { authSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_success_emitsSnack_sessionCreated_navigateToSplash() = runTest {
        // Given
        val scenario = TestScenario(
            shouldFail = false
        )

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        // When
        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        assertTrue(sessionBootstrapper.wasCalled)
        verify {
            snackBarManager.show(
                UiText.StringRes(R.string.signup_msg_success),
                ZibeSnackType.SUCCESS
            )
        }
        verify { appNavigator.finishFlowNavigateToSplash() }
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun smoke_onRegister_failure_emitsSnack_setsIsLoading_false() = runTest {
        // Given
        val scenario = TestScenario(
            shouldFail = true
        )

        val vm = buildVm(
            scenario = scenario,
            snackBarManager = snackBarManager
        )

        // When
        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        // Then
        verify { snackBarManager.show(any(), ZibeSnackType.ERROR) }
        assertFalse(vm.uiState.value.isLoading)
    }

    private fun buildVm(
        scenario: TestScenario = TestScenario(),
        authSessionActions: AuthSessionActions = FakeAuthSessionActions { scenario },
        sessionBootstrapper: SessionBootstrapper = FakeSessionBootstrapper { scenario },
        snackBarManager: SnackBarManager
    ): SignUpViewModel =
        SignUpViewModel(
            authSessionActions = authSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            updateProfileUseCase = updateProfileUseCase,
            snackBarManager = snackBarManager,
            appNavigator = appNavigator,
            config = config,
            emailValidator = emailValidator
        )
}
