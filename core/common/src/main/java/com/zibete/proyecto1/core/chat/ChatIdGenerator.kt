package com.zibete.proyecto1.core.chat

object ChatIdGenerator {
    fun getChatId(uidA: String, uidB: String): String {
        val (first, second) = listOf(uidA, uidB).sorted()
        require('|' !in first && '|' !in second) {
            "UIDs containing '|' are not supported"
        }
        return if ('_' in first || '_' in second) "$first|$second" else "${first}_${second}"
    }

    fun getOtherUid(chatId: String, myUid: String): String? {
        if (chatId.isBlank() || myUid.isBlank()) return null
        val delimiter = if ('|' in chatId) '|' else '_'
        val prefix = "$myUid$delimiter"
        val suffix = "$delimiter$myUid"
        return when {
            chatId.startsWith(prefix) -> chatId.removePrefix(prefix).takeIf(String::isNotBlank)
            chatId.endsWith(suffix) -> chatId.removeSuffix(suffix).takeIf(String::isNotBlank)
            else -> null
        }
    }
}
