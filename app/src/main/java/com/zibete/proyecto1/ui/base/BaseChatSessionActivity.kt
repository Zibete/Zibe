package com.zibete.proyecto1.ui.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

open class BaseChatSessionActivity : BaseToolbarActivity() {

    protected fun observeChatSessionEvents(
        events: Flow<ChatSessionUiEvent>
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                events.collect { event ->

                    when (event) {
                        is ChatSessionUiEvent.CloseChat -> {
                            finish()
                        }

                        else -> {
                            ChatSessionUiHandler.handle(
                                context = this@BaseChatSessionActivity,
                                root = findViewById(android.R.id.content),
                                event = event,
                                scope = this
                            )
                        }
                    }
                }
            }
        }
    }
}


