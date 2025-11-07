package com.zibete.proyecto1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.zibete.proyecto1.adapters.SliderPhotoAdapter
import com.zibete.proyecto1.databinding.SlideActivityBinding
import com.zibete.proyecto1.utils.UserRepository
import com.zibete.proyecto1.utils.FirebaseRefs.user

class SlidePhotoActivity : AppCompatActivity() {

    private lateinit var binding: SlideActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = SlideActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Acepta 1 o N fotos
        val photoList = intent.getStringArrayListExtra("photoList") ?: arrayListOf()
        val single = intent.getStringExtra("photo") // por si algún flujo manda 1 sola url
        if (photoList.isEmpty() && single != null) photoList.add(single)

        val position = intent.getIntExtra("position", 0).coerceIn(0, (photoList.size - 1).coerceAtLeast(0))
        val rotationY = intent.getIntExtra("rotation", 0)

        // Adapter con PhotoView + Coil
        val adapter = SliderPhotoAdapter(
            urls = photoList,
            onLoadStart = { binding.progressbarImage.show() },
            onLoadEnd = { binding.progressbarImage.hide() }
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(position, false)
        binding.viewPager.rotationY = rotationY.toFloat()

        binding.toolbar2.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onPause() {
        super.onPause()
        user?.uid?.let { UserRepository.setUserOffline(applicationContext, it) }
    }

    override fun onResume() {
        super.onResume()
        user?.uid?.let { UserRepository.setUserOnline(applicationContext, it) }
    }

    // helpers cortos
    private fun android.view.View.show() { this.visibility = android.view.View.VISIBLE }
    private fun android.view.View.hide() { this.visibility = android.view.View.GONE }
}
