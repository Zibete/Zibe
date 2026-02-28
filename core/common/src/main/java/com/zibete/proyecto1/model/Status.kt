package com.zibete.proyecto1.model

data class Status(
    val status: String = "",      // "online", "ultVez", "offline", "escribiendo..."
    val lastSeenMs: Long = 0L      // ServerValue.TIMESTAMP cuando se desconecta
)
