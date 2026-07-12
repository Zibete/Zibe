package com.zibete.proyecto1.core.chat

object ChatIdGenerator {
    fun getChatId(uidA: String, uidB: String): String {
        val (first, second) = listOf(uidA, uidB).sorted()
        return "${first}_${second}"
    }

    fun getOtherUid(chatId: String, myUid: String): String? {
        if (chatId.isBlank() || myUid.isBlank()) return null
        val prefix = "${myUid}_"
        val suffix = "_${myUid}"
        return when {
            chatId.startsWith(prefix) -> chatId.removePrefix(prefix).takeIf(String::isNotBlank)
            chatId.endsWith(suffix) -> chatId.removeSuffix(suffix).takeIf(String::isNotBlank)
            else -> null
        }
    }
}
