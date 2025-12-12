package com.zibete.proyecto1.ui.profile

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.databinding.FragmentProfileBinding
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.base.BaseChatSessionFragment
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NAME
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseChatSessionFragment() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()

    // Args
    private val userId: String by lazy {
        requireArguments().getString(ARG_USER_ID).orEmpty()
    }

    // Fotos (perfil + chat)
    private val photoList = arrayListOf<String>()
    private val receivedPhotos = arrayListOf<String>()
    private lateinit var adapterPhotoReceived: AdapterPhotoReceived

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // menu_main ya está inflado por BaseToolbarActivity
                }

                override fun onPrepareMenu(menu: Menu) {
                    val state = profileViewModel.uiState.value

                    menu.findItem(R.id.action_block)?.isVisible = !state.iBlockedUser
                    menu.findItem(R.id.action_unblock)?.isVisible = state.iBlockedUser
                    menu.findItem(R.id.action_delete_chat)?.isVisible = false
                    menu.findItem(R.id.action_notifications_on)?.isVisible = false
                    menu.findItem(R.id.action_notifications_off)?.isVisible = false
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {

                        R.id.action_notifications_off, R.id.action_notifications_on -> {
                            profileViewModel.onToggleNotificationsClicked(userId, profileViewModel.uiState.value.name, NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_block -> {
                            profileViewModel.onBlockClicked(userId, profileViewModel.uiState.value.name, NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_unblock -> {
                            profileViewModel.onUnblockClicked(userId, profileViewModel.uiState.value.name, NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_delete_chat -> {
                            profileViewModel.onDeleteClicked(userId, profileViewModel.uiState.value.name, NODE_CURRENT_CHAT)
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        observeChatSessionEvents(profileViewModel.events)

        setupToolbar()
        setupRecycler()
        setupFabMenu()
        setupImageLayout()
        bindClicks()

        // Cargar
        profileViewModel.loadProfile(userId)

        // Collects
        collectUiState()
        collectUserStatus()
        collectChatPhotos()

    }

    private fun bindClicks() {
        binding.profileFavoriteOff.setOnClickListener {
            profileViewModel.onToggleFavorite(userId)
        }
        binding.profileFavoriteOn.setOnClickListener {
            profileViewModel.onToggleFavorite(userId)
        }

        binding.linearImageActivity.setOnClickListener {
            if (photoList.isEmpty()) return@setOnClickListener
            val intent = Intent(requireContext(), SlidePhotoActivity::class.java).apply {
                putStringArrayListExtra("photoList", ArrayList(photoList))
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            startActivity(intent)
        }

        binding.menuGoChat.setOnClickListener { goChat() }
        binding.menuGoChatGroup.setOnClickListener { goChat() }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.uiState.collect { state ->

                    binding.loadingPhoto.isVisible = state.isLoading

                    state.age?.let { binding.ageView.text = it.toString() }
                    binding.nameUser.text = state.name
                    binding.distanceUser.text = state.distance

                    binding.linearDesc.isVisible = !state.description.isNullOrEmpty()
                    binding.desc.text = state.description.orEmpty()

                    // Foto perfil
                    val url = state.photoUrl
                    if (!url.isNullOrBlank()) {
                        if (!photoList.contains(url)) photoList.add(url)

                        Glide.with(requireContext())
                            .load(url)
                            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.loadingPhoto.isVisible = false
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.loadingPhoto.isVisible = false
                                    return false
                                }
                            })
                            .into(binding.profilePhoto)
                    }

                    // Mostrar botón "chat privado en grupo"
                    binding.menuGoChatGroup.isVisible = state.isGroupMatch
                    if (state.isGroupMatch) {
                        binding.menuGoChatGroup.labelText =
                            getString(R.string.chat_private_in_group, userPreferencesRepository.groupName)
                    }

                    // Favorito
                    binding.profileFavoriteOn.isVisible = state.isFavorite
                    binding.profileFavoriteOff.isVisible = !state.isFavorite

                    // Bloqueos
                    binding.profileBlock.isVisible = state.iBlockedUser
                    binding.profileBlockMe.isVisible = state.userBlockedMe
                }
            }
        }
    }

    private fun collectUserStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // ✅ Si en tu VM el flow se llama distinto, cambiá ESTA línea:
                profileViewModel.userStatus.collect { status ->
                    when (status) {
                        is UserStatus.Online -> {
                            binding.iconConnected.isVisible = true
                            binding.iconDisconnected.isVisible = false
                            binding.tvStatus.text = getString(R.string.online)
                            binding.tvStatus.setTypeface(null, Typeface.NORMAL)
                        }
                        is UserStatus.TypingOrRecording -> {
                            binding.iconConnected.isVisible = true
                            binding.iconDisconnected.isVisible = false
                            binding.tvStatus.text = status.text
                            binding.tvStatus.setTypeface(null, Typeface.ITALIC)
                        }
                        is UserStatus.LastSeen -> {
                            binding.iconConnected.isVisible = false
                            binding.iconDisconnected.isVisible = true
                            binding.tvStatus.text = status.text
                        }
                        is UserStatus.Offline -> {
                            binding.iconDisconnected.isVisible = true
                            binding.iconConnected.isVisible = false
                            binding.tvStatus.text = getString(R.string.offline)
                        }
                    }
                }
            }
        }
    }

    private fun collectChatPhotos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.photosFromChat.collect { photos ->
                    binding.linearPhotos.isVisible = photos.isNotEmpty()
                    if (photos.isNotEmpty()) {
                        receivedPhotos.clear()
                        receivedPhotos.addAll(photos)
                        adapterPhotoReceived.updateData(receivedPhotos)
                    }
                }
            }
        }
    }

    // region Setup
    private fun setupToolbar() {
        setupToolbar(binding.toolbarProfile)
    }

    private fun setupRecycler() {
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, true).apply {
                stackFromEnd = true
            }

        binding.recyclerPhotos.layoutManager = layoutManager

        adapterPhotoReceived = AdapterPhotoReceived(
            receivedPhotos,
            Constants.MAXCHATSIZE,
            requireContext()
        )

        binding.recyclerPhotos.adapter = adapterPhotoReceived
    }

    private fun setupFabMenu() {
        binding.floatingActionMenu.setClosedOnTouchOutside(true)
    }

    private fun setupImageLayout() {
        val height = ScreenUtils.heightPx
        binding.linearImageActivity.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            height - (height / 4)
        )
    }

    // endregion

    private fun goChat() {
        val name = profileViewModel.uiState.value.name
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, userId)
            putExtra(EXTRA_CHAT_NODE, NODE_CURRENT_CHAT)
            putExtra(EXTRA_CHAT_NAME, name)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(
            userId: String,
        ) = ProfileFragment().apply {
            arguments = bundleOf(
                ARG_USER_ID to userId
            )
        }
    }

    override fun onStart() {
        super.onStart()
        profileViewModel.setUserOnline()
    }

    override fun onStop() {
        super.onStop()
        profileViewModel.setUserLastSeen()
    }

}
