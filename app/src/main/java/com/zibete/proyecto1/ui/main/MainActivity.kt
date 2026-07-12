package com.zibete.proyecto1.ui.main

import android.animation.LayoutTransition
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.IntentCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
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
import com.zibete.proyecto1.core.chat.ChatIdGenerator.getOtherUid
import com.zibete.proyecto1.core.constants.Constants.EXTRA_DELETE_ACCOUNT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_PENDING_DM_TYPE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SESSION_CONFLICT
import com.zibete.proyecto1.core.constants.Constants.EXTRA_SNACK_TYPE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_UI_TEXT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.constants.ERROR_NAV_HOST_FRAGMENT
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.navigation.AppNavigator
import com.zibete.proyecto1.core.navigation.NavAppEvent
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.core.utils.ZibeApp
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.base.BaseEdgeToEdgeActivity
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.editprofile.EditProfileExitHandler
import com.zibete.proyecto1.ui.extensions.getColorCompat
import com.zibete.proyecto1.ui.main.chrome.CurrentScreen
import com.zibete.proyecto1.ui.main.chrome.MainDestinationUiMapper
import com.zibete.proyecto1.ui.main.search.MainSearchCoordinator
import com.zibete.proyecto1.ui.search.SearchHandler
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import com.zibete.proyecto1.ui.users.UsersToolbarHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseEdgeToEdgeActivity(), EditProfileExitHandler {

    @Inject
    lateinit var snackBarManager: SnackBarManager

    @Inject
    lateinit var appNavigator: AppNavigator
    @Inject
    lateinit var authSessionProvider: AuthSessionProvider
    val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var materialToolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentScreen: CurrentScreen = CurrentScreen.OTHER
    private var usersFragmentSettings: View? = null
    private var filterButton: ImageView? = null
    private var refreshButton: ImageView? = null
    private val destinationUiMapper = MainDestinationUiMapper()
    private val searchCoordinator = MainSearchCoordinator { activeSearchHandler() }
    private var badgeDrawableChat: BadgeDrawable? = null
    private var badgeDrawableGroup: BadgeDrawable? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var locationUpdatesActive = false

    override val toolbarMenuRes: Int = R.menu.menu_main

    override fun activityRootView(): View = binding.root
    override fun appBarContainerView(): View = binding.appBarMain.materialToolbar
    override fun bottomNavView(): View = binding.appBarMain.contentMain.bottomNav
    override fun contentViewForInsets(): View = binding.appBarMain.contentMain.navHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ZibeApp.ScreenUtils.init(this)

        setupUI()

        setupNavigation(isFreshStart = savedInstanceState == null)

        setupObservers() // <--- Acá ViewModel

        setupLocation()

        mainViewModel.checkFirstLoginDone()

        setupOnBackPressedDispatcher()

        // Capturar extras del Splash para mostrar el snack pendiente
        handleLaunchIntent()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun handleLaunchIntent(sourceIntent: Intent = intent) {
        handleIntentExtras(sourceIntent)
        handlePendingDmOpen(sourceIntent)
    }

    private fun handleIntentExtras(sourceIntent: Intent) {
        val uiText = IntentCompat.getParcelableExtra(sourceIntent, EXTRA_UI_TEXT, UiText::class.java)
        val snackType =
            IntentCompat.getParcelableExtra(
                sourceIntent,
                EXTRA_SNACK_TYPE,
                ZibeSnackType::class.java
            )

        if (uiText != null) {
            mainViewModel.showSnack(
                delay = true,
                uiText = uiText,
                snackType = snackType ?: ZibeSnackType.SUCCESS
            )
        }
        sourceIntent.removeExtra(EXTRA_UI_TEXT)
        sourceIntent.removeExtra(EXTRA_SNACK_TYPE)
    }

    private fun handlePendingDmOpen(sourceIntent: Intent) {
        val pendingType = sourceIntent.getStringExtra(EXTRA_PENDING_DM_TYPE)
        val chatId = sourceIntent.getStringExtra(EXTRA_PENDING_DM_CHAT_ID)
        if (pendingType.isNullOrBlank() && chatId.isNullOrBlank()) return

        sourceIntent.removeExtra(EXTRA_PENDING_DM_TYPE)
        sourceIntent.removeExtra(EXTRA_PENDING_DM_CHAT_ID)

        if (pendingType != NODE_DM || chatId.isNullOrBlank()) return

        val myUid = authSessionProvider.currentUser?.uid ?: return
        val otherUid = getOtherUid(chatId, myUid)
            ?.takeIf { it.isNotBlank() }
            ?: return

        startActivity(
            Intent(this, ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, otherUid)
                putExtra(EXTRA_CHAT_NODE, NODE_DM)
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    // ==========================================
    // 1. SETUP UI & NAVIGATION
    // ==========================================

    private fun setupUI() {
        // Toolbar & Layouts
        val appBarMain = binding.appBarMain
        materialToolbar = appBarMain.materialToolbar
        usersFragmentSettings = appBarMain.usersFragmentSettings
        filterButton = appBarMain.filterButton
        refreshButton = appBarMain.refreshButton

        // Botón de refrescar
        refreshButton?.setOnClickListener {
            activeUsersToolbarHandler()?.onRefreshUsers()
        }

        // Botón de filtro → dispara un evento, el Fragment abre el diálogo
        filterButton?.setOnClickListener {
            activeUsersToolbarHandler()?.onFilterUsers()
        }

        // Transiciones suaves
        appBarMain.materialToolbar.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        setupToolbar(
            toolbar = materialToolbar,
            showBack = false,
            handleBackInBase = false
        )

        materialToolbar.setNavigationOnClickListener {
            if (dismissImeIfVisible()) return@setNavigationOnClickListener
            if (mainViewModel.destinationUiState.value.showBack) {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        setupAccountUi(appBarMain.accountAvatarHost)

        // Bottom Nav
        bottomNavigationView = appBarMain.contentMain.bottomNav

        setupBadges()

    }

    private fun setupAccountUi(host: ComposeView) {
        host.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        host.setContent {
            ZibeTheme {
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AccountAvatarButton(
                        photoUrl = uiState.accountPhotoUrl,
                        onClick = mainViewModel::openAccountSheet
                    )
                    AccountSheet(
                        isOpen = uiState.isAccountSheetOpen,
                        displayName = uiState.accountName,
                        email = uiState.accountEmail,
                        photoUrl = uiState.accountPhotoUrl,
                        onDismiss = mainViewModel::closeAccountSheet,
                        onEditProfile = mainViewModel::onEditProfileSelected,
                        onSettings = mainViewModel::onAccountSettingsSelected,
                        onLogout = mainViewModel::onAccountLogoutSelected
                    )
                }
            }
        }
    }

    private fun setupBadges() {
        // Chat Badge
        badgeDrawableChat = bottomNavigationView.getOrCreateBadge(R.id.navBottomChat).apply {
            backgroundColor = getColorCompat(DsR.color.accent)
            badgeTextColor = getColorCompat(DsR.color.white)
            isVisible = false
        }
        // Group Badge
        badgeDrawableGroup = bottomNavigationView.getOrCreateBadge(R.id.navBottomGroups).apply {
            backgroundColor = getColorCompat(DsR.color.accent)
            badgeTextColor = getColorCompat(DsR.color.white)
            isVisible = false
        }
    }

    private fun setupNavigation(isFreshStart: Boolean) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
            ?: error(ERROR_NAV_HOST_FRAGMENT)

        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_users,
                R.id.nav_chat_list,
                R.id.nav_group_select,
                R.id.nav_favorites
            )
        )

        navController.let { nav ->
            NavigationUI.setupActionBarWithNavController(this, nav, appBarConfiguration)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            mainViewModel.onBottomItemSelected(item.itemId)
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            applyDestinationUi(destination.id)
        }

        if (isFreshStart) {
            goToChatTab()
        } else {
            syncBottomNavSelection()
        }
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
                    binding.appBarMain.globalLoadingOverlay.isVisible = state.isGlobalLoading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.destinationUiState.collect { state ->
                    // Visibilidad
                    materialToolbar.isVisible = state.showToolbar
                    usersFragmentSettings?.isVisible = state.showUsersFragmentSettings
                    binding.appBarMain.accountAvatarHost.isVisible = state.showAccountAvatar
                    bottomNavigationView.isVisible = state.showBottomNav
                    // title
                    materialToolbar.title = when {
                        state.useGroupNameTitle -> mainViewModel.groupName.value
                        state.title != null -> state.title.asString(this@MainActivity)
                        else -> state.currentScreen.titleRes.asString(this@MainActivity)
                    }

                    currentScreen = state.currentScreen

                    invalidateOptionsMenu()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.hasActiveFilter.collect { hasActiveFilter ->
                    val colorRes = if (hasActiveFilter) DsR.color.accent else DsR.color.blanco
                    filterButton?.setColorFilter(
                        this@MainActivity.getColorCompat(colorRes),
                        PorterDuff.Mode.SRC_IN
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Navegación
                launch {
                    mainViewModel.uiEvents.collect { event ->
                        when (event) {

                            is MainUiEvent.ToUsers -> {
                                navigateRoot(R.id.nav_users)
                            }

                            is MainUiEvent.ToChat -> {
                                navigateRoot(R.id.nav_chat_list)
                            }

                            is MainUiEvent.BackToChat -> {
                                goToChatTab()
                            }

                            is MainUiEvent.ToGroupHost -> {
                                navigateRoot(R.id.nav_group_host)
                            }

                            is MainUiEvent.ToGroupsSelect -> {
                                navigateRoot(R.id.nav_group_select)
                            }

                            is MainUiEvent.ToFavorites -> {
                                navigateRoot(R.id.nav_favorites)
                            }

                            is MainUiEvent.ToEditProfile -> {
                                navController.navigate(R.id.editProfileFragment)
                            }

                            is MainUiEvent.ToGroupsAfterExit -> {
                                navigateRoot(R.id.nav_group_select)
                            }

                            is MainUiEvent.BackExitAppOrCloseSearch -> {
                                if (searchCoordinator.isSearchOpen()) {
                                    searchCoordinator.collapseAndClear()
                                } else if (
                                    currentScreen == CurrentScreen.GROUPS &&
                                    navController.currentDestination?.id == R.id.nav_group_host
                                ) {
                                    val popped =
                                        navController.popBackStack(R.id.nav_group_select, false)
                                    if (!popped) {
                                        navController.navigate(
                                            R.id.nav_group_select,
                                            navOptions { launchSingleTop = true }
                                        )
                                    }
                                    bottomNavigationView.menu.findItem(R.id.navBottomGroups)?.isChecked =
                                        true
                                } else {
                                    finish()
                                }
                            }

                            is MainUiEvent.NavigateToSettings -> {
                                ensureNavHostController().navigate(R.id.settingsFragment)
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
                                    title = getString(R.string.logout),
                                    message = getString(R.string.dialog_logout_message),
                                    positiveText = getString(R.string.logout),
                                    onConfirm = {
                                        mainViewModel.onLogoutRequested()
                                    }
                                )
                            }

                            is MainUiEvent.ShowSnack -> {
                                snackBarManager.show(
                                    uiText = event.uiText,
                                    type = event.snackType
                                )
                            }

                            is MainUiEvent.ShowUnblockUsersDialog -> {
                                val users = event.users
                                if (users.isEmpty()) {
                                    UserMessageUtils.dialog(
                                        context = this@MainActivity,
                                        message = getString(R.string.msg_no_blocked_users)
                                    )
                                    return@collect
                                }

                                var selectedIndex = 0
                                val choices = users.map { it.name }.toTypedArray()

                                UserMessageUtils.confirm(
                                    context = this@MainActivity,
                                    title = resources.getQuantityString(
                                        R.plurals.unblock_users_title,
                                        users.size, users.size
                                    ),
                                    choices = choices,
                                    selectedIndex = selectedIndex,
                                    onChoiceSelected = { index -> selectedIndex = index },
                                    onConfirm = {
                                        val user = users.getOrNull(selectedIndex) ?: return@confirm
                                        mainViewModel.onConfirmUnblockAction(user.id, user.name)
                                    }
                                )
                            }

                            is MainUiEvent.ShowUnhideChatsDialog -> {
                                val chats = event.chats
                                if (chats.isEmpty()) {
                                    UserMessageUtils.dialog(
                                        context = this@MainActivity,
                                        message = getString(R.string.msg_no_hidden_chats)
                                    )
                                    return@collect
                                }

                                var selectedIndex = 0
                                val choices = chats.map { it.name }.toTypedArray()

                                UserMessageUtils.confirm(
                                    context = this@MainActivity,
                                    title = getString(R.string.menu_unhide_chats),
                                    choices = choices,
                                    selectedIndex = selectedIndex,
                                    onChoiceSelected = { index -> selectedIndex = index },
                                    onConfirm = {
                                        val chat = chats.getOrNull(selectedIndex) ?: return@confirm
                                        mainViewModel.onUnhideChatConfirmed(chat.id, chat.name)
                                    }
                                )
                            }

                            is MainUiEvent.HandleChatSessionEvent -> {
                                ChatSessionUiHandler.handle(
                                    context = this@MainActivity,
                                    event = event.event,
                                    scope = lifecycleScope,
                                    snackBarManager = snackBarManager
                                )
                            }

                        }
                    }
                }
                launch {
                    appNavigator.events.collect { event ->
                        when (event) {
                            is NavAppEvent.FinishFlowNavigateToSplash -> {
                                navigateToSplash(
                                    sessionConflict = event.sessionConflict,
                                    deleteAccount = event.deleteAccount,
                                    snackMessage = event.snackMessage,
                                    snackType = event.snackType
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

    private fun navigateRoot(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        navController.navigate(
            destinationId,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
        )
    }

    override fun onExitEditProfile() {
        goToChatTab()
    }

    private fun activeNavFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.primaryNavigationFragment
    }

    private fun activeSearchHandler(): SearchHandler? =
        activeNavFragment() as? SearchHandler

    private fun activeUsersToolbarHandler(): UsersToolbarHandler? =
        activeNavFragment() as? UsersToolbarHandler

    private fun ensureNavHostController(): NavController {
        val current = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (current is NavHostFragment) {
            navController = current.navController
            return navController
        }

        val navHost = NavHostFragment.create(R.navigation.mobile_navigation)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, navHost)
            .setPrimaryNavigationFragment(navHost)
            .commitNow()

        navController = navHost.navController
        return navController
    }

    private fun applyDestinationUi(destinationId: Int) {
        val state = destinationUiMapper.map(destinationId)

        mainViewModel.setDestinationUiState(state)

        state.selectedBottomNavItemId?.let { itemId ->
            bottomNavigationView.menu.findItem(itemId)?.isChecked = true
        }

        searchCoordinator.updateAvailability(state.menuConfig.showSearch)
    }

    private fun syncBottomNavSelection() {
        navController.currentDestination?.id?.let { destinationId ->
            applyDestinationUi(destinationId)
        }
    }

    private fun setupLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    mainViewModel.onLocationChanged(it.latitude, it.longitude)
                }
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
        if (locationUpdatesActive) return
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            mainLooper
        )
        locationUpdatesActive = true
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
        locationUpdatesActive = false
    }

    private fun navigateToSplash(
        sessionConflict: Boolean = false,
        deleteAccount: Boolean = false,
        snackMessage: UiText? = null,
        snackType: ZibeSnackType? = null
    ) {
        val intent = Intent(this@MainActivity, SplashActivity::class.java)
        if (sessionConflict) intent.putExtra(EXTRA_SESSION_CONFLICT, true)
        if (deleteAccount) intent.putExtra(EXTRA_DELETE_ACCOUNT, true)
        if (snackMessage != null) intent.putExtra(EXTRA_UI_TEXT, snackMessage)
        if (snackType != null) intent.putExtra(EXTRA_SNACK_TYPE, snackType)
        stopLocationUpdates()
        finish()
        startActivity(intent)
    }

    override val toolbarMenuVisiblePredicate: (Menu) -> Unit = menu@{ menu ->
        for (i in 0 until menu.size) menu[i].isVisible = false
        val destinationUiState = mainViewModel.destinationUiState.value
        val menuConfig = destinationUiState.menuConfig

        menu.findItem(R.id.action_settings)?.isVisible = menuConfig.showSettings
        menu.findItem(R.id.action_unblock_users)?.isVisible = menuConfig.showUnblockUsers
        menu.findItem(R.id.action_unhide_chats)?.isVisible = menuConfig.showUnhideChats
        menu.findItem(R.id.action_favorites)?.isVisible = menuConfig.showFavorites
        menu.findItem(R.id.action_search)?.isVisible = menuConfig.showSearch
        menu.findItem(R.id.action_exit_group)?.isVisible = menuConfig.showExitGroup
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchView = (menu.findItem(R.id.action_search)?.actionView as? SearchView)
        searchCoordinator.bind(searchView)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 1) delegás al VM
        mainViewModel.onToolbarItemSelected(item.itemId)

        // 2) dejás que el framework maneje lo demás (home/up incluido)
        return super.onOptionsItemSelected(item)
    }

    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (dismissImeIfVisible()) return
                    if (navController.currentDestination?.id == R.id.settingsFragment) {
                        if (navController.popBackStack()) return
                    }

                    if (navController.currentDestination?.id == R.id.editProfileFragment) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
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
        mainViewModel.refreshAccountProfile()
        invalidateOptionsMenu()
        val hasPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            stopLocationUpdates()
            return
        }

        if (!locationUpdatesActive) {
            ensureLocationSettingsAndStart()
        }
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
