package com.zibete.proyecto1.domain.session

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.fakes.ActiveSessionCall
import com.zibete.proyecto1.fakes.CreateUserNodeCall
import com.zibete.proyecto1.fakes.FakeSessionRepositoryActions
import com.zibete.proyecto1.fakes.FakeSessionRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserSessionProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.UnitScenario
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SessionBootstrapperTest {

    @Test
    fun `sets active session with installId and fcm Token`() = runTest {
        // Given
        val scenario = UnitScenario()

        val sessionRepositoryActions = FakeSessionRepositoryActions { scenario }

        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            sessionRepositoryActions = sessionRepositoryActions
        )

        // When
        bootstrapper.bootstrap(
            uid = TestData.UID
        )

        //Then
        assertEquals(
            ActiveSessionCall(
                uid = TestData.UID,
                installId = TestData.INSTALL_ID,
                fcmToken = TestData.TOKEN
            ),
            sessionRepositoryActions.lastSetActiveSessionCall
        )
    }

    @Test
    fun `new user with currentUser create node and set firstLoginDone false`() = runTest {
        // Given
        val firebaseUser = mockk<FirebaseUser>(relaxed = true)

        val scenario = UnitScenario(
            accountExists = false, // new user
        )

        val userRepositoryActions = FakeUserRepositoryActions { scenario }

        val userPreferencesActions = FakeUserPreferencesActions { scenario }

        val userSessionProvider = FakeUserSessionProvider(
            currentUser = firebaseUser
        )

        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            userRepositoryActions = userRepositoryActions,
            userSessionProvider = userSessionProvider,
            userPreferencesActions = userPreferencesActions
        )

        // When
        bootstrapper.bootstrap(
            uid = TestData.UID
        )

        // Then
        assertEquals(
            CreateUserNodeCall(
                user = firebaseUser,
                birthDate = "",
                description = ""
            ),
            userRepositoryActions.lastCreateUserNodeCall
        )

        assertFalse(scenario.firstLoginDone)
    }

    @Test
    fun `new user without currentUser does not create node and not set firstLoginDone false`() = runTest {
        // Given
        val scenario = UnitScenario(
            accountExists = false, // new user
            firstLoginDone = true
        )

        val userRepositoryActions = FakeUserRepositoryActions { scenario }

        val userSessionProvider = FakeUserSessionProvider(
            currentUser = null
        )

        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            userRepositoryActions = userRepositoryActions,
            userSessionProvider = userSessionProvider
        )

        // When
        bootstrapper.bootstrap(
            uid = TestData.UID
        )

        // Then
        assertNull(userRepositoryActions.lastCreateUserNodeCall)
        assertTrue(scenario.firstLoginDone)
    }

    @Test
    fun `existing user with birthDate set firstLoginDone true and does not create node`() = runTest {
        // Given
        val scenario = UnitScenario(
            accountExists = true,
            hasBirthDate = true,
            firstLoginDone = false
        )

        val userRepositoryActions = FakeUserRepositoryActions { scenario }

        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            userRepositoryActions = userRepositoryActions,
        )

        // When
        bootstrapper.bootstrap(
            uid = TestData.UID
        )

        // Then
        assertNull(userRepositoryActions.lastCreateUserNodeCall)
        assertTrue(scenario.firstLoginDone)
    }

    @Test
    fun `existing user without birthDate set firstLoginDone false and does not create node`() = runTest {
        // Given
        val scenario = UnitScenario(
            accountExists = true,
            hasBirthDate = false,
            firstLoginDone = true
        )

        val userRepositoryActions = FakeUserRepositoryActions { scenario }

        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            userRepositoryActions = userRepositoryActions,
        )

        // When
        bootstrapper.bootstrap(
            uid = TestData.UID
        )

        // Then
        assertNull(userRepositoryActions.lastCreateUserNodeCall)
        assertFalse(scenario.firstLoginDone)
    }

    private fun buildBootstrapper(
        scenario: UnitScenario = UnitScenario(),
        sessionRepositoryActions: SessionRepositoryActions = FakeSessionRepositoryActions { scenario },
        sessionRepositoryProvider: SessionRepositoryProvider = FakeSessionRepositoryProvider { scenario },
        userRepositoryActions: UserRepositoryActions = FakeUserRepositoryActions { scenario },
        userRepositoryProvider: UserRepositoryProvider = FakeUserRepositoryProvider { scenario },
        userSessionProvider: UserSessionProvider = FakeUserSessionProvider(),
        userPreferencesActions: UserPreferencesActions = FakeUserPreferencesActions{ scenario }
    ): DefaultSessionBootstrapper =
        DefaultSessionBootstrapper(
            sessionRepositoryActions = sessionRepositoryActions,
            sessionRepositoryProvider = sessionRepositoryProvider,
            userRepositoryActions = userRepositoryActions,
            userRepositoryProvider = userRepositoryProvider,
            userSessionProvider = userSessionProvider,
            userPreferencesActions = userPreferencesActions
        )
}
