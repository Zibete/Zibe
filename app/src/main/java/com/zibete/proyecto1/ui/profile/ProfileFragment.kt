package com.zibete.proyecto1.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : BaseChatSessionFragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZibeTheme {
                    ProfileRoute(
                        profileViewModel = profileViewModel,
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onOpenChat = { userId ->
                            startActivity(
                                Intent(requireContext(), ChatActivity::class.java).apply {
                                    putExtra(EXTRA_CHAT_ID, userId)
                                    putExtra(EXTRA_CHAT_NODE, NODE_DM)
                                }
                            )
                        },
                        onOpenPhoto = { url ->
                            PhotoViewerActivity.startSingle(requireContext(), url)
                        }
                    )
                }
            }
        }
    }
}
