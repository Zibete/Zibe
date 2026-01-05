package com.zibete.proyecto1.ui.signup

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_NAME_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_MSG_SUCCESS
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionBootstrapper: SessionBootstrapper
    private lateinit var userSessionActions: UserSessionActions
    private lateinit var snackBarManager: SnackBarManager

    @Before
    fun setup() {
        sessionBootstrapper = mockk<SessionBootstrapper>(relaxed = true)
        userSessionActions = mockk<UserSessionActions>(relaxed = true)
        snackBarManager = mockk<SnackBarManager>(relaxed = true)

    }

    @Test
    fun flow_onRegister_blankEmail_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

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
        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(ERR_EMAIL_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        coVerify { userSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_blankPassword_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

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
        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(ERR_PASSWORD_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        coVerify { userSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_blankName_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

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
        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(SIGNUP_ERR_NAME_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        coVerify { userSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_blankBirthdate_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "",
            description = TestData.DESC
        )
        advanceUntilIdle()

        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(SIGNUP_ERR_BIRTHDAY_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        coVerify { userSessionActions wasNot Called }
        coVerify { sessionBootstrapper wasNot Called }
    }

    @Test
    fun flow_onRegister_underAge_emitsSnack_noSideEffects() = runTest {
        // Given
        val vm = SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "2015-01-01",
            description = TestData.DESC
        )
        advanceUntilIdle()

        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(ERR_UNDER_AGE, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        coVerify { userSessionActions wasNot Called }
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

        val deferred = async { awaitEvent(vm) }
        runCurrent()

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
        val event = deferred.await()

        assertTrue(sessionBootstrapper.wasCalled)
        verify { snackBarManager.show(SIGNUP_MSG_SUCCESS, ZibeSnackType.SUCCESS) }
        assertEquals(SignUpUiEvent.NavigateToSplash, event)
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

        val deferred = async { awaitEvent(vm) }
        runCurrent()

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
        val event = deferred.await()
        val snack = assertIs<SignUpUiEvent.ShowSnack>(event)

        assertEquals(ZibeSnackType.ERROR, snack.type)
        assertFalse(vm.uiState.value.isLoading)
    }

    private suspend fun awaitEvent(vm: SignUpViewModel): SignUpUiEvent {
        return withTimeout(2_000) { vm.events.first() }
    }

    private fun buildVm(
        scenario: TestScenario = TestScenario(),
        userSessionActions: UserSessionActions = FakeUserSessionActions { scenario },
        sessionBootstrapper: SessionBootstrapper = FakeSessionBootstrapper { scenario },
        snackBarManager: SnackBarManager
        ): SignUpViewModel =
        SignUpViewModel(
            userSessionActions = userSessionActions,
            sessionBootstrapper = sessionBootstrapper,
            snackBarManager = snackBarManager
        )
}
