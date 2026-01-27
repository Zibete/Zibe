//package com.zibete.proyecto1
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.view.isVisible
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentManager
//import androidx.fragment.app.FragmentPagerAdapter
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import com.google.android.material.snackbar.Snackbar
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.ValueEventListener
//import com.zibete.proyecto1.data.GroupRepository
//import com.zibete.proyecto1.data.UserPreferencesRepository
//import com.zibete.proyecto1.databinding.PagerGroupsChatBinding
//import com.zibete.proyecto1.ui.chatgroup.ChatGroupFragment
//import com.zibete.proyecto1.ui.components.ZibeSnackType
//import com.zibete.proyecto1.ui.constants.NO_INTERNET
//import com.zibete.proyecto1.ui.main.MainViewModel
//import com.zibete.proyecto1.utils.UserMessageUtils
//import com.zibete.proyecto1.utils.Utils.AppChecks
//import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class GroupPagerFragment : Fragment() {
//
//    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
//    @Inject lateinit var groupRepository: GroupRepository
//
//    private var _binding: PagerGroupsChatBinding? = null
//    private val binding get() = _binding!!
//
//    private val fragments: List<Fragment> = listOf(
//        GroupUsersFragment(),
//        ChatGroupFragment(),
//        ChatListGroupsFragment()
//    )
//
//    private var titleListener: ValueEventListener? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = PagerGroupsChatBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // TEMP: Groups UI legacy disabled while migrating to Compose/VP2
//        binding.linearProgressBar.isVisible = false
//        binding.progressBar.isVisible = false
//        binding.viewPager.isVisible = false
//        binding.tabLayout.isVisible = false
//
//        UserMessageUtils.finishActionShowSnack(
//            root = binding.root,
//            message = "Sección en migración. Próximamente disponible.",
//            type = ZibeSnackType.INFO,
//            duration = Snackbar.LENGTH_LONG,
//            actionText = "Volver",
//            action = { requireActivity().onBackPressedDispatcher.onBackPressed() }
//        )
//    }
//
//
//    // ========== Network + loader ==========
//
//    private fun startNetworkCheck() {
//        binding.linearProgressBar.isVisible = true
//        binding.progressBar.isVisible = true
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            delay(800)
//
//            if (AppChecks.hasInternetConnection(requireContext())) {
//                binding.viewPager.currentItem = 1
//                delay(200)
//                binding.linearProgressBar.isVisible = false
//                binding.progressBar.isVisible = false
//            } else {
//                binding.progressBar.isVisible = false
//                binding.linearProgressBar.isVisible = true
//
//                UserMessageUtils.finishActionShowSnack(
//                    root = binding.viewPager,
//                    message = NO_INTERNET,
//                    type = ZibeSnackType.ERROR,
//                    duration = Snackbar.LENGTH_INDEFINITE,
//                    actionText = "Reintentar",
//                    action = { startNetworkCheck() }
//                )
//            }
//        }
//    }
//
//    // ========== Badge interno (BadgedTabLayout) ==========
//    private val mainViewModel: MainViewModel by activityViewModels()
//
//    private fun observeUnreadBadgeForTab() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                mainViewModel.uiState.collect { state ->
//                    binding.tabLayout.setBadgeText(
//                        2, // Tab de "chat" es el índice 2
//                        state.groupBadgeCount.takeIf { it > 0 }?.toString()
//                    )
//                }
//            }
//        }
//    }
//
//    // ========== Adapter interno ==========
//
//    inner class MyPagerAdapter(fm: FragmentManager) :
//        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
//
//        private var membersCount: Int = 0
//
//        init {
//            val groupName = userPreferencesRepository.groupName
//            if (groupName.isNotEmpty()) {
//                val listener = object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        if (!isAdded) return
//                        membersCount = snapshot.childrenCount.toInt()
//                        notifyDataSetChanged()
//                    }
//
//                    override fun onCancelled(error: DatabaseError) = Unit
//                }
//                titleListener = listener
//                refGroupUsers.child(groupName).addValueEventListener(listener)
//            }
//        }
//
//        override fun getCount(): Int = fragments.size
//        override fun getItem(position: Int): Fragment = fragments[position]
//
//        override fun getPageTitle(position: Int): CharSequence? = when (position) {
//            0 -> "(${membersCount}) ${requireContext().getString(R.string.menu_users)}"
//            1 -> userPreferencesRepository.groupName
//            2 -> requireContext().getString(R.string.menu_chat)
//            else -> null
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//
//        titleListener?.let { listener ->
//            val groupName = userPreferencesRepository.groupName
//            if (groupName.isNotEmpty()) {
//                refGroupUsers.child(groupName).removeEventListener(listener)
//            }
//        }
//        titleListener = null
//
//        _binding = null
//    }
//}
