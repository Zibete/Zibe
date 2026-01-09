package com.zibete.proyecto1.ui.splash

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.utils.AppChecksProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeLogoutUseCase
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeAuthSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun flow_start_onboardingDone_false_navigateToOnBoarding_setsOnboardingDone_true() = runTest {
        // Given
        val scenario = TestScenario(onboardingDone = false)

        val vm = buildVm(scenario = scenario)

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(SplashUiEvent.NavigateOnBoarding, event)
        assertTrue(scenario.onboardingDone)
    }

    @Test
    fun flow_start_onboardingDone_true_navigateToAuth_onboardingDone_stillTrue() = runTest {
        // Given
        val scenario = TestScenario(onboardingDone = true)

        val vm = buildVm(scenario = scenario)

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(SplashUiEvent.NavigateAuth, event)
        assertTrue(scenario.onboardingDone)
    }

    @Test
    fun smoke_start_extraSessionConflict_emitsDialog_interruptFlow() = runTest {
        // Given
        val scenario = TestScenario(
            currentUserUid = TestData.UID,
            onboardingDone = true
        )

        val savedStateHandle = SavedStateHandle()
        savedStateHandle[Constants.EXTRA_SESSION_CONFLICT] = true

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            savedStateHandle = savedStateHandle,
            sessionBootstrapper = sessionBootstrapper
        )
        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(SplashUiEvent.ShowSessionConflictDialog, event)
        assertFalse(sessionBootstrapper.wasCalled)
    }

    @Test
    fun smoke_onSessionConflictConfirmed_navigateToMain() = runTest {
        // Given
        val scenario = TestScenario(
            currentUserUid = TestData.UID
        )

        val vm = buildVm(
            scenario = scenario
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onSessionConflictConfirmed()
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals((SplashUiEvent.NavigateMain), event)
    }

    @Test
    fun flow_onSessionConflictCancelled_logoutWasCalled_navigateToAuth() = runTest {
        // Given
        val scenario = TestScenario(
            currentUserUid = TestData.UID,
        )

        val logoutUseCase = FakeLogoutUseCase { scenario }

        val vm = buildVm(
            scenario = scenario,
            logoutUseCase = logoutUseCase
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onSessionConflictCancelled()
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertTrue(logoutUseCase.wasCalled)
        assertEquals((SplashUiEvent.NavigateAuth), event)
    }

    @Test
    fun flow_start_withoutInternet_emitsDialog_interruptFlow() = runTest {
        // Given
        val scenario = TestScenario(
            onboardingDone = true,
            currentUserUid = TestData.UID,
            hasInternet = false
        )

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            sessionBootstrapper = sessionBootstrapper
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals(SplashUiEvent.ShowNoInternetDialog, event)
        assertFalse(sessionBootstrapper.wasCalled)
    }

    @Test
    fun flow_start_withUser_hasLocationPermission_false_navigateToPermission() = runTest {
        // Given
        val scenario = TestScenario(
            currentUserUid = TestData.UID,
            onboardingDone = true,
            hasLocationPermission = false
        )

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            sessionBootstrapper = sessionBootstrapper
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(
            context = mockk(relaxed = true)
        )
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals((SplashUiEvent.NavigatePermission), event)
        assertFalse(sessionBootstrapper.wasCalled)
    }

    @Test
    fun smoke_onLogoutRequested_navigateToAuth() = runTest {
        // Given
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.onLogoutRequested()
        advanceUntilIdle()

        // Then
        val event = deferred.await()
        assertEquals((SplashUiEvent.NavigateAuth), event)
    }

    @Test
    fun flow_start_withoutUser_navigateToAuth() = runTest {
        // Given
        val scenario = TestScenario(
            currentUserUid = null,
            onboardingDone = true
        )

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            sessionBootstrapper = sessionBootstrapper
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertEquals((SplashUiEvent.NavigateAuth), event)
        assertFalse(sessionBootstrapper.wasCalled)
    }

    @Test
    fun flow_start_allOk_callSessionBootstrapper_navigateToMain() = runTest {
        // Given
        val scenario = TestScenario(
            onboardingDone = true,
            hasInternet = true,
            hasLocationPermission = true,
            currentUserUid = TestData.UID
        )

        val sessionBootstrapper = FakeSessionBootstrapper { scenario }

        val vm = buildVm(
            scenario = scenario,
            sessionBootstrapper = sessionBootstrapper
        )

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        // When
        vm.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        val event = deferred.await()

        assertTrue(sessionBootstrapper.wasCalled)
        assertEquals((SplashUiEvent.NavigateMain), event)
    }

    private suspend fun awaitEvent(vm: SplashViewModel): SplashUiEvent {
        return withTimeout(2_000) { vm.events.first() }
    }

    private fun buildVm(
        scenario: TestScenario = TestScenario(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(
            currentUser = scenario.currentUserUid?.let { uid ->
                mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
            }
        ),
        savedStateHandle : SavedStateHandle = SavedStateHandle(),
        appChecksProvider : AppChecksProvider = FakeAppChecksProvider { scenario },
        userPreferencesProvider : UserPreferencesProvider = FakeUserPreferencesProvider { scenario },
        userPreferencesActions : UserPreferencesActions = FakeUserPreferencesActions { scenario },
        sessionBootstrapper : SessionBootstrapper = FakeSessionBootstrapper { scenario },
        logoutUseCase : LogoutUseCase = FakeLogoutUseCase { scenario }
    ) : SplashViewModel {
        return SplashViewModel(
            authSessionProvider = authSessionProvider,
            savedStateHandle = savedStateHandle,
            appChecksProvider = appChecksProvider,
            userPreferencesProvider = userPreferencesProvider,
            userPreferencesActions = userPreferencesActions,
            sessionBootstrapper = sessionBootstrapper,
            logoutUseCase = logoutUseCase
        )
    }

}
