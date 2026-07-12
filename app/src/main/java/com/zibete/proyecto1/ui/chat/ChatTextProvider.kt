package com.zibete.proyecto1.ui.chat

import android.content.Context
import com.zibete.proyecto1.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface ChatTextProvider {
    val loading: String
    val online: String
    val offline: String
    val sentPhoto: String
    val receivedPhoto: String
    val sentAudio: String
    val receivedAudio: String
}

class AndroidChatTextProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ChatTextProvider {
    override val loading: String get() = context.getString(R.string.loading)
    override val online: String get() = context.getString(R.string.online)
    override val offline: String get() = context.getString(R.string.offline)
    override val sentPhoto: String get() = context.getString(R.string.photo_send)
    override val receivedPhoto: String get() = context.getString(R.string.photo_received)
    override val sentAudio: String get() = context.getString(R.string.audio_send)
    override val receivedAudio: String get() = context.getString(R.string.audio_received)
}
