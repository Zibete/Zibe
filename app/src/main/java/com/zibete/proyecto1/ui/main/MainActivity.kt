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
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SettingsActivity
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.EditProfileFragment
import com.zibete.proyecto1.ui.GruposFragment
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.FirebaseRefs.user
import com.zibete.proyecto1.utils.UserRepository
import com.zibete.proyecto1.utils.ZibeApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint // <--- ESTO HACE QUE LA ACTIVITY PUEDA RECIBIR INYECCIONES
class MainActivity : AppCompatActivity() {

    // ViewModel y Repository
    private val viewModel: MainUiViewModel by viewModels()

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var userSessionManager: UserSessionManager
    @Inject
    lateinit var firebaseRefsContainer: FirebaseRefsContainer

     // ViewBinding
    private lateinit var binding: ActivityMainBinding
    // UI Components
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var navController: NavController? = null
    private var drawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var toolbar: MaterialToolbar? = null
    private var layoutSettings: View? = null
    private var filter: ImageView? = null
    private var refresh: ImageView? = null
    private var mBottomNavigation: BottomNavigationView? = null
    private var searchView: SearchView? = null

    // Badges
    private var badgeDrawableChat: BadgeDrawable? = null
    private var badgeDrawableGroup: BadgeDrawable? = null

