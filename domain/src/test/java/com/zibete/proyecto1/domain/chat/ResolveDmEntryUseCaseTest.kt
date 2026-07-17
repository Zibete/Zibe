package com.zibete.proyecto1.domain.chat

import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.ChatRepositoryContract
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ResolveDmEntryUseCaseTest {
    private lateinit var repository: ChatRepositoryContract
    private lateinit var useCase: ResolveDmEntryUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = DefaultResolveDmEntryUseCase(repository)
    }

    @Test
    fun `existing conversation resolves direct entry`() = runTest {
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(true)

        val result = useCase(OTHER_UID)

        assertEquals(
            DmEntryDecision.OpenExisting,
            (result as ZibeResult.Success).data
        )
        coVerify(exactly = 1) { repository.hasConversation(OTHER_UID, NODE_DM) }
    }

    @Test
    fun `missing conversation requires first contact confirmation`() = runTest {
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(false)

        val result = useCase(OTHER_UID)

        assertEquals(
            DmEntryDecision.RequireFirstContactConfirmation,
            (result as ZibeResult.Success).data
        )
    }

    @Test
    fun `repository failure is preserved`() = runTest {
        val error = IllegalStateException("lookup failed")
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Failure(error)

        val result = useCase(OTHER_UID)

        assertSame(error, (result as ZibeResult.Failure).exception)
    }

    @Test
    fun `missing repository decision becomes controlled failure`() = runTest {
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(null)

        val result = useCase(OTHER_UID)

        assertTrue(result is ZibeResult.Failure)
    }

    @Test
    fun `coroutine cancellation is rethrown`() = runTest {
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } throws
            CancellationException("cancelled")

        try {
            useCase(OTHER_UID)
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            Unit
        }
    }

    @Test
    fun `cancellation returned as repository failure is rethrown`() = runTest {
        val cancellation = CancellationException("cancelled result")
        coEvery { repository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Failure(cancellation)

        try {
            useCase(OTHER_UID)
            fail("CancellationException expected")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private companion object {
        const val OTHER_UID = "other-uid"
    }
}
