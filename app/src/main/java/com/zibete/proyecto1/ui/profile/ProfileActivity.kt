package com.zibete.proyecto1.ui.profile

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.FragmentProfileBinding
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.core.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_IDS
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : BaseChatSessionActivity() {

    private lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userIds = intent.getStringArrayListExtra(EXTRA_USER_IDS)
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        val singleUserId = intent.getStringExtra(EXTRA_USER_ID)

        if (!userIds.isNullOrEmpty()) {
            binding.pager.isVisible = true
            binding.singleContainer.isVisible = false

            binding.pager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount() = userIds.size
                override fun createFragment(position: Int) =
                    ProfileFragment().apply {
                        arguments = bundleOf(EXTRA_USER_ID to userIds[position])
                    }

            }

            binding.pager.setCurrentItem(startIndex.coerceIn(0, userIds.lastIndex), false)
            binding.pager.offscreenPageLimit = 1

        } else {
            val uid = singleUserId.orEmpty()
            binding.pager.isVisible = false
            binding.singleContainer.isVisible = true

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.singleContainer, ProfileFragment().apply {
                        arguments = bundleOf(EXTRA_USER_ID to uid)
                    })
                    .commit()

            }
        }
    }
}

