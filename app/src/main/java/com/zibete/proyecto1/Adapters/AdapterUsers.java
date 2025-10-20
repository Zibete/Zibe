package com.zibete.proyecto1.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.ChatActivity;
import com.zibete.proyecto1.Constants;
import com.zibete.proyecto1.MainActivity;
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.SlideProfileActivity;
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.zibete.proyecto1.Constants.chat;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.MainActivity.latitud;
import static com.zibete.proyecto1.MainActivity.longitud;
import static com.zibete.proyecto1.Constants.getDistanceMeters;
import static com.zibete.proyecto1.MainActivity.ref_datos;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.viewHolderAdapter> implements Filterable {

    List<Users> usersList;
    List<Users> usersListAll;
    List<Users> usersArrayList2;
    Context context;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    public AdapterUsers(List<Users> usersList, List<Users> originalUsersList, Context context) {
        this.usersList = usersList;
        this.usersListAll = originalUsersList;
        this.usersArrayList2 = new ArrayList<>();
        this.context = context;

    }


    @Override
    public Filter getFilter() {
        return filterChats;
    }

    Filter filterChats = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            ArrayList<Users> usersArrayListFiltered = new ArrayList<>();

            if (constraint.toString().isEmpty()){

                usersArrayListFiltered.addAll(usersListAll);

            }else{

                for (Users user : usersListAll){

                    String name = user.getNombre().toLowerCase(Locale.ROOT).trim();
                    String search = constraint.toString().toLowerCase(Locale.ROOT).trim();

                    if (name.contains(search)) {

                        usersArrayListFiltered.add(user);

                    }

                }

            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = usersArrayListFiltered;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {



            usersList.clear();
            usersList.addAll((Collection<? extends Users>) filterResults.values);
            notifyDataSetChanged();
            UsuariosFragment.setScrollbar();

        }
    };






    public class viewHolderAdapter extends RecyclerView.ViewHolder {
        TextView tv_usuario1, tv_edad, distance, tv_desc;
        ImageView img_user;
        CardView cardView;
        TextView tv_estado;
        ImageView icon_conectado, icon_desconectado;
        ImageView goChat, favorite_on, bloq_me, bloq;
        LinearLayout linear_desc;


        public viewHolderAdapter(@NonNull View itemView) {
            super(itemView);

            tv_usuario1 = itemView.findViewById(R.id.tv_usuario1);
            tv_edad = itemView.findViewById(R.id.tv_edad);
            img_user = itemView.findViewById(R.id.image_user1);
            cardView = itemView.findViewById(R.id.cardviewUsers);
            tv_estado = itemView.findViewById(R.id.tv_estado);
            icon_conectado = itemView.findViewById(R.id.icon_conectado);
            icon_desconectado = itemView.findViewById(R.id.icon_desconectado);
            goChat = itemView.findViewById(R.id.goChat);
            distance = itemView.findViewById(R.id.distance);
            favorite_on = itemView.findViewById(R.id.favorite_on);
            bloq_me = itemView.findViewById(R.id.bloq_me);
            bloq = itemView.findViewById(R.id.bloq);
            tv_desc = itemView.findViewById(R.id.tv_desc);
            linear_desc = itemView.findViewById(R.id.linear_desc);

        }


    }

    @NonNull
    @Override
    public viewHolderAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_usuarios, parent, false);

        viewHolderAdapter holder = new viewHolderAdapter(v);



        return holder;


    }

    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, final int position, List <Object> payloads) {

        final Users userss = usersList.get(position);

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

            loadUserCard(holder, userss);


        } else {

            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("distance")) {

                    loadUserCard(holder, userss);

                }
            }
        }



// BOTÓN PARA IR AL CHAT
        holder.goChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("id_user",userss.getID());
                v.getContext().startActivity(intent);

            }
        });
        // FIN BOTÓN PARA IR AL CHAT



        // BOTÓN PARA IR AL PERFIL
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {


                /*
                Intent intent = new Intent(v.getContext(), PerfilActivity.class);
                intent.putExtra("id_user",userss.getID());

                v.getContext().startActivity(intent);

                 */


                Intent intent = new Intent(context, SlideProfileActivity.class);


                ArrayList <Users> extraUserList = new ArrayList<>();

                extraUserList.addAll(usersList);

                Collections.reverse(extraUserList);




                intent.putExtra("userList", extraUserList);
                intent.putExtra("position",extraUserList.indexOf(userss));
                intent.putExtra("rotation",0);
                v.getContext().startActivity(intent);








            }
        });
        // FIN BOTÓN PARA IR AL PERFIL




    }//Fin del onBindViewHolder 2

    public void loadUserCard(@NonNull final viewHolderAdapter holder, Users userss) {
        //Nombre y foto
        Glide.with(context).load(userss.getFoto()).into(holder.img_user);
        holder.tv_usuario1.setText(userss.getNombre());


//Distancia
        Double distanceMeters = getDistanceMeters(latitud, longitud, userss.getLatitud(), userss.getLongitud());


//Como mostrar la distancia al user
        if (distanceMeters > 10000) {

            double distanceKm = distanceMeters / 1000;
            BigDecimal bd = new BigDecimal(distanceKm);
            bd = bd.setScale(0, RoundingMode.HALF_UP);
            holder.distance.setText("A " + bd + " kilómetros");

        } else if (distanceMeters > 1000) {

            double distanceKm = distanceMeters / 1000;
            BigDecimal bd = new BigDecimal(distanceKm);
            bd = bd.setScale(1, RoundingMode.HALF_UP);
            holder.distance.setText("A " + bd + " kilómetros");

        } else {

            BigDecimal bd = new BigDecimal(distanceMeters);
            bd = bd.setScale(0, RoundingMode.HALF_UP);
            holder.distance.setText("A " + bd + " metros");

        }


//Edad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String edad1 = userss.getBirthDay();
            if (!edad1.isEmpty()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate fechaNac = LocalDate.parse(userss.getBirthDay(), fmt);
                LocalDate ahora = LocalDate.now();

                Period periodo = Period.between(fechaNac, ahora);

                String edad = String.valueOf(periodo.getYears());
                holder.tv_edad.setText(edad);
            }
        }

