package com.zibete.proyecto1.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.zibete.proyecto1.model.Users;
import com.zibete.proyecto1.PerfilActivity;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.zibete.proyecto1.utils.FirebaseRefs.ref_cuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;

public class AdapterFavoriteUsers extends RecyclerView.Adapter<AdapterFavoriteUsers.viewHolderAdapter> {

    ArrayList <String> favoritesArrayList;
    ArrayList <String> favoritesArrayList2;
    Context context;
    ArrayList <Users> extraUserList = new ArrayList<>();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterFavoriteUsers(ArrayList<String> favoritesArrayList, Context context) {
        this.favoritesArrayList = favoritesArrayList;
        this.context = context;

    }

    public class viewHolderAdapter extends RecyclerView.ViewHolder {
        TextView tv_favorite_user, tv_favorite_age;
        ImageView image_favorite_user;
        CardView cardview_favorites;
        ImageView icon_conectado, icon_desconectado;
        LinearLayout linearCardFavorites;

        public viewHolderAdapter(@NonNull View itemView) {
            super(itemView);

            tv_favorite_user = itemView.findViewById(R.id.tv_favorite_user);
            tv_favorite_age = itemView.findViewById(R.id.tv_favorite_age);
            image_favorite_user = itemView.findViewById(R.id.image_favorite_user);
            cardview_favorites = itemView.findViewById(R.id.cardview_favorites);
            icon_conectado = itemView.findViewById(R.id.icon_conectado);
            icon_desconectado = itemView.findViewById(R.id.icon_desconectado);
            linearCardFavorites = itemView.findViewById(R.id.linearCardFavorites);

        }

    }

    @NonNull
    @Override
    public viewHolderAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_favorites, parent, false);
        viewHolderAdapter holder = new viewHolderAdapter(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, final int position, List <Object> payloads) {

        final String favoriteUser = favoritesArrayList.get(position);

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

            loadUserCard(holder, favoriteUser);

        } else {

            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("id")) {

                    loadUserCard(holder, favoriteUser);
                }
            }
        }

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, widthPixels/3);

        holder.linearCardFavorites.setLayoutParams(layoutParams);

        // IR AL PERFIL
        holder.cardview_favorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                Intent intent = new Intent(v.getContext(), PerfilActivity.class);
                intent.putExtra("id_user",favoriteUser);
                v.getContext().startActivity(intent);

            }
        });// FIN IR AL PERFIL

    }//Fin del onBindViewHolder 2

    public void loadUserCard(@NonNull final viewHolderAdapter holder, final String favoriteUser) {


        ref_cuentas.child(favoriteUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    Users this_user = dataSnapshot.getValue(Users.class);
                    extraUserList.add(this_user);

                    String birthDay = dataSnapshot.child("birthDay").getValue(String.class);
                    String foto = dataSnapshot.child("foto").getValue(String.class);
                    String nombre = dataSnapshot.child("nombre").getValue(String.class);

                    int age = DateUtils.calcularEdad(birthDay);

                    holder.tv_favorite_age.setText(String.valueOf(age));
                    holder.tv_favorite_user.setText(nombre);
                    Glide.with(context).load(foto).into(holder.image_favorite_user);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//Mostrar estado
        ref_datos.child(favoriteUser).child("Estado").addListenerForSingleValueEvent(new ValueEventListener() {
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
                    } else {
                        holder.icon_conectado.setVisibility(View.GONE);
                        holder.icon_desconectado.setVisibility(View.VISIBLE);

                    }
                } else {
                    holder.icon_conectado.setVisibility(View.GONE);
                    holder.icon_desconectado.setVisibility(View.VISIBLE);
                }

            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }


    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, int position) {


    }//Fin del onBindViewHolder

    public void addUser(String user){

        favoritesArrayList.add(user);
        notifyItemInserted(favoritesArrayList.size());
    }

    @Override
    public int getItemCount() {
        return favoritesArrayList.size();
    }

    public void updateDataUsers(ArrayList <String> usersList2){

        final FavoritesDiffCallback usersDiffCallback = new FavoritesDiffCallback (usersList2, favoritesArrayList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(usersDiffCallback);
        diffResult.dispatchUpdatesTo(this);

        favoritesArrayList.clear();
        favoritesArrayList.addAll(usersList2);

        favoritesArrayList.clear();
        favoritesArrayList.addAll(usersList2);

    }

}