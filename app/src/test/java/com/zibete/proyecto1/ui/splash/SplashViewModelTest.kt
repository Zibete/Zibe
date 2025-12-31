package com.zibete.proyecto1.ui.splash

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeLogoutUseCase
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserPreferences
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.ui.constants.Constants
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---------------------------------------------------------------------------------
    // Onboarding on/off
    // ---------------------------------------------------------------------------------

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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.NavigateOnBoarding), events)
        Assert.assertTrue(state.onboardingDone) // se marcó onboarding done
        Assert.assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando onboarding esta hecho no navega a onboarding`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val fakeSessionBootstrapper = FakeSessionBootstrapper()
        val fakeUserPreferences = FakeUserPreferences(state)

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = fakeUserPreferences,
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertFalse(events.contains(SplashUiEvent.NavigateOnBoarding))
        Assert.assertEquals(0, fakeUserPreferences.setOnboardingDoneCalls)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // Session conflict on/off
    // ---------------------------------------------------------------------------------

    @Test
    fun `cuando hay conflicto de sesion muestra dialogo`() = runTest {
        // Given
        val state = FakeUserPreferencesState(
            onboardingDone = true,
            firstLoginDone = false
        )

        val savedStateHandle = SavedStateHandle()

        savedStateHandle[Constants.EXTRA_SESSION_CONFLICT] = true

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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.ShowSessionConflictDialog), events)

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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.onSessionConflictConfirmed()
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.NavigateMain), events)
        Assert.assertNotNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    @Test
    fun `cuando se cancela conflicto de sesion hace logout y navega a auth`() = runTest {
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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = FakeSessionBootstrapper(),
            logoutUseCase = fakeLogoutOrchestrator
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.onSessionConflictCancelled()
        advanceUntilIdle()

        // Then
        Assert.assertEquals(1, events.size)
        Assert.assertTrue(events.first() is SplashUiEvent.NavigateAuth)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // Internet conflict
    // ---------------------------------------------------------------------------------

    @Test
    fun `cuando no hay internet muestra dialogo`() = runTest {
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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.ShowNoInternetDialog), events)
        Assert.assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // permission
    // ---------------------------------------------------------------------------------

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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.RequestLocationPermission), events)
        Assert.assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // Helper logout
    // ---------------------------------------------------------------------------------

    @Test
    fun `cuando se solicita logout navega a auth`() = runTest {
        // Given
        val fakeLogoutOrchestrator = FakeLogoutUseCase()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = FakeAppChecksProvider(),
            sessionProvider = FakeUserSessionProvider(),
            preferencesProvider = FakeUserPreferences(
                FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
            ),
            preferencesActions = FakeUserPreferences(
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
        Assert.assertEquals(1, events.size)
        Assert.assertTrue(events.first() is SplashUiEvent.NavigateAuth)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // user null
    // ---------------------------------------------------------------------------------

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
            preferencesProvider = FakeUserPreferences(state),
            preferencesActions = FakeUserPreferences(state),
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutUseCase = FakeLogoutUseCase()
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // When
        viewModel.start(context = mockk(relaxed = true))
        advanceUntilIdle()

        // Then
        Assert.assertEquals(listOf(SplashUiEvent.NavigateAuth), events)
        Assert.assertNull(fakeSessionBootstrapper.calledWithUid)

        job.cancel()
    }

    // ---------------------------------------------------------------------------------
    // user OK
    // ---------------------------------------------------------------------------------

    @Test
    fun `cuando internet ok + user ok + location ok llama SessionBootstrapper con el uid correcto`() =
        runTest {
            // Given
            val state = FakeUserPreferencesState(
                onboardingDone = true,
                firstLoginDone = false
            )

            val fakeAppChecksProvider = FakeAppChecksProvider().apply {
                internet = true
                locationPermission = true
            }

            val fakeSessionBootstrapper = FakeSessionBootstrapper()

            val fakeSessionProvider = FakeUserSessionProvider()

            val fakeUser = mockk<FirebaseUser> { every { uid } returns TestData.UID }
            fakeSessionProvider.currentUser = fakeUser


            val viewModel = SplashViewModel(
                savedStateHandle = SavedStateHandle(),
                appChecksProvider = fakeAppChecksProvider,
                sessionProvider = fakeSessionProvider,
                preferencesProvider = FakeUserPreferences(state),
                preferencesActions = FakeUserPreferences(state),
                sessionBootstrapper = fakeSessionBootstrapper,
                logoutUseCase = FakeLogoutUseCase()
            )

            val events = mutableListOf<SplashUiEvent>()
            val job = launch { viewModel.events.toList(events) }

            // When
            viewModel.start(context = mockk(relaxed = true))
            advanceUntilIdle()

            // Then
            Assert.assertEquals(listOf(SplashUiEvent.NavigateMain), events)
            Assert.assertTrue(fakeSessionBootstrapper.calledWithUid != null)
            Assert.assertEquals(TestData.UID, fakeSessionBootstrapper.calledWithUid)

            job.cancel()
        }

}