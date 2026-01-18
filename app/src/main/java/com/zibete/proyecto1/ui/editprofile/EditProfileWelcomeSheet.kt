package com.zibete.proyecto1.ui.editprofile

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.BottomSheetEditProfileWelcomeBinding

class EditProfileWelcomeSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onEditProfileWelcomeDismissed()
    }

    private var _binding: BottomSheetEditProfileWelcomeBinding? = null
    private val binding get() = _binding!!

    private var dismissNotified = false

    override fun getTheme(): Int = R.style.Zibe_BottomSheetTheme

    private val pages = listOf(
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_1_title,
            R.string.editprofile_welcome_pager_1_body
        ),
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_2_title,
            R.string.editprofile_welcome_pager_2_body
        ),
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_3_title,
            R.string.editprofile_welcome_pager_3_body
        )
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = BottomSheetDialog(requireContext(), getTheme())
        dialog.setCanceledOnTouchOutside(false)

        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = false
            isHideable = false
            peekHeight = 10

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_SETTLING ||
                        newState == BottomSheetBehavior.STATE_COLLAPSED
                    ) {
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            })
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditProfileWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPager()
        setupButtons()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        notifyDismissedOnce()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun notifyDismissedOnce() {
        if (dismissNotified) return
        dismissNotified = true

        val listener = (parentFragment as? Listener) ?: (activity as? Listener)
        listener?.onEditProfileWelcomeDismissed()
    }

    private fun setupPager() {
        binding.viewPager.adapter = EditProfileWelcomePagerAdapter(pages)

        TabLayoutMediator(binding.tabDots, binding.viewPager) { tab, _ ->
            tab.text = ""
        }.attach()

        updateButtons(0)

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateButtons(position)
                }
            }
        )
    }

    private fun setupButtons() {
        binding.btnPrev.setOnClickListener {
            val prev = binding.viewPager.currentItem - 1
            if (prev >= 0) binding.viewPager.currentItem = prev
        }

        binding.btnNext.setOnClickListener {
            val next = binding.viewPager.currentItem + 1
            if (next < pages.size) {
                binding.viewPager.currentItem = next
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun updateButtons(position: Int) {
        binding.btnPrev.isEnabled = position > 0
        binding.btnNext.setText(
            if (position == pages.lastIndex) R.string.action_continue else R.string.action_next
        )
    }
}