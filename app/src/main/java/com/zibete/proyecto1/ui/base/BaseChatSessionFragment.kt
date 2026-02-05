package com.zibete.proyecto1.ui.base

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseChatSessionFragment : Fragment() {
    @Inject
    lateinit var snackBarManager: SnackBarManager
    protected fun observeChatSessionEvents(events: Flow<ChatSessionUiEvent>) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                events.collect { event ->
                    ChatSessionUiHandler.handle(
                        requireContext(),
                        event,
                        this,
                        snackBarManager = snackBarManager
                    )
                }
            }
        }
    }

    // Profile Toolbar setup
    protected fun setupToolbar(toolbar: MaterialToolbar) {
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = ""
        }
    }
}
