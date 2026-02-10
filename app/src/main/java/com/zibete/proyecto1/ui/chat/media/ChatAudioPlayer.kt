package com.zibete.proyecto1.ui.chat.media

import android.media.MediaPlayer
import android.os.Handler

enum class MediaState { NOT_STARTED, PLAY, PAUSE }

object ChatAudioPlayer {
    var mediaPlayer: MediaPlayer? = null
    var handler: Handler? = null
    var moveSeekBarThread: Runnable? = null
    var mediaSelectedMs: Long = 0
    var chronStateSave: Long = 0

    fun release() {
        handler?.removeCallbacks(moveSeekBarThread ?: return)
        moveSeekBarThread = null

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
            mediaPlayer?.reset()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSelectedMs = 0
        chronStateSave = 0
        handler = null
    }
}
