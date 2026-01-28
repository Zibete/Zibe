package com.zibete.proyecto1.domain.session

import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.SessionRepositoryActions
import com.zibete.proyecto1.data.SessionRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.fakes.ActiveSessionCall
import com.zibete.proyecto1.fakes.FakeAuthSessionProvider
import com.zibete.proyecto1.fakes.FakeSessionRepositoryActions
import com.zibete.proyecto1.fakes.FakeSessionRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryActions
import com.zibete.proyecto1.fakes.FakeUserRepositoryProvider
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionBootstrapperTest {

    @Test
    fun flow_bootstrap_newUser_withCurrentUser_createsNode_setsFirstLoginDone_false() = runTest {
        // Given
        val scenario = TestScenario(
            accountExists = false,
            firstLoginDone = true
        )
        val firebaseUser = mockk<FirebaseUser>(relaxed = true)
        every { firebaseUser.uid } returns TestData.UID
        
        val authSessionProvider = FakeAuthSessionProvider(
            currentUser = firebaseUser
        )
        val userRepositoryActions = FakeUserRepositoryActions { scenario }
        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            userRepositoryActions = userRepositoryActions,
            authSessionProvider = authSessionProvider
        )

        // When
        bootstrapper.bootstrap(uid = TestData.UID)
        advanceUntilIdle()

        // Then
        val lastCall = userRepositoryActions.lastCreateUserNodeCall
        assertTrue("Expected createUserNode to be called", lastCall != null)
        assertEquals(firebaseUser, lastCall?.user)
        assertEquals("", lastCall?.birthDate)
        assertEquals("", lastCall?.description)
        
        assertFalse(scenario.firstLoginDone)
    }

    @Test
    fun flow_bootstrap_newUser_withoutCurrentUser_doesNotCreateNode_setsFirstLoginDone_false() =
        runTest {
            // Given
            val scenario = TestScenario(
                accountExists = false, // new user
                firstLoginDone = true
            )
            val userRepositoryActions = FakeUserRepositoryActions { scenario }

            val authSessionProvider = FakeAuthSessionProvider(
                currentUser = null
            )

            val bootstrapper = buildBootstrapper(
                scenario = scenario,
                userRepositoryActions = userRepositoryActions,
                authSessionProvider = authSessionProvider
            )

            // When
            bootstrapper.bootstrap(uid = TestData.UID)
            advanceUntilIdle()

            // Then
            assertNull(userRepositoryActions.lastCreateUserNodeCall)
            assertFalse(scenario.firstLoginDone)
        }

    @Test
    fun flow_bootstrap_existingUser_withBirthDate_doesNotCreateNode_setsFirstLoginDone_true() =
        runTest {
            // Given
            val scenario = TestScenario(
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
            bootstrapper.bootstrap(uid = TestData.UID)
            advanceUntilIdle()

            // Then
            assertNull(userRepositoryActions.lastCreateUserNodeCall)
            assertTrue(scenario.firstLoginDone)
        }

    @Test
    fun flow_bootstrap_existingUser_withoutBirthDate_doesNotCreateNode_setsFirstLoginDone_false() =
        runTest {
            // Given
            val scenario = TestScenario(
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
            bootstrapper.bootstrap(uid = TestData.UID)
            advanceUntilIdle()

            // Then
            assertNull(userRepositoryActions.lastCreateUserNodeCall)
            assertFalse(scenario.firstLoginDone)
        }

    @Test
    fun flow_bootstrap_success_setActiveSession() = runTest {
        // Given
        val scenario = TestScenario()
        val sessionRepositoryActions = FakeSessionRepositoryActions { scenario }
        val bootstrapper = buildBootstrapper(
            scenario = scenario,
            sessionRepositoryActions = sessionRepositoryActions
        )

        // When
        bootstrapper.bootstrap(uid = TestData.UID)
        advanceUntilIdle()

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

    private fun buildBootstrapper(
        scenario: TestScenario = TestScenario(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(),
        sessionRepositoryActions: SessionRepositoryActions = FakeSessionRepositoryActions { scenario },
        sessionRepositoryProvider: SessionRepositoryProvider = FakeSessionRepositoryProvider { scenario },
        userRepositoryActions: UserRepositoryActions = FakeUserRepositoryActions { scenario },
        userRepositoryProvider: UserRepositoryProvider = FakeUserRepositoryProvider { scenario },
        userPreferencesActions: UserPreferencesActions = FakeUserPreferencesActions { scenario }
    ): DefaultSessionBootstrapper =
        DefaultSessionBootstrapper(
            authSessionProvider = authSessionProvider,
            sessionRepositoryActions = sessionRepositoryActions,
            sessionRepositoryProvider = sessionRepositoryProvider,
            userRepositoryActions = userRepositoryActions,
            userRepositoryProvider = userRepositoryProvider,
            userPreferencesActions = userPreferencesActions
        )
}
