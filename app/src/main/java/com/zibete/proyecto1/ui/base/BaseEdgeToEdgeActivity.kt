package com.zibete.proyecto1.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.SnackBarManagerEntryPoint
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collectLatest

abstract class BaseEdgeToEdgeActivity : AppCompatActivity() {

    protected lateinit var toolbar: MaterialToolbar
    protected open val toolbarMenuRes: Int? = null
    protected open val toolbarMenuVisiblePredicate: (Menu) -> Unit = { }
    protected open val enableComposeSnackHost: Boolean = true

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

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        installComposeSnackHost()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        installComposeSnackHost()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        installComposeSnackHost()
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

    private fun installComposeSnackHost() {
        if (!enableComposeSnackHost) return

        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        if (root.findViewById<ComposeView>(R.id.snack_compose_host) != null) return

        val snackBarManager = EntryPointAccessors.fromApplication(
            applicationContext,
            SnackBarManagerEntryPoint::class.java
        ).snackBarManager()

        val bottomNav = bottomNavView()
        val appBar = appBarContainerView()
        val composeView = ComposeView(this).apply {
            id = R.id.snack_compose_host
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZibeTheme {
                    ZibeGlobalSnackHost(
                        snackBarManager = snackBarManager,
                        bottomNav = bottomNav,
                        appBar = appBar
                    )
                }
            }
        }

        root.addView(composeView)
    }

    private fun installGlobalInsetsOnce() {
        if (insetsInstalled) return
        insetsInstalled = true

        val appBarContainer = appBarContainerView()
        val bottomNav = bottomNavView()
        val content = contentViewForInsets()

        fun View.readMargins(): Margins {
            val lp = layoutParams as? ViewGroup.MarginLayoutParams
            return if (lp != null) Margins(
                lp.leftMargin,
                lp.topMargin,
                lp.rightMargin,
                lp.bottomMargin
            )
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

    fun dismissImeIfVisible(): Boolean {
        val root = window.decorView.rootView
        val imeVisible = ViewCompat.getRootWindowInsets(root)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

        if (!imeVisible) return false

        val imm =
            getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(root.windowToken, 0)
        return true
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
                if (dismissImeIfVisible()) true
                else {
                    onBackPressedDispatcher.onBackPressed(); true
                }
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}

@Composable
private fun ZibeGlobalSnackHost(
    snackBarManager: SnackBarManager,
    bottomNav: View?,
    appBar: View?
) {
    val snackHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var bottomNavHeightPx by remember { mutableIntStateOf(0) }
    var bottomNavVisible by remember { mutableStateOf(false) }

    var appBarHeightPx by remember { mutableIntStateOf(0) }
    var appBarVisible by remember { mutableStateOf(false) }

    DisposableEffect(bottomNav) {
        if (bottomNav == null) {
            bottomNavHeightPx = 0
            bottomNavVisible = false
            return@DisposableEffect onDispose { }
        }

        val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            bottomNavHeightPx = view.height
            bottomNavVisible = view.isVisible
        }

        bottomNav.addOnLayoutChangeListener(listener)
        bottomNavHeightPx = bottomNav.height
        bottomNavVisible = bottomNav.isVisible

        onDispose { bottomNav.removeOnLayoutChangeListener(listener) }
    }

    DisposableEffect(appBar) {
        if (appBar == null) {
            appBarHeightPx = 0
            appBarVisible = false
            return@DisposableEffect onDispose { }
        }

        val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            appBarHeightPx = view.height
            appBarVisible = view.isVisible
        }

        appBar.addOnLayoutChangeListener(listener)
        appBarHeightPx = appBar.height
        appBarVisible = appBar.isVisible

        onDispose { appBar.removeOnLayoutChangeListener(listener) }
    }

    val extraBottom = if (bottomNavVisible) with(density) { bottomNavHeightPx.toDp() } else 0.dp
    val extraTop = if (appBarVisible) with(density) { appBarHeightPx.toDp() } else 0.dp

    LaunchedEffect(lifecycleOwner, snackBarManager) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snackBarManager.events.collectLatest { event ->
                snackHostState.showZibeMessage(
                    message = event.uiText.asString(context),
                    snackType = event.type
                )
            }
        }
    }

    ZibeSnackbar(
        hostState = snackHostState,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(
                top = extraTop + dimensionResource(DsR.dimen.element_spacing_medium),
                bottom = dimensionResource(DsR.dimen.element_spacing_medium) + extraBottom
            )
    )
}





