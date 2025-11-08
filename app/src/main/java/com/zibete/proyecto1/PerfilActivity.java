package com.zibete.proyecto1;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.adapters.AdapterPhotoReceived;
import com.zibete.proyecto1.utils.DateUtils;
import com.zibete.proyecto1.utils.FirebaseRefs;
import com.zibete.proyecto1.utils.ProfileUiBinder;
import com.zibete.proyecto1.utils.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;

import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.Constants.maxChatSize;
import static com.zibete.proyecto1.utils.FirebaseRefs.refChat;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.groupName;

public class PerfilActivity extends AppCompatActivity {

    ImageView ft_perfil;
    ImageView icon_conectado, icon_desconectado;
    TextView nameUser, tv_estado, desc, age, distanceUser;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    LinearLayout linearImageActivity, linearPhotos, linear_desc;
    RecyclerView recyclerPhotos;
    String foto;
    ArrayList <String> photoList;
    ArrayList <String> receivedPhotos;
    AdapterPhotoReceived adapterPhotoReceived;
    ImageView perfil_favorite_on, perfil_favorite_off, perfil_bloq, perfil_bloq_me;
    String id_user, nombre, unknownName;
    ProgressBar loadingPhoto;

    FloatingActionMenu floatingActionMenu;
    com.github.clans.fab.FloatingActionButton subMenu_chatWith, subMenu_chatWithUnknown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);


        floatingActionMenu = findViewById(R.id.floatingActionMenu);
        subMenu_chatWith = findViewById(R.id.subMenu_chatWith);
        subMenu_chatWithUnknown = findViewById(R.id.subMenu_chatWithUnknown);


        id_user = getIntent().getExtras().getString("id_user");
        linear_desc = findViewById(R.id.linear_desc);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true);
        layoutManager.setStackFromEnd(true);
        recyclerPhotos = findViewById(R.id.recyclerPhotos);
        recyclerPhotos.setLayoutManager(layoutManager);

        photoList = new ArrayList<>();
        receivedPhotos = new ArrayList<>();
        adapterPhotoReceived = new AdapterPhotoReceived(receivedPhotos, maxChatSize, getApplicationContext());

        recyclerPhotos.setAdapter(adapterPhotoReceived);

        linearImageActivity = findViewById(R.id.linearImageActivity);
        linearPhotos = findViewById(R.id.linearPhotos);
        distanceUser = findViewById(R.id.distanceUser);
        ft_perfil = findViewById(R.id.ftPerfil);
        nameUser = findViewById(R.id.nameUser);
        desc = findViewById(R.id.desc);
        age = findViewById(R.id.edad);
        tv_estado = findViewById(R.id.tv_estado);
        icon_conectado = findViewById(R.id.icon_conectado);
        icon_desconectado = findViewById(R.id.icon_desconectado);
        perfil_favorite_off = findViewById(R.id.perfil_favorite_off);
        perfil_favorite_on = findViewById(R.id.perfil_favorite_on);
        perfil_bloq = findViewById(R.id.perfil_bloq);
        perfil_bloq_me = findViewById(R.id.perfil_bloq_me);
        loadingPhoto = findViewById(R.id.loadingPhoto);

        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        floatingActionMenu.setClosedOnTouchOutside(true);


        refGroupUsers.child(groupName).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    unknownName = dataSnapshot.child("user_name").getValue(String.class);
                    subMenu_chatWithUnknown.setLabelText("Chat privado de: " + groupName);
                    //subMenu_chatWithUnknown.setVisibility(View.VISIBLE);
                }else{
                    subMenu_chatWithUnknown.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        subMenu_chatWithUnknown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(PerfilActivity.this, ChatActivity.class);
                intent.putExtra("unknownName", unknownName); //Nombre incógnito o UID
                intent.putExtra("idUserUnknown", id_user); //Su UID
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

            }
        });

        subMenu_chatWith.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(PerfilActivity.this, ChatActivity.class);
                intent.putExtra("id_user",id_user);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);


            }
        });




