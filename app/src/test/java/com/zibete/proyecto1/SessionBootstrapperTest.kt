package com.zibete.proyecto1

import com.zibete.proyecto1.domain.session.DefaultSessionBootstrapper
import com.zibete.proyecto1.fakes.FakeSessionRepositoryActions
import com.zibete.proyecto1.fakes.FakeSessionRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserPreferences
import com.zibete.proyecto1.fakes.FakeUserPreferencesState
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
            localInstallId = "install-123",
            localFcmToken = "fcm-abc"
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
        bootstrapper.bootstrap("uid-1")

        // Then
        assertEquals(
            FakeSessionRepositoryActions.ActiveSessionCall(
                uid = "uid-1",
                installId = "install-123",
                fcmToken = "fcm-abc"
            ),
            sessionRepoActions.lastSetActiveSessionCall
        )
    }

    @Test
    fun `bootstrap usuario nuevo crea nodo y setea firstLoginDone false`() = runTest {
        // Given
        val sessionRepoProvider = FakeSessionRepositoryProvider(
            localInstallId = "install-1",
            localFcmToken = "fcm-1"
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
        bootstrapper.bootstrap("uid-1")

        // Then
        assertTrue(userRepoActions.createUserNodeCalled)
        assertFalse(prefsState.firstLoginDone)
        assertNotNull(sessionRepoActions.lastSetActiveSessionCall)
    }

    @Test
    fun `bootstrap usuario existente setea firstLoginDone segun birthDate`() = runTest {
        // Given
        val sessionRepoProvider = FakeSessionRepositoryProvider(
            localInstallId = "install-1",
            localFcmToken = "fcm-1"
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
        bootstrapper.bootstrap("uid-1")

        // Then
        assertTrue(prefsState.firstLoginDone)
        assertFalse(userRepoActions.createUserNodeCalled)
        assertNotNull(sessionRepoActions.lastSetActiveSessionCall)
    }
}
