package com.zibete.proyecto1.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

fun waitTag(tag: String, composeRule: ComposeTestRule) {
    composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
}

fun waitText(text: String, composeRule: ComposeTestRule) {
    composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText(text).assertExists()
}