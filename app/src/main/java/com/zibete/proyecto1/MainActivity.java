package com.zibete.proyecto1;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging; // (API nueva de FCM)
import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.POJOS.ChatsGroup;
import com.zibete.proyecto1.Splash.SplashActivity;
import com.zibete.proyecto1.ui.ChatList.ChatListFragment;
import com.zibete.proyecto1.ui.EditProfileFragment;
import com.zibete.proyecto1.ui.FavoritesFragment;
import com.zibete.proyecto1.ui.GruposFragment;
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment;

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
import static com.zibete.proyecto1.Constants.listenerToken;
import static com.zibete.proyecto1.PageAdapterGroup.valueEventListenerTitle;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.editor;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.inGroup;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.readGroupMsg;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userDate;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userType;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private AppBarConfiguration mAppBarConfiguration;

    public static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    public static final FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static final DatabaseReference ref_datos = database.getReference("Usuarios").child("Datos");
    public static final DatabaseReference ref_cuentas = database.getReference("Usuarios").child("Cuentas");
    public static final DatabaseReference ref_chat_path = database.getReference("Chats");
    public static final DatabaseReference ref_chat = database.getReference("Chats").child("Chats");
    public static final DatabaseReference ref_chat_unknown = database.getReference("Chats").child("Unknown");
    public static final DatabaseReference ref_zibe = database.getReference("Zibe");
    public static final DatabaseReference ref_groups = database.getReference("Groups").child("Data");
    public static final DatabaseReference ref_group_chat = database.getReference("Groups").child("Chat");
    public static final DatabaseReference ref_group_users = database.getReference("Groups").child("Users");

    public static final int REQUEST_LOCATION = 0;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    int flagIntent;

    BadgeDrawable badgeDrawableChat;
    static BadgeDrawable badgeDrawableGroup;

    public static Toolbar toolbar;
    DrawerLayout drawer;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;
    NavController navController;
    View headerView;
    public static ImageView search;
    public static ImageView filter;
    public static ImageView refresh;

    public static RelativeLayout layoutSettings;

    private String userToken;

    public static BottomNavigationView mBottomNavigation;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationClient;
    public static final long UPDATE_INTERVAL = 1000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;

    public SearchView searchView;

    private GoogleApiClient mGoogleApiClient;
    private static final String NOTIFICATION_MSG = "NOTIFICATION_MSG";
    private static String CHANNEL_ID;
    private final static int NOTIFICATION_ID = 0;
    private ViewGroup viewGroup;
    private ViewGroup viewGroup2;
    public static Double latitud;
    public static Double longitud;


    public static int PERMISSION_CAMERA = 0;





    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);






        setContentView(R.layout.activity_main);
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        viewGroup = (ViewGroup) findViewById(R.id.toolbar);
        viewGroup.setLayoutTransition(layoutTransition);





/*
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Aquí muestras confirmación explicativa al usuario
                // por si rechazó los permisos anteriormente
            } else {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
            }
        }

 */















        if (user == null){

            logout(null);
        }else {

            CHANNEL_ID = user.getUid();
        }



        toolbar = findViewById(R.id.toolbar);
        layoutSettings = findViewById(R.id.layoutSettings);
        filter = findViewById(R.id.filter);
        refresh = findViewById(R.id.refresh);


        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // Do something with locationResult
            }
        };














        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();

        mLocationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient, builder.build()
        );

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        processLastLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            Log.d("TAG", "Los ajustes de ubicación no satisfacen la configuración. " +
                                    "Se mostrará un diálogo de ayuda.");
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.d("TAG", "El Intent del diálogo no funcionó.");
                            // Sin operaciones
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.d("TAG", "Los ajustes de ubicación no son apropiados.");
                        break;

                }
            }
        });






        //Intent intent1 = new Intent(getApplicationContext(),Notify.class);

        //getApplicationContext().startService(intent1);

        //ContextCompat.startForegroundService(this, intent1);
        //startService(new Intent(this,Notify.class));





        setSupportActionBar(toolbar);


        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);












        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        /*
        AppBarConfiguration appBarConfiguration1 = new AppBarConfiguration.Builder(
                R.id.navBottomUsers, R.id.navBottomChat, R.id.navBottomGrupos)
                .build();
        final NavController navController3 = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController3, appBarConfiguration1);
        NavigationUI.setupWithNavController(navView2, navController3);

         */


        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Boton que sale con un mensaje", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

         */


        // Pasar cada ID de menú como un conjunto de ID porque cada
        // el menú debe considerarse como destinos de nivel superior.




        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();



