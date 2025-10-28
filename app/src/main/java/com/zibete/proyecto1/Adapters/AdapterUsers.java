package com.zibete.proyecto1.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
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
import com.zibete.proyecto1.SlideProfileActivity;
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


import static com.zibete.proyecto1.Constants.getDistanceMeters;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.MainActivity.latitud;
import static com.zibete.proyecto1.MainActivity.longitud;
import static com.zibete.proyecto1.MainActivity.ref_datos;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.ViewHolderAdapter> implements Filterable {

    private final List<Users> usersList;
    private final List<Users> usersListAll;
    private final Context context;
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterUsers(List<Users> usersList, List<Users> originalUsersList, Context context) {
        this.usersList = usersList;
        this.usersListAll = originalUsersList;
        this.context = context;
    }

    // --------------------- Filtro --------------------- //
    @Override public Filter getFilter() { return filterChats; }

    private final Filter filterChats = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Users> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(usersListAll);
            } else {
                String search = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (Users u : usersListAll) {
                    if (u.getNombre().toLowerCase(Locale.ROOT).contains(search)) filtered.add(u);
                }
            }
            FilterResults res = new FilterResults();
            res.values = filtered;
            return res;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            usersList.clear();
            usersList.addAll((Collection<? extends Users>) results.values);
            notifyDataSetChanged();
            UsuariosFragment.setScrollbar();
        }
    };

    // --------------------- ViewHolder --------------------- //
    public static class ViewHolderAdapter extends RecyclerView.ViewHolder {
        TextView tv_usuario1, tv_edad, distance, tv_desc, tv_estado;
        ImageView img_user, icon_conectado, icon_desconectado;
        ImageView goChat, favorite_on, bloq_me, bloq;
        LinearLayout linear_desc;
        CardView cardView;
        BlurView blurView;
        View glowBorder;

        public ViewHolderAdapter(@NonNull View itemView) {
            super(itemView);
            tv_usuario1 = itemView.findViewById(R.id.tv_usuario1);
            tv_edad = itemView.findViewById(R.id.tv_edad);
            distance = itemView.findViewById(R.id.distance);
            tv_desc = itemView.findViewById(R.id.tv_desc);
            tv_estado = itemView.findViewById(R.id.tv_estado);
            img_user = itemView.findViewById(R.id.image_user1);
            icon_conectado = itemView.findViewById(R.id.icon_conectado);
            icon_desconectado = itemView.findViewById(R.id.icon_desconectado);
            goChat = itemView.findViewById(R.id.goChat);
            favorite_on = itemView.findViewById(R.id.favorite_on);
            bloq_me = itemView.findViewById(R.id.bloq_me);
            bloq = itemView.findViewById(R.id.bloq);
            linear_desc = itemView.findViewById(R.id.linear_desc);
            cardView = itemView.findViewById(R.id.cardviewUsers);
            blurView = itemView.findViewById(R.id.blur_view);
            glowBorder = itemView.findViewById(R.id.glow_border);

            applyGlassEffect();
            startGlowIfAny();
        }

        private void startGlowIfAny() {
            if (glowBorder == null) return;
            Drawable bg = glowBorder.getBackground();
            if (bg instanceof AnimationDrawable) {
                AnimationDrawable anim = (AnimationDrawable) bg;
                anim.setEnterFadeDuration(600);
                anim.setExitFadeDuration(600);
                anim.start();
            }
        }

        /** Configura el efecto glass (BlurView 3.x con BlurTarget) */
        private void applyGlassEffect() {
            if (blurView == null) return;

            float radius = 16f;

            // El BlurTarget es el "contenedor base" a desenfocar
            BlurTarget blurTarget = new BlurTarget(itemView.getRootView().getContext());

            blurView.setupWith(blurTarget)
                    .setBlurRadius(radius)
                    .setBlurAutoUpdate(true);

            // sombreado suave para contraste
            blurView.setOverlayColor(0x1A000000);
        }
    }

    // --------------------- Create --------------------- //
    @NonNull
    @Override
    public ViewHolderAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_usuarios, parent, false);
        return new ViewHolderAdapter(v);
    }

    // --------------------- Bind con payloads --------------------- //
    @Override
    public void onBindViewHolder(@NonNull final ViewHolderAdapter holder, final int position, @NonNull List<Object> payloads) {
        final Users u = usersList.get(position);

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            loadUserCard(holder, u);
        } else {
            Bundle o = (Bundle) payloads.get(0);
            if (o.containsKey("distance")) loadUserCard(holder, u);
        }

        // Acciones
        holder.goChat.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("id_user", u.getID());
            v.getContext().startActivity(intent);
        });

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SlideProfileActivity.class);
            ArrayList<Users> extra = new ArrayList<>(usersList);
            Collections.reverse(extra);
            intent.putExtra("userList", extra);
            intent.putExtra("position", extra.indexOf(u));
            intent.putExtra("rotation", 0);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderAdapter holder, int position) { /* manejado arriba */ }

    // --------------------- Datos por card --------------------- //
    @SuppressLint("SetTextI18n")
    public void loadUserCard(@NonNull final ViewHolderAdapter h, Users u) {

        Glide.with(context).load(u.getFoto()).into(h.img_user);
        h.tv_usuario1.setText(u.getNombre());

        // Distancia
        Double dist = getDistanceMeters(latitud, longitud, u.getLatitud(), u.getLongitud());
        if (dist > 10000) {
            h.distance.setText("A " + new BigDecimal(dist / 1000).setScale(0, RoundingMode.HALF_UP) + " km");
        } else if (dist > 1000) {
            h.distance.setText("A " + new BigDecimal(dist / 1000).setScale(1, RoundingMode.HALF_UP) + " km");
        } else {
            h.distance.setText("A " + new BigDecimal(dist).setScale(0, RoundingMode.HALF_UP) + " m");
        }

        // Edad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && u.getBirthDay() != null && !u.getBirthDay().isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate birth = LocalDate.parse(u.getBirthDay(), fmt);
            h.tv_edad.setText(String.valueOf(Period.between(birth, LocalDate.now()).getYears()));
        }

        // Descripción
        if (u.getDescripcion() != null && !u.getDescripcion().isEmpty()) {
            h.tv_desc.setText(u.getDescripcion());
            h.linear_desc.setVisibility(View.VISIBLE);
        } else {
            h.linear_desc.setVisibility(View.GONE);
        }

        // Estado on/off
        new Constants().StateUser(context, u.getID(), h.icon_conectado, h.icon_desconectado, h.tv_estado, chatWith);

        // Favoritos
        ref_datos.child(user.getUid()).child("FavoriteList").child(u.getID())
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        h.favorite_on.setVisibility(snap.exists() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Bloqueado
        MainActivity.ref_datos.child(user.getUid()).child(chatWith).child(u.getID()).child("estado")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        h.bloq.setVisibility(Objects.equals(snap.getValue(String.class), "bloq") ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Me bloqueó
        MainActivity.ref_datos.child(u.getID()).child(chatWith).child(user.getUid()).child("estado")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        boolean blocked = Objects.equals(snap.getValue(String.class), "bloq");
                        h.bloq_me.setVisibility(blocked ? View.VISIBLE : View.GONE);
                        h.goChat.setVisibility(blocked ? View.GONE : View.VISIBLE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --------------------- Utilidades de lista --------------------- //
    public void addUser(Users u) {
        usersList.add(u);
        usersListAll.add(u);
        notifyItemInserted(usersList.size());
    }

    @Override public int getItemCount() { return usersList.size(); }

    public void updateDataUsers(ArrayList<Users> list2) {
        UsersDiffCallback diff = new UsersDiffCallback(list2, usersList);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(diff);
        result.dispatchUpdatesTo(this);
        usersList.clear();
        usersList.addAll(list2);
        usersListAll.clear();
        usersListAll.addAll(list2);
    }
}
