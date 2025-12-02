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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.EditProfileFragment
import com.zibete.proyecto1.ui.GruposFragment
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_EXIT
import com.zibete.proyecto1.ui.extensions.getColorCompat
import com.zibete.proyecto1.ui.settings.SettingsActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.users.UsersViewModel
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.Utils
import com.zibete.proyecto1.utils.ZibeApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint // <--- ESTO HACE QUE LA ACTIVITY PUEDA RECIBIR INYECCIONES
class MainActivity : AppCompatActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userSessionManager: UserSessionManager
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer
    @Inject lateinit var userRepository: UserRepository

    private val user get() = userSessionManager.user
    private val mainViewModel: MainViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var navController: NavController? = null
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var materialToolbar: MaterialToolbar? = null
    private var layoutSettings: View? = null
    private var filterButton: ImageView? = null
    private var refreshButton: ImageView? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var searchView: SearchView? = null
    private var badgeDrawableChat: BadgeDrawable? = null
    private var badgeDrawableGroup: BadgeDrawable? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupNavigation()
        setupObservers() // <--- Aquí conectamos el ViewModel
        setupLocation()

        mainViewModel.checkIfMustOpenEditProfile()

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
            usersViewModel.loadUsers(isRefresh = true)
        }

        // Botón de filtro → dispara un evento, el Fragment abre el diálogo
        filterButton?.setOnClickListener {
            usersViewModel.onFilterClicked()
        }

        val hasActiveFilter = userPreferencesRepository.filterSwitch

        val colorRes = if (hasActiveFilter) R.color.accent else R.color.blanco
        filterButton?.setColorFilter(
            this.getColorCompat(colorRes),
            PorterDuff.Mode.SRC_IN
        )


        // Transiciones suaves
        appBarMain.materialToolbar.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        setSupportActionBar(materialToolbar)

        // Drawer
        drawerLayout = binding.drawerLayout
        navigationView = binding.navView

        // Header Info
        val headerView = navigationView?.getHeaderView(0)
        setupHeader(headerView)

        // Bottom Nav
        bottomNavigationView = appBarMain.contentMain.navView3
        setupBadges()

        ZibeApp.ScreenUtils.init(this)
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

        tvUserName?.text = user.displayName
        tvUserEmail?.text = user.email
        Glide.with(this).load(user.photoUrl).into(userImage)

        editProfileButton?.setOnClickListener { editProfileNavigation() }
    }

    private fun setupBadges() {
        // Chat Badge
        badgeDrawableChat = bottomNavigationView?.getOrCreateBadge(R.id.navBottomChat)?.apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.white)
            isVisible = false
        }
        // Group Badge
        badgeDrawableGroup = bottomNavigationView?.getOrCreateBadge(R.id.navBottomGrupos)?.apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.white)
            isVisible = false
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = navHostFragment?.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_chat,
                R.id.nav_users,
                R.id.nav_groups,
                R.id.nav_favorites),
            drawerLayout
        )

        navController?.let { nav ->
            NavigationUI.setupActionBarWithNavController(this, nav, appBarConfiguration)
            navigationView?.let { nv -> NavigationUI.setupWithNavController(nv, nav) }
        }

        setupBottomNavListeners()

        // Estado inicial
        chatNavigation()
    }

    // ==========================================
    // 2. VIEWMODEL OBSERVERS (El Cerebro)
    // ==========================================

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Visibilidad
                launch { mainViewModel.toolbarVisible.collect { materialToolbar?.isVisible = it } }
                launch { mainViewModel.layoutSettingsVisible.collect { layoutSettings?.isVisible = it } }
                launch { mainViewModel.bottomNavVisible.collect { bottomNavigationView?.isVisible = it } }

                // Badges
                launch {
                    mainViewModel.chatBadgeCount.collect { count ->
                        badgeDrawableChat?.isVisible = count > 0
                        badgeDrawableChat?.number = count
                    }
                }
                launch {
                    mainViewModel.groupBadgeCount.collect { count ->
                        badgeDrawableGroup?.isVisible = count > 0
                        badgeDrawableGroup?.number = count
                    }
                }

                // Navegación
                launch {
                    mainViewModel.navEvents.collect { event ->
                        when (event) {

                            is MainNavEvent.ToUsers -> {
                                invalidateOptionsMenu()
                                navController?.navigate(R.id.nav_users)
                                materialToolbar?.setTitle(R.string.menu_users)

                                bottomNavigationView?.selectedItemId = R.id.navBottomUsers
                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainNavEvent.ToChat -> {
                                invalidateOptionsMenu()
                                navController?.navigate(R.id.nav_chat)
                                materialToolbar?.setTitle(R.string.menu_chat)

                                bottomNavigationView?.selectedItemId = R.id.navBottomChat
                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainNavEvent.ToGroupsDetail -> {

                                userPreferencesRepository.groupName = event.groupName
                                userPreferencesRepository.userNameGroup = event.userName
                                userPreferencesRepository.inGroup = true

                                mainViewModel.showToolbar(true)
                                invalidateOptionsMenu()

                                val newFragment = PageAdapterGroup()

                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, newFragment)
                                    .commit()

                                materialToolbar?.title = event.groupName
                                bottomNavigationView?.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainNavEvent.ToGroupsSelect -> {
                                invalidateOptionsMenu()
                                navController?.navigate(R.id.nav_groups)
                                materialToolbar?.setTitle(R.string.menu_groups)

                                bottomNavigationView?.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainNavEvent.ToFavorites -> {
                                invalidateOptionsMenu()
                                navController?.navigate(R.id.nav_favorites)
                                materialToolbar?.setTitle(R.string.menu_favorites)

                                bottomNavigationView?.selectedItemId = R.id.navBottomFavorites
                            }

                            is MainNavEvent.ToEditProfile -> {
                                invalidateOptionsMenu()
                                navController?.navigate(R.id.nav_editPerfil)
                                materialToolbar?.setTitle(R.string.menu_edit)

                                drawerLayout?.closeDrawer(GravityCompat.START)
                            }

                            is MainNavEvent.ToSplashAfterLogout -> {
                                stopLocationUpdates()
                                finish()
                                startActivity(Intent(this@MainActivity, SplashActivity::class.java))
                            }

                            is MainNavEvent.ToGroupsAfterExit -> {
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, GruposFragment())
                                    .commit()

                                materialToolbar?.setTitle(R.string.menu_groups)
                                invalidateOptionsMenu()
                                bottomNavigationView?.selectedItemId = R.id.navBottomGrupos
                            }

                            is MainNavEvent.BackFromEditProfile -> {
                                handleBackFromEditProfile()
                            }

                            is MainNavEvent.BackExitAppOrCloseSearch -> {
                                if (searchView?.isIconified == false) {
                                    searchView?.onActionViewCollapsed()
                                } else {
                                    finish()
                                }
                            }

                            is MainNavEvent.BackToChat -> {
                                mainViewModel.onChatTabSelected()
                            }

                            is MainNavEvent.ToSettings -> {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }

                            is MainNavEvent.ConfirmExitGroup -> {
                                UserMessageUtils.confirm(
                                    context = this@MainActivity,
                                    title = DIALOG_EXIT,
                                    message = "¿Desea abandonar ${event.groupName}?",
                                    positiveText = DIALOG_ACCEPT,
                                    negativeText = DIALOG_CANCEL,
                                    onConfirm = {
                                        mainViewModel.onExitGroupConfirmed()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. NAVEGACIÓN (BottomNav & Menu)
    // ==========================================

    private fun setupBottomNavListeners() {
        bottomNavigationView?.setOnItemSelectedListener { item ->
            mainViewModel.onBottomItemSelected(item.itemId)
            true
        }
    }

    fun chatNavigation() {
        mainViewModel.onChatTabSelected()
    }

    fun editProfileNavigation() {
        mainViewModel.onEditProfileSelected()
    }

    private fun handleBackFromEditProfile() {
        val frag = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        if (frag is EditProfileFragment) {
            if (!frag.canExit()) {
                UserMessageUtils.showSnack(
                    root = binding.root,
                    message = "Guarde los cambios antes de salir",
                    duration = Snackbar.LENGTH_SHORT,
                    iconRes = R.drawable.ic_info_24
                )
                return
            }
        }

        mainViewModel.onChatTabSelected()
    }

    // ==========================================
    // 4. LOGIC (Logout, Exit, Etc)
    // ==========================================

    fun logout() {
        UserMessageUtils.confirm(
            context = this,
            title = "Cerrar sesión",
            message = "¿Está seguro de cerrar su sesión?",
            positiveText = DIALOG_ACCEPT,
            negativeText = DIALOG_CANCEL,
            onConfirm = {
                mainViewModel.onLogoutConfirmed()
            }
        )
    }

    // ==========================================
    // 5. LOCATION (Mantenido en Activity)
    // ==========================================

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        val settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!).setAlwaysShow(true).build()
        settingsClient?.checkLocationSettings(settingsRequest)
            ?.addOnSuccessListener { startLocationUpdates() }
            ?.addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try { e.startResolutionForResult(this, 0x1) }
                    catch (_: IntentSender.SendIntentException) {}
                }
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!, locationCallback!!, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
    }

    // ==========================================
    // 6. OVERRIDES & HELPERS
    // ==========================================

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mainViewModel.onToolbarItemSelected(item.itemId)
        return true
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { userRepository.setUserOnline() }
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch { userRepository.setUserLastSeen() }
        stopLocationUpdates()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(findNavController(R.id.nav_host_fragment), appBarConfiguration) || super.onSupportNavigateUp()
    }
}