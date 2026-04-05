package com.zibete.proyecto1.core.chat

object ChatIdGenerator {
    fun getChatId(uidA: String, uidB: String): String {
        val (first, second) = listOf(uidA, uidB).sorted()
        return "${first}_${second}"
    }

    fun getOtherUid(chatId: String, myUid: String): String? {
        val parts = chatId.split("_").filter { it.isNotBlank() }
        if (parts.size != 2) return null

        val (first, second) = parts
        return when (myUid) {
            first -> second
            second -> first
            else -> null
        }
    }
}
