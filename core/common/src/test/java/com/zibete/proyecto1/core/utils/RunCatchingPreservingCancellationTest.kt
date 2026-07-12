package com.zibete.proyecto1.core.utils

import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCatchingPreservingCancellationTest {

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown`() {
        runCatchingPreservingCancellation<Unit> { throw CancellationException("cancel") }
    }

    @Test
    fun `ordinary failure remains inspectable`() {
        val result = runCatchingPreservingCancellation<Unit> { error("failure") }

        assertTrue(result.isFailure)
        assertEquals("failure", result.exceptionOrNull()?.message)
    }
}
