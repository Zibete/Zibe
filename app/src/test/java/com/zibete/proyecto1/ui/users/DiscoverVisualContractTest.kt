package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.core.designsystem.R as DsR
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverVisualContractTest {
    @Test
    fun `person card uses shared glass background token`() {
        assertEquals(DsR.color.glass_bg_light, discoverPersonCardColorRes)
    }
}
