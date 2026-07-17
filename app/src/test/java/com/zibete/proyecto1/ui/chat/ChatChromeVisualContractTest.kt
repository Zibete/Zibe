package com.zibete.proyecto1.ui.chat

import androidx.compose.ui.graphics.Color
import com.zibete.proyecto1.ui.chat.components.chatTopBarContainerColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatChromeVisualContractTest {
    @Test
    fun `chat top bar container is transparent in every mode`() {
        assertEquals(Color.Transparent, chatTopBarContainerColor)
    }

    @Test
    fun `chat status bar is transparent`() {
        assertEquals(android.graphics.Color.TRANSPARENT, CHAT_STATUS_BAR_COLOR)
    }
}
