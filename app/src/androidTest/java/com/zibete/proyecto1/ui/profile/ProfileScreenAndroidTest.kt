package com.zibete.proyecto1.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.theme.ZibeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileUsesTheSharedFirstContactSheetAndCopy() {
        val confirmed = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        setProfileContent(
            state = readyState(pendingFirstContactUserId = USER_ID),
            onConfirmFirstContact = { confirmed.set(true) },
            onCancelFirstContact = { cancelled.set(true) }
        )

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar una nueva conversación").assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar chat").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.runOnIdle {
            assertTrue(confirmed.get())
            assertTrue(cancelled.get())
        }
    }

    @Test
    fun existingConversationKeepsDirectProfileChatActionWithoutSheet() {
        val requested = AtomicBoolean(false)
        setProfileContent(
            state = readyState(hasConversation = true),
            onDmChatClick = { requested.set(true) }
        )

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Chatea en Zibe")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle { assertTrue(requested.get()) }
    }

    @Test
    fun inactivePagerPageCannotRenderItsPendingFirstContactSheet() {
        setProfileContent(
            state = readyState(pendingFirstContactUserId = USER_ID),
            isActive = false
        )

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertDoesNotExist()
    }

    private fun setProfileContent(
        state: ProfileUiState,
        isActive: Boolean = true,
        onDmChatClick: () -> Unit = {},
        onConfirmFirstContact: () -> Unit = {},
        onCancelFirstContact: () -> Unit = {}
    ) {
        composeRule.setContent {
            ZibeTheme {
                ProfileScreen(
                    state = state,
                    userStatus = UserStatus.Online,
                    photoList = emptyList(),
                    groupName = "",
                    distanceLabel = "1 km",
                    isActive = isActive,
                    onBack = {},
                    onRefresh = {},
                    onToggleFavorite = {},
                    onDmChatClick = onDmChatClick,
                    onConfirmFirstContact = onConfirmFirstContact,
                    onCancelFirstContact = onCancelFirstContact,
                    onOpenGroupDmChat = {},
                    onOpenPhoto = {},
                    onToggleNotifications = {},
                    onConfirmBlockAction = {},
                    onDeleteChoiceMode = {},
                    onConfirmHide = {}
                )
            }
        }
    }

    private fun readyState(
        pendingFirstContactUserId: String? = null,
        hasConversation: Boolean = false
    ): ProfileUiState {
        val profile = Users(
            id = USER_ID,
            name = "Ada",
            birthDate = "1997-01-01"
        )
        return ProfileUiState(
            content = ProfileContent.Ready(profile),
            profile = profile,
            hasConversation = hasConversation,
            pendingFirstContactUserId = pendingFirstContactUserId
        )
    }

    private companion object {
        const val USER_ID = "profile-uid"
    }
}
