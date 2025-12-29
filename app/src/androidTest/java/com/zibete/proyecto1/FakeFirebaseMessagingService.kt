package com.zibete.proyecto1

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FakeFirebaseMessagingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
