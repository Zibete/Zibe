package com.zibete.proyecto1

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.fakes.FakeAppChecksProvider
import com.zibete.proyecto1.fakes.FakeUserPreferences
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserSessionActions
import com.zibete.proyecto1.fakes.FakeUserSessionManager
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

        val fakeAppChecksProvider = FakeAppChecksProvider()
        val fakeUserPreferencesProvider = FakeUserPreferencesProvider(state)
        val fakeUserPreferencesActions = FakeUserPreferencesActions(state)
        val fakeSessionProvider = FakeUserSessionProvider()
        val fakeSessionActions = FakeUserSessionActions()

        val viewModel = SplashViewModel(
            savedStateHandle = SavedStateHandle(),
            appChecksProvider = fakeAppChecksProvider,
            sessionProvider = fakeSessionProvider,
            sessionActions = fakeSessionActions,
            preferencesProvider = fakeUserPreferencesProvider,
            preferencesActions = fakeUserPreferencesActions,
            userRepository = fakeUserPreferences, //falta
            sessionRepository = fakeUserPreferences//falta
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

