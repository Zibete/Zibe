package com.zibete.proyecto1.ui.signup

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.zibete.proyecto1.core.constants.BUTTON_REGISTER
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
import com.zibete.proyecto1.core.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.core.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_NAME_REQUIRED
import com.zibete.proyecto1.core.constants.SIGNUP_ERR_UNEXPECTED_PREFIX
import com.zibete.proyecto1.core.constants.SIGNUP_MSG_SUCCESS
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.pickBirthDateSetText
import com.zibete.proyecto1.testing.setText
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.testing.waitText
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltAndroidTest
class SignUpValidationAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        launchWithScenario(
            scenario = TestScenario(
                onboardingDone = true,
                currentUserUid = null,
                shouldFail = false
            )
        )
        waitTag(AUTH_SCREEN, composeRule)
        composeRule.onNodeWithText(BUTTON_REGISTER, useUnmergedTree = true).performClick()
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankEmail_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(email = "", composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(ERR_EMAIL_REQUIRED, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankPassword_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(password = "", composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(ERR_PASSWORD_REQUIRED, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankName_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(name = "", composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(SIGNUP_ERR_NAME_REQUIRED, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankBirthdate_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(birthdate = "", composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(SIGNUP_ERR_BIRTHDAY_REQUIRED, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_underAge_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(birthdate = "2025-01-01", composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(ERR_UNDER_AGE, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }
}

@HiltAndroidTest
class SignUpFailureAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        Intents.init()
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = Intent(context, SplashActivity::class.java)

            launchWithScenario(
                scenario = TestScenario(
                    onboardingDone = true,
                    currentUserUid = null,
                    shouldFail = true
                ),
                intent = intent
            )

            waitTag(AUTH_SCREEN, composeRule)
            composeRule.onNodeWithText(BUTTON_REGISTER, useUnmergedTree = true).performClick()
            waitTag(SIGNUP_SCREEN, composeRule)

        } catch (t: Throwable) {
            Intents.release()
            throw t
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun flow_onRegister_failure_showsErrorSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        //Then
        composeRule.onNode(
            hasText(SIGNUP_ERR_UNEXPECTED_PREFIX, substring = true),
            useUnmergedTree = true
        ).assertExists()

        composeRule.onNodeWithTag(SIGNUP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
    }
}

@HiltAndroidTest
class SignUpSuccessAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    @Before
    fun setup() {
        Intents.init()
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = Intent(context, SplashActivity::class.java)

            launchWithScenario(
                scenario = TestScenario(
                    onboardingDone = true,
                    currentUserUid = null,
                    shouldFail = false
                ),
                intent = intent
            )

            waitTag(AUTH_SCREEN, composeRule)
            composeRule.onNodeWithText(BUTTON_REGISTER, useUnmergedTree = true).performClick()
            waitTag(SIGNUP_SCREEN, composeRule)

        } catch (t: Throwable) {
            Intents.release()
            throw t
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun flow_onRegister_success_showsSuccessSnack_andNavigatesToSplash_andIntentToMain() {
        // Given
        fillValidForm(composeRule = composeRule)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(SIGNUP_MSG_SUCCESS, composeRule)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(MainActivity::class.java.name))
                true
            } catch (_: AssertionError) { false }
        }
    }
}

fun fillValidForm(
    email: String = TestData.EMAIL,
    password: String = TestData.PASSWORD,
    name: String = TestData.NAME,
    birthdate: String = TestData.BIRTHDATE,
    description: String = TestData.DESCRIPTION,
    composeRule: ComposeTestRule
) {
    setText(TestTags.EMAIL, email, composeRule)
    setText(TestTags.PASSWORD, password, composeRule)
    setText(TestTags.NAME, name, composeRule)
    if (birthdate.isNotBlank()){
        pickBirthDateSetText(
            openDialogTag = TestTags.BIRTHDATE,
            dateText = LocalDate.parse(birthdate).format(DateTimeFormatter.ofPattern("ddMMyyyy")),
            composeRule = composeRule
        )
    }
    setText(TestTags.DESCRIPTION, description, composeRule)
}
