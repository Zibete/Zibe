package com.zibete.proyecto1.ui.profile

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.MaterialToolbar
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.ActivityProfileBinding
import com.zibete.proyecto1.databinding.FragmentProfileBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.ui.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject






@AndroidEntryPoint
class ProfileActivity : BaseChatSessionActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer

    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var binding: FragmentProfileBinding
    // Estado / datos
    private val userId = intent.extras?.getString("userId")?: ""
    private val userName = intent.extras?.getString("userName")?: ""
    private val photoList = ArrayList<String>()
    private val receivedPhotos = ArrayList<String>()
    private lateinit var adapterPhotoReceived: AdapterPhotoReceived

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityProfileBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        observeChatSessionEvents(profileViewModel.events)
//
//        setupToolbar()
//
//        setupRecycler()
//
//        setupFabMenu()
//
//        binding.profileFavoriteOff.setOnClickListener {
//            profileViewModel.onToggleFavorite(userId)
//            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
//        }
//
//        binding.profileFavoriteOn.setOnClickListener {
//            profileViewModel.onToggleFavorite(userId)
//            Toast.makeText(this, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
//        }
//
//        collectChatPhotos()
//
//        collectUiState()
//        profileViewModel.loadProfile(userId)
//
//        setupImageLayout()
//
//        lifecycleScope.launch {
//            profileViewModel.userStatus.collect { status ->
//                when (status) {
//                    is UserStatus.Online -> {
//                        binding.iconConnected.isVisible = true
//                        binding.iconDisconnected.isVisible = false
//                        binding.tvStatus.text = getString(R.string.online)
//                        binding.tvStatus.setTypeface(null, Typeface.NORMAL)
//                    }
//                    is UserStatus.TypingOrRecording -> {
//                        binding.iconConnected.isVisible = true
//                        binding.iconDisconnected.isVisible = false
//                        binding.tvStatus.text = status.text
//                        binding.tvStatus.setTypeface(null, Typeface.ITALIC)
//                    }
//                    is UserStatus.LastSeen -> {
//                        binding.iconConnected.isVisible = false
//                        binding.iconDisconnected.isVisible = true
//                        binding.tvStatus.text = status.text
//                    }
//                    is UserStatus.Offline -> {
//                        binding.iconDisconnected.isVisible = true
//                        binding.iconConnected.isVisible = false
//                        binding.tvStatus.text = getString(R.string.offline)
//                    }
//                }
//            }
//        }
//    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userIds = intent.getStringArrayListExtra(EXTRA_USER_IDS)
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        val singleUserId = intent.getStringExtra(EXTRA_USER_ID)

        if (!userIds.isNullOrEmpty()) {
            // SLIDER MODE
            binding.pager.isVisible = true
            binding.singleContainer.isVisible = false

            binding.pager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount() = userIds.size
                override fun createFragment(position: Int) = ProfileFragment.newInstance(userIds[position])
            }
            binding.pager.setCurrentItem(startIndex.coerceIn(0, userIds.lastIndex), false)

        } else {
            // SINGLE MODE
            val uid = singleUserId.orEmpty()
            binding.pager.isVisible = false
            binding.singleContainer.isVisible = true

            supportFragmentManager.beginTransaction()
                .replace(R.id.singleContainer, ProfileFragment.newInstance(uid))
                .commit()
        }
    }










    private fun collectChatPhotos() {
        lifecycleScope.launchWhenStarted {
            profileViewModel.photosFromChat.collect { photos ->
                if (photos.isNotEmpty()) {
                    binding.linearPhotos.visibility = View.VISIBLE
                    adapterPhotoReceived.updateData(photos) // o items.clear+addAll
                } else {
                    binding.linearPhotos.visibility = View.GONE
                }
            }
        }
    }

    private fun collectUiState() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.uiState.collect { state ->

                    binding.loadingPhoto.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    state.age?.let { binding.ageView.text = it.toString() }
                    binding.nameUser.text = state.name
                    binding.distanceUser.text = state.distance

                    if (!state.description.isNullOrEmpty()) {
                        binding.linearDesc.visibility = View.VISIBLE
                        binding.desc.text = state.description
                    } else {
                        binding.linearDesc.visibility = View.GONE
                    }

                    state.photoUrl?.let { url ->
                        if (!photoList.contains(url)) photoList.add(url)

                        Glide.with(this@ProfileActivity)
                            .load(url)
                            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.loadingPhoto.visibility = View.GONE
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.loadingPhoto.visibility = View.GONE
                                    return false
                                }
                            })
                            .into(binding.profilePhoto)
                    }

                    if (state.isGroupMatch) {
                        binding.menuGoChatGroup.visibility = View.VISIBLE
                        binding.menuGoChatGroup.labelText =
                            "Chat privado de: ${userPreferencesRepository.groupName}"
                    } else {
                        binding.menuGoChatGroup.visibility = View.GONE
                    }

                    // Favorito
                    if (state.isFavorite) {
                        binding.profileFavoriteOn.visibility = View.VISIBLE
                        binding.profileFavoriteOff.visibility = View.GONE
                    } else {
                        binding.profileFavoriteOn.visibility = View.GONE
                        binding.profileFavoriteOff.visibility = View.VISIBLE
                    }

                    // Bloqueos
                    binding.profileBlock.visibility =
                        if (state.iBlockedUser) View.VISIBLE else View.GONE

                    binding.profileBlockMe.visibility =
                        if (state.userBlockedMe) View.VISIBLE else View.GONE
                }
            }
        }
    }

    // region Setup

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = binding.toolbarProfile
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecycler() {
        val layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true).apply {
                stackFromEnd = true
            }

        binding.recyclerPhotos.layoutManager = layoutManager
        adapterPhotoReceived =
            AdapterPhotoReceived(receivedPhotos, Constants.MAXCHATSIZE, applicationContext)
        binding.recyclerPhotos.adapter = adapterPhotoReceived
    }

    private fun setupFabMenu() {
        binding.floatingActionMenu.setClosedOnTouchOutside(true)

        binding.menuGoChatGroup.setOnClickListener {
            goChat()
        }

        binding.menuGoChat.setOnClickListener {
            goChat()
        }
    }

    fun goChat(){
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("nodeType", NODE_CURRENT_CHAT)
            putExtra("userName", userName)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    private fun setupImageLayout() {
        val height = ScreenUtils.heightPx

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            height - (height / 4)
        )
        binding.linearImageActivity.layoutParams = layoutParams

        binding.linearImageActivity.setOnClickListener {
            val intent = Intent(this, SlidePhotoActivity::class.java).apply {
                putStringArrayListExtra("photoList", ArrayList(photoList))
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            startActivity(intent)
        }
    }

    // endregion
    // region Lifecycle

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch { userRepository.setUserLastSeen() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { userRepository.setUserOnline() }
    }

    // endregion
    // region Menu

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val actionSilent     = menu.findItem(R.id.action_notifications_off)
        val actionNotif      = menu.findItem(R.id.action_notifications_on)
        val actionBlock      = menu.findItem(R.id.action_block)
        val actionUnblock    = menu.findItem(R.id.action_unblock)
        val actionDeleteChat = menu.findItem(R.id.action_delete_chat)

        val state = profileViewModel.uiState.value.chatState

        actionDeleteChat.isVisible = true

        when (state) {
            CHAT_STATE_BLOQ -> {
                actionSilent.isVisible = false
                actionNotif.isVisible  = false
                actionBlock.isVisible  = false
                actionUnblock.isVisible = true
            }

            CHAT_STATE_SILENT -> {
                actionSilent.isVisible = false
                actionNotif.isVisible  = true
                actionBlock.isVisible  = true
                actionUnblock.isVisible = false
            }

            else -> { // chat, delete, etc.
                actionSilent.isVisible = true
                actionNotif.isVisible  = false
                actionBlock.isVisible  = true
                actionUnblock.isVisible = false
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.action_notifications_off, R.id.action_notifications_on -> {
                profileViewModel.onToggleNotificationsClicked(userId, userName, NODE_CURRENT_CHAT)
                true
            }

            R.id.action_block -> {
                profileViewModel.onBlockClicked(userId, userName, NODE_CURRENT_CHAT)
                true
            }

            R.id.action_unblock -> {
                profileViewModel.onUnblockClicked(userId, userName, NODE_CURRENT_CHAT)
                true
            }

            R.id.action_delete_chat -> {
                profileViewModel.onDeleteClicked(userId, userName, NODE_CURRENT_CHAT)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
