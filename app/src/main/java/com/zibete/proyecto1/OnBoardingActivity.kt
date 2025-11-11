package com.zibete.proyecto1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.adapters.OnboardingAdapter
import com.zibete.proyecto1.adapters.OnboardingPage

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dots: TabLayout
    private lateinit var btnBack: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnNext: MaterialButton

    private lateinit var adapter: OnboardingAdapter

    private val pages = listOf(
        OnboardingPage(
            animationRes = R.raw.chat_right,
            title = "Chatea",
            description = "Chatea con familiares y amigos, cuando quieras, en tiempo real!"
        ),
        OnboardingPage(
            animationRes = R.raw.lf30_editor_miibzys8,
            title = "Descubre",
            description = "Encuentra personas cercanas a tu ubicación. Haz nuevos amigos!"
        ),
        OnboardingPage(
            animationRes = R.raw.onboarding_persons,
            title = "Socializa",
            description = "Únete a las salas de chat existentes, o crea una a tu medida!"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya se mostró onboarding, saltearlo
        if (shouldSkipOnboarding()) {
            finish()
            return
        }

        setContentView(R.layout.activity_on_boarding)

        viewPager = findViewById(R.id.viewPager)
        dots = findViewById(R.id.dots)
        btnBack = findViewById(R.id.btnBack)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)

        adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        // Dots con TabLayoutMediator (no usamos títulos, solo indicadores)
        TabLayoutMediator(dots, viewPager) { _, _ -> }.attach()

        updateButtons(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })

        btnNext.setOnClickListener {
            val position = viewPager.currentItem
            if (position == pages.lastIndex) {
                completeOnboarding()
            } else {
                viewPager.currentItem = position + 1
            }
        }

        btnBack.setOnClickListener {
            val position = viewPager.currentItem
            if (position > 0) {
                viewPager.currentItem = position - 1
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun updateButtons(position: Int) {
        val lastIndex = pages.lastIndex

        btnBack.isVisible = position > 0
        btnBack.isEnabled = position > 0

        btnNext.text = if (position == lastIndex) {
            getString(R.string.comenzar)
        } else {
            getString(R.string.siguiente)
        }

        // Skip visible solo si no es la última
        btnSkip.isVisible = position < lastIndex
    }

    private fun shouldSkipOnboarding(): Boolean {
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        val completed = prefs.getBoolean("completed", false)
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        // Si ya lo vio una vez, o si decidís saltar para logueados
        return completed && isLoggedIn
    }

    private fun completeOnboarding() {
        getSharedPreferences("onboarding", MODE_PRIVATE)
            .edit()
            .putBoolean("completed", true)
            .apply()

        // Volver a Splash / Login / Main según tu flujo actual
        // Si solo querés cerrar:
        finish()

        // Ejemplo si querés mandarlo a Splash:
        // startActivity(Intent(this, SplashActivity::class.java))
        // finish()
    }
}
