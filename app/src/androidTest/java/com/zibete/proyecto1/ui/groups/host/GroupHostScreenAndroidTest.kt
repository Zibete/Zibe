package com.zibete.proyecto1.ui.groups.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.theme.ZibeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupHostScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hostStartsInChatAndExposesMembersAndPrivateConversations() {
        composeRule.setContent {
            var state by remember { mutableStateOf(hostState()) }
            ZibeTheme {
                GroupHostScreen(
                    state = state,
                    onTabSelected = { state = state.copy(selectedTab = it) },
                    onComposerChanged = {},
                    onSendText = {},
                    onSendPhoto = {},
                    onRetrySend = {},
                    onDismissSendError = {},
                    onMemberSelected = {},
                    onDismissMemberActions = {},
                    onOpenProfile = {},
                    onOpenMemberPrivateChat = {},
                    onOpenPrivateConversation = {},
                    onRetryLoad = {},
                    onExitRequested = {}
                )
            }
        }

        composeRule.onNodeWithText("Todavía no hay mensajes. Iniciá la conversación.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Personas").performClick()
        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Alias seguro").assertIsDisplayed()

        composeRule.onNodeWithText("Privados").performClick()
        composeRule.onNodeWithText("Alias seguro").assertIsDisplayed()
        composeRule.onNodeWithText("Mensaje contextual").assertIsDisplayed()
    }

    private fun hostState() = GroupHostUiState(
        isLoading = false,
        groupContext = GroupContext(
            inGroup = true,
            groupName = "Sala Android",
            userName = "Ada",
            userType = 1,
            roomKey = "room-android",
            displayName = "Sala Android"
        ),
        currentUid = "me",
        creatorUid = "me",
        users = listOf(
            UserGroup(userId = "me", userName = "Ada", type = 1),
            UserGroup(userId = "other", userName = "Alias seguro", type = 0)
        ),
        privateConversations = listOf(
            Conversation(
                userId = "me",
                otherId = "other",
                otherName = "Alias seguro",
                lastContent = "Mensaje contextual",
                roomKey = "room-android"
            )
        )
    )
}
