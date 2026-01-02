package com.zibete.proyecto1.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.zibete.proyecto1.adapters.SliderPhotoAdapter
import com.zibete.proyecto1.databinding.SlideActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var binding: SlideActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = SlideActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val photoList =
            intent.getStringArrayListExtra(EXTRA_PHOTO_LIST) ?: arrayListOf()
        val single = intent.getStringExtra(EXTRA_PHOTO)
        if (photoList.isEmpty() && single != null) photoList.add(single)

        val position = intent.getIntExtra(EXTRA_POSITION, 0)
            .coerceIn(0, (photoList.size - 1).coerceAtLeast(0))

        val rotationY = intent.getIntExtra(EXTRA_ROTATION, 0)

        val adapter = SliderPhotoAdapter(
            urls = photoList,
            onLoadStart = { binding.progressbarImage.isVisible = true },
            onLoadEnd = { binding.progressbarImage.isVisible = false }
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(position, false)
        binding.viewPager.rotationY = rotationY.toFloat()

        binding.toolbar2.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
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
