package com.zibete.proyecto1.ui.main

import android.Manifest
import android.animation.LayoutTransition
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.ERROR_NAV_HOST_FRAGMENT
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.core.utils.ZibeApp
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.ui.base.BaseToolbarActivity
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.editprofile.EditProfileFragment
import com.zibete.proyecto1.ui.extensions.getColorCompat
import com.zibete.proyecto1.ui.groups.GroupsFragment
import com.zibete.proyecto1.ui.groups.host.GroupHostFragment
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.ui.settings.SettingsActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.users.UsersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseToolbarActivity() {

    val mainViewModel: MainViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var materialToolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentScreen: CurrentScreen = CurrentScreen.OTHER
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var layoutSettings: View? = null
    private var filterButton: ImageView? = null
    private var refreshButton: ImageView? = null
    private var searchView: SearchView? = null
    private var badgeDrawableChat: BadgeDrawable? = null
    private var badgeDrawableGroup: BadgeDrawable? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    override val toolbarMenuRes: Int = R.menu.menu_main

    override fun activityRootView(): View = binding.root

    override fun bottomNavView(): View = binding.appBarMain.contentMain.navView3

    override fun appBarContainerView(): View = binding.appBarMain.materialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ZibeApp.ScreenUtils.init(this)

        setupUI()

        setupNavigation()

        setupObservers() // <--- Acá ViewModel

        setupLocation()

        mainViewModel.checkFirstLoginDone()

        setupOnBackPressedDispatcher()
    }

    // ==========================================
    // 1. SETUP UI & NAVIGATION
    // ==========================================

    private fun setupUI() {
        // Toolbar & Layouts
        val appBarMain = binding.appBarMain
        materialToolbar = appBarMain.materialToolbar
        layoutSettings = appBarMain.linearLayoutSettings
        filterButton = appBarMain.filterButton
        refreshButton = appBarMain.refreshButton

        // Botón de refrescar
        refreshButton?.setOnClickListener {
            usersViewModel.loadUsers()
        }

        // Botón de filtro → dispara un evento, el Fragment abre el diálogo
        filterButton?.setOnClickListener {
            usersViewModel.onFilterClicked()
        }

        val hasActiveFilter = mainViewModel.hasActiveFilter.value

        val colorRes = if (hasActiveFilter) R.color.accent else R.color.blanco
        filterButton?.setColorFilter(
            this.getColorCompat(colorRes),
            PorterDuff.Mode.SRC_IN
        )

        // Transiciones suaves
        appBarMain.materialToolbar.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        setupToolbar(
            toolbar = materialToolbar,
            showBack = false,
            handleBackInBase = false
        )

        // Drawer
        drawerLayout = binding.drawerLayout
        materialToolbar.setNavigationOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        navigationView = binding.navView

        // Header Info
        val headerView = navigationView?.getHeaderView(0)

        setupHeader(headerView)

        // Bottom Nav
        bottomNavigationView = appBarMain.contentMain.navView3

        setupBadges()

    }

    private fun setupHeader(headerView: View?) {
        if (headerView == null) return

        val cardUserImage = headerView.findViewById<MaterialCardView>(R.id.card_user_image)
        val userImage = headerView.findViewById<ImageView>(R.id.user_image)
        val tvUserName = headerView.findViewById<TextView>(R.id.tv_user_name)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tv_mail)
        val editProfileButton = headerView.findViewById<LinearLayout>(R.id.edit_profile_button)

        cardUserImage?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ZibeApp.ScreenUtils.heightPx / 2
        )

        tvUserName?.text = mainViewModel.myDisplayName()
        tvUserEmail?.text = mainViewModel.myEmail()
        Glide.with(this).load(mainViewModel.myPhotoUrl()).into(userImage)

        editProfileButton?.setOnClickListener { editProfileNavigation() }
    }


    private fun setupBadges() {
        // Chat Badge
        badgeDrawableChat = bottomNavigationView.getOrCreateBadge(R.id.navBottomChat).apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.white)
            isVisible = false
        }
        // Group Badge
        badgeDrawableGroup = bottomNavigationView.getOrCreateBadge(R.id.navBottomGrupos).apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.white)
            isVisible = false
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: error(ERROR_NAV_HOST_FRAGMENT)

        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_chat,
                R.id.nav_users,
                R.id.nav_groups,
                R.id.nav_favorites
            ),
            drawerLayout
        )

        navController.let { nav ->
            NavigationUI.setupActionBarWithNavController(this, nav, appBarConfiguration)
            navigationView?.let { nv -> NavigationUI.setupWithNavController(nv, nav) }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            mainViewModel.onBottomItemSelected(item.itemId)
            true
        }

