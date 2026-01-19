package com.zibete.proyecto1.testing

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.zibete.proyecto1.R

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

fun setText(
    tag: String,
    text: String,
    composeRule: ComposeTestRule
) {
    val node = waitTag(tag, composeRule)
    node.performTextClearance()
    node.performTextInput(text)
}

fun pickBirthDateSetText(
    openDialogTag: String,
    dateText: String,
    composeRule: ComposeTestRule,
    context: Context
) {
    val okText = context.getString(R.string.action_accept)

    // Abrir diálogo
    waitTag(openDialogTag, composeRule).performClick()
    waitText(okText, composeRule)

    // Click en el 1er nodo clickeable dentro del diálogo (pencil)
    composeRule
        .onAllNodes(
            hasClickAction().and(hasAnyAncestor(isDialog())),
            useUnmergedTree = true
        )[0]
        .performClick()

    // Campo editable dentro del diálogo
    val dialogInput = hasSetTextAction().and(hasAnyAncestor(isDialog()))

    composeRule.onNode(dialogInput, useUnmergedTree = true)
        .performTextReplacement(dateText)

    // Confirmar
    waitText(okText, composeRule).performClick()
}