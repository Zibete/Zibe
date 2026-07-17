package com.zibete.proyecto1.ui.chat.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.constants.Constants.UiTags.CHAT_TOP_BAR
import com.zibete.proyecto1.core.constants.Constants.UiTags.CHAT_TOP_BAR_OVERFLOW
import com.zibete.proyecto1.ui.theme.ZibeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatTopBarAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun normalModeKeepsBackProfileAndOverflowInteractions() {
        val back = AtomicBoolean(false)
        val profile = AtomicBoolean(false)

        setTopBar(
            onBackClick = { back.set(true) },
            onProfileClick = { profile.set(true) }
        )

        composeRule.onNodeWithTag(CHAT_TOP_BAR).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.onNodeWithText("Ada").performClick()
        composeRule.onNodeWithTag(CHAT_TOP_BAR_OVERFLOW).performClick()
        composeRule.onNodeWithText("Eliminar chat").assertIsDisplayed()

        composeRule.runOnIdle {
            assertTrue(back.get())
            assertTrue(profile.get())
        }
    }

    @Test
    fun selectionModeUsesSameTopBarAndKeepsClearAndDeleteInteractions() {
        val cleared = AtomicBoolean(false)
        val deleted = AtomicBoolean(false)

        setTopBar(
            selectionCount = 3,
            onClearSelection = { cleared.set(true) },
            onDeleteSelected = { deleted.set(true) }
        )

        composeRule.onNodeWithTag(CHAT_TOP_BAR).assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_TOP_BAR_OVERFLOW).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.onNodeWithContentDescription("Eliminar").performClick()

        composeRule.runOnIdle {
            assertTrue(cleared.get())
            assertTrue(deleted.get())
        }
    }

    private fun setTopBar(
        selectionCount: Int = 0,
        onBackClick: () -> Unit = {},
        onProfileClick: () -> Unit = {},
        onDeleteSelected: () -> Unit = {},
        onClearSelection: () -> Unit = {}
    ) {
        composeRule.setContent {
            ZibeTheme {
                ChatTopBar(
                    name = "Ada",
                    status = "en línea",
                    photoUrl = null,
                    notificationsEnabled = true,
                    selectionCount = selectionCount,
                    onBackClick = onBackClick,
                    onProfileClick = onProfileClick,
                    onToggleNotifications = {},
                    onDeleteChat = {},
                    onHideChat = {},
                    onDeleteSelected = onDeleteSelected,
                    onClearSelection = onClearSelection
                )
            }
        }
    }
}