//        // Estado inicial
        goToChatTab()
    }

    private fun setupObservers() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect { state ->
                    // Badges
                    badgeDrawableChat?.isVisible = state.chatListBadgeCount > 0
                    badgeDrawableChat?.number = state.chatListBadgeCount
                    badgeDrawableGroup?.isVisible = state.groupBadgeCount > 0
                    badgeDrawableGroup?.number = state.groupBadgeCount
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.toolbarState.collect { state ->
                    // Visibilidad
                    materialToolbar.isVisible = state.showToolbar
                    layoutSettings?.isVisible = state.showSettings
                    bottomNavigationView.isVisible = state.showBottomNav

                    // title
                    materialToolbar.title =
                        state.currentScreen.titleRes.asString(this@MainActivity)

                    currentScreen = state.currentScreen

                    invalidateOptionsMenu()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.groupName.collect { materialToolbar.title = it }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Navegación
                launch {
                    mainViewModel.uiEvents.collect { event ->
                        when (event) {

                            is MainUiEvent.ToUsers -> {
                                navController.navigate(R.id.nav_users)
                                bottomNavigationView.selectedItemId = R.id.navBottomUsers
                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainUiEvent.ToChat -> {
                                navController.navigate(R.id.nav_chat)
                                bottomNavigationView.selectedItemId = R.id.navBottomChat
                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainUiEvent.BackToChat -> {
                                goToChatTab()
                            }

                            is MainUiEvent.ToGroupHost -> {
                                mainViewModel.showToolbar(true)
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, GroupHostFragment())
//                                    .replace(R.id.nav_host_fragment, GroupPagerFragment())
                                    .commit()

                                bottomNavigationView.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainUiEvent.ToGroupsSelect -> {
                                navController.navigate(R.id.nav_groups)
                                bottomNavigationView.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainUiEvent.ToFavorites -> {
                                navController.navigate(R.id.nav_favorites)
                                bottomNavigationView.selectedItemId = R.id.navBottomFavorites
                            }

                            is MainUiEvent.ToEditProfile -> {
                                navController.navigate(R.id.nav_editPerfil)
                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainUiEvent.NavigateToSplash -> {
                                val intent = Intent(this@MainActivity, SplashActivity::class.java)
                                if (event.sessionConflict) intent.putExtra(
                                    EXTRA_SESSION_CONFLICT,
                                    true
                                )
                                stopLocationUpdates()
                                finish()
                                startActivity(intent)
                            }

                            is MainUiEvent.ToGroupsAfterExit -> {
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, GroupsFragment())
                                    .commit()
                                bottomNavigationView.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainUiEvent.BackFromEditProfile -> {
                                handleBackFromEditProfile()
                            }

                            is MainUiEvent.BackExitAppOrCloseSearch -> {
                                if (searchView?.isIconified == false) {
                                    searchView?.onActionViewCollapsed()
                                } else {
                                    finish()
                                }
                            }

                            is MainUiEvent.ToSettings -> {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SettingsActivity::class.java
                                    )
                                )
                            }

                            is MainUiEvent.ConfirmExitGroup -> {
                                UserMessageUtils.confirm(
                                    context = this@MainActivity,
                                    title = getString(R.string.action_exit),
                                    message = "¿Desea abandonar ${mainViewModel.groupName.value}?",
                                    onConfirm = {
                                        mainViewModel.onExitGroupConfirmed(getString(R.string.msg_user_leaved))
                                    }
                                )
                            }

                            is MainUiEvent.ConfirmLogout -> {
                                UserMessageUtils.confirm(
                                    context = this@MainActivity,
                                    title = "Cerrar sesión",
                                    message = "¿Está seguro de cerrar su sesión?",
                                    positiveText = getString(R.string.action_accept),
                                    negativeText = getString(R.string.action_cancel),
                                    onConfirm = {
                                        mainViewModel.onLogoutConfirmed()
                                    }
                                )
                            }

                            is MainUiEvent.ShowSnack -> {
                                UserMessageUtils.showSnack(
                                    root = binding.root,
                                    message = event.uiText.asString(this@MainActivity),
                                    type = event.type
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun goToChatTab() {
        mainViewModel.onChatTabSelected()
    }

    fun editProfileNavigation() {
        mainViewModel.onEditProfileSelected()
    }

    private fun handleBackFromEditProfile() {
        val frag = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        if (frag is EditProfileFragment) {
            if (frag.hasPendingChanges()) {
                UserMessageUtils.showSnack(
                    root = binding.root,
                    message = "Guarde los cambios antes de salir",
                    type = ZibeSnackType.WARNING
                )
                return
            }
        }
        goToChatTab()
    }

    private fun setupLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { mainViewModel.onLocationChanged(it) }
            }
        }
        ensureLocationSettingsAndStart()
    }

    private fun ensureLocationSettingsAndStart() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        val settingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
                .setAlwaysShow(true).build()
        settingsClient?.checkLocationSettings(settingsRequest)
            ?.addOnSuccessListener { startLocationUpdates() }
            ?.addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, 0x1)
                    } catch (_: IntentSender.SendIntentException) {
                    }
                }
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
    }

//    override fun onSearchViewReady(searchView: SearchView) {
//        this.searchView = searchView
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?) = false
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                val active = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
//                    ?.childFragmentManager
//                    ?.primaryNavigationFragment
//
//                (active as? SearchHandler)?.onSearchQueryChanged(newText)
//                return true
//            }
//        })
//    }

    override val toolbarMenuVisiblePredicate: (Menu) -> Unit = { menu ->
        for (i in 0 until menu.size()) menu.getItem(i).isVisible = false

        when (currentScreen) {
            CurrentScreen.USERS -> {
                menu.findItem(R.id.action_search)?.isVisible = true
                menu.findItem(R.id.action_settings)?.isVisible = true
            }

            CurrentScreen.CHAT -> {
                menu.findItem(R.id.action_settings)?.isVisible = true
            }

            CurrentScreen.GROUPS -> {
                menu.findItem(R.id.action_unhide_chats)?.isVisible = true
            }

            CurrentScreen.FAVORITES -> {
                menu.findItem(R.id.action_favorites)?.isVisible = true
            }

            else -> Unit
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchView = (menu.findItem(R.id.action_search)?.actionView as? SearchView)
        this.searchView = searchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val active = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager
                    ?.primaryNavigationFragment

                (active as? SearchHandler)?.onSearchQueryChanged(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 1) delegás al VM
        mainViewModel.onToolbarItemSelected(item.itemId)

        // 2) dejás que el framework maneje lo demás (home/up incluido)
        return super.onOptionsItemSelected(item)
    }

    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                // 1. Si el drawer está abierto → cerrarlo
                if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    drawerLayout?.closeDrawer(GravityCompat.START)
                    return
                }

                // 2. Manejo global delegado al ViewModel
                mainViewModel.onBackPressed()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.startPresence()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        val screen = mainViewModel.toolbarState.value.currentScreen
        materialToolbar.title = screen.titleRes.asString(this)
        invalidateOptionsMenu()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            findNavController(R.id.nav_host_fragment),
            appBarConfiguration
        ) || super.onSupportNavigateUp()
    }
}