package com.zibete.proyecto1.ui.users

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_EMPTY
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_FILTER_SHEET
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_LIST
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SCROLL_TOP
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.components.ProfileCard
import com.zibete.proyecto1.ui.profile.ProfileUiState
import com.zibete.proyecto1.ui.theme.ZibeTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoverScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsProgressCopyWithoutEmptyOrList() {
        setDiscoverContent(state = UsersUiState(isLoading = true))

        composeRule.onNodeWithText("Cargando personas").assertIsDisplayed()
        composeRule.onNodeWithTag(DISCOVER_EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(DISCOVER_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyStateKeepsForYouWithoutInlineSearchOrFilters() {
        setDiscoverContent(state = UsersUiState())

        composeRule.onNodeWithText("Para vos").assertIsDisplayed()
        composeRule.onNodeWithTag(DISCOVER_EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("Buscar personas").assertDoesNotExist()
        composeRule.onNodeWithText("Filtros").assertDoesNotExist()
        composeRule.onNodeWithText("Solo en línea").assertDoesNotExist()
    }

    @Test
    fun errorStateShowsMessageAndRetries() {
        val retried = AtomicBoolean(false)
        setDiscoverContent(
            state = UsersUiState(),
            errorMessage = "No se pudo cargar",
            onRetry = { retried.set(true) }
        )

        composeRule.onNodeWithText("No se pudo cargar").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        composeRule.runOnIdle { assertTrue(retried.get()) }
    }

    @Test
    fun cardsShowLegacyVisualContractPresenceTagsAndActions() {
        val profileUserId = AtomicReference<String>()
        val chatUserId = AtomicReference<String>()
        val online = testUser(
            id = "online",
            name = "Ada",
            isOnline = true,
            isFavorite = true,
            isBlockedByMe = true,
            isNotificationsSilenced = true
        )
        val offline = testUser(
            id = "offline",
            name = "",
            isOnline = false,
            hasBlockedMe = true
        )
        setDiscoverContent(
            state = UsersUiState(users = listOf(online, offline)),
            formatDistance = { "1,5 km" },
            onProfileClick = profileUserId::set,
            onChatClick = chatUserId::set
        )

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Perfil eliminado").assertIsDisplayed()
        composeRule.onAllNodesWithText("29 años").assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Foto de perfil de Ada").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_presence_online_online", useUnmergedTree = true).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "En línea")
        )
        composeRule.onNodeWithTag("discover_presence_offline_offline", useUnmergedTree = true).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desconectado")
        )
        composeRule.onNodeWithText("En línea").assertDoesNotExist()
        composeRule.onNodeWithText("Desconectado").assertDoesNotExist()
        composeRule.onAllNodesWithTag("user_status_tag_distance", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onNodeWithTag("user_status_tag_favorite", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_blocked_by_me", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_has_blocked_me", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_silenced", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("discover_description_online", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Agregar a Ada a Favoritos").assertDoesNotExist()
        composeRule.onNodeWithTag("discover_chat_online", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("discover_person_online").performClick()
        composeRule.onNodeWithContentDescription("Iniciar chat con Ada", useUnmergedTree = true)
            .performClick()

        composeRule.runOnIdle {
            assertEquals("online", profileUserId.get())
            assertEquals("online", chatUserId.get())
        }
    }

    @Test
    fun chatActionShowsLoadingWithoutFavoriteAction() {
        setDiscoverContent(
            state = UsersUiState(
                users = listOf(testUser()),
                chatCheckUserId = "user-1"
            )
        )

        composeRule.onNodeWithTag("discover_chat_loading_user-1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Agregar a Ada a Favoritos").assertDoesNotExist()
    }

    @Test
    fun filterSheetOwnsOnlineAndAgeControls() {
        setDiscoverContent(state = UsersUiState(isFilterSheetOpen = true))

        composeRule.onNodeWithTag(DISCOVER_FILTER_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Solo en línea").assertIsDisplayed()
        composeRule.onNodeWithText("Filtrar por edad").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edad mínima").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edad máxima").assertIsDisplayed()
    }

    @Test
    fun scrollToTopStartsHiddenAppearsAfterScrollAndReturnsToFirstItem() {
        val users = List(16) { index -> testUser(id = "user-$index", name = "Persona $index") }
        setDiscoverContent(state = UsersUiState(users = users))

        composeRule.onNodeWithTag(DISCOVER_SCROLL_TOP).assertDoesNotExist()
        composeRule.onNodeWithTag(DISCOVER_LIST).performScrollToIndex(10)
        composeRule.waitUntil {
            composeRule.onNodeWithTag(DISCOVER_SCROLL_TOP).isDisplayed()
        }

        composeRule.onNodeWithTag(DISCOVER_SCROLL_TOP).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithTag("discover_person_user-0").isDisplayed()
        }
    }

    @Test
    fun profileAndDiscoverExposeTheSameSharedTagContract() {
        composeRule.setContent {
            ZibeTheme {
                ProfileCard(
                    profile = Users(name = "Ada", birthDate = "1997-01-01"),
                    state = ProfileUiState(
                        isFavorite = true,
                        isBlockedByMe = true,
                        isNotificationsSilenced = true,
                        isGroupMatch = true
                    ),
                    userStatus = UserStatus.Online,
                    distanceLabel = "1,5 km",
                    photoList = emptyList(),
                    onToggleFavorite = {},
                    onOpenPhoto = {}
                )
            }
        }

        composeRule.onNodeWithTag("user_status_tag_distance", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_favorite", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_blocked_by_me", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_silenced", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("user_status_tag_group_match", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun firstConversationSheetConfirmsStart() {
        val confirmed = AtomicBoolean(false)
        setDiscoverContent(
            state = UsersUiState(pendingFirstContact = testUser()),
            onConfirmFirstContact = { confirmed.set(true) }
        )

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar una nueva conversación").assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar chat").performClick()

        composeRule.runOnIdle { assertTrue(confirmed.get()) }
    }

    @Test
    fun firstConversationSheetCanBeCancelled() {
        val cancelled = AtomicBoolean(false)
        setDiscoverContent(
            state = UsersUiState(pendingFirstContact = testUser()),
            onCancelFirstContact = { cancelled.set(true) }
        )

        composeRule.onNodeWithTag(FIRST_DM_SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.runOnIdle { assertTrue(cancelled.get()) }
    }

    private fun setDiscoverContent(
        state: UsersUiState,
        errorMessage: String? = null,
        formatDistance: (Double) -> String = { "" },
        onApplyFilters: (Boolean, Boolean, Int, Int) -> Unit = { _, _, _, _ -> },
        onClearFilters: () -> Unit = {},
        onClearAllCriteria: () -> Unit = {},
        onDismissFilters: () -> Unit = {},
        onRetry: () -> Unit = {},
        onProfileClick: (String) -> Unit = {},
        onChatClick: (String) -> Unit = {},
        onConfirmFirstContact: () -> Unit = {},
        onCancelFirstContact: () -> Unit = {}
    ) {
        composeRule.setContent {
            ZibeTheme {
                DiscoverScreen(
                    state = state,
                    errorMessage = errorMessage,
                    formatDistance = formatDistance,
                    onApplyFilters = onApplyFilters,
                    onClearFilters = onClearFilters,
                    onClearAllCriteria = onClearAllCriteria,
                    onDismissFilters = onDismissFilters,
                    onRefresh = {},
                    onRetry = onRetry,
                    onProfileClick = onProfileClick,
                    onChatClick = onChatClick,
                    onConfirmFirstContact = onConfirmFirstContact,
                    onCancelFirstContact = onCancelFirstContact
                )
            }
        }
    }

    private fun testUser(
        id: String = "user-1",
        name: String = "Ada",
        isOnline: Boolean = true,
        isFavorite: Boolean = false,
        isBlockedByMe: Boolean = false,
        hasBlockedMe: Boolean = false,
        isNotificationsSilenced: Boolean = false
    ) = UsersRowUiModel(
        id = id,
        name = name,
        age = 29,
        isOnline = isOnline,
        distanceMeters = 1_500.0,
        photoUrl = "",
        description = "Descripción extensa que debe mostrarse en una sola línea con ellipsis",
        isFavorite = isFavorite,
        isBlockedByMe = isBlockedByMe,
        hasBlockedMe = hasBlockedMe,
        isNotificationsSilenced = isNotificationsSilenced
    )
}
