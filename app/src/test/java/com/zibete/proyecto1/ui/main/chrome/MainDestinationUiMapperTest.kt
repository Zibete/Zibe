package com.zibete.proyecto1.ui.main.chrome

import com.zibete.proyecto1.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainDestinationUiMapperTest {
    private val mapper = MainDestinationUiMapper()

    @Test
    fun `four root destinations show account avatar and bottom navigation`() {
        val destinations = listOf(
            R.id.nav_users to R.id.navBottomUsers,
            R.id.nav_chat_list to R.id.navBottomChat,
            R.id.nav_group_select to R.id.navBottomGroups,
            R.id.nav_favorites to R.id.navBottomFavorites
        )

        destinations.forEach { (destinationId, bottomItemId) ->
            val state = mapper.map(destinationId)
            assertTrue(state.showAccountAvatar)
            assertTrue(state.showBottomNav)
            assertFalse(state.showBack)
            assertEquals(bottomItemId, state.selectedBottomNavItemId)
        }
    }

    @Test
    fun `nested account destinations show back without account avatar`() {
        listOf(R.id.editProfileFragment, R.id.settingsFragment).forEach { destinationId ->
            val state = mapper.map(destinationId)
            assertFalse(state.showAccountAvatar)
            assertFalse(state.showBottomNav)
            assertTrue(state.showBack)
        }
    }

    @Test
    fun `rooms v2 host owns its chat chrome`() {
        val state = mapper.map(R.id.nav_room_v2_host)

        assertFalse(state.showToolbar)
        assertFalse(state.showBottomNav)
        assertFalse(state.showAccountAvatar)
        assertFalse(state.showBack)
        assertEquals(R.id.navBottomGroups, state.selectedBottomNavItemId)
    }

    @Test
    fun `discover enables toolbar search and filter only for its destination`() {
        val discover = mapper.map(R.id.nav_users).menuConfig

        assertTrue(discover.showSearch)
        assertTrue(discover.showDiscoverFilter)

        listOf(R.id.nav_chat_list, R.id.nav_group_select, R.id.nav_favorites).forEach { id ->
            assertFalse(mapper.map(id).menuConfig.showDiscoverFilter)
        }
    }
}
