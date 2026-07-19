package com.zibete.proyecto1.ui.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.ui.theme.ZibeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomsScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun realRoomListRendersWithoutComingSoonPlaceholder() {
        setRoomsContent()

        composeRule.onNodeWithTag(RoomsTestTags.LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Sala Android").assertIsDisplayed()
        composeRule.onNodeWithText("Arquitectura y producto móvil").assertIsDisplayed()
        composeRule.onNodeWithText("Salas próximamente").assertDoesNotExist()
    }

    @Test
    fun createActionOpensCreateSheet() {
        setRoomsContent()

        composeRule.onNodeWithTag(RoomsTestTags.CREATE_ACTION).performClick()

        composeRule.onNodeWithTag(RoomsTestTags.CREATE_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Nombre de la sala").assertIsDisplayed()
        composeRule.onNodeWithText("Vas a crearla como").assertIsDisplayed()
        composeRule.onNodeWithText("Public User").assertIsDisplayed()
        composeRule.onNodeWithText("Usar un alias").assertDoesNotExist()
        composeRule.onNodeWithTag(RoomsTestTags.CREATE_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun roomCardOpensJoinSheetWithIdentityChoice() {
        setRoomsContent()

        composeRule.onNodeWithTag(RoomsTestTags.room(ROOM_KEY)).performClick()

        composeRule.onNodeWithTag(RoomsTestTags.JOIN_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Usar mi perfil").assertIsDisplayed()
        composeRule.onNodeWithText("Usar un alias").assertIsDisplayed()
        composeRule.onNodeWithTag(RoomsTestTags.JOIN_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun loadingEmptyAndErrorStatesHaveStableActions() {
        composeRule.setContent {
            ZibeTheme {
                RoomsScreen(
                    state = GroupsUiState(isLoading = true),
                    onRefresh = {},
                    onRetry = {},
                    onCreateRoom = {},
                    onRoomSelected = {},
                    onDismissSheet = {},
                    onIdentitySelected = {},
                    onAliasChanged = {},
                    onRoomNameChanged = {},
                    onRoomDescriptionChanged = {},
                    onSubmitSheet = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {}
                )
            }
        }
        composeRule.onNodeWithTag(RoomsTestTags.LOADING).assertIsDisplayed()

        composeRule.setContent {
            ZibeTheme {
                RoomsScreen(
                    state = GroupsUiState(),
                    onRefresh = {},
                    onRetry = {},
                    onCreateRoom = {},
                    onRoomSelected = {},
                    onDismissSheet = {},
                    onIdentitySelected = {},
                    onAliasChanged = {},
                    onRoomNameChanged = {},
                    onRoomDescriptionChanged = {},
                    onSubmitSheet = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {}
                )
            }
        }
        composeRule.onNodeWithTag(RoomsTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Crear sala").assertIsDisplayed()

        composeRule.setContent {
            ZibeTheme {
                RoomsScreen(
                    state = GroupsUiState(error = UiText.Dynamic("Sin conexión")),
                    onRefresh = {},
                    onRetry = {},
                    onCreateRoom = {},
                    onRoomSelected = {},
                    onDismissSheet = {},
                    onIdentitySelected = {},
                    onAliasChanged = {},
                    onRoomNameChanged = {},
                    onRoomDescriptionChanged = {},
                    onSubmitSheet = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {}
                )
            }
        }
        composeRule.onNodeWithTag(RoomsTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    private fun setRoomsContent() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    GroupsUiState(
                        rooms = listOf(room()),
                        visibleRooms = listOf(room()),
                        publicIdentityName = "Public User"
                    )
                )
            }
            ZibeTheme {
                RoomsScreen(
                    state = state,
                    onRefresh = {},
                    onRetry = {},
                    onCreateRoom = { state = state.copy(sheet = RoomsSheet.Create) },
                    onRoomSelected = { state = state.copy(sheet = RoomsSheet.Join(it)) },
                    onDismissSheet = { state = state.copy(sheet = null) },
                    onIdentitySelected = { state = state.copy(identityType = it) },
                    onAliasChanged = { state = state.copy(alias = it) },
                    onRoomNameChanged = { state = state.copy(roomName = it) },
                    onRoomDescriptionChanged = { state = state.copy(roomDescription = it) },
                    onSubmitSheet = {},
                    onConfirmSwitch = {},
                    onDismissSwitch = {}
                )
            }
        }
    }

    private fun room() = Groups(
        name = "Sala Android",
        description = "Arquitectura y producto móvil",
        users = 4,
        roomId = ROOM_KEY,
        roomKey = ROOM_KEY,
        displayName = "Sala Android",
        lastActivityAt = 1L
    )

    private companion object {
        const val ROOM_KEY = "room-android"
    }
}
