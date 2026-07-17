package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
import com.zibete.proyecto1.core.constants.Constants.UiTags.PROFILE_PAGER
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocationRepositoryProvider
import com.zibete.proyecto1.data.profile.BlockState
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.domain.chat.DmEntryDecision
import com.zibete.proyecto1.domain.chat.ResolveDmEntryUseCase
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.theme.ZibeTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ProfileActivityContentAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun pagerSwipeCancelsOldGateAndEmitsOnlyTheActivePageUid() {
        val firstGate = CompletableDeferred<ZibeResult<DmEntryDecision>>()
        val secondGate = CompletableDeferred<ZibeResult<DmEntryDecision>>()
        val openedUserIds = CopyOnWriteArrayList<String>()
        val viewModels = mapOf(
            FIRST_UID to viewModel(FIRST_UID, "Ada", firstGate),
            SECOND_UID to viewModel(SECOND_UID, "Bruno", secondGate)
        )

        setActivityContent(
            userIds = listOf(FIRST_UID, SECOND_UID),
            openedUserIds = openedUserIds,
            viewModels = viewModels
        )

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chatea en Zibe").performClick()
        composeRule.onNodeWithTag(PROFILE_PAGER).performTouchInput { swipeLeft() }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithText("Bruno").assertIsDisplayed() }.isSuccess
        }

        firstGate.complete(ZibeResult.Success(DmEntryDecision.OpenExisting))
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), openedUserIds.toList())
        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Chatea en Zibe").performClick()
        secondGate.complete(ZibeResult.Success(DmEntryDecision.OpenExisting))
        composeRule.waitUntil(5_000) { openedUserIds.isNotEmpty() }

        assertEquals(listOf(SECOND_UID), openedUserIds.toList())
    }

    @Test
    fun singleProfilePathUsedByFavoritesRequiresFirstContactConfirmation() {
        val openedUserIds = CopyOnWriteArrayList<String>()
        val gate = CompletableDeferred<ZibeResult<DmEntryDecision>>().apply {
            complete(ZibeResult.Success(DmEntryDecision.RequireFirstContactConfirmation))
        }
        val viewModel = viewModel(FIRST_UID, "Ada", gate)

        setActivityContent(
            userIds = listOf(FIRST_UID),
            openedUserIds = openedUserIds,
            viewModels = mapOf(FIRST_UID to viewModel)
        )

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chatea en Zibe").performClick()
        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar chat").performClick()
        composeRule.waitUntil(5_000) { openedUserIds.isNotEmpty() }

        assertEquals(listOf(FIRST_UID), openedUserIds.toList())
    }

    @Test
    fun singleProfilePathUsedByChatActivityOpensExistingConversationDirectly() {
        val openedUserIds = CopyOnWriteArrayList<String>()
        val gate = CompletableDeferred<ZibeResult<DmEntryDecision>>().apply {
            complete(ZibeResult.Success(DmEntryDecision.OpenExisting))
        }
        val viewModel = viewModel(FIRST_UID, "Ada", gate)

        setActivityContent(
            userIds = listOf(FIRST_UID),
            openedUserIds = openedUserIds,
            viewModels = mapOf(FIRST_UID to viewModel)
        )

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chatea en Zibe").performClick()
        composeRule.waitUntil(5_000) { openedUserIds.isNotEmpty() }

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertDoesNotExist()
        assertEquals(listOf(FIRST_UID), openedUserIds.toList())
    }

    private fun setActivityContent(
        userIds: List<String>,
        openedUserIds: MutableList<String>,
        viewModels: Map<String, ProfileViewModel>
    ) {
        composeRule.setContent {
            ZibeTheme {
                ProfileActivityContent(
                    userIds = userIds,
                    startIndex = 0,
                    onBack = {},
                    onOpenDmChat = { openedUserIds.add(it) },
                    onOpenGroupDmChat = {},
                    onOpenPhoto = {},
                    viewModelProvider = viewModels::getValue
                )
            }
        }
    }

    private fun viewModel(
        userId: String,
        name: String,
        gateResult: CompletableDeferred<ZibeResult<DmEntryDecision>>
    ): ProfileViewModel {
        val chatRepository = mockk<ChatRepositoryContract>(relaxed = true)
        val groupRepository = mockk<GroupRepositoryProvider>(relaxed = true)
        val locationRepository = mockk<LocationRepositoryProvider>()
        val profileProvider = mockk<ProfileRepositoryProvider>()
        val gate = mockk<ResolveDmEntryUseCase>()
        val profile = Users(
            id = userId,
            name = name,
            birthDate = "1997-01-01"
        )

        every { profileProvider.observeUserStatus(userId, NODE_DM) } returns
            flowOf(UserStatus.Offline)
        coEvery { profileProvider.getOtherAccount(userId) } returns ZibeResult.Success(profile)
        coEvery { profileProvider.getMyChatState(userId) } returns ZibeResult.Success("")
        coEvery { profileProvider.isFavorite(userId) } returns ZibeResult.Success(false)
        coEvery { profileProvider.getBlockStateWith(userId, NODE_DM) } returns
            ZibeResult.Success(BlockState(isBlockedByMe = false, hasBlockedMe = false))
        coEvery { profileProvider.getDmPhotoList(userId, NODE_DM) } returns
            ZibeResult.Success(emptyList())
        coEvery { locationRepository.getDistanceToUser(userId) } returns ZibeResult.Success("")
        coEvery { groupRepository.isGroupMatch(userId, any()) } returns ZibeResult.Success(false)
        coEvery { chatRepository.hasConversation(userId, NODE_DM) } returns
            ZibeResult.Success(false)
        coEvery { gate(userId) } coAnswers { gateResult.await() }

        return ProfileViewModel(
            savedStateHandle = SavedStateHandle(mapOf(EXTRA_USER_ID to userId)),
            chatRepository = chatRepository,
            groupRepositoryProvider = groupRepository,
            locationRepository = locationRepository,
            profileRepositoryProvider = profileProvider,
            profileRepositoryActions = mockk<ProfileRepositoryActions>(relaxed = true),
            userPreferencesProvider = FakeUserPreferencesProvider { TestScenario() },
            resolveDmEntry = gate,
            snackBarManager = SnackBarManager()
        )
    }

    private companion object {
        const val FIRST_UID = "first-uid"
        const val SECOND_UID = "second-uid"
    }
}
