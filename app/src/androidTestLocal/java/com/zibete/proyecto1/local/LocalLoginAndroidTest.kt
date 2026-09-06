package com.zibete.proyecto1.local

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.ui.groups.RoomsTestTags
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production login/navigation UI and real repositories, using only fictional emulator accounts. */
@RunWith(AndroidJUnit4::class)
class LocalLoginAndroidTest {
    private var fixtureUid: String? = null

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test(timeout = 180_000)
    fun emailLoginBootstrapsSessionAndOpensRooms() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val auth = FirebaseAuth.getInstance()
        val email = "login-${UUID.randomUUID()}@example.invalid"
        val password = "Local-login-123!"
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext, LocalBackendProbeEntryPoint::class.java
        )
        val uid = runBlocking {
            withTimeout(20_000) {
                auth.signOut()
                val user = checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)
                FirebaseDatabase.getInstance().getReference("Users/Accounts/${user.uid}").setValue(
                    mapOf(
                        "id" to user.uid, "name" to "Prueba Local", "email" to email,
                        "birthDate" to "1995-01-01", "age" to 31, "createdAt" to System.currentTimeMillis(),
                        "photoUrl" to "", "isOnline" to false, "description" to "Cuenta ficticia",
                        "latitude" to -34.6037, "longitude" to -58.3816
                    )
                ).await()
                entry.preferencesActions().setOnboardingDone(true)
                entry.preferencesActions().setFirstLoginDone(true)
                auth.signOut()
                user.uid
            }
        }
        fixtureUid = uid
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ).forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }

        ActivityScenario.launch(SplashActivity::class.java).use {
            compose.waitUntil(20_000) {
                compose.onAllNodesWithTag(AUTH_SCREEN).fetchSemanticsNodes().isNotEmpty()
            }
            val inputs = compose.onAllNodes(hasSetTextAction())
            inputs[0].performTextInput(email)
            inputs[1].performTextInput(password)
            compose.onNodeWithText(context.getString(R.string.action_login))
                .performScrollTo().performClick()
            compose.waitUntil(25_000) {
                var mainVisible = false
                instrumentation.runOnMainSync {
                    mainVisible = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED).any { activity -> activity is MainActivity }
                }
                mainVisible
            }
            assertEquals(uid, auth.currentUser?.uid)
            onView(withId(R.id.navBottomGroups)).perform(click())
            compose.waitUntil(20_000) {
                compose.onAllNodesWithTag(RoomsTestTags.SCREEN).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(RoomsTestTags.SCREEN).assertIsDisplayed()
            val capture = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "local-login-rooms-${System.currentTimeMillis()}.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ZibeLocal")
            }
            val destination = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, capture))
            checkNotNull(context.contentResolver.openOutputStream(destination)).use { stream ->
                checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                    .compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            runBlocking {
                withTimeout(10_000) {
                    assertEquals(entry.sessionProvider().getLocalInstallId(), entry.sessionProvider().getInstallId(uid))
                }
            }
        }
    }

    @After
    fun cleanUpFictionalSession() {
        val uid = fixtureUid ?: return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val auth = FirebaseAuth.getInstance()
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, LocalBackendProbeEntryPoint::class.java)
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filter { it.packageName == context.packageName }.forEach { it.finish() }
        }
        runBlocking {
            withTimeout(10_000) {
                if (auth.currentUser?.uid == uid) {
                    FirebaseDatabase.getInstance().getReference("Users/Accounts/$uid").removeValue().await()
                    entry.sessionActions().clearSession(uid)
                    auth.currentUser?.delete()?.await()
                }
                auth.signOut()
            }
        }
    }
}
