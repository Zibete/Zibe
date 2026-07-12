package com.zibete.proyecto1.model

import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_SENDER_DLT
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageVisibilityTest {

    @Test
    fun senderDeleteTypes_onlyHideMessageFromSender() {
        senderDeleteTypes.forEach { type ->
            val message = ChatMessage(senderUid = SENDER_UID, type = type)

            assertTrue(message.isDeletedFor(SENDER_UID))
            assertFalse(message.isDeletedFor(RECEIVER_UID))
        }
    }

    @Test
    fun receiverDeleteTypes_onlyHideMessageFromReceiver() {
        receiverDeleteTypes.forEach { type ->
            val message = ChatMessage(senderUid = SENDER_UID, type = type)

            assertFalse(message.isDeletedFor(SENDER_UID))
            assertTrue(message.isDeletedFor(RECEIVER_UID))
        }
    }

    @Test
    fun regularMessage_remainsVisibleForBothParticipants() {
        val message = ChatMessage(senderUid = SENDER_UID, type = MSG_AUDIO)

        assertTrue(message.isVisibleFor(SENDER_UID))
        assertTrue(message.isVisibleFor(RECEIVER_UID))
    }

    private companion object {
        const val SENDER_UID = "sender"
        const val RECEIVER_UID = "receiver"

        val senderDeleteTypes = listOf(
            MSG_TEXT_SENDER_DLT,
            MSG_PHOTO_SENDER_DLT,
            MSG_AUDIO_SENDER_DLT
        )
        val receiverDeleteTypes = listOf(
            MSG_TEXT_RECEIVER_DLT,
            MSG_PHOTO_RECEIVER_DLT,
            MSG_AUDIO_RECEIVER_DLT
        )
    }
}
