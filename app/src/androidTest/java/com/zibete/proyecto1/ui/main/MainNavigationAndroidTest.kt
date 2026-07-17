package com.zibete.proyecto1.ui.main

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.performClick
import androidx.appcompat.R as AppCompatR
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getChatId
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_TYPE
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_AVATAR
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_EDIT_PROFILE
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_LOGOUT
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_SETTINGS
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_SHEET
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_FILTER_SHEET
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SCREEN
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.data.ConversationOverviewRepository
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.testing.BaseHiltComposeManualLaunchTest
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.testing.waitTag
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.users.DiscoverToolbarHandler
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MainNavigationAndroidTest :
    BaseHiltComposeManualLaunchTest<MainActivity>(MainActivity::class.java) {

    @Inject
    lateinit var conversationOverviewRepository: ConversationOverviewRepository

    @Inject
    lateinit var groupRepositoryProvider: GroupRepositoryProvider

    private val chatBadgeCount = MutableStateFlow(0)
    private val groupBadgeCount = MutableStateFlow(0)
    private lateinit var mainScenario: ActivityScenario<MainActivity>

    @Before
    fun prepareMain() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        every { conversationOverviewRepository.observeUnreadChatList() } returns chatBadgeCount
        every { groupRepositoryProvider.unreadGroupBadgeCount(any()) } returns groupBadgeCount
        every { groupRepositoryProvider.observeUnreadGroupChat(any()) } returns flowOf(0)
        every { groupRepositoryProvider.observeUnreadPrivateMessages() } returns flowOf(0)
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun mainStartsInChatsWithExpectedRootOrderAndNoDrawer() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)

        launchWithCurrentActivity { activity ->
            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
            assertEquals(R.id.navBottomChat, bottomNav.selectedItemId)
            assertEquals(
                listOf(
                    R.id.navBottomUsers,
                    R.id.navBottomChat,
                    R.id.navBottomGroups,
                    R.id.navBottomFavorites
                ),
                (0 until bottomNav.menu.size()).map { bottomNav.menu.getItem(it).itemId }
            )
            assertNull(
                activity.findViewById<View>(
                    activity.resources.getIdentifier("drawerLayout", "id", activity.packageName)
                )
            )
            assertNull(activity.findViewById<MaterialToolbar>(R.id.materialToolbar).navigationIcon)
        }
    }

    @Test
    fun roomsAndFavoritesItemsNavigateToTheirRootDestinations() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)

        selectBottomItem(R.id.navBottomGroups)
        waitForDestination(R.id.nav_group_select)

        selectBottomItem(R.id.navBottomFavorites)
        waitForDestination(R.id.nav_favorites)

        launchWithCurrentActivity { activity ->
            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
            assertEquals(R.id.navBottomFavorites, bottomNav.selectedItemId)
        }
    }

    @Test
    fun discoverToolbarShowsSearchFilterWithoutRefreshOrHamburger() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        selectBottomItem(R.id.navBottomUsers)
        waitForDestination(R.id.nav_users)
        waitTag(DISCOVER_SCREEN, composeRule)

        onView(withId(R.id.action_search)).check(matches(isDisplayed()))
        onView(withId(R.id.action_discover_filter)).check(matches(isDisplayed()))
        composeRule.onNodeWithTag(ACCOUNT_AVATAR).assertExists()

        launchWithCurrentActivity { activity ->
            val toolbar = activity.findViewById<MaterialToolbar>(R.id.materialToolbar)
            assertNull(toolbar.navigationIcon)
            assertEquals(
                0,
                activity.resources.getIdentifier(
                    "action_refresh",
                    "id",
                    activity.packageName
                )
            )
        }
    }

    @Test
    fun discoverSearchExpandsDelegatesQueryAndBackClearsIt() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        selectBottomItem(R.id.navBottomUsers)
        waitForDestination(R.id.nav_users)
        waitTag(DISCOVER_SCREEN, composeRule)

        onView(withId(R.id.action_search)).perform(click())
        onView(withId(AppCompatR.id.search_src_text))
            .check(matches(isDisplayed()))
            .perform(replaceText("sin resultados"))
        composeRule.waitUntil {
            composeRule.onNodeWithText("Limpiar filtros").isDisplayed()
        }

        launchWithCurrentActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            !composeRule.onNodeWithText("Limpiar filtros").isDisplayed()
        }
    }

    @Test
    fun discoverFilterOpensSheetAndActiveStateTintsToolbarIcon() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        selectBottomItem(R.id.navBottomUsers)
        waitForDestination(R.id.nav_users)
        waitTag(DISCOVER_SCREEN, composeRule)

        onView(withId(R.id.action_discover_filter)).perform(click())
        waitTag(DISCOVER_FILTER_SHEET, composeRule)
        composeRule.onNodeWithText("Solo en línea").performClick()
        composeRule.onNodeWithText("Aplicar filtros").performClick()

        composeRule.waitUntil {
            var active = false
            launchWithCurrentActivity { activity ->
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val handler = navHost.childFragmentManager.primaryNavigationFragment
                    as DiscoverToolbarHandler
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.materialToolbar)
                active = handler.hasActiveFilters &&
                    toolbar.menu.findItem(R.id.action_discover_filter)
                        .iconTintList?.defaultColor == ContextCompat.getColor(
                        activity,
                        DsR.color.accent
                    )
            }
            active
        }
        composeRule.onNodeWithTag(DISCOVER_FILTER_SHEET).assertDoesNotExist()
    }

    @Test
    fun backFromSettingsReturnsToPreviousRootAndRestoresBottomSelection() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_AVATAR).performClick()
        waitTag(ACCOUNT_SHEET, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_SETTINGS).performClick()
        waitForDestination(R.id.settingsFragment)

        pressBack()

        waitForDestination(R.id.nav_chat_list)
        launchWithCurrentActivity { activity ->
            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
            assertEquals(R.id.navBottomChat, bottomNav.selectedItemId)
        }
    }

    @Test
    fun pendingDmIntentOpensChatWithOtherUidAndDmNode() {
        val otherUid = "other_uid"
        intending(hasComponent(ChatActivity::class.java.name)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_PENDING_DM_TYPE, NODE_DM)
            putExtra(EXTRA_PENDING_DM_CHAT_ID, getChatId(TestData.UID, otherUid))
        }

        launchMain(intent = intent)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            intendedSafely(
                allOf(
                    hasComponent(ChatActivity::class.java.name),
                    hasExtra(EXTRA_CHAT_ID, otherUid),
                    hasExtra(EXTRA_CHAT_NODE, NODE_DM)
                )
            )
        }
    }

    @Test
    fun unreadCountsUpdateChatAndRoomsBadges() {
        launchMain(
            scenario = TestScenario(
                currentUserUid = TestData.UID,
                onboardingDone = true,
                firstLoginDone = true,
                hasLocationPermission = true,
                inGroup = true,
                groupName = "sala-test"
            )
        )
        waitTag(ACCOUNT_AVATAR, composeRule)

        chatBadgeCount.value = 4
        groupBadgeCount.value = 2

        composeRule.waitUntil(timeoutMillis = 10_000) {
            var badgesMatch = false
            launchWithCurrentActivity { activity ->
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                val chatBadge = bottomNav.getBadge(R.id.navBottomChat)
                val groupBadge = bottomNav.getBadge(R.id.navBottomGroups)
                badgesMatch = chatBadge?.isVisible == true && chatBadge.number == 4 &&
                    groupBadge?.isVisible == true && groupBadge.number == 2
            }
            badgesMatch
        }

        chatBadgeCount.value = 0
        groupBadgeCount.value = 0

        composeRule.waitUntil(timeoutMillis = 10_000) {
            var badgesHidden = false
            launchWithCurrentActivity { activity ->
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                badgesHidden = bottomNav.getBadge(R.id.navBottomChat)?.isVisible == false &&
                    bottomNav.getBadge(R.id.navBottomGroups)?.isVisible == false
            }
            badgesHidden
        }
    }

    @Test
    fun logoutRequiresConfirmationAndConfirmedActionNavigatesToSplash() {
        intending(hasComponent(SplashActivity::class.java.name)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)

        openLogoutConfirmation()
        onView(withText(R.string.dialog_logout_message))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())

        composeRule.onNodeWithTag(ACCOUNT_AVATAR).assertExists()
        assertFalse(intendedSafely(hasComponent(SplashActivity::class.java.name)))

        openLogoutConfirmation()
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

        composeRule.waitUntil(timeoutMillis = 10_000) {
            intendedSafely(hasComponent(SplashActivity::class.java.name))
        }
    }

    @Test
    fun accountAvatarOpensSheetAndEditProfileNavigation() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_AVATAR).performClick()
        waitTag(ACCOUNT_SHEET, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_EDIT_PROFILE).performClick()

        waitForDestination(R.id.editProfileFragment)
        composeRule.onNodeWithTag(ACCOUNT_SHEET).assertDoesNotExist()
    }

    @Test
    fun accountSettingsActionNavigatesWithoutReopeningSheet() {
        launchMain()
        waitTag(ACCOUNT_AVATAR, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_AVATAR).performClick()
        waitTag(ACCOUNT_SHEET, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_SETTINGS).performClick()

        waitForDestination(R.id.settingsFragment)
        composeRule.onNodeWithTag(ACCOUNT_SHEET).assertDoesNotExist()
    }

    private fun launchMain(
        scenario: TestScenario = TestScenario(
            currentUserUid = TestData.UID,
            onboardingDone = true,
            firstLoginDone = true,
            hasLocationPermission = true
        ),
        intent: Intent? = null
    ) {
        mainScenario = launchWithScenario(scenario, intent)
    }

    private fun openLogoutConfirmation() {
        composeRule.onNodeWithTag(ACCOUNT_AVATAR).performClick()
        waitTag(ACCOUNT_SHEET, composeRule)
        composeRule.onNodeWithTag(ACCOUNT_LOGOUT).performClick()
    }

    private fun selectBottomItem(itemId: Int) {
        launchWithCurrentActivity { activity ->
            activity.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = itemId
        }
    }

    private fun waitForDestination(destinationId: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            currentDestinationId() == destinationId
        }
    }

    private fun currentDestinationId(): Int? {
        var destinationId: Int? = null
        launchWithCurrentActivity { activity ->
            destinationId = activity.findNavController(R.id.nav_host_fragment)
                .currentDestination?.id
        }
        return destinationId
    }

    private fun intendedSafely(matcher: org.hamcrest.Matcher<Intent>): Boolean = try {
        Intents.intended(matcher)
        true
    } catch (_: AssertionError) {
        false
    } catch (_: NoActivityResumedException) {
        false
    }

    private fun launchWithCurrentActivity(block: (MainActivity) -> Unit) {
        mainScenario.onActivity(block)
    }
}
