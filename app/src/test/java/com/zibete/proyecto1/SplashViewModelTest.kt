package com.zibete.proyecto1

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeLogoutOrchestrator
import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.ui.splash.SplashUiEvent
import com.zibete.proyecto1.ui.splash.SplashViewModel
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cuando onboarding no esta hecho navega a onboarding`() = runTest {
        // Given

        val state = FakeUserPreferencesState(
            onboardingDone = false,
            firstLoginDone = false
        )

        val savedStateHandle = SavedStateHandle()
        val fakeAppChecksProvider = FakeAppChecksProvider()
        val fakeSessionProvider = FakeUserSessionProvider()
        val fakeUserPreferencesProvider = FakeUserPreferencesProvider(state)
        val fakeUserPreferencesActions = FakeUserPreferencesActions(state)
        val fakeSessionBootstrapper = FakeSessionBootstrapper()
        val fakeLogoutOrchestrator = FakeLogoutOrchestrator()


        val viewModel = SplashViewModel(
            savedStateHandle = savedStateHandle,
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            preferencesProvider = fakeUserPreferencesProvider,
            preferencesActions = fakeUserPreferencesActions,
            sessionBootstrapper = fakeSessionBootstrapper,
            logoutOrchestrator = fakeLogoutOrchestrator
        )

        val events = mutableListOf<SplashUiEvent>()
        val job = launch {
            viewModel.events.toList(events)
        }

        // When
        viewModel.start(context = mockk(relaxed = true))

        advanceUntilIdle()

        // Then
        assertTrue(events.contains(SplashUiEvent.NavigateOnBoarding))

        job.cancel()
    }
}

