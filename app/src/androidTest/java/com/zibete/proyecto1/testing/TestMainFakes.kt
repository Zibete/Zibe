package com.zibete.proyecto1.testing

import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.PresenceRepository
import com.zibete.proyecto1.data.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

fun fakeUserRepository(): UserRepository =
    mockk<UserRepository>(relaxed = true).apply {
        every { myUserName } returns "Test User"
        every { myEmail } returns "test@example.com"
        every { myProfilePhotoUrl } returns ""
        every { observeUnreadChatList(any()) } returns flowOf(0)
    }

fun fakeGroupRepository(): GroupRepository =
    mockk<GroupRepository>(relaxed = true).apply {
        every { unreadGroupBadgeCount(any()) } returns flowOf(0)
        every { observeUnreadGroupChat(any()) } returns flowOf(0)
        every { observeUnreadPrivateMessages() } returns flowOf(0)
    }

fun fakePresenceRepository(): PresenceRepository =
    mockk<PresenceRepository>(relaxed = true).apply {
        coEvery { startPresence() } returns Unit
    }

fun fakeLocationRepository(): LocationRepository =
    mockk<LocationRepository>(relaxed = true)
