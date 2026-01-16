package com.zibete.proyecto1.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.zibete.proyecto1.adapters.SliderPhotoAdapter
import com.zibete.proyecto1.databinding.SlideActivityBinding
import com.zibete.proyecto1.ui.base.BaseEdgeToEdgeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoViewerActivity : BaseEdgeToEdgeActivity() {

    override fun activityRootView(): View = binding.root
    override fun appBarContainerView(): View = binding.toolbar

    private lateinit var binding: SlideActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = SlideActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(
            toolbar = binding.toolbar,
            showBack = true,
            handleBackInBase = true
        )

        val photoList =
            intent.getStringArrayListExtra(EXTRA_PHOTO_LIST) ?: arrayListOf()
        val single = intent.getStringExtra(EXTRA_PHOTO)
        if (photoList.isEmpty() && single != null) photoList.add(single)

        val position = intent.getIntExtra(EXTRA_POSITION, 0)
            .coerceIn(0, (photoList.size - 1).coerceAtLeast(0))

        val rotationY = intent.getIntExtra(EXTRA_ROTATION, 0)

        val adapter = SliderPhotoAdapter(
            urls = photoList,
            onLoadStart = { pos ->
                if (pos == binding.viewPager.currentItem) {
                    binding.circularLoading.isVisible = true
                }
            },
            onLoadEnd = { pos ->
                if (pos == binding.viewPager.currentItem) {
                    binding.circularLoading.isVisible = false
                }
            }
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(position, false)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.circularLoading.isVisible = true
            }
        })

        binding.viewPager.rotationY = rotationY.toFloat()
    }

    companion object {

        private const val EXTRA_PHOTO_LIST = "extra_photo_list"
        private const val EXTRA_PHOTO = "extra_photo"
        private const val EXTRA_POSITION = "extra_position"
        private const val EXTRA_ROTATION = "extra_rotation"

        fun start(
            context: Context,
            photoList: ArrayList<String>,
            position: Int = 0,
            rotationY: Int = 0
        ) {
            if (photoList.isEmpty()) return

            val intent = Intent(context, PhotoViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_PHOTO_LIST, photoList)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_ROTATION, rotationY)
            }
            context.startActivity(intent)
        }

        fun startSingle(
            context: Context,
            photoUrl: String,
            rotationY: Int = 0
        ) {
            if (photoUrl.isBlank()) return

            val intent = Intent(context, PhotoViewerActivity::class.java).apply {
                putExtra(EXTRA_PHOTO, photoUrl)
                putExtra(EXTRA_ROTATION, rotationY)
            }
            context.startActivity(intent)
        }
    }
}