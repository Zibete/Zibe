package com.zibete.proyecto1.ui.auth

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.domain.session.DefaultSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeSessionRepositoryActions
import com.zibete.proyecto1.fakes.FakeSessionRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserPreferences
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestData
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionBootstrapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `bootstrap setea sesion activa con installId y fcmToken`() = runTest {
        // Given
        val sessionRepoProvider = FakeSessionRepositoryProvider(
            localInstallId = TestData.INSTALL_ID,
            localFcmToken = TestData.TOKEN
        )
        val sessionRepoActions = FakeSessionRepositoryActions()

        val userRepoProvider = FakeUserRepositoryProvider(
            accountExists = true,
            hasBirthDate = false
        )
        val userRepoActions = FakeUserRepositoryActions()

        val sessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val prefsActions = FakeUserPreferences(prefsState)

        val bootstrapper = DefaultSessionBootstrapper(
            sessionRepositoryActions = sessionRepoActions,
            sessionRepositoryProvider = sessionRepoProvider,
            userRepositoryActions = userRepoActions,
            userRepositoryProvider = userRepoProvider,
            sessionProvider = sessionProvider,
            preferencesActions = prefsActions
        )

        // When
        bootstrapper.bootstrap(TestData.UID)

        // Then
        Assert.assertEquals(
            FakeSessionRepositoryActions.ActiveSessionCall(
                uid = TestData.UID,
                installId = TestData.INSTALL_ID,
                fcmToken = TestData.TOKEN
            ),
            sessionRepoActions.lastSetActiveSessionCall
        )
    }

    @Test
    fun `bootstrap usuario nuevo crea nodo y setea firstLoginDone false`() = runTest {
        // Given
        val sessionRepoProvider = FakeSessionRepositoryProvider(
            localInstallId = TestData.INSTALL_ID,
            localFcmToken = TestData.TOKEN
        )
        val sessionRepoActions = FakeSessionRepositoryActions()

        val userRepoProvider = FakeUserRepositoryProvider(
            accountExists = false,
            hasBirthDate = false
        )
        val userRepoActions = FakeUserRepositoryActions()

        val sessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = true)
        val prefsActions = FakeUserPreferences(prefsState)

        val bootstrapper = DefaultSessionBootstrapper(
            sessionRepositoryActions = sessionRepoActions,
            sessionRepositoryProvider = sessionRepoProvider,
            userRepositoryActions = userRepoActions,
            userRepositoryProvider = userRepoProvider,
            sessionProvider = sessionProvider,
            preferencesActions = prefsActions
        )

        // When
        bootstrapper.bootstrap(TestData.UID)

        // Then
        Assert.assertTrue(userRepoActions.createUserNodeCalled)
        Assert.assertFalse(prefsState.firstLoginDone)
        Assert.assertNotNull(sessionRepoActions.lastSetActiveSessionCall)
    }

    @Test
    fun `bootstrap usuario existente setea firstLoginDone segun birthDate`() = runTest {
        // Given
        val sessionRepoProvider = FakeSessionRepositoryProvider(
            localInstallId = TestData.INSTALL_ID,
            localFcmToken = TestData.TOKEN
        )
        val sessionRepoActions = FakeSessionRepositoryActions()

        val userRepoProvider = FakeUserRepositoryProvider(
            accountExists = true,
            hasBirthDate = true
        )
        val userRepoActions = FakeUserRepositoryActions()

        val sessionProvider = FakeUserSessionProvider().apply {
            currentUser = mockk(relaxed = true)
        }

        val prefsState = FakeUserPreferencesState(onboardingDone = true, firstLoginDone = false)
        val prefsActions = FakeUserPreferences(prefsState)

        val bootstrapper = DefaultSessionBootstrapper(
            sessionRepositoryActions = sessionRepoActions,
            sessionRepositoryProvider = sessionRepoProvider,
            userRepositoryActions = userRepoActions,
            userRepositoryProvider = userRepoProvider,
            sessionProvider = sessionProvider,
            preferencesActions = prefsActions
        )

        // When
        bootstrapper.bootstrap(TestData.UID)

        // Then
        Assert.assertTrue(prefsState.firstLoginDone)
        Assert.assertFalse(userRepoActions.createUserNodeCalled)
        Assert.assertNotNull(sessionRepoActions.lastSetActiveSessionCall)
    }
}