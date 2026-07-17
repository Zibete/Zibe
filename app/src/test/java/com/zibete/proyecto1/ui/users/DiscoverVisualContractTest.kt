package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.core.designsystem.R as DsR
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverVisualContractTest {
    @Test
    fun `person card uses shared glass background token`() {
        assertEquals(DsR.color.glass_bg_light, discoverPersonCardColorRes)
    }

    @Test
    fun `person card restores legacy title and description typography tokens`() {
        assertEquals(DsR.dimen.text_size_row_title, discoverPersonTitleTextSizeRes)
        assertEquals(DsR.dimen.text_size_row_description, discoverPersonDescriptionTextSizeRes)
    }
}
