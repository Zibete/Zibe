package com.zibete.proyecto1.core.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatIdGeneratorTest {

    @Test
    fun getChatId_isStableRegardlessOfParticipantOrder() {
        assertEquals("alice_bob", ChatIdGenerator.getChatId("bob", "alice"))
        assertEquals("alice_bob", ChatIdGenerator.getChatId("alice", "bob"))
    }

    @Test
    fun getOtherUid_resolvesBothParticipants() {
        val chatId = ChatIdGenerator.getChatId("alice", "bob")

        assertEquals("bob", ChatIdGenerator.getOtherUid(chatId, "alice"))
        assertEquals("alice", ChatIdGenerator.getOtherUid(chatId, "bob"))
    }

    @Test
    fun getOtherUid_rejectsMalformedAndUnrelatedChatIds() {
        assertNull(ChatIdGenerator.getOtherUid("missing-delimiter", "alice"))
        assertNull(ChatIdGenerator.getOtherUid("alice_bob", "charlie"))
    }

    @Test
    fun getOtherUid_supportsParticipantsContainingUnderscores() {
        val chatId = ChatIdGenerator.getChatId("alice_team", "bob")

        assertEquals("alice_team", ChatIdGenerator.getOtherUid(chatId, "bob"))
        assertEquals("bob", ChatIdGenerator.getOtherUid(chatId, "alice_team"))
    }
}