//Favoritos
        refDatos.child(user.getUid()).child("FavoriteList").child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){
                    perfil_favorite_on.setVisibility(View.VISIBLE);
                    perfil_favorite_off.setVisibility(View.GONE);
                }else{
                    perfil_favorite_on.setVisibility(View.GONE);
                    perfil_favorite_off.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


//Bloquedo
        FirebaseRefs.refDatos.child(user.getUid()).child("ChatWith").child(id_user).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    perfil_bloq.setVisibility(View.VISIBLE);
                } else {
                    perfil_bloq.setVisibility(View.GONE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });




        //Me Bloqueó
        FirebaseRefs.refDatos.child(id_user).child("ChatWith").child(user.getUid()).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    perfil_bloq_me.setVisibility(View.VISIBLE);
                } else {
                    perfil_bloq_me.setVisibility(View.GONE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });



        //FAVORITE LIST
        perfil_favorite_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                refDatos.child(user.getUid()).child("FavoriteList").child(id_user).setValue(id_user);
                perfil_favorite_on.setVisibility(View.VISIBLE);
                perfil_favorite_off.setVisibility(View.GONE);
                Toast.makeText(PerfilActivity.this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();

            }
        });

        perfil_favorite_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {


                refDatos.child(user.getUid()).child("FavoriteList").child(id_user).removeValue();
                perfil_favorite_on.setVisibility(View.GONE);
                perfil_favorite_off.setVisibility(View.VISIBLE);
                Toast.makeText(PerfilActivity.this, "Quitado de favoritos", Toast.LENGTH_SHORT).show();



            }
        });









        FirebaseRefs.refCuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String birthDay = dataSnapshot.child("birthDay").getValue(String.class);
                foto = dataSnapshot.child("foto").getValue(String.class);
                nombre = dataSnapshot.child("nombre").getValue(String.class);
                String descripcion = dataSnapshot.child("descripcion").getValue(String.class);
                Double otherLatitude = dataSnapshot.child("latitud").getValue(Double.class);
                Double otherLongitude = dataSnapshot.child("longitud").getValue(Double.class);

                int edad = DateUtils.calcularEdad(birthDay);
                age.setText(String.valueOf(edad));

                //Calcular distancia
                double distanceMeters = ProfileUiBinder.getDistanceMeters(
                        UserRepository.latitude, UserRepository.longitude, otherLatitude, otherLongitude);

                //Como mostrar la distancia al user
                if (distanceMeters > 10000) {

                    double distanceKm = distanceMeters / 1000;
                    BigDecimal bd = new BigDecimal(distanceKm);
                    bd = bd.setScale(0, RoundingMode.HALF_UP);
                    distanceUser.setText("A " + bd + " kilómetros");

                } else if (distanceMeters > 1000) {

                    double distanceKm = distanceMeters / 1000;
                    BigDecimal bd = new BigDecimal(distanceKm);
                    bd = bd.setScale(1, RoundingMode.HALF_UP);
                    distanceUser.setText("A " + bd + " kilómetros");

                } else {

                    BigDecimal bd = new BigDecimal(distanceMeters);
                    bd = bd.setScale(0, RoundingMode.HALF_UP);
                    distanceUser.setText("A " + bd + " metros");

                }


                photoList.add(foto);

                nameUser.setText(nombre);
                desc.setText(descripcion);


                if (!descripcion.isEmpty()) {
                    linear_desc.setVisibility(View.VISIBLE);
                    desc.setText(descripcion);
                }else{
                    linear_desc.setVisibility(View.GONE);
                }



               // Glide.with(PerfilActivity.this).load(foto).into(ft_perfil);

                loadingPhoto.setVisibility(View.VISIBLE);
                Glide.with(PerfilActivity.this)
                        .load(foto)
                        .apply(new RequestOptions().transform( new CenterCrop(), new RoundedCorners(35)))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }

                        })
                        .into(ft_perfil);

            } //fin

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);

        int height = dimension.heightPixels;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height-(height/4));

        linearImageActivity.setLayoutParams(layoutParams);
        linearImageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(PerfilActivity.this, SlidePhotoActivity.class);
                intent.putExtra("photoList",photoList);
                intent.putExtra("position",0);
                intent.putExtra("rotation",180);
                startActivity(intent);
            }
        });







        UserRepository.stateUser(getApplicationContext(), id_user, icon_conectado, icon_desconectado, tv_estado, chatWith);

        ft_perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



            }
        });





        refChat.child(user.getUid() + " <---> " + id_user).child("Mensajes").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

                addChat(dataSnapshot);

            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        refChat.child(id_user+ " <---> " + user.getUid()).child("Mensajes").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

                addChat(dataSnapshot);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });





    }// FIN OnCreate


    private void addChat(@NonNull DataSnapshot dataSnapshot) {

        if (dataSnapshot.exists()) {

            Integer type = dataSnapshot.child("type").getValue(Integer.class);
            String sender = dataSnapshot.child("envia").getValue(String.class);
            String string = dataSnapshot.child("mensaje").getValue(String.class);

            if (!sender.equals(user.getUid())) {

                if (type == PHOTO | type == PHOTO_SENDER_DLT) {

                    adapterPhotoReceived.addString(string);
                    linearPhotos.setVisibility(View.VISIBLE);

                }
            }
        }
    }




    @Override
    protected void onPause() {
        super.onPause();
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserRepository.setUserOnline(getApplicationContext(), user.getUid());
    }





    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_activity, menu);


        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_silent = menu.findItem(R.id.action_silent);
        final MenuItem action_notif = menu.findItem(R.id.action_notif);
        final MenuItem action_bloq = menu.findItem(R.id.action_bloq);
        final MenuItem action_desbloq = menu.findItem(R.id.action_desbloq);
        final MenuItem action_delete = menu.findItem(R.id.action_delete);


        action_delete.setVisible(true);


        //Para el menu1
        FirebaseRefs.refDatos.child(user.getUid()).child("ChatWith").child(id_user).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    String state = dataSnapshot.getValue(String.class);

                    if(Objects.equals(state, "silent")){
                        action_silent.setVisible(false);
                        action_notif.setVisible(true);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if(Objects.equals(state, "chat")){
                        action_silent.setVisible(true);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if(Objects.equals(state, "bloq")){
                        action_silent.setVisible(false);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(true);
                        action_bloq.setVisible(false);
                    }
                    if(Objects.equals(state, "delete")){
                        action_silent.setVisible(true);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }

                }else{

                    action_silent.setVisible(true);
                    action_notif.setVisible(false);
                    action_desbloq.setVisible(false);
                    action_bloq.setVisible(true);

                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        View view = findViewById(android.R.id.content);
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;

        } else if (id == R.id.action_silent) { // Silenciar notificaciones
            UserRepository.Silent(nombre, id_user, chatWith);
            Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_notif) {
            UserRepository.Silent(nombre, id_user, chatWith);
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_bloq) { // Bloquear
            UserRepository.setBlockUser(this, nombre, id_user, view, chatWith);

        } else if (id == R.id.action_desbloq) { // Desbloquear
            UserRepository.setUnBlockUser(this, id_user, nombre, view, chatWith);
        } else if (id == R.id.action_delete) { // Eliminar
            new Constants().DeleteChat(this, id_user, nombre, view, chatWith);
        }

        return super.onOptionsItemSelected(item);
    }









}