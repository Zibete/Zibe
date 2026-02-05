package com.zibete.proyecto1.ui.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

open class BaseChatSessionActivity : BaseEdgeToEdgeActivity() {
    @Inject
    lateinit var snackBarManager: SnackBarManager
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
                                event = event,
                                scope = this,
                                snackBarManager = snackBarManager
                            )
                        }
                    }
                }
            }
        }
    }
}


