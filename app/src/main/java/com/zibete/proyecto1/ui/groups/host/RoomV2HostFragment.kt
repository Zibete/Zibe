package com.zibete.proyecto1.ui.groups.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.zibete.proyecto1.ui.main.MainUiEvent
import com.zibete.proyecto1.ui.main.MainViewModel
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RoomV2HostFragment : androidx.fragment.app.Fragment() {

    private val viewModel: RoomV2HostViewModel by viewModels()
    private val profileViewModel: RoomV2ContextProfileViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.tryHandleBack()) return
                    findNavController().popBackStack()
                }
            },
        )

        val restoredConversationId = if (
            savedInstanceState?.containsKey(STATE_SELECTED_CONVERSATION_ID) == true
        ) {
            savedInstanceState.getString(STATE_SELECTED_CONVERSATION_ID)
        } else {
            arguments?.getString(ROOM_V2_CONVERSATION_ARG)
        }
        restoredConversationId
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let(viewModel::selectConversation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ZibeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    RoomV2HostWithProfileRoute(
                        viewModel = viewModel,
                        profileViewModel = profileViewModel,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RoomV2HostEvent.ShowSnack -> mainViewModel.emit(
                            MainUiEvent.ShowSnack(event.uiText, event.snackType)
                        )

                        RoomV2HostEvent.NavigateBack -> findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenVisible(true)
    }

    override fun onStop() {
        viewModel.onScreenVisible(false)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            STATE_SELECTED_CONVERSATION_ID,
            viewModel.uiState.value.selectedConversationId,
        )
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_SELECTED_CONVERSATION_ID = "room_v2_selected_conversation_id"
    }
}
