package com.zibete.proyecto1.ui.users

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_EMPTY
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_FILTERS
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_LIST
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_ONLINE_FILTER
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SEARCH
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
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
    fun emptyStateShowsEmptyMessage() {
        setDiscoverContent(state = UsersUiState())

        composeRule.onNodeWithText("Para vos").assertIsDisplayed()
        composeRule.onNodeWithTag(DISCOVER_EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No encontramos personas con estos criterios.")
            .assertIsDisplayed()
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
    fun listStateShowsPersonDataAndRoutesProfileAndChatActions() {
        val profileUserId = AtomicReference<String>()
        val chatUserId = AtomicReference<String>()
        val user = testUser()
        setDiscoverContent(
            state = UsersUiState(users = listOf(user)),
            formatDistance = { "1,5 km" },
            onProfileClick = profileUserId::set,
            onChatClick = chatUserId::set
        )

        composeRule.onNodeWithTag(DISCOVER_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("29 años").assertIsDisplayed()
        composeRule.onAllNodesWithText("en línea").assertCountEquals(2)
        composeRule.onNodeWithText("1,5 km").assertIsDisplayed()
        composeRule.onNodeWithText("Android y café").assertIsDisplayed()

        composeRule.onNodeWithTag("discover_person_user-1").performClick()
        composeRule.onNodeWithContentDescription("Iniciar chat con Ada").performClick()

        composeRule.runOnIdle {
            assertEquals("user-1", profileUserId.get())
            assertEquals("user-1", chatUserId.get())
        }
    }

    @Test
    fun searchAndFilterControlsForwardSelectedValues() {
        val search = AtomicReference<String>()
        val online = AtomicReference<Boolean>()
        val applied = AtomicReference<AppliedFilters>()
        setDiscoverContent(
            state = UsersUiState(),
            onSearchChanged = search::set,
            onOnlineFilterChanged = online::set,
            onApplyFilters = { ageEnabled, onlineEnabled, minAge, maxAge ->
                applied.set(AppliedFilters(ageEnabled, onlineEnabled, minAge, maxAge))
            }
        )

        composeRule.onNodeWithTag(DISCOVER_SEARCH).performTextReplacement("Ada")
        composeRule.runOnIdle { assertEquals("Ada", search.get()) }
        composeRule.onNodeWithTag(DISCOVER_ONLINE_FILTER).performClick()
        composeRule.onNodeWithTag(DISCOVER_FILTERS).performClick()
        composeRule.onNodeWithText("Solo en línea").performClick()
        composeRule.onNodeWithText("Filtrar por edad").performClick()
        composeRule.onNodeWithContentDescription("Edad mínima").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edad máxima").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar filtros").performClick()

        composeRule.runOnIdle {
            assertEquals(true, online.get())
            assertEquals(AppliedFilters(true, true, 18, 99), applied.get())
        }
    }

    @Test
    fun activeFiltersCanBeClearedFromEmptyState() {
        val searchChanges = mutableListOf<String>()
        val cleared = AtomicBoolean(false)
        setDiscoverContent(
            state = UsersUiState(
                searchQuery = "sin resultados",
                applyOnlineFilter = true
            ),
            onSearchChanged = { searchChanges.add(it) },
            onClearFilters = { cleared.set(true) }
        )

        composeRule.onNodeWithText("Limpiar filtros").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(""), searchChanges)
            assertTrue(cleared.get())
        }
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
        onSearchChanged: (String) -> Unit = {},
        onOnlineFilterChanged: (Boolean) -> Unit = {},
        onApplyFilters: (Boolean, Boolean, Int, Int) -> Unit = { _, _, _, _ -> },
        onClearFilters: () -> Unit = {},
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
                    onSearchChanged = onSearchChanged,
                    onOnlineFilterChanged = onOnlineFilterChanged,
                    onApplyFilters = onApplyFilters,
                    onClearFilters = onClearFilters,
                    onRefresh = {},
                    onRetry = onRetry,
                    onProfileClick = onProfileClick,
                    onChatClick = onChatClick,
                    onFavoriteClick = {},
                    onConfirmFirstContact = onConfirmFirstContact,
                    onCancelFirstContact = onCancelFirstContact
                )
            }
        }
    }

    private fun testUser() = UsersRowUiModel(
        id = "user-1",
        name = "Ada",
        age = 29,
        isOnline = true,
        distanceMeters = 1_500.0,
        photoUrl = "",
        description = "Android y café"
    )

    private data class AppliedFilters(
        val ageEnabled: Boolean,
        val onlineEnabled: Boolean,
        val minAge: Int,
        val maxAge: Int
    )
}
