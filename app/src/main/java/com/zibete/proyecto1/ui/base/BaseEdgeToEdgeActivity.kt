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
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseEdgeToEdgeActivity : AppCompatActivity() {

    protected lateinit var toolbar: MaterialToolbar
    protected open val toolbarMenuRes: Int? = null
    protected open val toolbarMenuVisiblePredicate: (Menu) -> Unit = { }

    // Hooks (override where applies)
    protected open fun activityRootView(): View? = null
    protected open fun appBarContainerView(): View? = null
    protected open fun bottomNavView(): View? = null
    protected open fun contentViewForInsets(): View? = null

    // Internal state (avoid doubles)
    private var insetsInstalled = false
    private data class Margins(val l: Int, val t: Int, val r: Int, val b: Int)
    private data class Paddings(val l: Int, val t: Int, val r: Int, val b: Int)
    private var cachedAppBarBase: Margins? = null
    private var cachedBottomBase: Margins? = null
    private var cachedContentBase: Paddings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
    }

    protected fun setupToolbar(
        toolbar: MaterialToolbar,
        showBack: Boolean = true,
        handleBackInBase: Boolean = true
    ) {
        this.toolbar = toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)

        if (handleBackInBase) {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        installGlobalInsetsOnce()
    }

    private fun installGlobalInsetsOnce() {
        if (insetsInstalled) return
        insetsInstalled = true

        val appBarContainer = appBarContainerView()
        val bottomNav = bottomNavView()
        val content = contentViewForInsets()

        fun View.readMargins(): Margins {
            val lp = layoutParams as? ViewGroup.MarginLayoutParams
            return if (lp != null) Margins(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
            else Margins(0, 0, 0, 0)
        }

        fun View.readPaddings(): Paddings =
            Paddings(paddingLeft, paddingTop, paddingRight, paddingBottom)

        // Cache base values once (XML)
        if (cachedAppBarBase == null) cachedAppBarBase = appBarContainer?.readMargins()
        if (cachedBottomBase == null) cachedBottomBase = bottomNav?.readMargins()
        if (cachedContentBase == null) cachedContentBase = content?.readPaddings()

        // 1) App bar container: safe margins for status bar + gesture sides (landscape)
        appBarContainer?.let { container ->
            val base = cachedAppBarBase ?: container.readMargins()

            ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = base.t + sys.top
                    leftMargin = base.l + sys.left
                    rightMargin = base.r + sys.right
                }
                insets
            }
            ViewCompat.requestApplyInsets(container)
        }

        // 2) Bottom nav: safe margins for nav/gesture bar + gesture sides (landscape)
        bottomNav?.let { bar ->
            val base = cachedBottomBase ?: bar.readMargins()

            ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = base.b + sys.bottom
                    leftMargin = base.l + sys.left
                    rightMargin = base.r + sys.right
                }
                insets
            }
            ViewCompat.requestApplyInsets(bar)
        }

        // 3) Content (NavHost / fragment container): safe padding for sides + bottom
        // Top is handled by the app bar container.
        content?.let { c ->
            val base = cachedContentBase ?: c.readPaddings()

            ViewCompat.setOnApplyWindowInsetsListener(c) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updatePadding(
                    left = base.l + sys.left,
                    right = base.r + sys.right
                )
                insets
            }
            ViewCompat.requestApplyInsets(c)
        }
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
