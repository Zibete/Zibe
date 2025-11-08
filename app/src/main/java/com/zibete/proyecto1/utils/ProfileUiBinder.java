package com.zibete.proyecto1.utils;

import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.groupName;
import static com.zibete.proyecto1.utils.FirebaseRefs.refChat;
import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.utils.FirebaseRefs.user;

import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.toRadians;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.adapters.AdapterPhotoReceived;
import com.zibete.proyecto1.ChatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class ProfileUiBinder {


    public static void getAge (final String id_user, final TextView age){

        refCuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String birthDay = dataSnapshot.child("birthDay").getValue(String.class);

                int edad = DateUtils.calcularEdad(birthDay);
                age.setText(String.valueOf(edad));


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    public static void getDistanceToUser (final String id_user, final TextView distanceUser){

        refCuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Double otherLatitude = dataSnapshot.child("latitud").getValue(Double.class);
                Double otherLongitude = dataSnapshot.child("longitud").getValue(Double.class);

                double distanceMeters = getDistanceMeters(UserRepository.latitude, UserRepository.longitude, otherLatitude, otherLongitude);

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
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    public static void setMenuProfile(final Context context, final String id_user, final FloatingActionButton subMenu_chatWithUnknown, FloatingActionButton subMenu_chatWith){

        refGroupUsers.child(groupName).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    final String unknownName = dataSnapshot.child("user_name").getValue(String.class);
                    subMenu_chatWithUnknown.setLabelText("Chat privado de: " + groupName);

                    subMenu_chatWithUnknown.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("unknownName", unknownName); //Nombre incógnito o UID
                            intent.putExtra("idUserUnknown", id_user); //Su UID
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.getApplicationContext().startActivity(intent);

                        }
                    });

                }else{
                    subMenu_chatWithUnknown.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        subMenu_chatWith.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("id_user",id_user);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.getApplicationContext().startActivity(intent);

            }
        });


    }

    public static void setFavorite (String user_id, final ImageView profile_favorite_on, final ImageView profile_favorite_off){

        refDatos.child(user.getUid()).child("FavoriteList").child(user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){
                    profile_favorite_on.setVisibility(View.VISIBLE);
                    profile_favorite_off.setVisibility(View.GONE);
                }else{
                    profile_favorite_on.setVisibility(View.GONE);
                    profile_favorite_off.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public static void setBloq (String user_id, final ImageView profile_bloc){// bindBlockStatus = vincular estado de bloqueo

        refDatos.child(user.getUid()).child(chatWith).child(user_id).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    profile_bloc.setVisibility(View.VISIBLE);
                } else {
                    profile_bloc.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public static void getBloqMe (String user_id, final ImageView profile_bloc_me){

        //Me Bloqueó
        refDatos.child(user_id).child(chatWith).child(user.getUid()).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    profile_bloc_me.setVisibility(View.VISIBLE);
                } else {
                    profile_bloc_me.setVisibility(View.GONE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    public static void addPhotoReceived (String id_user, final AdapterPhotoReceived adapterPhotoReceived, final LinearLayout linearPhotos){

        refChat.child(user.getUid() + " <---> " + id_user).child("Mensajes").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

                addPhoto(dataSnapshot, adapterPhotoReceived, linearPhotos);

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

        refChat.child(id_user + " <---> " + user.getUid()).child("Mensajes").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

                addPhoto(dataSnapshot, adapterPhotoReceived, linearPhotos);

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

    }

    public static void addPhoto(@NonNull DataSnapshot dataSnapshot, AdapterPhotoReceived adapterPhotoReceived, LinearLayout linearPhotos) {
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


    public static double getDistanceMeters(double myLatitude, double myLongitude, double otherLatitude, double otherLongitude) {

        double l1 = toRadians(myLatitude);
        double l2 = toRadians(otherLatitude);
        double g1 = toRadians(myLongitude);
        double g2 = toRadians(otherLongitude);

        double dist = acos(sin(l1) * sin(l2) + cos(l1) * cos(l2) * cos(g1 - g2));
        if(dist < 0) {
            dist = dist + Math.PI;
        }

        return (double) Math.round(dist * 6378100);
    }



}
