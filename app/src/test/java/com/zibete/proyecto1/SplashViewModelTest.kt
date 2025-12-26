package com.zibete.proyecto1

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeLogoutUseCase
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.ui.splash.SplashUiEvent
import com.zibete.proyecto1.ui.splash.SplashViewModel
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cuando no hay internet muestra dialogo no internet`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val fakeAppChecksProvider = FakeAppChecksProvider().apply {
            internet = false
            locationPermission = true
        }

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.ShowNoInternetDialog), events)
        assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando onboarding no esta hecho navega a onboarding`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = false,
            firstLoginDone = false
        )

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then (nivel pro)
        assertEquals(listOf(SplashUiEvent.NavigateOnBoarding), events)
        assertTrue(state.onboardingDone) // se marcó onboarding done
        assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando no hay usuario navega a auth`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val fakeAppChecksProvider = FakeAppChecksProvider().apply {
            internet = true
            locationPermission = true
        }

        val fakeSessionProvider = FakeUserSessionProvider().apply {
            currentUser = null
        }

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.NavigateAuth), events)
        assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando hay usuario pero no hay permiso de ubicacion solicita permiso`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val fakeAppChecksProvider = FakeAppChecksProvider().apply {
            internet = true
            locationPermission = false
        }

        val fakeSessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.RequestLocationPermission), events)
        assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando todo esta OK navega a main`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val fakeAppChecksProvider = FakeAppChecksProvider().apply {
            internet = true
            locationPermission = true
        }

        val fakeSessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.NavigateMain), events)
        assertTrue(fakeSessionBootstrapper.calledWithUid != null)

        job.cancel()
    }

    @Test
    fun `cuando hay conflicto externo muestra dialogo`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val savedStateHandle = SavedStateHandle()

        savedStateHandle[EXTRA_SESSION_CONFLICT] = true

        val fakeAppChecksProvider = FakeAppChecksProvider().apply {
            internet = true
            locationPermission = true
        }

        val fakeSessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val viewModel = SplashViewModel(
            savedStateHandle = savedStateHandle,
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.ShowSessionConflictDialog), events)

        job.cancel()
    }

    @Test
    fun `cuando se confirma conflicto de sesion continua a main`() = runTest {
        // Given
        val fakeSessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }
        val fakeSessionBootstrapper = FakeSessionBootstrapper()

        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = fakeSessionProvider,
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.onSessionConflictConfirmed()
        advanceUntilIdle()

        // Then
        assertEquals(listOf(SplashUiEvent.NavigateMain), events)
        assertNotNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando se cancela conflicto de sesion hace logout y navega`() = runTest {
        // Given
        val fakeLogoutOrchestrator = FakeLogoutUseCase()

        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferencesProvider(state),
            preferencesActions = FakeUserPreferencesActions(state),
            sessionBootstrapper = FakeSessionBootstrapper(),
            logoutUseCase = fakeLogoutOrchestrator
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.onSessionConflictCancelled()
        advanceUntilIdle()

        // Then
        assertEquals(1, events.size)
        assertTrue(events.first() is SplashUiEvent.Navigate)

        job.cancel()
    }

    @Test
    fun `cuando se solicita logout navega con intent`() = runTest {
        // Given
        val fakeLogoutOrchestrator = FakeLogoutUseCase()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferencesProvider(
                FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
            ),
            preferencesActions = FakeUserPreferencesActions(
                FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
            ),
            sessionBootstrapper = FakeSessionBootstrapper(),
            logoutUseCase = fakeLogoutOrchestrator
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.onLogoutRequested()
        advanceUntilIdle()

        // Then
        assertEquals(1, events.size)
        assertTrue(events.first() is SplashUiEvent.Navigate)

        job.cancel()
    }


}
