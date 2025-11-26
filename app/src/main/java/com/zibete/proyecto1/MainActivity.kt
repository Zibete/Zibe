package com.zibete.proyecto1

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.zibete.proyecto1.databinding.ActivityMainBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.EditProfileFragment
import com.zibete.proyecto1.ui.FavoritesFragment
import com.zibete.proyecto1.ui.GruposFragment
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.constants.Constants.CHATWITH
import com.zibete.proyecto1.ui.constants.Constants.CHATWITHUNKNOWN
import com.zibete.proyecto1.ui.constants.Constants.EMPTY
import com.zibete.proyecto1.ui.constants.DIALOG_ACCEPT
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_KEEP_HERE
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_LOGOUT
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_MESSAGE
import com.zibete.proyecto1.ui.constants.SESSION_CONFLICT_TITLE
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import com.zibete.proyecto1.utils.UserRepository.setUserOffline
import com.zibete.proyecto1.utils.UserRepository.setUserOnline
import com.zibete.proyecto1.utils.UserRepository.updateLocationUI
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.ui.main.MainUiViewModel
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val mainUiViewModel: MainUiViewModel by viewModels()

    // Representación lógica de la pantalla actual (no depender de UI)
    private enum class CurrentScreen {
        CHAT, USERS, GROUPS, EDIT_PROFILE, FAVORITES, OTHER
    }

    private var currentScreen: CurrentScreen = CurrentScreen.OTHER

    // ========= ViewBinding =========
    private lateinit var binding: ActivityMainBinding

    // ========= AppBar / Nav =========
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var navController: NavController? = null
    private var navHostFragment: NavHostFragment? = null

    // ========= Drawer =========
    private var drawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var headerView: View? = null

    // ========= Toolbar / Layout / UI =========
    private var toolbar: MaterialToolbar? = null
    private var layoutSettings: View? = null
    private var filter: ImageView? = null
    private var refresh: ImageView? = null
    private var mBottomNavigation: BottomNavigationView? = null
    private var viewGroup: ViewGroup? = null
    private var searchView: SearchView? = null

    // ========= Badges =========
    private var badgeDrawableChat: BadgeDrawable? = null

    // ========= Ubicación =========
    private var lastLocation: Location? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    // ========= Sesión por dispositivo =========
    private var myInstallId: String? = null
    private var myFcmToken: String? = null
    private var tokenListenerInitialized = false

    // ========= Notificación / flags =========
    private var flagIntent: Int = 0
    private var sessionConflictHandled = false

    private val user get() = currentUser!!

    val repo = UserPreferencesRepository.getInstance(this)

    // ========= Ciclo de vida =========

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appBarBinding = binding.appBarMain

        // Transiciones suaves en toolbar
        viewGroup = appBarBinding.toolbar
        viewGroup?.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        // Toolbar / layout / botones top
        toolbar = appBarBinding.toolbar
        layoutSettings = appBarBinding.layoutSettings

        lifecycleScope.launch {
            mainUiViewModel.toolbarVisible.collect { visible ->
                toolbar?.isVisible = visible
            }
        }

        lifecycleScope.launch {
            mainUiViewModel.layoutSettingsVisible.collect { visible ->
                layoutSettings?.isVisible = visible
            }
        }

        filter = appBarBinding.filter
        refresh = appBarBinding.refresh
        setSupportActionBar(toolbar)

        // Drawer
        drawer = binding.drawerLayout
        navigationView = binding.navView

        // NavController
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = navHostFragment?.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_chat,
                R.id.nav_usuarios,
                R.id.nav_grupos,
                R.id.nav_favoritos
            ),
            drawer
        )

        navController?.let { nav ->
            NavigationUI.setupActionBarWithNavController(this, nav, appBarConfiguration)
            navigationView?.let { nv -> NavigationUI.setupWithNavController(nv, nav) }
        }

        // Header navigation
        headerView = navigationView?.getHeaderView(0)
        val linearImageUser = headerView?.findViewById<MaterialCardView>(R.id.linear_image_user)
        val imageUser = headerView?.findViewById<ImageView>(R.id.image_user)
        val tvUsuario = headerView?.findViewById<TextView>(R.id.tv_usuario)
        val tvMail = headerView?.findViewById<TextView>(R.id.tv_mail)
        val editPerfil = headerView?.findViewById<LinearLayout>(R.id.editPerfil)

        // Screen utils
        ScreenUtils.init(this)

        linearImageUser?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ScreenUtils.heightPx / 2
        )

        tvUsuario?.text = user.displayName
        tvMail?.text = user.email
        Glide.with(applicationContext)
            .load(user.photoUrl)
            .into(imageUser!!)

        editPerfil?.setOnClickListener { editProfile(null) }

        // BottomNavigation (está en content_main dentro de app_bar_main)
        mBottomNavigation = appBarBinding.contentMain.navView3
        // Pantalla inicial: chat
        mBottomNavigation?.selectedItemId = R.id.navBottomChat
        navChatList() // setea currentScreen = CHAT

        // Badges Chat
        badgeDrawableChat = mBottomNavigation?.getOrCreateBadge(R.id.navBottomChat)?.apply {
            backgroundColor = getColorCompat(R.color.accent)
            badgeTextColor = getColorCompat(R.color.zibe_night_start)
            isVisible = false
        }

        // Listener mensajes no leídos (chat normal)
        val newQuery = FirebaseRefs.refDatos
            .child(user.uid)
            .child(CHATWITH)
            .orderByChild("noVisto")
            .startAt(1.0)

        newQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var countMsgUnread = 0
                    for (child in snapshot.children) {
                        val unRead = child.child("noVisto").getValue(Int::class.java)
                        if (unRead != null) countMsgUnread += unRead
                    }
                    badgeDrawableChat?.isVisible = true
                    badgeDrawableChat?.number = countMsgUnread
                } else {
                    badgeDrawableChat?.isVisible = false
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Badges Grupos
        badgeDrawableGroup =
            mBottomNavigation?.getOrCreateBadge(R.id.navBottomGrupos)?.apply {
                backgroundColor = getColorCompat(R.color.accent)
                badgeTextColor = getColorCompat(R.color.zibe_night_start)
                isVisible = false
            }

        initGroupBadgeListeners()
        setupBottomNav()
        setupLocation()
        setupInstallIdAndFcm()
        handleIncomingIntentFlag()
        setupOnBackPressedDispatcher()
    }

    override fun onStart() {
        super.onStart()
        setUserOnline(applicationContext, user.uid)
        ensureLocationSettingsAndStart()
    }

    override fun onResume() {
        super.onResume()
        setUserOnline(applicationContext, user.uid)
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        setUserOffline(applicationContext, user.uid)
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()

        listenerToken?.let {
            FirebaseRefs.refCuentas.child(user.uid)
                .child("installId")
                .removeEventListener(it)
        }
        setUserOffline(applicationContext, user.uid)
        stopLocationUpdates()
    }

    // ========= Helpers generales =========

    private fun getColorCompat(resId: Int): Int =
        ContextCompat.getColor(this, resId)

    private fun setupOnBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    this@MainActivity.onBackPressedLegacy()
                }
            }
        )
    }

    // ========= Badges grupos =========

    private fun initGroupBadgeListeners() {
        // Listener badge grupos
        listenerGroupBadge = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                // Si estoy dentro del grupo, no actualizo badge
                if (repo.inGroup) return

                val totalMsg = snapshot.childrenCount

                FirebaseRefs.refDatos.child(user.uid)
                    .child("ChatList")
                    .child("msgReadGroup")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot1: DataSnapshot) {
                            if (!dataSnapshot1.exists()) return

                            val query = FirebaseRefs.refDatos.child(user.uid)
                                .child(CHATWITHUNKNOWN)
                                .orderByChild("noVisto")
                                .startAt(1.0)

                            query.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(ds: DataSnapshot) {
                                    val leidos = dataSnapshot1.getValue(Int::class.java) ?: 0
                                    var countMsgUnread = 0

                                    if (ds.exists()) {
                                        for (child in ds.children) {
                                            val unRead =
                                                child.child("noVisto").getValue(Int::class.java)
                                            if (unRead != null) countMsgUnread += unRead
                                        }
                                    }

                                    if (repo.inGroup) {
                                        val total =
                                            (totalMsg - leidos + countMsgUnread).toInt()
                                        badgeDrawableGroup?.number = total
                                        if (total > 0) {
                                            badgeDrawableGroup?.isVisible = true
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        // Listener mensajes no leídos grupos
        listenerMsgUnreadBadge = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Si estoy dentro del grupo, no hace falta badge
                if (repo.inGroup) return
                if (!snapshot.exists()) return

                var countMsgUnread = 0
                for (child in snapshot.children) {
                    val unRead = child.child("noVisto").getValue(Int::class.java)
                    if (unRead != null) countMsgUnread += unRead
                }
                val finalCount = countMsgUnread

                FirebaseRefs.refGroupChat.child(repo.groupName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ds: DataSnapshot) {
                            if (!ds.exists()) return

                            val totalMsg = ds.childrenCount

                            FirebaseRefs.refDatos.child(user.uid)
                                .child("ChatList")
                                .child("msgReadGroup")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(d1: DataSnapshot) {
                                        val leidos = d1.getValue(Int::class.java) ?: 0
                                        if (repo.inGroup) {
                                            val total =
                                                (totalMsg - leidos + finalCount).toInt()
                                            badgeDrawableGroup?.number = total
                                            if (total > 0) {
                                                badgeDrawableGroup?.isVisible = true
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        if (repo.inGroup) {
            FirebaseRefs.refGroupChat.child(repo.groupName)
                .addValueEventListener(listenerGroupBadge as ValueEventListener)

            val query = FirebaseRefs.refDatos.child(user.uid)
                .child(CHATWITHUNKNOWN)
                .orderByChild("noVisto")
                .startAt(1.0)

            query.addValueEventListener(listenerMsgUnreadBadge as ValueEventListener)
        }
    }

    // ========= BottomNavigation =========

    private fun setupBottomNav() {
        mBottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navBottomUsers -> {
                    if (currentScreen != CurrentScreen.USERS) {
                        layoutSettings?.visibility = View.VISIBLE
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_usuarios)
                        toolbar?.setTitle(R.string.menu_usuarios)
                        currentScreen = CurrentScreen.USERS
                    }
                    true
                }

                R.id.navBottomChat -> {
                    navChatList()
                    true
                }

                R.id.navBottomFavorites -> {
                    if (currentScreen != CurrentScreen.FAVORITES) {
                        layoutSettings?.visibility = View.GONE
                        toolbar?.setTitle(R.string.favoritos)
                        invalidateOptionsMenu()
                        navController?.navigate(R.id.nav_favoritos)
                        currentScreen = CurrentScreen.FAVORITES
                    }
                    true
                }

                R.id.navBottomGrupos -> {
                    if (!repo.inGroup) {
                        if (currentScreen != CurrentScreen.GROUPS) {
                            layoutSettings?.visibility = View.GONE
                            invalidateOptionsMenu()
                            navController?.navigate(R.id.nav_grupos)
                            toolbar?.setTitle(R.string.menu_grupos)
                            currentScreen = CurrentScreen.GROUPS
                        }
                    } else {
                        toolbar?.visibility = View.VISIBLE
                        layoutSettings?.visibility = View.GONE
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

                        toolbar?.title = repo.groupName
                        currentScreen = CurrentScreen.GROUPS
                    }
                    true
                }

                else -> false
            }
        }
    }

    // ========= Ubicación =========

    private val isLocationPermissionGranted: Boolean
        get() = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        )
            .setMinUpdateIntervalMillis(UPDATE_FASTEST_INTERVAL)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lastLocation = result.lastLocation
                lastLocation?.let { updateLocationUI(it) }
            }
        }

        ensureLocationSettingsAndStart()
    }

    private fun ensureLocationSettingsAndStart() {
        if (!isLocationPermissionGranted) {
            Log.d("MainActivity", "Sin permiso de ubicación, redirigiendo a Splash")
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)
            .setAlwaysShow(true)
            .build()

        settingsClient
            ?.checkLocationSettings(settingsRequest)
            ?.addOnSuccessListener(this) {
                processLastLocation()
                startLocationUpdates()
            }
            ?.addOnFailureListener(this) { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (ex: SendIntentException) {
                        Log.d("MainActivity", "Error lanzando diálogo de ajustes de ubicación")
                    }
                } else {
                    Log.d("MainActivity", "Ajustes de ubicación no apropiados")
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!isLocationPermissionGranted) return
        fusedLocationClient?.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
        }
    }

    @SuppressLint("MissingPermission")
    private fun processLastLocation() {
        if (!isLocationPermissionGranted) return
        fusedLocationClient
            ?.lastLocation
            ?.addOnSuccessListener { location ->
                location?.let { updateLocationUI(it) }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Log.d("MainActivity", "Usuario aceptó ajustes de ubicación")
                processLastLocation()
                startLocationUpdates()
            } else {
                Log.d("MainActivity", "Usuario no aceptó ajustes de ubicación")
            }
        }
    }

    // ========= Sesión única por dispositivo =========

    private fun setupInstallIdAndFcm() {
        FirebaseInstallations.getInstance().id
            .addOnCompleteListener { task: Task<String?> ->
                if (task.isSuccessful) {
                    myInstallId = task.result
                }
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { tk: Task<String?> ->
                        if (tk.isSuccessful) {
                            myFcmToken = tk.result
                            FirebaseRefs.refCuentas.child(user.uid)
                                .child("fcmToken")
                                .setValue(myFcmToken)
                        }
                        registerInstallIdAndAttachListener()
                    }
            }
    }

    private fun registerInstallIdAndAttachListener() {
        val installIdRef =
            FirebaseRefs.refCuentas.child(user.uid).child("installId")

        installIdRef.get()
            .addOnSuccessListener { snap ->
                val current = if (snap.exists()) snap.getValue(String::class.java) else null
                if (myInstallId != null && (current == null || current != myInstallId)) {
                    installIdRef.setValue(myInstallId)
                }
                attachInstallIdListener(installIdRef)
            }
            .addOnFailureListener {
                attachInstallIdListener(installIdRef)
            }
    }

    private fun attachInstallIdListener(installIdRef: DatabaseReference) {

        listenerToken = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val remoteInstallId = dataSnapshot.getValue(String::class.java)

                if (myInstallId == null) return

                if (!tokenListenerInitialized) {
                    tokenListenerInitialized = true
                    if (remoteInstallId == null || remoteInstallId != myInstallId) {
                        dataSnapshot.ref.setValue(myInstallId)
                    }
                    return
                }

                if (remoteInstallId != null && remoteInstallId != myInstallId) {
                    if (sessionConflictHandled) return
                    sessionConflictHandled = true

                    showSessionConflictDialog(
                        onKeepHere = {
                            dataSnapshot.ref.setValue(myInstallId)
                            sessionConflictHandled = false
                        },
                        onLogout = {
                            listenerToken?.let { installIdRef.removeEventListener(it) }
                            logout(null)
                        }
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        installIdRef.addValueEventListener(listenerToken as ValueEventListener)
    }

    private fun showSessionConflictDialog(
        onKeepHere: () -> Unit,
        onLogout: () -> Unit
    ) {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
            .setTitle(SESSION_CONFLICT_TITLE)
            .setMessage(SESSION_CONFLICT_MESSAGE)
            .setPositiveButton(SESSION_CONFLICT_KEEP_HERE) { _, _ -> onKeepHere() }
            .setNegativeButton(SESSION_CONFLICT_LOGOUT) { _, _ -> onLogout() }
            .setCancelable(false)
            .show()
    }

    // ========= Navegación / UI =========

    private fun handleIncomingIntentFlag() {
        intent.extras?.let {
            flagIntent = it.getInt("flagIntent", -1)
            if (flagIntent == 0) {
                editProfile(null)
            }
        }
    }

    fun navChatList() {
        if (currentScreen != CurrentScreen.CHAT) {
            toolbar?.visibility = View.VISIBLE
            layoutSettings?.visibility = View.GONE
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_chat)
            toolbar?.setTitle(R.string.menu_chat)

            mBottomNavigation?.visibility = View.VISIBLE
            mBottomNavigation?.selectedItemId = R.id.navBottomChat

            drawer?.closeDrawer(GravityCompat.START)

            currentScreen = CurrentScreen.CHAT
        }
    }

    fun editProfile(item: MenuItem?) {
        if (currentScreen != CurrentScreen.EDIT_PROFILE) {
            mBottomNavigation?.visibility = View.GONE
            layoutSettings?.visibility = View.GONE
            toolbar?.setTitle(R.string.menu_edit)
            invalidateOptionsMenu()

            navController?.navigate(R.id.nav_editPerfil)
            currentScreen = CurrentScreen.EDIT_PROFILE
        }
        drawer?.closeDrawer(GravityCompat.START)
    }

    fun index(item: MenuItem?) {
        if (currentScreen == CurrentScreen.EDIT_PROFILE ||
            currentScreen == CurrentScreen.FAVORITES ||
            currentScreen == CurrentScreen.OTHER
        ) {
            onBackPressedLegacy()
        } else {
            drawer?.closeDrawer(GravityCompat.START)
        }
    }

    fun settings(item: MenuItem?) {
        drawer?.closeDrawer(GravityCompat.START)
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                settings(null)
                true
            }

            R.id.action_unlock -> {
                unlockUser()
                true
            }

            R.id.action_unhide_chats -> {
                unhideChats()
                true
            }

            R.id.action_favorites -> {
                mBottomNavigation?.selectedItemId = R.id.navBottomFavorites
                true
            }

            R.id.action_exit -> {
                AlertDialog.Builder(
                    ContextThemeWrapper(this, R.style.AlertDialogApp)
                )
                    .setTitle("Salir")
                    .setMessage("¿Desea abandonar ${repo.groupName}?")
                    .setPositiveButton("Salir") { _, _ -> exitGroup() }
                    .setNegativeButton(DIALOG_CANCEL, null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    // ========= Back legacy =========

    private fun onBackPressedLegacy() {
        val btSave = findViewById<ImageButton?>(R.id.bt_save)
        val edtFecha = findViewById<EditText?>(R.id.edtFecha)

        // Editar perfil
        if (currentScreen == CurrentScreen.EDIT_PROFILE) {

            if (btSave?.isEnabled == true) {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
                    .setTitle("Salir")
                    .setMessage("Se perderán los cambios, ¿Desea continuar?")
                    .setCancelable(false)
                    .setPositiveButton("Si") { _, _ ->
                        FirebaseRefs.refCuentas.child(user.uid)
                            .child("birthDay")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val fecha = snapshot.getValue(String::class.java)
                                    if (fecha != null && fecha == "") {
                                        val snack = Snackbar.make(
                                            findViewById(android.R.id.content),
                                            "Complete su fecha de nacimiento",
                                            Snackbar.LENGTH_INDEFINITE
                                        )
                                        snack.setAction("OK") { snack.dismiss() }
                                        snack.setBackgroundTint(getColorCompat(R.color.colorC))
                                        val tv = snack.view.findViewById<TextView>(
                                            com.google.android.material.R.id.snackbar_text
                                        )
                                        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                                        snack.show()
                                    } else {
                                        navChatList()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                val fecha = edtFecha?.text?.toString()?.trim().orEmpty()
                if (fecha.isEmpty()) {
                    val snack = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Complete su fecha de nacimiento",
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snack.setAction("OK") { snack.dismiss() }
                    snack.setBackgroundTint(getColorCompat(R.color.colorC))
                    val tv = snack.view.findViewById<TextView>(
                        com.google.android.material.R.id.snackbar_text
                    )
                    tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    snack.show()
                } else {
                    navChatList()
                }
            }

            return
        }

        // Drawer abierto
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
            return
        }

        // Pantallas raíz
        if (currentScreen == CurrentScreen.CHAT ||
            currentScreen == CurrentScreen.USERS
        ) {
            if (searchView?.isIconified == false) {
                searchView?.onActionViewCollapsed()
            } else {
                finish()
            }
        } else {
            navChatList()
        }
    }

    // ========= Acciones de chat =========

    fun logout(item: MenuItem?) {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
            .setTitle("Cerrar sesión")
            .setMessage("¿Está seguro de cerrar su sesión?")
            .setCancelable(false)
            .setPositiveButton("Si") { _, _ -> logout() }
            .setNegativeButton("No", null)
            .show()
    }

    fun logout() {
        setUserOffline(applicationContext, user.uid)

        if (repo.inGroup) {
            exitGroup()
        }

        listenerToken?.let {
            FirebaseRefs.refCuentas.child(user.uid)
                .child("installId")
                .removeEventListener(it)
        }

        repo.clearAllData()

        EditProfileFragment.deleteProfilePreferences(this)

        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()

        stopLocationUpdates()
        finish()
        startActivity(Intent(applicationContext, SplashActivity::class.java))
    }

    fun unlockUser() {
        val type = if (repo.inGroup) {
            CHATWITHUNKNOWN
        } else {
            CHATWITH
        }

        FirebaseRefs.refDatos.child(user.uid).child(type)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showSnack("No hay usuarios bloqueados")
                        return
                    }

                    val listName = ArrayList<String>()
                    val listID = ArrayList<String>()

                    for (child in snapshot.children) {
                        val chat = child.getValue(ChatWith::class.java)
                        if (chat != null && chat.state == "bloq") {
                            listName.add(chat.userName)
                            listID.add(chat.userId)
                        }
                    }

                    if (listName.isEmpty()) {
                        showSnack("No hay usuarios bloqueados")
                        return
                    }

                    val listaName = listName.toTypedArray<CharSequence>()
                    val listaID = listID.toTypedArray<CharSequence>()
                    var itemSelected = 0

                    AlertDialog.Builder(ContextThemeWrapper(this@MainActivity, R.style.AlertDialogApp))
                        .setTitle("¿A quién deseas desbloquear?")
                        .setSingleChoiceItems(listaName, itemSelected) { _, index ->
                            itemSelected = index
                        }
                        .setPositiveButton(DIALOG_ACCEPT) { _, _ ->
                            val targetId = listaID[itemSelected].toString()
                            FirebaseRefs.refDatos.child(user.uid).child(type)
                                .child(targetId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(ds: DataSnapshot) {
                                        val photo =
                                            ds.child("wUserPhoto").getValue(String::class.java)
                                        if (photo == EMPTY) {
                                            ds.ref.removeValue()
                                        } else {
                                            ds.ref.child("estado").setValue(type)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            showSnack("Desbloqueaste a ${listaName[itemSelected]}")
                        }
                        .setNegativeButton(DIALOG_CANCEL, null)
                        .setCancelable(false)
                        .show()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun unhideChats() {
        val type = if (repo.inGroup) {
            CHATWITHUNKNOWN
        } else {
            CHATWITH
        }

        FirebaseRefs.refDatos.child(user.uid).child(type)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showSnack("No hay chats ocultos")
                        return
                    }

                    val listName = ArrayList<String>()
                    val listID = ArrayList<String>()

                    for (child in snapshot.children) {
                        val chat = child.getValue(ChatWith::class.java)
                        if (chat != null && chat.state == "delete") {
                            listName.add(chat.userName)
                            listID.add(chat.userId)
                        }
                    }

                    if (listName.isEmpty()) {
                        showSnack("No hay chats ocultos")
                        return
                    }

                    val listaName = listName.toTypedArray<CharSequence>()
                    val listaID = listID.toTypedArray<CharSequence>()
                    var itemSelected = 0

                    AlertDialog.Builder(ContextThemeWrapper(this@MainActivity, R.style.AlertDialogApp))
                        .setTitle("Chats ocultos")
                        .setSingleChoiceItems(listaName, itemSelected) { _, index ->
                            itemSelected = index
                        }
                        .setPositiveButton("Mostrar") { _, _ ->
                            val targetId = listaID[itemSelected].toString()
                            FirebaseRefs.refDatos.child(user.uid).child(type)
                                .child(targetId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(ds: DataSnapshot) {
                                        val photo =
                                            ds.child("wUserPhoto").getValue(String::class.java)
                                        if (photo == EMPTY) {
                                            ds.ref.removeValue()
                                        } else {
                                            ds.ref.child("estado").setValue(type)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            showSnack("${listaName[itemSelected]} ya no está oculto")
                        }
                        .setNegativeButton(DIALOG_CANCEL, null)
                        .setCancelable(false)
                        .show()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun exitGroup() {
        repo.inGroup = false

        listenerGroupBadge?.let {
            FirebaseRefs.refGroupChat.child(repo.groupName)
                .removeEventListener(it)
        }

        listenerMsgUnreadBadge?.let {
            val query = FirebaseRefs.refDatos.child(user.uid)
                .child(CHATWITHUNKNOWN)
                .orderByChild("noVisto")
                .startAt(1.0)
            query.removeEventListener(it)
        }

        PageAdapterGroup.valueEventListenerTitle?.let {
            FirebaseRefs.refGroupUsers.child(repo.groupName)
                .removeEventListener(it)
        }

        ChatGroupFragment.listenerGroupChat?.let {
            FirebaseRefs.refGroupChat.child(repo.groupName)
                .removeEventListener(it)
        }

        FirebaseRefs.refDatos.child(user.uid).child(CHATWITHUNKNOWN)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        FirebaseRefs.refChatUnknown.child("${user.uid} <---> $key")
                            .removeValue()
                        FirebaseRefs.refChatUnknown.child("$key <---> ${user.uid}")
                            .removeValue()
                        FirebaseRefs.refDatos.child(key)
                            .child(CHATWITHUNKNOWN)
                            .child(user.uid)
                            .removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        FirebaseRefs.refDatos.child(user.uid).child(CHATWITHUNKNOWN).removeValue()

        @SuppressLint("SimpleDateFormat")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")

        val chatMsg = ChatsGroup(
            "abandonó la sala",
            dateFormat.format(Calendar.getInstance().time),
            repo.userName,
            user.uid,
            0,
            repo.userType
        )

        FirebaseRefs.refGroupChat.child(repo.groupName)
            .push()
            .setValue(chatMsg)

        FirebaseRefs.refGroupUsers.child(repo.groupName)
            .child(user.uid)
            .removeValue()

        repo.inGroup = false
        repo.userName = ""
        repo.groupName = ""
        repo.userType = 2
        repo.readGroupMsg = 0
        repo.userDate = ""


        repo.inGroup = false
        repo.userName = ""
        repo.groupName = ""
        repo.userType = 2
        repo.readGroupMsg = 0
        repo.userDate = ""

        layoutSettings?.visibility = View.GONE
        invalidateOptionsMenu()

        val newFragment: Fragment = GruposFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, newFragment)
            .commit()

        toolbar?.setTitle(R.string.menu_grupos)
        currentScreen = CurrentScreen.GROUPS
    }

    private fun showSnack(message: String) {
        val snack = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        )
        snack.setBackgroundTint(getColorCompat(R.color.colorC))
        val tv =
            snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }

    fun configureUsersToolbar(
        filterActive: Boolean,
        onRefresh: () -> Unit,
        onFilterClick: () -> Unit
    ) {
        refresh?.setOnClickListener { onRefresh() }
        filter?.setOnClickListener { onFilterClick() }

        val colorRes = if (filterActive) R.color.accent else R.color.blanco
        filter?.setColorFilter(
            getColorCompat(colorRes),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }


    companion object {
        @JvmStatic
        var listenerToken: ValueEventListener? = null

        @JvmStatic
        var listenerGroupBadge: ValueEventListener? = null

        @JvmStatic
        var listenerMsgUnreadBadge: ValueEventListener? = null

        const val REQUEST_CHECK_SETTINGS: Int = 0x1

        var badgeDrawableGroup: BadgeDrawable? = null

        const val UPDATE_INTERVAL: Long = 1000
        val UPDATE_FASTEST_INTERVAL: Long = UPDATE_INTERVAL / 2
        private var CHANNEL_ID: String? = null

        private const val NOTIFICATION_ID = 0
    }
}