//Desc
        if (!userss.getDescripcion().isEmpty()) {
            holder.tv_desc.setText(userss.getDescripcion());
            holder.linear_desc.setVisibility(View.VISIBLE);
        }else{
            holder.linear_desc.setVisibility(View.GONE);
        }
//Mostrar estado
        new Constants().StateUser(context, userss.getID(), holder.icon_conectado, holder.icon_desconectado, holder.tv_estado, chatWith);


/*
        MainActivity.ref_datos.child(userss.getID()).child("Estado").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {


                if (dataSnapshot.exists()) {

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    String estado = dataSnapshot.child("estado").getValue(String.class);
                    String fecha = dataSnapshot.child("fecha").getValue(String.class);
                    String hora = dataSnapshot.child("hora").getValue(String.class);

                    if (estado.equals(context.getString(R.string.conectado))) {
                        holder.icon_conectado.setVisibility(View.VISIBLE);
                        holder.icon_desconectado.setVisibility(View.GONE);
                        holder.tv_estado.setText(context.getString(R.string.enlinea));
                    } else {
                        holder.icon_conectado.setVisibility(View.GONE);
                        holder.icon_desconectado.setVisibility(View.VISIBLE);

                        if (fecha.equals(dateFormat.format(c.getTime()))) {
                            holder.tv_estado.setText(context.getString(R.string.ultVez) + " " + context.getString(R.string.hoy) + " " + context.getString(R.string.a_las) + " " + hora);
                        } else {

                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.DATE, -1);

                            if (fecha.equals(dateFormat.format(calendar.getTime()))) {
                                holder.tv_estado.setText(context.getString(R.string.ultVez) + " " + context.getString(R.string.ayer) + " " + context.getString(R.string.a_las) + " " + hora);
                            } else {
                                holder.tv_estado.setText(context.getString(R.string.ultVez) + " " + fecha + " " + context.getString(R.string.a_las) + " " + hora);
                            }
                        }
                    }
                } else {
                    holder.icon_conectado.setVisibility(View.GONE);
                    holder.icon_desconectado.setVisibility(View.VISIBLE);
                    holder.tv_estado.setText(context.getString(R.string.desconectado));
                }

            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });

 */


//FAVORITOS

        ref_datos.child(user.getUid()).child("FavoriteList").child(userss.getID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    holder.favorite_on.setVisibility(View.VISIBLE);
                }else{
                    holder.favorite_on.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
//Bloquedo
        MainActivity.ref_datos.child(user.getUid()).child(chatWith).child(userss.getID()).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    holder.bloq.setVisibility(View.VISIBLE);
                } else {
                    holder.bloq.setVisibility(View.GONE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });




        //Me Bloqueó
        MainActivity.ref_datos.child(userss.getID()).child(chatWith).child(user.getUid()).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (Objects.equals(dataSnapshot.getValue(String.class), "bloq")) {
                    holder.bloq_me.setVisibility(View.VISIBLE);
                    holder.goChat.setVisibility(View.GONE);
                } else {
                    holder.bloq_me.setVisibility(View.GONE);
                    holder.goChat.setVisibility(View.VISIBLE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });



    }


    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, int position) {

        //final Users userss = usersList.get(position);

    }//Fin del onBindViewHolder

    public void addUser(Users user){

        usersList.add(user);
        usersListAll.add(user);
        notifyItemInserted(usersList.size());
        notifyItemInserted(usersListAll.size());
    }





    @Override
    public int getItemCount() {
        return usersList.size();
    }


    public void updateDataUsers(ArrayList <Users> usersList2){

        final UsersDiffCallback usersDiffCallback = new UsersDiffCallback (usersList2, usersList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(usersDiffCallback);
        diffResult.dispatchUpdatesTo(this);
        usersList.clear();
        usersList.addAll(usersList2);

        usersListAll.clear();
        usersListAll.addAll(usersList2);

    }




}


