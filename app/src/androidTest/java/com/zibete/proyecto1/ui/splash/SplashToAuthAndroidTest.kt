package com.zibete.proyecto1.ui.splash

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.data.UserPreferencesActions
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserSessionActions
import com.zibete.proyecto1.data.UserSessionProvider
import com.zibete.proyecto1.domain.session.DeleteAccountResult
import com.zibete.proyecto1.domain.session.DeleteAccountUseCase
import com.zibete.proyecto1.domain.session.LogoutUseCase
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.utils.AppChecksProvider
import dagger.hilt.android.testing.UninstallModules
import com.zibete.proyecto1.di.AppBindingsModule
import com.zibete.proyecto1.domain.session.SessionBootstrapper
import com.zibete.proyecto1.testing.fakes.*

@UninstallModules(AppBindingsModule::class)
@HiltAndroidTest
class SplashToAuthAndroidTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<SplashActivity>()

    private val scenario = TestScenario(
        onboardingDone = true,
        hasInternet = true,
        currentUser = null // => NavigateAuth
    )

    @BindValue @JvmField
    val prefsProvider: UserPreferencesProvider = FakeUserPreferencesProvider(scenario)

    @BindValue @JvmField
    val checksProvider: AppChecksProvider = FakeAppChecksProvider(scenario)

    @BindValue @JvmField
    val sessionProvider: UserSessionProvider = FakeUserSessionProvider(scenario)

    @BindValue @JvmField
    val prefsActions: UserPreferencesActions = object : UserPreferencesActions {
        override suspend fun setOnboardingDone(done: Boolean) {}
        override suspend fun setFirstLoginDone(done: Boolean) {}
        override suspend fun setDeleteUser(done: Boolean) {}
    }

    @BindValue @JvmField
    val sessionActions: UserSessionActions = object : UserSessionActions {
        override suspend fun logOutCleanup() {}
        override suspend fun signInWithEmail(email: String, password: String) {}
        override suspend fun signInWithCredential(credential: AuthCredential) {}
        override suspend fun sendPasswordResetEmail(email: String) {}
        override suspend fun deleteFirebaseUser() {}
    }

    @BindValue @JvmField
    val deleteAccountUseCase: DeleteAccountUseCase = object : DeleteAccountUseCase {
        override suspend fun execute(): DeleteAccountResult {
            return DeleteAccountResult.Success
        }
    }


    @BindValue @JvmField
    val logoutUseCase: LogoutUseCase = object : LogoutUseCase {
        override suspend fun execute() {
        }
    }

    @BindValue @JvmField
    val userRepoActions: UserRepositoryActions = object : UserRepositoryActions {
        override suspend fun createUserNode(
            firebaseUser: FirebaseUser,
            birthDate: String,
            description: String
        ) {
        }

        override suspend fun setUserLastSeen() {
        }

        override suspend fun setUserActivityStatus(status: String) {
        }

        override suspend fun deleteMyAccountData() {
        }
    }

    @BindValue @JvmField
    val sessionBootstrapper: SessionBootstrapper = object : SessionBootstrapper {
        override suspend fun bootstrap(uid: String) {
            // no-op
        }
    }

    @Before fun setup() { hiltRule.inject() }

    @Test
    fun launch_navigatesToAuth_whenUserIsNull() {
        composeRule.waitUntil(timeoutMillis = 6_000) {
            composeRule.onAllNodesWithTag("auth_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("auth_screen").assertExists()
    }
}
