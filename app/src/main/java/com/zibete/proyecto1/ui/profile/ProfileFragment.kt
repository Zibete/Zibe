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
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NAME
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.utils.Utils
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

    // Fotos (perfil + chat)
    private val receivedPhotos = arrayListOf<String>()
    private lateinit var adapterPhotoReceived: AdapterPhotoReceived

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Swipe refresh ---
        binding.swipeRefresh.setOnRefreshListener {
            profileViewModel.loadProfile(force = true)
        }

        // --- Cargar por primera vez ---
        profileViewModel.loadProfile()

        // Collects
        collectUiState()
        collectUserStatus()
        collectChatPhotos()

        observeChatSessionEvents(profileViewModel.events)

        setupToolbar()
        setupRecycler()
        setupFabMenu()
        setupImageLayout()
        bindClicks()

        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // menu_main ya está inflado por BaseToolbarActivity
                }

                override fun onPrepareMenu(menu: Menu) {
                    val state = profileViewModel.uiState.value

                    val isSilent = state.chatState == CHAT_STATE_SILENT

                    menu.findItem(R.id.action_block)?.isVisible = !state.iBlockedUser
                    menu.findItem(R.id.action_unblock)?.isVisible = state.iBlockedUser
                    menu.findItem(R.id.action_delete_chat)?.isVisible = true
                    menu.findItem(R.id.action_notifications_on)?.isVisible = isSilent
                    menu.findItem(R.id.action_notifications_off)?.isVisible = !isSilent
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {

                        R.id.action_notifications_off, R.id.action_notifications_on -> {
                            profileViewModel.onToggleNotificationsClicked(NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_block -> {
                            profileViewModel.onBlockClicked(NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_unblock -> {
                            profileViewModel.onUnblockClicked(NODE_CURRENT_CHAT)
                            true
                        }

                        R.id.action_delete_chat -> {
                            profileViewModel.onDeleteClicked(NODE_CURRENT_CHAT)
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun bindClicks() {
        binding.profileFavoriteOff.setOnClickListener {
            profileViewModel.onToggleFavorite()
        }
        binding.profileFavoriteOn.setOnClickListener {
            profileViewModel.onToggleFavorite()
        }

        binding.linearImageActivity.setOnClickListener {
            val url = profileViewModel.uiState.value.profile?.profilePhoto
                ?.takeIf { it.isNotBlank() }
                ?: return@setOnClickListener

            val intent = Intent(requireContext(), SlidePhotoActivity::class.java).apply {
                putStringArrayListExtra("photoList", arrayListOf(url))
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

                    val profile = state.profile ?: return@collect

                    binding.swipeRefresh.isRefreshing = state.isLoading

                    val age = Utils.calcAge(profile.birthDay).toString()
                    val name = profile.name
                    val distance = profileViewModel.getDistanceToUser(profile)
                    val description = profile.description
                    val photoUrl = profile.profilePhoto

                    binding.ageView.text = age
                    binding.nameUser.text = name
                    binding.distanceUser.text = distance

                    binding.linearDesc.isVisible = description.isNotEmpty()
                    binding.desc.text = description

                    binding.loadingPhoto.isVisible = false

                    // Foto perfil
                    Glide.with(requireContext())
                        .load(photoUrl)
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

        val profile = profileViewModel.uiState.value.profile ?: return

        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, profile.id)
            putExtra(EXTRA_CHAT_NODE, NODE_CURRENT_CHAT)
            putExtra(EXTRA_CHAT_NAME, profile.name)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onStart()
        profileViewModel.setUserOnline()
    }

    override fun onPause() {
        super.onStop()
        profileViewModel.setUserLastSeen()
    }

}