//APP BAR





        navController = Navigation.findNavController(this, R.id.nav_host_fragment);



        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_chat, R.id.nav_usuarios, R.id.nav_grupos, R.id.nav_editPerfil)

                .build();



        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);









//APP BAR

        headerView = navigationView.getHeaderView(0);

        LinearLayout linear_image_user = headerView.findViewById(R.id.linear_image_user);
        ImageView image_user = headerView.findViewById(R.id.image_user);

        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int height = dimension.heightPixels;


        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height/2);


        linear_image_user.setLayoutParams(layoutParams);

        TextView tv_usuario = headerView.findViewById(R.id.tv_usuario);
        TextView tv_mail = headerView.findViewById(R.id.tv_mail);
        LinearLayout editPerfil = headerView.findViewById(R.id.editPerfil);


        tv_usuario.setText(user.getDisplayName());
        tv_mail.setText(user.getEmail());
        Glide.with(getApplicationContext()).load(user.getPhotoUrl()).into(image_user);



// Ir a Edit Perfil
        editPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditProfile(null);

            }
        });
















        mBottomNavigation = findViewById(R.id.nav_view3);
        mBottomNavigation.findViewById(R.id.navBottomChat).performClick();


        badgeDrawableChat = mBottomNavigation.getOrCreateBadge(R.id.navBottomChat);
        badgeDrawableChat.setBadgeTextColor(getResources().getColor(R.color.colorA));
        badgeDrawableChat.setBackgroundColor(getResources().getColor(R.color.accent));
        badgeDrawableChat.setVisible(false);

        final Query newQuery = ref_datos.child(user.getUid()).child(chatWith).orderByChild("noVisto").startAt(1);
        newQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    int countMsgUnread = 0;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        int unRead = snapshot.child("noVisto").getValue(int.class);

                        countMsgUnread = countMsgUnread + unRead;

                    }
                    badgeDrawableChat.setVisible(true);
                    badgeDrawableChat.setNumber(countMsgUnread);

                }else{
                    badgeDrawableChat.setVisible(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        /*
        MainActivity.ref_datos.child(user.getUid()).child("ChatList").child("msgNoLeidos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    //if (!toolbar.getTitle().equals(getString(R.string.menu_chat))) {

                        int msgNoLeidos = dataSnapshot.getValue(int.class);

                        if (msgNoLeidos > 0) {

                            badgeDrawableChat.setVisible(true);
                            badgeDrawableChat.setNumber(msgNoLeidos);

                        }else{

                            badgeDrawableChat.setVisible(false);
                        }
                    //}
                }else{
                    badgeDrawableChat.setVisible(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });

         */



        badgeDrawableGroup = mBottomNavigation.getOrCreateBadge(R.id.navBottomGrupos);
        badgeDrawableGroup.setBadgeTextColor(getResources().getColor(R.color.colorA));
        badgeDrawableGroup.setBackgroundColor(getResources().getColor(R.color.accent));
        badgeDrawableGroup.setVisible(false);

        listenerGroupBadge = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    if (!toolbar.getTitle().equals(groupName)) {

                        final long totalMsg = dataSnapshot.getChildrenCount();

                        ref_datos.child(user.getUid()).child("ChatList").child("msgReadGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                if (dataSnapshot1.exists()) {

                                    final int Leidos = dataSnapshot1.getValue(int.class);

                                    final Query query = ref_datos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            int countMsgUnread = 0;

                                            if (dataSnapshot.exists()) {

                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                                    int unRead = snapshot.child("noVisto").getValue(int.class);

                                                    countMsgUnread = countMsgUnread + unRead;

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
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });

                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };




        listenerMsgUnreadBadge = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                if (!toolbar.getTitle().equals(groupName)) {

                    int countMsgUnread = 0;

                    if (dataSnapshot.exists()) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            int unRead = snapshot.child("noVisto").getValue(int.class);

                            countMsgUnread = countMsgUnread + unRead;

                        }

                        final int finalCountMsgUnread = countMsgUnread;
                        ref_group_chat.child(groupName).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()) {

                                    final long totalMsg = dataSnapshot.getChildrenCount();

                                    ref_datos.child(user.getUid()).child("ChatList").child("msgReadGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {


                                            int Leidos = dataSnapshot1.getValue(int.class);

                                            if (inGroup) {
                                                badgeDrawableGroup.setNumber((int) (totalMsg - Leidos + finalCountMsgUnread));

                                                if (badgeDrawableGroup.getNumber() > 0) {
                                                    badgeDrawableGroup.setVisible(true);

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });


                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }




            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };







        if (inGroup){
            ref_group_chat.child(groupName).addValueEventListener(listenerGroupBadge);
            final Query query = ref_datos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
            query.addValueEventListener(listenerMsgUnreadBadge);
        }



        mBottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.navBottomUsers:

                        if (!toolbar.getTitle().equals(getString(R.string.menu_usuarios))) {

                            layoutSettings.setVisibility(View.VISIBLE);
                            invalidateOptionsMenu();

                            Fragment newFragment = new UsuariosFragment();
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.nav_host_fragment, newFragment);
                            toolbar.setTitle(R.string.menu_usuarios);
                            //transaction.addToBackStack(null);
                            transaction.commit();


                        }

                        break;

                    case R.id.navBottomChat:


                        NavChatList();


                        break;

                    case R.id.navBottomFavorites:


                        Favorites();


                        break;



                    case R.id.navBottomGrupos:

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


                            }else{




                                toolbar.setVisibility(View.VISIBLE);
                                layoutSettings.setVisibility(View.GONE);

                                invalidateOptionsMenu();

                                //Fragment newFragment = new ChatGroupFragment();
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





/*

                            ref_group_users.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (dataSnapshot.exists()){ //Si existen grupos

                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) { //Recorro uno por uno

                                            final String group = snapshot.getRef().getKey();//Agarro el nombre

                                            ref_group_users.child(group).child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    if (dataSnapshot.exists()) { //Si yo estoy dentro del grupo:

                                                        String getMyName = dataSnapshot.getValue(String.class);
                                                        String getMyGroup = group;

                                                        toolbar.setVisibility(View.VISIBLE);
                                                        layoutSettings.setVisibility(View.GONE);

                                                        invalidateOptionsMenu();

                                                        Fragment newFragment = new ChatGroupFragment();


                                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                                        transaction.replace(R.id.nav_host_fragment, newFragment);

                                                        toolbar.setTitle(getMyGroup);

                                                        Bundle data = new Bundle();
                                                        data.putString("group_name", getMyGroup);
                                                        data.putString("getUid", getMyName);
                                                        newFragment.setArguments(data);

                                                        transaction.commit();


                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                }
                                            });







                                            break;
                                        }

                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });




 */



                        break;
                }

                return true;
            }
        });






        listenerToken = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    String token = dataSnapshot.getValue(String.class);
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(task -> {
                                userToken = task.getResult();
                            });

                    if (!token.equals(userToken)) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp));
                        builder.setTitle("Atención");
                        builder.setMessage("Se registró un inicio de sesión en otro dispositivo ¿Que desea hacer?");
                        builder.setPositiveButton("Continuar en este dispositivo", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                dataSnapshot.getRef().setValue(userToken);

                            }
                        });


                        builder.setNegativeButton("Cerrar sesión", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                logout(null);
                                return;

                            }
                        });
                        builder.setCancelable(false);
                        builder.show();

                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        ref_cuentas.child(user.getUid()).child("token").addValueEventListener(listenerToken);


        //signUp();




        flagIntent = getIntent().getExtras().getInt("flagIntent");



        if(flagIntent == 0){
           EditProfile(null);
        }



        //Fin del OnCreate
    }



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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.d("TAG", "El usuario permitió el cambio de ajustes de ubicación.");
                        processLastLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("TAG", "El usuario no permitió el cambio de ajustes de ubicación");
                        break;
                }
                break;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        new Constants().StateOnLine(getApplicationContext(), user.getUid());
        mGoogleApiClient.connect();
    }


    @Override
    protected void onPause() {
        super.onPause();

        new Constants().StateOffLine(getApplicationContext(),user.getUid());

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();

        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        new Constants().StateOnLine(getApplicationContext(), user.getUid());

        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }


    }



    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void stopLocationUpdates() {

        fusedLocationClient.removeLocationUpdates(locationCallback);

    }


    @Override
    public void onDestroy () {
        super.onDestroy();

        ref_cuentas.child(user.getUid()).child("token").removeEventListener(listenerToken);
        new Constants().StateOffLine(getApplicationContext(),user.getUid());


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
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface builder, int id) {

                        logout();


                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface builder, int id) {
                        return;
                    }
                })
                .show();


    }

    public void logout() {

        new Constants().StateOffLine(getApplicationContext(),user.getUid());

        if (inGroup) {
            exitGroup();
        }

        if (listenerToken !=null) {
            ref_cuentas.child(user.getUid()).child("token").removeEventListener(listenerToken);
        }


        UsuariosFragment.DeletePreferences();
        EditProfileFragment.DeleteProfilePreferences();

        FirebaseAuth.getInstance().signOut();
        com.facebook.login.LoginManager.getInstance().logOut();
        if (mGoogleApiClient !=null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
        }

        finish();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        startActivity(intent);

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Zibe necesita acceso a su ubicación para poder funcionar", Snackbar.LENGTH_SHORT);
                snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();

            } else {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
            }
        }


        // Obtenemos la última ubicación al ser la primera vez
        processLastLocation();
        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (isLocationPermissionGranted()) {

            LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, (LocationListener) this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, (LocationListener) this);



            fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback,
                    null /* Looper */);

        } else {
            manageDeniedPermission();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                processLastLocation();
            }else{

                Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Se cerrará su sesión", Snackbar.LENGTH_SHORT);
                snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();

                logout(null);
            }
        }

    }

    private boolean isLocationPermissionGranted() {
        int permission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manageDeniedPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Zibe necesita acceso a su ubicación para poder funcionar", Snackbar.LENGTH_SHORT);
            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
            TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            snack.show();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    private void updateLocationUI() {

        latitud = mLastLocation.getLatitude();
        longitud = mLastLocation.getLongitude();


        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ref_cuentas.child(user.getUid()).child("latitud").setValue(latitud);
                    ref_cuentas.child(user.getUid()).child("longitud").setValue(longitud);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void processLastLocation() {

        getLastLocation();
        if (mLastLocation != null) {
            updateLocationUI();
        }

    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {

        if (isLocationPermissionGranted()) {


            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            manageDeniedPermission();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("TAG", String.format("Nueva ubicación: (%s, %s)",
                location.getLatitude(), location.getLongitude()));
        mLastLocation = location;
        updateLocationUI();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }



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

            if (btSave.isEnabled()) {

                new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp))
                        .setTitle("Salir")
                        .setMessage("Se perderán los cambios, ¿Desea continuar?")
                        .setCancelable(false)
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                MainActivity.ref_cuentas.child(user.getUid()).child("birthDay").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        String fecha = dataSnapshot.getValue(String.class);

                                        if (fecha.equals("")) {


                                            final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
                                            snack.setAction("OK", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    snack.dismiss();
                                                }
                                            });
                                            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                                            TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                                            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            snack.show();
                                            return;


                                        } else {

                                            NavChatList();


                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {
                                return;
                            }
                        })
                        .show();

            } else {

                String fecha = edtFecha.getText().toString().trim();

                if (fecha.isEmpty()) {


                    final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Complete su fecha de nacimiento", Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                        }
                    });
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = (TextView) snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();


                } else {

                    NavChatList();
                }
            }


        }else{

            if (drawer.isOpen()){


                drawer.closeDrawer(GravityCompat.START);

            }else {

                if (toolbar.getTitle().equals(getString(R.string.menu_chat)) | toolbar.getTitle().equals(getString(R.string.menu_usuarios))) {

                    if (!searchView.isIconified()) {
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

        switch (item.getItemId()) {

            case android.R.id.home:

                onBackPressed();
                return true;

            case R.id.action_settings:

                Ajustes(null);
                return true;

            case R.id.action_unlock:

                unlockUser();
                return true;

            case R.id.action_unhide_chats:

                unhideChats();
                return true;

            case R.id.action_favorites:

                mBottomNavigation.findViewById(R.id.navBottomFavorites).performClick();
                return true;

            case R.id.action_exit:


                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogApp));
                builder.setTitle("Salir");
                builder.setMessage("¿Desea abandonar " + groupName + "?");
                builder.setPositiveButton("Salir", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface builder, int id) {

                        exitGroup();

                    }
                });
                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface builder, int id) {
                        return;
                    }
                });
                builder.show();

                return true;

        }

        return true;

    }

    public void unhideChats() {

        final String type;

        if (toolbar.getTitle().equals(groupName)){
            type = chatWithUnknown;
        }else{
            type = chatWith;
        }

        ref_datos.child(user.getUid()).child(type).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    final ArrayList<String> listName = new ArrayList<>();
                    final ArrayList<String> listID = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        ChatWith chat = snapshot.getValue(ChatWith.class);
                        if (chat.getEstado().equals("delete")) {

                            String name = chat.getwUserName();
                            String user_id = chat.getwUserID();
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
                                .setSingleChoiceItems(listaName, itemSelected[0], new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                                        itemSelected[0] = selectedIndex;
                                    }
                                })


                                .setPositiveButton("Mostrar", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface builder, int selectedIndex) {


                                        ref_datos.child(user.getUid()).child(type).child(String.valueOf(listaID [itemSelected[0]])).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);

                                                if (Objects.equals(photo, Empty)){
                                                    dataSnapshot.getRef().removeValue();
                                                }else{
                                                    dataSnapshot.getRef().child("estado").setValue(type);
                                                }
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                            }
                                        });


                                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), listaName [itemSelected[0]] + " ya no está oculto", Snackbar.LENGTH_SHORT);
                                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        snack.show();

                                    }

                                })

                                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface builder, int id) {
                                        return;
                                    }
                                })

                                .setCancelable(false)
                                .show();

                    }else {
                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay chats ocultos", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();
                    }
                }else{
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay chats ocultos", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void unlockUser() {
        final String type;

        if (toolbar.getTitle().equals(groupName)){
            type = chatWithUnknown;
        }else{
            type = chatWith;
        }

        ref_datos.child(user.getUid()).child(type).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    final ArrayList<String> listName = new ArrayList<>();
                    final ArrayList<String> listID = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        ChatWith chat = snapshot.getValue(ChatWith.class);
                        if (chat.getEstado().equals("bloq")) {

                            String name = chat.getwUserName();
                            String user_id = chat.getwUserID();
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
                                .setSingleChoiceItems(listaName, itemSelected[0], new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                                        itemSelected[0] = selectedIndex;
                                    }
                                })


                                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface builder, int selectedIndex) {


                                        ref_datos.child(user.getUid()).child(type).child(String.valueOf(listaID [itemSelected[0]])).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);

                                                if (Objects.equals(photo, Empty)){
                                                    dataSnapshot.getRef().removeValue();
                                                }else{
                                                    dataSnapshot.getRef().child("estado").setValue(type);
                                                }
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                            }
                                        });


                                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "Desbloqueaste a " + listaName [itemSelected[0]], Snackbar.LENGTH_SHORT);
                                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        snack.show();

                                    }

                                })

                                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface builder, int id) {
                                        return;
                                    }
                                })

                                .setCancelable(false)
                                .show();

                    }else {
                        Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay usuarios bloqueados", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();
                    }
                }else{
                    Snackbar snack = Snackbar.make(findViewById(android.R.id.content), "No hay usuarios bloqueados", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void exitGroup() {
        inGroup = false;

        if (listenerGroupBadge != null) {
            ref_group_chat.child(groupName).removeEventListener(listenerGroupBadge);
        }
        if (listenerMsgUnreadBadge != null) {
            final Query query = ref_datos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
            query.removeEventListener(listenerMsgUnreadBadge);
        }
        if (valueEventListenerTitle != null) {
            ref_group_users.child(groupName).removeEventListener(valueEventListenerTitle); //Elimino el usuario
        }
        if (listenerGroupChat != null) {
            ref_group_chat.child(groupName).removeEventListener(listenerGroupChat);
        }


        ref_datos.child(user.getUid()).child(chatWithUnknown).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String key = snapshot.getKey();
                    ref_chat_unknown.child(user.getUid() + " <---> " + key).removeValue(); //Elimino mi chat con él
                    ref_chat_unknown.child(key + " <---> " + user.getUid()).removeValue(); //Elimino su chat conmigo
                    ref_datos.child(key).child(chatWithUnknown).child(user.getUid()).removeValue(); //Elimino su chat lista
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        ref_datos.child(user.getUid()).child(chatWithUnknown).removeValue(); //Elimino mis chat lista


        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");

        final ChatsGroup chatmsg = new ChatsGroup(
                "abandonó la sala",
                dateFormat3.format(Calendar.getInstance().getTime()),
                userName,
                user.getUid(),
                0,
                userType);
        ref_group_chat.child(groupName).push().setValue(chatmsg);


        //badgeDrawableGroup.setVisible(false);
        ref_group_users.child(groupName).child(user.getUid()).removeValue();

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

        if (toolbar.getTitle().equals(getString(R.string.menu_edit)) | toolbar.getTitle().equals(getString(R.string.ajustes)) | toolbar.getTitle().equals(getString(R.string.favoritos))) {

            onBackPressed();

        } else {

            drawer.closeDrawer(GravityCompat.START);
        }

    }
}