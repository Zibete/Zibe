package com.zibete.proyecto1.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseToolbarActivity : AppCompatActivity() {

    protected lateinit var toolbar: MaterialToolbar
    protected open val toolbarMenuRes: Int? = null
    protected open val toolbarMenuVisiblePredicate: (Menu) -> Unit = { }
    protected open fun activityRootView(): View? = null
    protected open fun bottomNavView(): View? = null
    protected open fun appBarContainerView(): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
    }

    protected fun setupToolbar(
        toolbar: MaterialToolbar,
        showBack: Boolean = true,
        handleBackInBase: Boolean = true,
        title: CharSequence = ""
    ) {
        this.toolbar = toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
        supportActionBar?.title = title

        if (handleBackInBase) {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        applyBarsInsetsWhenReady()
    }

    private fun applyBarsInsetsWhenReady() {
        val bottomNav = bottomNavView()

        // Bottom bar: marginBottom base real
        val bottomBaseMargin =
            (bottomNav?.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        val appBarContainer = appBarContainerView()
        val topBaseMargin =
            (appBarContainer?.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

        appBarContainer?.let { container ->
            ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = topBaseMargin + sys.top
                }
                insets
            }
            ViewCompat.requestApplyInsets(container)
        }

        // Listener en BOTTOM BAR
        bottomNav?.let { bar ->
            ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = bottomBaseMargin + sys.bottom
                }
                insets
            }
        }

        // Pedimos insets para ambos
        ViewCompat.requestApplyInsets(toolbar)
        bottomNav?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        toolbarMenuRes?.let { menuInflater.inflate(it, menu) }
        return toolbarMenuRes != null
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        toolbarMenuVisiblePredicate(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}