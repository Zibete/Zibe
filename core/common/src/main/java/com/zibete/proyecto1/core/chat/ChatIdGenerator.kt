package com.zibete.proyecto1.core.chat

object ChatIdGenerator {
    fun getChatId(uidA: String, uidB: String): String {
        val (first, second) = listOf(uidA, uidB).sorted()
        return "${first}_${second}"
    }
}
