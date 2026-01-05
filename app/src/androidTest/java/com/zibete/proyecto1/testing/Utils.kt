package com.zibete.proyecto1.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

private const val DEFAULT_TIMEOUT_MS = 10_000L

fun waitTag(
    tag: String,
    composeRule: ComposeTestRule,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    useUnmergedTree: Boolean = true
): SemanticsNodeInteraction {
    composeRule.waitUntil(timeoutMs) {
        composeRule
            .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }

    return composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertExists()
}

fun waitText(
    text: String,
    composeRule: ComposeTestRule,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS
): SemanticsNodeInteraction {
    composeRule.waitUntil(timeoutMs) {
        composeRule
            .onAllNodesWithText(text)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }

    return composeRule.onNodeWithText(text).assertExists()
}