    // Ubicación
    private var fusedLocationClient: FusedLocationProviderClient? = null
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
        handleIncomingIntentFlag()
        setupOnBackPressedDispatcher()
    }

    // ==========================================
    // 1. SETUP UI & NAVIGATION
    // ==========================================

    private fun setupUI() {
        // Toolbar & Layouts
        val appBar = binding.appBarMain
        toolbar = appBar.toolbar
        layoutSettings = appBar.layoutSettings
        filter = appBar.filter
        refresh = appBar.refresh

        // Transiciones suaves
        appBar.toolbar.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        setSupportActionBar(toolbar)

        // Drawer
        drawer = binding.drawerLayout
        navigationView = binding.navView

        // Header Info
        val headerView = navigationView?.getHeaderView(0)
        setupHeader(headerView)

        // Bottom Nav
        mBottomNavigation = appBar.contentMain.navView3
        setupBadges()

        ZibeApp.ScreenUtils.init(this)
    }

    private fun setupHeader(headerView: View?) {
        if (headerView == null) return

        val linearImage = headerView.findViewById<MaterialCardView>(R.id.linear_image_user)
        val imgUser = headerView.findViewById<ImageView>(R.id.image_user)
        val tvUser = headerView.findViewById<TextView>(R.id.tv_usuario)
        val tvMail = headerView.findViewById<TextView>(R.id.tv_mail)
        val editProfileBtn = headerView.findViewById<LinearLayout>(R.id.editPerfil)

        linearImage?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ZibeApp.ScreenUtils.heightPx / 2
        )

        tvUser?.text = user.displayName
        tvMail?.text = user.email
        Glide.with(this).load(user.photoUrl).into(imgUser!!)

        editProfileBtn?.setOnClickListener { editProfile(null) }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = navHostFragment?.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_chat, R.id.nav_usuarios, R.id.nav_grupos, R.id.nav_favoritos),
            drawer
        )

        navController?.let { nav ->
            NavigationUI.setupActionBarWithNavController(this, nav, appBarConfiguration)
            navigationView?.let { nv -> NavigationUI.setupWithNavController(nv, nav) }
        }

        setupBottomNavListeners()

        // Estado inicial
        navChatList()
    }

    private fun setupBadges() {
        // Chat Badge
        badgeDrawableChat = mBottomNavigation?.getOrCreateBadge(R.id.navBottomChat)?.apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.zibe_night_start)
            isVisible = false
        }
        // Group Badge
        badgeDrawableGroup = mBottomNavigation?.getOrCreateBadge(R.id.navBottomGrupos)?.apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.zibe_night_start)
            isVisible = false
        }
    }

    // ==========================================
    // 2. VIEWMODEL OBSERVERS (El Cerebro)
    // ==========================================

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar Visibilidad
                launch { viewModel.toolbarVisible.collect { toolbar?.isVisible = it } }
                launch { viewModel.layoutSettingsVisible.collect { layoutSettings?.isVisible = it } }
                launch { viewModel.bottomNavVisible.collect { mBottomNavigation?.isVisible = it } }

                // Observar Badges
                launch {
                    viewModel.chatBadgeCount.collect { count ->
                        badgeDrawableChat?.isVisible = count > 0
                        badgeDrawableChat?.number = count
                    }
                }
                launch {
                    viewModel.groupBadgeCount.collect { count ->
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
        mBottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navBottomUsers -> {
                    if (viewModel.currentScreen.value != CurrentScreen.USERS) {
                        viewModel.setScreen(CurrentScreen.USERS)
                        viewModel.showLayoutSettings(true)
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_usuarios)
                        toolbar?.setTitle(R.string.menu_usuarios)
                    }
                    true
                }
                R.id.navBottomChat -> {
                    navChatList()
                    true
                }
                R.id.navBottomFavorites -> {
                    if (viewModel.currentScreen.value != CurrentScreen.FAVORITES) {
                        viewModel.setScreen(CurrentScreen.FAVORITES)
                        viewModel.showLayoutSettings(false)
                        toolbar?.setTitle(R.string.favoritos)
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_favoritos)
                    }
                    true
                }
                R.id.navBottomGrupos -> {
                    handleGroupsNavigation()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleGroupsNavigation() {
        if (!userPreferencesRepository.inGroup) {
            if (viewModel.currentScreen.value != CurrentScreen.GROUPS) {
                viewModel.setScreen(CurrentScreen.GROUPS)
                viewModel.showLayoutSettings(false)
                invalidateOptionsMenu()
                navController?.navigate(R.id.nav_grupos)
                toolbar?.setTitle(R.string.menu_grupos)
            }
        } else {
            viewModel.setScreen(CurrentScreen.GROUPS)
            viewModel.showToolbar(true)
            viewModel.showLayoutSettings(false)
            invalidateOptionsMenu()

            val newFragment = PageAdapterGroup().apply {
                arguments = Bundle().apply {
                    putString("group_name", repo.groupName)
                    putString("getUid", repo.userName)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, newFragment)
                .commit()

            toolbar?.title = userPreferencesRepository.groupName
        }
    }

    // Navigation Helpers

    fun navChatList() {
        if (viewModel.currentScreen.value != CurrentScreen.CHAT) {
            viewModel.setScreen(CurrentScreen.CHAT)

            viewModel.showToolbar(true)
            viewModel.showLayoutSettings(false)
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_chat)
            toolbar?.setTitle(R.string.menu_chat)

            mBottomNavigation?.visibility = View.VISIBLE

            // Fix loop infinito
            if (mBottomNavigation?.selectedItemId != R.id.navBottomChat) {
                mBottomNavigation?.selectedItemId = R.id.navBottomChat
            }

            drawer?.closeDrawer(GravityCompat.START)
        }
    }

    fun editProfile(item: MenuItem?) {
        if (viewModel.currentScreen.value != CurrentScreen.EDIT_PROFILE) {
            viewModel.showBottomNav(false)
            viewModel.showLayoutSettings(false)
            toolbar?.setTitle(R.string.menu_edit)
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_editPerfil)
            viewModel.setScreen(CurrentScreen.EDIT_PROFILE)
        }
        drawer?.closeDrawer(GravityCompat.START)
    }

    // ==========================================
    // 4. LOGIC (Logout, Exit, Etc)
    // ==========================================

    fun logout(item: MenuItem?) {
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
        viewModel.logout() // Delega lógica a VM
        EditProfileFragment.Companion.deleteProfilePreferences(this)
        stopLocationUpdates()

        // Navegación final
        finish()
        startActivity(Intent(applicationContext, SplashActivity::class.java))
    }

    fun exitGroup() {
        // La lógica visual inmediata
        val groupName = userPreferencesRepository.groupName // Guardamos antes de limpiar
        viewModel.exitGroup() // VM limpia Firebase y Repo

        // Limpieza de UI local (Fragments)
        // Nota: Idealmente el Fragmento debería observar y cerrarse solo, pero forzamos por ahora
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, GruposFragment())
            .commit()

        toolbar?.setTitle(R.string.menu_grupos)
        viewModel.setScreen(CurrentScreen.GROUPS)
    }

    // ==========================================
    // 5. LOCATION (Mantenido en Activity)
    // ==========================================

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        fusedLocationClient?.requestLocationUpdates(locationRequest!!, locationCallback!!, mainLooper)
    }

    private fun stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
        }
    }

    // ==========================================
    // 6. OVERRIDES & HELPERS
    // ==========================================

    private fun handleIncomingIntentFlag() {
        intent.extras?.getInt("flagIntent", -1)?.let { flag ->
            if (flag == 0) editProfile(null)
        }
    }

    // --- Legacy / UI Utils ---

    fun configureUsersToolbar(filterActive: Boolean, onRefresh: () -> Unit, onFilterClick: () -> Unit) {
        refresh?.setOnClickListener { onRefresh() }
        filter?.setOnClickListener { onFilterClick() }
        val colorRes = if (filterActive) R.color.accent else R.color.blanco
        filter?.setColorFilter(getColorCompat(colorRes), PorterDuff.Mode.SRC_IN)
    }

    private fun getColorCompat(resId: Int) = ContextCompat.getColor(this, resId)

    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { onBackPressedLegacy() }
        })
    }

    private fun onBackPressedLegacy() {
        if (viewModel.currentScreen.value == CurrentScreen.EDIT_PROFILE) {
            // Lógica simple de back en edit profile
            val btSave = findViewById<ImageButton?>(R.id.bt_save)
            if (btSave?.isEnabled == true) {
                showSnack("Guarde los cambios antes de salir") // Simplificado
            } else {
                navChatList()
            }
            return
        }

        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
            return
        }

        if (viewModel.currentScreen.value == CurrentScreen.CHAT || viewModel.currentScreen.value == CurrentScreen.USERS) {
            if (searchView?.isIconified == false) searchView?.onActionViewCollapsed()
            else finish()
        } else {
            navChatList()
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
                mBottomNavigation?.selectedItemId = R.id.navBottomFavorites
                true
            }
            R.id.action_exit -> {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
                    .setTitle("Salir")
                    .setMessage("¿Desea abandonar ${userPreferencesRepository.groupName}?")
                    .setPositiveButton("Salir") { _, _ -> exitGroup() }
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