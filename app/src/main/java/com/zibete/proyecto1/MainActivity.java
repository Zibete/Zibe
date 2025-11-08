package com.zibete.proyecto1;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zibete.proyecto1.model.ChatWith;
import com.zibete.proyecto1.model.ChatsGroup;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.adapters.ChatListFragment;
import com.zibete.proyecto1.ui.EditProfileFragment;
import com.zibete.proyecto1.ui.FavoritesFragment;
import com.zibete.proyecto1.ui.GruposFragment;
import com.zibete.proyecto1.utils.FirebaseRefs;
import com.zibete.proyecto1.utils.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import static com.zibete.proyecto1.ChatGroupFragment.listenerGroupChat;
import static com.zibete.proyecto1.Constants.Empty;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.Constants.listenerGroupBadge;
import static com.zibete.proyecto1.Constants.listenerMsgUnreadBadge;
import static com.zibete.proyecto1.Constants.listenerToken; // Usamos este handle global existente
import static com.zibete.proyecto1.PageAdapterGroup.valueEventListenerTitle;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.editor;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.groupName;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.inGroup;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.readGroupMsg;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.userDate;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.userType;
import static com.zibete.proyecto1.utils.FirebaseRefs.user;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    int flagIntent;
    BadgeDrawable badgeDrawableChat;
    static BadgeDrawable badgeDrawableGroup;
    public static MaterialToolbar toolbar;
    DrawerLayout drawer;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;
    NavController navController;
    View headerView;
    public static ImageView search;
    public static ImageView filter;
    public static ImageView refresh;
    public static View layoutSettings;
    public static BottomNavigationView mBottomNavigation;
    // Ubicación (API moderna)
    private Location mLastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    public static final long UPDATE_INTERVAL = 1000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;
    public SearchView searchView;
    private static String CHANNEL_ID;
    @SuppressWarnings("unused")
    private final static int NOTIFICATION_ID = 0;
    private ViewGroup viewGroup;
    @SuppressWarnings("unused")
    private ViewGroup viewGroup2;


    // Control de sesión por dispositivo
    private String myInstallId = null; // Identificador estable por instalación
    private String myFcmToken = null;  // Token FCM para notificaciones
    private boolean tokenListenerInitialized = false; // Evitar falso positivo en primera carga

    private NavHostFragment navHostFragment;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        viewGroup = findViewById(R.id.toolbar);
        viewGroup.setLayoutTransition(layoutTransition);

        if (user == null) {
            logout(null);
            return;
        } else {
            CHANNEL_ID = user.getUid();
        }

        toolbar = findViewById(R.id.toolbar);
        layoutSettings = findViewById(R.id.layoutSettings);
        filter = findViewById(R.id.filter);
        refresh = findViewById(R.id.refresh);

        // Ubicación: inicialización
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(UPDATE_FASTEST_INTERVAL)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
                if (mLastLocation != null) {
                    UserRepository.updateLocationUI(mLastLocation);
                }
            }
        };
        ensureLocationSettingsAndStart();

        // ✅ MaterialToolbar funciona igual con setSupportActionBar
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ✅ MaterialToolbar hereda de Toolbar, funciona con el Toggle
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // NavController / AppBar
        navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_chat, R.id.nav_usuarios, R.id.nav_grupos, R.id.nav_editPerfil
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        headerView = navigationView.getHeaderView(0);

        MaterialCardView linear_image_user = headerView.findViewById(R.id.linear_image_user);
        ImageView image_user = headerView.findViewById(R.id.image_user);

        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int height = dimension.heightPixels;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height / 2
        );
        linear_image_user.setLayoutParams(layoutParams);

        TextView tv_usuario = headerView.findViewById(R.id.tv_usuario);
        TextView tv_mail = headerView.findViewById(R.id.tv_mail);
        LinearLayout editPerfil = headerView.findViewById(R.id.editPerfil);

        tv_usuario.setText(user.getDisplayName());
        tv_mail.setText(user.getEmail());
        Glide.with(getApplicationContext()).load(user.getPhotoUrl()).into(image_user);

        // Ir a Edit Perfil
        editPerfil.setOnClickListener(v -> EditProfile(null));

        mBottomNavigation = findViewById(R.id.nav_view3);
        mBottomNavigation.findViewById(R.id.navBottomChat).performClick();

        badgeDrawableChat = mBottomNavigation.getOrCreateBadge(R.id.navBottomChat);
        badgeDrawableChat.setBadgeTextColor(getResources().getColor(R.color.zibe_night_start));
        badgeDrawableChat.setBackgroundColor(getResources().getColor(R.color.accent));
        badgeDrawableChat.setVisible(false);

        final Query newQuery = FirebaseRefs.refDatos.child(user.getUid()).child(chatWith).orderByChild("noVisto").startAt(1);
        newQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    int countMsgUnread = 0;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        Integer unRead = snapshot.child("noVisto").getValue(Integer.class);
                        if (unRead != null) countMsgUnread += unRead;

                    }
                    badgeDrawableChat.setVisible(true);
                    badgeDrawableChat.setNumber(countMsgUnread);

                } else {
                    badgeDrawableChat.setVisible(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        badgeDrawableGroup = mBottomNavigation.getOrCreateBadge(R.id.navBottomGrupos);
        badgeDrawableGroup.setBadgeTextColor(getResources().getColor(R.color.zibe_night_start));
        badgeDrawableGroup.setBackgroundColor(getResources().getColor(R.color.accent));
        badgeDrawableGroup.setVisible(false);

        listenerGroupBadge = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    if (!toolbar.getTitle().equals(groupName)) {

                        final long totalMsg = dataSnapshot.getChildrenCount();

                        FirebaseRefs.refDatos.child(user.getUid()).child("ChatList").child("msgReadGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                if (dataSnapshot1.exists()) {

                                    final Query query = FirebaseRefs.refDatos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            Integer Leidos = dataSnapshot1.getValue(Integer.class);
                                            if (Leidos == null) Leidos = 0;

                                            int countMsgUnread = 0;

                                            if (dataSnapshot.exists()) {

                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                                    Integer unRead = snapshot.child("noVisto").getValue(Integer.class);
                                                    if (unRead != null) countMsgUnread += unRead;

                                                }
                                            }

                                            if (inGroup) {
                                                badgeDrawableGroup.setNumber((int) (totalMsg - Leidos + countMsgUnread));

                                                if (badgeDrawableGroup.getNumber() > 0) {
                                                    badgeDrawableGroup.setVisible(true);

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) { }
                                    });

                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        listenerMsgUnreadBadge = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!toolbar.getTitle().equals(groupName)) {

                    int countMsgUnread = 0;

                    if (dataSnapshot.exists()) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            Integer unRead = snapshot.child("noVisto").getValue(Integer.class);
                            if (unRead != null) countMsgUnread += unRead;

                        }

                        final int finalCountMsgUnread = countMsgUnread;
                        FirebaseRefs.refGroupChat.child(groupName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()) {

                                    final long totalMsg = dataSnapshot.getChildrenCount();

                                    FirebaseRefs.refDatos.child(user.getUid()).child("ChatList").child("msgReadGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                            Integer Leidos = dataSnapshot1.getValue(Integer.class);
                                            if (Leidos == null) Leidos = 0;

                                            if (inGroup) {
                                                badgeDrawableGroup.setNumber((int) (totalMsg - Leidos + finalCountMsgUnread));

                                                if (badgeDrawableGroup.getNumber() > 0) {
                                                    badgeDrawableGroup.setVisible(true);

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) { }
                                    });

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        if (inGroup) {
            FirebaseRefs.refGroupChat.child(groupName).addValueEventListener(listenerGroupBadge);
            final Query query = FirebaseRefs.refDatos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
            query.addValueEventListener(listenerMsgUnreadBadge);
        }

        mBottomNavigation.setOnNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.navBottomUsers) {
                if (!toolbar.getTitle().equals(getString(R.string.menu_usuarios))) {

                    layoutSettings.setVisibility(View.VISIBLE);
                    invalidateOptionsMenu();

                    Fragment newFragment = new EditProfileFragment.UsuariosFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.nav_host_fragment, newFragment);
                    toolbar.setTitle(R.string.menu_usuarios);
                    transaction.commit();
                }
                return true;

            } else if (id == R.id.navBottomChat) {
                NavChatList();
                return true;

            } else if (id == R.id.navBottomFavorites) {
                Favorites();
                return true;

            } else if (id == R.id.navBottomGrupos) {

                if (!inGroup) {

                    if (!toolbar.getTitle().equals(getString(R.string.menu_grupos))) {

                        layoutSettings.setVisibility(View.GONE);
                        invalidateOptionsMenu();

                        Fragment newFragment = new GruposFragment();
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.nav_host_fragment, newFragment);
                        toolbar.setTitle(R.string.menu_grupos);
                        transaction.commit();
                    }

                } else {
                    toolbar.setVisibility(View.VISIBLE);
                    layoutSettings.setVisibility(View.GONE);

                    invalidateOptionsMenu();

                    Fragment newFragment = new PageAdapterGroup();

                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.nav_host_fragment, newFragment);

                    toolbar.setTitle(groupName);

                    Bundle data = new Bundle();
                    data.putString("group_name", groupName);
                    data.putString("getUid", userName);
                    newFragment.setArguments(data);

                    transaction.commit();
                }
                return true;
            }
            return false;
        });

        // ===== Registro de instalación + FCM y listener de "installId" =====
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        myInstallId = task.getResult();
                    }
                    // Obtener FCM token (independiente del control de sesión)
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(tk -> {
                                if (tk.isSuccessful()) {
                                    myFcmToken = tk.getResult();
                                    // Guardamos/actualizamos FCM token (no se usa para comparar sesión)
                                    FirebaseRefs.refCuentas.child(user.getUid()).child("fcmToken").setValue(myFcmToken);
                                }
                                // Auto-heal + listener de installId
                                registerInstallIdAndAttachListener();
                            });
                });

        // Flag de navegación entrante
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            flagIntent = extras.getInt("flagIntent", -1);
            if (flagIntent == 0) {
                EditProfile(null);
            }
        }
    }

    /** Escribe mi installId si falta/difiere y luego engancha el listener de cambios remotos. */
    private void registerInstallIdAndAttachListener() {
        final DatabaseReference installIdRef = FirebaseRefs.refCuentas.child(user.getUid()).child("installId");
        installIdRef.get().addOnSuccessListener(snap -> {
            String current = snap.exists() ? snap.getValue(String.class) : null;
            if (myInstallId != null && (current == null || !current.equals(myInstallId))) {
                installIdRef.setValue(myInstallId);
            }
            attachInstallIdListener(installIdRef);
        }).addOnFailureListener(e -> attachInstallIdListener(installIdRef));
    }

    /** Observa el valor remoto de installId para detectar sesiones en otro dispositivo. */
    private void attachInstallIdListener(DatabaseReference installIdRef) {
        listenerToken = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String remoteInstallId = dataSnapshot.getValue(String.class);

                // Si aún no tengo mi ID, no comparo
                if (myInstallId == null) return;

                // Ignorar la primera carga para evitar falso positivo de arranque
                if (!tokenListenerInitialized) {
                    tokenListenerInitialized = true;

                    // Auto-curación: si en la nube no coincide conmigo al iniciar, me escribo
                    if (remoteInstallId == null || !myInstallId.equals(remoteInstallId)) {
                        dataSnapshot.getRef().setValue(myInstallId);
                    }
                    return;
                }

                // Desde acá, si cambia y no soy yo -> otro dispositivo tomó la sesión
                if (remoteInstallId != null && !myInstallId.equals(remoteInstallId)) {
                    new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                            .setTitle("Atención")
                            .setMessage("Se registró un inicio de sesión en otro dispositivo. ¿Qué desea hacer?")
                            .setPositiveButton("Continuar en este dispositivo", (dialog, id) ->
                                    dataSnapshot.getRef().setValue(myInstallId))
                            .setNegativeButton("Cerrar sesión", (dialog, id) -> logout(null))
                            .setCancelable(false)
                            .show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        installIdRef.addValueEventListener(listenerToken);
    }

    // ======= Navegación / UI =======

    public void NavChatList() {
        if (!toolbar.getTitle().equals(getString(R.string.menu_chat))) {
            toolbar.setVisibility(View.VISIBLE);
            layoutSettings.setVisibility(View.GONE);
            invalidateOptionsMenu();

            Fragment newFragment = new ChatListFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.nav_host_fragment, newFragment);
            toolbar.setTitle(R.string.menu_chat);

            transaction.commit();

            mBottomNavigation.setVisibility(View.VISIBLE);
            mBottomNavigation.findViewById(R.id.navBottomChat).performClick();

            drawer.closeDrawer(GravityCompat.START);
        }
    }

    public void EditProfile(MenuItem item) {
        if (!toolbar.getTitle().equals(getString(R.string.menu_edit))) {
            mBottomNavigation.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.GONE);
            toolbar.setTitle(R.string.menu_edit);

            invalidateOptionsMenu();

            Fragment newFragment = new EditProfileFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.nav_host_fragment, newFragment);
            transaction.commit();
        }
        drawer.closeDrawer(GravityCompat.START);
    }

    public void Ajustes(MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
    }

    // ======= Ubicación: resultados de resolución de settings ======= //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("TAG", "El usuario permitió el cambio de ajustes de ubicación.");
                processLastLocation();
                startLocationUpdates();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("TAG", "El usuario no permitió el cambio de ajustes de ubicación");
            }
        }
    }

    // ======= Ciclo de vida (online/offline + ubicación moderna) ======= //
    @Override
    protected void onStart() {
        super.onStart();
        UserRepository.setUserOnline(getApplicationContext(), user.getUid());
        ensureLocationSettingsAndStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserRepository.setUserOnline(getApplicationContext(), user.getUid());
        startLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Quitamos el listener del nodo "installId"
        FirebaseRefs.refCuentas.child(user.getUid()).child("installId").removeEventListener(listenerToken);
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
        stopLocationUpdates();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void logout(MenuItem item) {
        new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                .setTitle("Cerrar sesión")
                .setMessage("¿Está seguro de cerrar su sesión?")
                .setCancelable(false)
                .setPositiveButton("Si", (builder, id) -> logout())
                .setNegativeButton("No", (builder, id) -> { })
                .show();
    }

    public void logout() {
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
        if (inGroup) {
            exitGroup();
        }

        // Quitamos el listener del nodo "installId"
        FirebaseRefs.refCuentas.child(user.getUid()).child("installId").removeEventListener(listenerToken);

        EditProfileFragment.UsuariosFragment.DeletePreferences();
        EditProfileFragment.DeleteProfilePreferences(this);

        FirebaseAuth.getInstance().signOut();
        com.facebook.login.LoginManager.getInstance().logOut();

        stopLocationUpdates();

        finish();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        startActivity(intent);
    }

    // ======= Ubicación: helpers modernas ======= //

    private void ensureLocationSettingsAndStart() {
        if (!isLocationPermissionGranted()) {
            manageDeniedPermission();
            return;
        }

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(this, (LocationSettingsResponse response) -> {
                    processLastLocation();
                    startLocationUpdates();
                })
                .addOnFailureListener(this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException ex) {
                            Log.d("TAG", "El Intent del diálogo no funcionó.");
                        }
                    } else {
                        Log.d("TAG", "Los ajustes de ubicación no son apropiados.");
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!isLocationPermissionGranted()) return;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void processLastLocation() {
        if (!isLocationPermissionGranted()) return;
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(mLastLocation -> {
                    if (mLastLocation != null) {
                        UserRepository.updateLocationUI(mLastLocation);
                    }
                });
    }

    private boolean isLocationPermissionGranted() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manageDeniedPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Zibe necesita acceso a su ubicación para poder funcionar", Snackbar.LENGTH_SHORT);
            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
            TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            snack.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ensureLocationSettingsAndStart();
            } else {
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Se cerrará su sesión", Snackbar.LENGTH_SHORT);
                snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();

                logout(null);
            }
        }
    }



    // ======= Menú / navegación ======= //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {

        searchView = findViewById(R.id.action_search);
        ImageButton btSave = findViewById(R.id.bt_save);
        EditText edtFecha = findViewById(R.id.edtFecha);

        if (toolbar.getTitle().equals(getString(R.string.menu_edit))) {

            if (btSave != null && btSave.isEnabled()) {

                new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                        .setTitle("Salir")
                        .setMessage("Se perderán los cambios, ¿Desea continuar?")
                        .setCancelable(false)
                        .setPositiveButton("Si", (builder, id) ->

                                FirebaseRefs.refCuentas.child(user.getUid()).child("birthDay").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        String fecha = dataSnapshot.getValue(String.class);

                                        if (fecha != null && fecha.equals("")) {

                                            final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
                                            snack.setAction("OK", v -> snack.dismiss());
                                            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                                            TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                                            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            snack.show();

                                        } else {
                                            NavChatList();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                })

                        )
                        .setNegativeButton("No", (builder, id) -> { })
                        .show();

            } else {

                String fecha = edtFecha != null ? edtFecha.getText().toString().trim() : "";

                if (fecha.isEmpty()) {

                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("OK", v -> snack.dismiss());
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();

                } else {
                    NavChatList();
                }
            }

        } else {

            if (drawer.isOpen()) {
                drawer.closeDrawer(GravityCompat.START);
            } else {

                if (toolbar.getTitle().equals(getString(R.string.menu_chat)) | toolbar.getTitle().equals(getString(R.string.menu_usuarios))) {

                    if (searchView != null && !searchView.isIconified()) {
                        searchView.onActionViewCollapsed();
                    } else {
                        super.onBackPressed();
                    }

                } else {
                    NavChatList();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;

        } else if (id == R.id.action_settings) {
            Ajustes(null);
            return true;

        } else if (id == R.id.action_unlock) {
            unlockUser();
            return true;

        } else if (id == R.id.action_unhide_chats) {
            unhideChats();
            return true;

        } else if (id == R.id.action_favorites) {
            mBottomNavigation.findViewById(R.id.navBottomFavorites).performClick();
            return true;

        } else if (id == R.id.action_exit) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp));
            builder.setTitle("Salir");
            builder.setMessage("¿Desea abandonar " + groupName + "?");
            builder.setPositiveButton("Salir", (dialog, which) -> exitGroup());
            builder.setNegativeButton("Cancelar", (dialog, which) -> { });
            builder.show();

            return true;
        }

        return false;
    }

    public void unhideChats() {

        final String type;

        if (toolbar.getTitle().equals(groupName)) {
            type = chatWithUnknown;
        } else {
            type = chatWith;
        }

        FirebaseRefs.refDatos.child(user.getUid()).child(type).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    final ArrayList<String> listName = new ArrayList<>();
                    final ArrayList<String> listID = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        ChatWith chat = snapshot.getValue(ChatWith.class);
                        if (chat != null && "delete".equals(chat.getState())) {

                            String name = chat.getUserName();
                            String user_id = chat.getUserId();
                            listName.add(name);
                            listID.add(user_id);
                        }
                    }

                    if (!listName.isEmpty()) {

                        final CharSequence[] listaName = listName.toArray(new CharSequence[0]);
                        final CharSequence[] listaID = listID.toArray(new CharSequence[0]);
                        final int[] itemSelected = {0};

                        new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                                .setTitle("Chats ocultos")
                                .setSingleChoiceItems(listaName, itemSelected[0], (dialogInterface, selectedIndex) -> itemSelected[0] = selectedIndex)
                                .setPositiveButton("Mostrar", (builder, selectedIndex) ->

                                        FirebaseRefs.refDatos.child(user.getUid()).child(type).child(String.valueOf(listaID[itemSelected[0]])).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);

                                                if (Objects.equals(photo, Empty)) {
                                                    dataSnapshot.getRef().removeValue();
                                                } else {
                                                    dataSnapshot.getRef().child("estado").setValue(type);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) { }
                                        })

                                )
                                .setNegativeButton("Cancelar", (builder, id) -> { })
                                .setCancelable(false)
                                .show();

                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), listaName[itemSelected[0]] + " ya no está oculto", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();

                    } else {
                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay chats ocultos", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();
                    }
                } else {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay chats ocultos", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public void unlockUser() {
        final String type;

        if (toolbar.getTitle().equals(groupName)) {
            type = chatWithUnknown;
        } else {
            type = chatWith;
        }

        FirebaseRefs.refDatos.child(user.getUid()).child(type).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    final ArrayList<String> listName = new ArrayList<>();
                    final ArrayList<String> listID = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        ChatWith chat = snapshot.getValue(ChatWith.class);
                        if (chat != null && "bloq".equals(chat.getState())) {

                            String name = chat.getUserName();
                            String user_id = chat.getUserId();
                            listName.add(name);
                            listID.add(user_id);
                        }
                    }

                    if (!listName.isEmpty()) {

                        final CharSequence[] listaName = listName.toArray(new CharSequence[0]);
                        final CharSequence[] listaID = listID.toArray(new CharSequence[0]);
                        final int[] itemSelected = {0};

                        new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                                .setTitle("¿A quién deseas desbloquear?")
                                .setSingleChoiceItems(listaName, itemSelected[0], (dialogInterface, selectedIndex) -> itemSelected[0] = selectedIndex)
                                .setPositiveButton("Aceptar", (builder, selectedIndex) ->

                                        FirebaseRefs.refDatos.child(user.getUid()).child(type).child(String.valueOf(listaID[itemSelected[0]])).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);

                                                if (Objects.equals(photo, Empty)) {
                                                    dataSnapshot.getRef().removeValue();
                                                } else {
                                                    dataSnapshot.getRef().child("estado").setValue(type);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) { }
                                        })

                                )
                                .setNegativeButton("Cancelar", (builder, id) -> { })
                                .setCancelable(false)
                                .show();

                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Desbloqueaste a " + listaName[itemSelected[0]], Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();

                    } else {
                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay usuarios bloqueados", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();
                    }
                } else {
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay usuarios bloqueados", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public void exitGroup() {
        inGroup = false;

        if (listenerGroupBadge != null) {
            FirebaseRefs.refGroupChat.child(groupName).removeEventListener(listenerGroupBadge);
        }
        if (listenerMsgUnreadBadge != null) {
            final Query query = FirebaseRefs.refDatos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
            query.removeEventListener(listenerMsgUnreadBadge);
        }
        if (valueEventListenerTitle != null) {
            FirebaseRefs.refGroupUsers.child(groupName).removeEventListener(valueEventListenerTitle); //Elimino el usuario
        }
        if (listenerGroupChat != null) {
            FirebaseRefs.refGroupChat.child(groupName).removeEventListener(listenerGroupChat);
        }

        FirebaseRefs.refDatos.child(user.getUid()).child(chatWithUnknown).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    FirebaseRefs.refChatUnknown.child(user.getUid() + " <---> " + key).removeValue(); //Elimino mi chat con él
                    FirebaseRefs.refChatUnknown.child(key + " <---> " + user.getUid()).removeValue(); //Elimino su chat conmigo
                    FirebaseRefs.refDatos.child(key).child(chatWithUnknown).child(user.getUid()).removeValue(); //Elimino su chat lista
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        FirebaseRefs.refDatos.child(user.getUid()).child(chatWithUnknown).removeValue(); //Elimino mis chat lista

        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");

        final ChatsGroup chatmsg = new ChatsGroup(
                "abandonó la sala",
                dateFormat3.format(Calendar.getInstance().getTime()),
                userName,
                user.getUid(),
                0,
                userType);
        FirebaseRefs.refGroupChat.child(groupName).push().setValue(chatmsg);

        FirebaseRefs.refGroupUsers.child(groupName).child(user.getUid()).removeValue();

        inGroup = false;
        userName = "";
        groupName = "";
        userType = 2;
        readGroupMsg = 0;
        userDate = "";

        editor.putBoolean("inGroup", false);
        editor.putString("userName", "");
        editor.putString("groupName", "");
        editor.putInt("userType", 2);
        editor.putInt("readGroupMsg", 0);
        editor.putString("userDate", "");

        editor.apply();

        layoutSettings.setVisibility(View.GONE);
        invalidateOptionsMenu();

        Fragment newFragment = new GruposFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, newFragment);
        toolbar.setTitle(R.string.menu_grupos);
        transaction.commit();
    }

    public void Favorites() {
        layoutSettings.setVisibility(View.GONE);
        toolbar.setTitle(R.string.favoritos);

        invalidateOptionsMenu();

        Fragment newFragment = new FavoritesFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, newFragment);

        transaction.commit();

    }

    public void Index(MenuItem item) {
        if (toolbar.getTitle().equals(getString(R.string.menu_edit)) |
                toolbar.getTitle().equals(getString(R.string.ajustes)) |
                toolbar.getTitle().equals(getString(R.string.favoritos))) {

            onBackPressed();

        } else {
            drawer.closeDrawer(GravityCompat.START);
        }
    }
}
