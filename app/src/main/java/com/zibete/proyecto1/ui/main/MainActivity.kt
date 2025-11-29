package com.zibete.proyecto1.ui.main

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.settings.SettingsActivity
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.EditProfileFragment
import com.zibete.proyecto1.ui.GruposFragment
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.DIALOG_EXIT
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.utils.ZibeApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint // <--- ESTO HACE QUE LA ACTIVITY PUEDA RECIBIR INYECCIONES
class MainActivity : AppCompatActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userSessionManager: UserSessionManager
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer
    @Inject lateinit var firebaseAuth: FirebaseAuth

    private val user: FirebaseUser
        get() = firebaseAuth.currentUser!!

    // ViewModel
    private val mainUiViewModel: MainUiViewModel by viewModels()

    // ViewBinding
    private lateinit var activityMainBinding: ActivityMainBinding
    // UI Components
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

    // Badges
    private var badgeDrawableChat: BadgeDrawable? = null
    private var badgeDrawableGroup: BadgeDrawable? = null

    // Ubicación
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        setupUI()
        setupNavigation()
        setupObservers() // <--- Aquí conectamos el ViewModel
        setupLocation()
        handleIncomingIntentFlag()
        setupOnBackPressedDispatcher()
    }

    // ==========================================
    // 1. SETUP UI & NAVIGATION
    // ==========================================

    private fun setupUI() {
        // Toolbar & Layouts
        val appBarMain = activityMainBinding.appBarMain
        materialToolbar = appBarMain.materialToolbar
        layoutSettings = appBarMain.linearLayoutSettings
        filterButton = appBarMain.filterButton
        refreshButton = appBarMain.refreshButton

        // Transiciones suaves
        appBarMain.materialToolbar.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        setSupportActionBar(materialToolbar)

        // Drawer
        drawerLayout = activityMainBinding.drawerLayout
        navigationView = activityMainBinding.navView

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
                // Observar Visibilidad
                launch { mainUiViewModel.toolbarVisible.collect { materialToolbar?.isVisible = it } }
                launch { mainUiViewModel.layoutSettingsVisible.collect { layoutSettings?.isVisible = it } }
                launch { mainUiViewModel.bottomNavVisible.collect { bottomNavigationView?.isVisible = it } }

                // Observar Badges
                launch {
                    mainUiViewModel.chatBadgeCount.collect { count ->
                        badgeDrawableChat?.isVisible = count > 0
                        badgeDrawableChat?.number = count
                    }
                }
                launch {
                    mainUiViewModel.groupBadgeCount.collect { count ->
                        // Nota: La lógica compleja de grupos está migrando, por ahora mostramos si VM dice
                        badgeDrawableGroup?.isVisible = count > 0
                        badgeDrawableGroup?.number = count
                    }
                }

                // Observar Eventos (Logout, Conflictos)
                // (Implementaremos SharedFlow en VM en el futuro para eventos puros)
                // Por ahora, manejo manual de eventos si el VM tuviera SharedFlow
            }
        }
    }

    // ==========================================
    // 3. NAVEGACIÓN (BottomNav & Menu)
    // ==========================================

    private fun setupBottomNavListeners() {
        bottomNavigationView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navBottomUsers -> {
                    if (mainUiViewModel.currentScreen.value != CurrentScreen.USERS) {
                        mainUiViewModel.setScreen(CurrentScreen.USERS)
                        mainUiViewModel.showLayoutSettings(true)
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_users)
                        materialToolbar?.setTitle(R.string.menu_users)
                    }
                    true
                }
                R.id.navBottomChat -> {
                    chatNavigation()
                    true
                }
                R.id.navBottomFavorites -> {
                    if (mainUiViewModel.currentScreen.value != CurrentScreen.FAVORITES) {
                        mainUiViewModel.setScreen(CurrentScreen.FAVORITES)
                        mainUiViewModel.showLayoutSettings(false)
                        materialToolbar?.setTitle(R.string.menu_favorites)
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_favorites)
                    }
                    true
                }
                R.id.navBottomGrupos -> {
                    groupsNavigation()
                    true
                }
                else -> false
            }
        }
    }

    fun groupsNavigation() {
        if (mainUiViewModel.currentScreen.value != CurrentScreen.GROUPS) {
            mainUiViewModel.setScreen(CurrentScreen.GROUPS)
            mainUiViewModel.showLayoutSettings(false)
            if (!userPreferencesRepository.inGroup) {
                invalidateOptionsMenu()
                navController?.navigate(R.id.nav_groups)
                materialToolbar?.setTitle(R.string.menu_groups)
            } else {
                mainUiViewModel.showToolbar(true)
                invalidateOptionsMenu()
                val newFragment = PageAdapterGroup().apply {
                    arguments = Bundle().apply {
                        putString("group_name", userPreferencesRepository.groupName)
                        putString("getUid", userPreferencesRepository.userName)
                    }
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, newFragment)
                    .commit()
                materialToolbar?.title = userPreferencesRepository.groupName
            }
        }
    }

    fun chatNavigation() {
        if (mainUiViewModel.currentScreen.value != CurrentScreen.CHAT) {
            mainUiViewModel.setScreen(CurrentScreen.CHAT)

            mainUiViewModel.showToolbar(true)
            mainUiViewModel.showLayoutSettings(false)
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_chat)
            materialToolbar?.setTitle(R.string.menu_chat)

//            bottomNavigationView?.visibility = View.VISIBLE

            // Fix loop infinito
            if (bottomNavigationView?.selectedItemId != R.id.navBottomChat) {
                bottomNavigationView?.selectedItemId = R.id.navBottomChat
            }

            drawerLayout?.closeDrawer(GravityCompat.START)
        }
    }

    fun editProfileNavigation() {
        if (mainUiViewModel.currentScreen.value != CurrentScreen.EDIT_PROFILE) {
            mainUiViewModel.showBottomNav(false)
            mainUiViewModel.showLayoutSettings(false)
            materialToolbar?.setTitle(R.string.menu_edit)
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_editPerfil)
            mainUiViewModel.setScreen(CurrentScreen.EDIT_PROFILE)
        }
        drawerLayout?.closeDrawer(GravityCompat.START)
    }

    // ==========================================
    // 4. LOGIC (Logout, Exit, Etc)
    // ==========================================

    fun logout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro de cerrar su sesión?")
            .setCancelable(false)
            .setPositiveButton("Si") { _, _ -> performLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        UserRepository.setUserOffline(applicationContext, user.uid)
        mainUiViewModel.logout() // Delega lógica a VM
        EditProfileFragment.deleteProfilePreferences(this)
        stopLocationUpdates()

        // Navegación final
        finish()
        startActivity(Intent(applicationContext, SplashActivity::class.java))
    }

    fun exitGroup() {
        userSessionManager.performExitGroupDataCleanup()
        // Limpieza de UI local (Fragments)
        // Nota: Idealmente el Fragmento debería observar y cerrarse solo, pero forzamos por ahora
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, GruposFragment())
            .commit()

        materialToolbar?.setTitle(R.string.menu_groups)
        mainUiViewModel.setScreen(CurrentScreen.GROUPS)
    }

    // ==========================================
    // 5. LOCATION (Mantenido en Activity)
    // ==========================================

    private fun setupLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { UserRepository.updateLocationUI(it) }
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
                    try { e.startResolutionForResult(this, 0x1) } catch (_: IntentSender.SendIntentException) {}
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!, locationCallback!!, mainLooper)
    }

    private fun stopLocationUpdates() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
        }
    }

    // ==========================================
    // 6. OVERRIDES & HELPERS
    // ==========================================

    private fun handleIncomingIntentFlag() {
        intent.extras?.getInt("flagIntent", -1)?.let { flag ->
            if (flag == 0) editProfileNavigation()
        }
    }

    // --- Legacy / UI Utils ---

    fun configureUsersToolbar(filterActive: Boolean, onRefresh: () -> Unit, onFilterClick: () -> Unit) {
        refreshButton?.setOnClickListener { onRefresh() }
        filterButton?.setOnClickListener { onFilterClick() }
        val colorRes = if (filterActive) R.color.accent else R.color.blanco
        filterButton?.setColorFilter(getColorCompat(colorRes), PorterDuff.Mode.SRC_IN)
    }

    private fun getColorCompat(resId: Int) = ContextCompat.getColor(this, resId)

    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { onBackPressedLegacy() }
        })
    }

    private fun onBackPressedLegacy() {
        if (mainUiViewModel.currentScreen.value == CurrentScreen.EDIT_PROFILE) {
            // Lógica simple de back en edit profile
            val btSave = findViewById<ImageButton?>(R.id.bt_save)
            if (btSave?.isEnabled == true) {
                showSnack("Guarde los cambios antes de salir") // Simplificado
            } else {
                chatNavigation()
            }
            return
        }

        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout?.closeDrawer(GravityCompat.START)
            return
        }

        if (mainUiViewModel.currentScreen.value == CurrentScreen.CHAT || mainUiViewModel.currentScreen.value == CurrentScreen.USERS) {
            if (searchView?.isIconified == false) searchView?.onActionViewCollapsed()
            else finish()
        } else {
            chatNavigation()
        }
    }

    // --- Menu Options (Unlock/Hide - Lógica visual se queda aquí por ahora) ---
    // (Puedes mantener tus funciones unlockUser y unhideChats aquí abajo como estaban,
    // usando repo en lugar de variables estáticas)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            R.id.action_favorites -> {
                bottomNavigationView?.selectedItemId = R.id.navBottomFavorites
                true
            }
            R.id.action_exit -> {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
                    .setTitle(DIALOG_EXIT)
                    .setMessage("¿Desea abandonar ${userPreferencesRepository.groupName}?")
                    .setPositiveButton(DIALOG_EXIT) { _, _ -> exitGroup() }
                    .setNegativeButton(DIALOG_CANCEL, null)
                    .show()
                true
            }
            // Agrega aquí unlockUser() y unhideChats() llamando a tus funciones legacy
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSnack(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        UserRepository.setUserOnline(applicationContext, user.uid)
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        UserRepository.setUserOffline(applicationContext, user.uid)
        stopLocationUpdates()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(findNavController(R.id.nav_host_fragment), appBarConfiguration) || super.onSupportNavigateUp()
    }
}