package com.zibete.proyecto1.ui.signup

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.TestTags
import com.zibete.proyecto1.core.constants.Constants.UiTags.AUTH_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.SIGNUP_SCREEN
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
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SignUpValidationAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    val actionRegister = context.getString(R.string.action_register)
    val emailRequired = context.getString(R.string.err_email_required)
    val passwordRequired = context.getString(R.string.err_password_required)
    val nameRequired = context.getString(R.string.signup_err_name_required)
    val birthdateRequired = context.getString(R.string.signup_err_birthdate_required)
    val underAge = context.getString(R.string.err_under_age)

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
        composeRule.onNodeWithText(actionRegister, useUnmergedTree = true).performClick()
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankEmail_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(email = "", composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(emailRequired, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankPassword_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(password = "", composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(passwordRequired, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankName_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(name = "", composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(nameRequired, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_blankBirthdate_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(birthdate = "", composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(birthdateRequired, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }

    @Test
    fun flow_onRegister_underAge_showsSnack_andStaysOnSignUp() {
        // Given
        fillValidForm(birthdate = "2025-01-01", composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        waitText(underAge, composeRule)
        waitTag(SIGNUP_SCREEN, composeRule)
    }
}

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SignUpFailureAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    val navigateToSignUpButton = context.getString(R.string.action_register)
    val errPrefixOnly = context.getString(R.string.err_zibe_prefix).substringBefore("%1")

    @Before
    fun setup() {
        Intents.init()
        try {
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
            composeRule.onNodeWithText(navigateToSignUpButton, useUnmergedTree = true).performClick()
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
        fillValidForm(composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        // Then
        composeRule.onNode(
            hasText(errPrefixOnly, substring = true),
            useUnmergedTree = true
        ).assertExists()

        composeRule.onNodeWithTag(SIGNUP_SCREEN, useUnmergedTree = true).assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SignUpSuccessAndroidTest :
    BaseHiltComposeManualLaunchTest<SplashActivity>(SplashActivity::class.java) {

    private lateinit var scenario: TestScenario
    val navigateToSignUpButton = context.getString(R.string.action_register)
    val signupSuccess = context.getString(R.string.signup_msg_success)

    @Before
    fun setup() {
        Intents.init()
        try {
            val intent = Intent(context, SplashActivity::class.java)

            scenario = TestScenario(
                onboardingDone = true,
                currentUserUid = null,
                shouldFail = false
            )

            launchWithScenario(
                scenario = scenario,
                intent = intent
            )

            waitTag(AUTH_SCREEN, composeRule)
            composeRule.onNodeWithText(navigateToSignUpButton, useUnmergedTree = true).performClick()
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
        fillValidForm(composeRule = composeRule, context = context)
        closeSoftKeyboard()

        // When
        waitTag(TestTags.BTN_REGISTER, composeRule).performClick()

        scenario.currentUserUid = TestData.UID

        // Then
        waitText(signupSuccess, composeRule)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(MainActivity::class.java.name))
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}

fun fillValidForm(
    email: String = TestData.EMAIL,
    password: String = TestData.PASSWORD,
    name: String = TestData.NAME,
    birthdate: String = TestData.BIRTHDATE,
    description: String = TestData.DESCRIPTION,
    composeRule: ComposeTestRule,
    context: Context
) {
    setText(TestTags.EMAIL, email, composeRule)
    setText(TestTags.PASSWORD, password, composeRule)
    setText(TestTags.NAME, name, composeRule)
    if (birthdate.isNotBlank()) {
        pickBirthDateSetText(
            openDialogTag = TestTags.BIRTHDATE_PICKER,
            dateText = LocalDate.parse(birthdate).format(DateTimeFormatter.ofPattern("ddMMyyyy")),
            composeRule = composeRule,
            context = context
        )
    }
    setText(TestTags.DESCRIPTION, description, composeRule)
}