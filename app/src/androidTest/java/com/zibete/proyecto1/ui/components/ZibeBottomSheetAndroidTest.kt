package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.ui.theme.ZibeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZibeBottomSheetAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun compactSheetKeepsFooterFixedWhileContentScrolls() {
        val confirmed = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)

        composeRule.setContent {
            ZibeTheme {
                ZibeBottomSheet(
                    isOpen = true,
                    onCancel = { cancelled.set(true) },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    modifier = Modifier.height(320.dp),
                    contentModifier = Modifier.testTag(CONTENT_TAG),
                    footer = {
                        SheetActions(
                            confirmText = "Aplicar filtros",
                            cancelText = "Cancelar",
                            onConfirm = { confirmed.set(true) },
                            onCancel = { cancelled.set(true) }
                        )
                    }
                ) {
                    repeat(14) { index -> Text("Control $index") }
                }
            }
        }

        composeRule.onNodeWithTag(CONTENT_TAG).assert(hasScrollAction())
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar filtros").assertIsDisplayed()

        composeRule.onNodeWithText("Control 13").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Aplicar filtros").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertTrue(cancelled.get())
            assertTrue(confirmed.get())
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun expandedFormShowsHeaderAndFooterBeforeInteractionAndKeepsFooterWithKeyboard() {
        lateinit var state: SheetState
        composeRule.setContent {
            ZibeTheme {
                state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ZibeBottomSheet(
                    isOpen = true,
                    onCancel = {},
                    openFullyExpanded = true,
                    sheetState = state,
                    modifier = Modifier.height(360.dp),
                    footer = {
                        SheetActions(
                            modifier = Modifier.testTag(FOOTER_TAG),
                            confirmText = "Confirmar",
                            onConfirm = {},
                            onCancel = {}
                        )
                    }
                ) {
                    SheetHeader(title = "Crear sala")
                    ZibeInputField(
                        value = "",
                        onValueChange = {},
                        label = "Nombre de la sala"
                    )
                    repeat(8) { index -> Text("Detalle $index") }
                }
            }
        }

        composeRule.onNodeWithText("Crear sala").assertIsDisplayed()
        composeRule.onNodeWithTag(FOOTER_TAG).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(SheetValue.Expanded, state.currentValue) }

        composeRule.onNode(hasSetTextAction()).performTextInput("Sala Android")
        composeRule.onNodeWithTag(FOOTER_TAG).assertIsDisplayed()
    }

    private companion object {
        const val CONTENT_TAG = "compact_sheet_scroll_content"
        const val FOOTER_TAG = "expanded_sheet_footer"
    }
}
