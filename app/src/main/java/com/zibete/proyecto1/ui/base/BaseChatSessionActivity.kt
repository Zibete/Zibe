package com.zibete.proyecto1.ui.base

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseChatSessionActivity : AppCompatActivity() {

    protected fun observeChatSessionEvents(events: Flow<ChatSessionUiEvent>) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                events.collect { event ->
                    ChatSessionUiHandler.handle(
                        this@BaseChatSessionActivity,
                        root = findViewById(android.R.id.content),
                        event,
                        this
                    )
                }
            }
        }
    }
}
