package com.zibete.proyecto1.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.utils.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.zibete.proyecto1.Constants.Empty;
import static com.zibete.proyecto1.Constants.FRAGMENT_ID_CHATGROUPLIST;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_chat_unknown;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_group_users;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;


public class AdapterChatGroupsLista extends RecyclerView.Adapter<AdapterChatGroupsLista.viewHolderAdapterChatGroupList> implements Filterable, View.OnCreateContextMenuListener {


    Context context;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ArrayList <ChatWith> chatArrayList;
    ArrayList <ChatWith> chatArrayListAll;


    private String menu1;
    private String menu2;
    private int position;



    public AdapterChatGroupsLista(ArrayList <ChatWith> chatArrayLista, Context context) {
        this.chatArrayList = chatArrayLista;
        this.chatArrayListAll = new ArrayList<>();
        this.context = context;
    }

    @Override
    public Filter getFilter() {
        return filterChats;
    }

    Filter filterChats = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            ArrayList <ChatWith> chatArrayListFiltered = new ArrayList<>();

            if (constraint.toString().isEmpty()){

                chatArrayListFiltered.addAll(chatArrayListAll);

            }else{

                for (ChatWith chat : chatArrayListAll){

                    String name = chat.getwUserName().toLowerCase(Locale.ROOT).trim();
                    String search = constraint.toString().toLowerCase(Locale.ROOT).trim();

                    if (name.contains(search)) {

                    chatArrayListFiltered.add(chat);

                    }

                }

            }

            Collections.sort(chatArrayListFiltered);
            FilterResults filterResults = new FilterResults();
            filterResults.values = chatArrayListFiltered;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {

            updateData((ArrayList<ChatWith>) filterResults.values);


        }
    };

    public void deleteChat(ChatWith chatWith) {

        int index = chatArrayList.indexOf(chatWith);

        if (index != -1) {

            chatArrayList.set(index, chatWith);

            notifyDataSetChanged();
            chatArrayList.remove(index);
            notifyItemRemoved(index);


        }
    }

    public static class viewHolderAdapterChatGroupList extends RecyclerView.ViewHolder {
        TextView tv_usuario, ult_msg, hora_ult_msg, nuevo_msg;
        ImageView img_user, notif_off;
        CardView cardView;
        TextView tv_estado;
        ImageView icon_conectado, icon_desconectado, checked, checked2;
        RelativeLayout relativeLayout;


        public viewHolderAdapterChatGroupList(@NonNull View itemView) {
            super(itemView);
            tv_usuario = itemView.findViewById(R.id.tv_usuario1);
            img_user = itemView.findViewById(R.id.image_user1);
            cardView = itemView.findViewById(R.id.cardview);
            tv_estado = itemView.findViewById(R.id.tv_estado);
            icon_conectado = itemView.findViewById(R.id.icon_conectado);
            icon_desconectado = itemView.findViewById(R.id.icon_desconectado);
            ult_msg = itemView.findViewById(R.id.ult_msg);
            hora_ult_msg = itemView.findViewById(R.id.hora_ult_msg);
            nuevo_msg = itemView.findViewById(R.id.nuevo_msg);
            notif_off = itemView.findViewById(R.id.notif_off);
            checked = itemView.findViewById(R.id.checked);
            checked2 = itemView.findViewById(R.id.checked2);
            relativeLayout = itemView.findViewById(R.id.relativeLayout);
        }
    }

    @NonNull
    @Override
    public viewHolderAdapterChatGroupList onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chatlista,parent,false);
        viewHolderAdapterChatGroupList holder = new viewHolderAdapterChatGroupList(v);
        v.setOnCreateContextMenuListener(this);


        return holder;
    }



    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapterChatGroupList holder, int position, List <Object> payloads) {

        final ChatWith wChat = chatArrayList.get(position);

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            loads(holder, wChat);

        } else {

            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("id")) {

                    loads(holder, wChat);

                }
            }
        }


        ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    cardView(holder, wChat);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        holder.tv_usuario.setText(wChat.getwUserName());


        Glide.with(context.getApplicationContext()).load(wChat.getwUserPhoto()).into(holder.img_user);


//Mostrar estado
        UserRepository.stateUser(context, wChat.getwUserID(), holder.icon_conectado, holder.icon_desconectado, holder.tv_estado, chatWithUnknown);


//Checked

        ref_datos.child(wChat.getwUserID()).child(chatWithUnknown).child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {


                    String sender = dataSnapshot.child("wEnvia").getValue(String.class);
                    int visto = dataSnapshot.child("wVisto").getValue(int.class);


                    if (sender.equals(user.getUid())) {

                        holder.relativeLayout.setVisibility(View.VISIBLE);

                        switch (visto) {


                            case 1:
                                holder.checked.setVisibility(View.VISIBLE);
                                holder.checked.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
                                holder.checked2.setVisibility(View.GONE);



                                break;

                            case 2:
                                holder.checked.setVisibility(View.VISIBLE);
                                holder.checked2.setVisibility(View.VISIBLE);
                                holder.checked.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
                                holder.checked2.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);


                                break;

                            case 3:

                                holder.checked.setVisibility(View.VISIBLE);
                                holder.checked2.setVisibility(View.VISIBLE);
                                holder.checked.setColorFilter(context.getResources().getColor(R.color.visto), PorterDuff.Mode.SRC_IN);
                                holder.checked2.setColorFilter(context.getResources().getColor(R.color.visto), PorterDuff.Mode.SRC_IN);

                                break;

                        }

                    }else{
                        holder.checked.setVisibility(View.GONE);
                        holder.checked2.setVisibility(View.GONE);
                        holder.relativeLayout.setVisibility(View.GONE);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).child("noVisto").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    final Integer noVistos = snapshot.getValue(Integer.class);

                    if (noVistos > 0) {

                        holder.nuevo_msg.setVisibility(View.VISIBLE);
                        holder.nuevo_msg.setText(noVistos.toString());

                    } else {
                        holder.nuevo_msg.setVisibility(View.GONE);
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





//Botón para ir al chat
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                ref_group_users.child(groupName).child(wChat.getwUserID()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()){

                            Intent intent = new Intent(v.getContext(), ChatActivity.class);
                            intent.putExtra("unknownName",wChat.getwUserName());
                            intent.putExtra("idUserUnknown",wChat.getwUserID());
                            v.getContext().startActivity(intent);

                        }else{

                            Toast.makeText(context, "Lo sentimos, " + wChat.getwUserName() + " ya no está disponible", Toast.LENGTH_SHORT).show();
                            ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).removeValue();

                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
        });

//Botón opciones
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                setPosition(holder.getAdapterPosition());

                if (holder.notif_off.getVisibility() == View.VISIBLE) {
                    menu2 = context.getString(R.string.mostrar_notif);
                } else {
                    menu2 = context.getString(R.string.silenciar);
                }


                if (holder.nuevo_msg.getVisibility() == View.VISIBLE) {
                    menu1 = context.getString(R.string.leido);
                } else {
                    menu1 = context.getString(R.string.noleido);
                }
                return false;
            }
        });




    }



    private void loads(@NonNull final viewHolderAdapterChatGroupList holder, final ChatWith wChat) {

        ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {


                    cardView(holder, wChat);

                    if (holder.cardView.getVisibility() == View.VISIBLE){

                        holder.ult_msg.setText(wChat.getwMsg());

                        if (wChat.getwMsg().equals(context.getResources().getString(R.string.photo_send))
                                || wChat.getwMsg().equals(context.getResources().getString(R.string.photo_received))
                                || wChat.getwMsg().equals(context.getResources().getString(R.string.audio_send))
                                || wChat.getwMsg().equals(context.getResources().getString(R.string.audio_received))){
                            holder.ult_msg.setTypeface(null, Typeface.ITALIC);
                        }else{
                            holder.ult_msg.setTypeface(null, Typeface.NORMAL);
                        }


                        LastMsg(holder, wChat);

                        setMyDoubleCheck(wChat);
                    }

                    ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).child("wVisto").setValue(2);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


    }

    public void setMyDoubleCheck(final ChatWith wChat) {

        ref_datos.child(user.getUid()).child(chatWithUnknown).child(wChat.getwUserID()).child("noVisto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    final Integer noVistos = dataSnapshot.getValue(Integer.class);
                    if (noVistos > 0) {

                        ref_chat_unknown.child(user.getUid() + " <---> " + wChat.getwUserID()).child("Mensajes").limitToLast(noVistos).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    setMyDoubleCheck(dataSnapshot);
                                }else{

                                    ref_chat_unknown.child(wChat.getwUserID() + " <---> " + user.getUid()).child("Mensajes").limitToLast(noVistos).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                setMyDoubleCheck(dataSnapshot);
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }
            }

            public void setMyDoubleCheck(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().child("visto").setValue(2);
                }
            }


            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    private void LastMsg(@NonNull viewHolderAdapterChatGroupList holder, ChatWith wChat) {
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        String date = wChat.getwDate().substring(0,10);

        if (date.equals(dateFormat.format(c.getTime()))) {
            holder.hora_ult_msg.setText(wChat.getwDate().substring(11,16));
        } else {

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);

            if (date.equals(dateFormat.format(calendar.getTime()))) {

                holder.hora_ult_msg.setText(context.getString(R.string.ayer));
            }else {
                holder.hora_ult_msg.setText(wChat.getwDate().substring(0,10));
            }
        }
    }



    private void cardView(@NonNull viewHolderAdapterChatGroupList holder, ChatWith wChat) {

        String state = wChat.getEstado();
        String photo = wChat.getwUserPhoto();

        if (state.equals(chatWithUnknown)){
            holder.cardView.setVisibility(View.VISIBLE);
            holder.notif_off.setVisibility(View.GONE);
        }else{

            if (photo.equals(Empty)){
                holder.cardView.setVisibility(View.GONE);

            }else {
                if (state.equals("silent")){
                    holder.cardView.setVisibility(View.VISIBLE);
                    holder.notif_off.setVisibility(View.VISIBLE);
                }
                if (state.equals("bloq") || state.equals("delete")){
                    holder.cardView.setVisibility(View.GONE);
                }
            }
        }

    }


    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapterChatGroupList holder, final int position) {

    }//Fin del onBindViewHolder




    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        menu.add(FRAGMENT_ID_CHATGROUPLIST, 1, position, menu1);
        menu.add(FRAGMENT_ID_CHATGROUPLIST, 2, position, menu2);
        menu.add(FRAGMENT_ID_CHATGROUPLIST, 3, position, R.string.bloquear);
        menu.add(FRAGMENT_ID_CHATGROUPLIST, 4, position, R.string.ocultar);
        menu.add(FRAGMENT_ID_CHATGROUPLIST, 5, position, R.string.eliminar);

    }


    public void setPosition(int position) {
        this.position = position;

    }

    @Override
    public int getItemCount() {
        return chatArrayList.size();
    }


    public void addChats(ChatWith wChats){
        chatArrayList.add(wChats);
        chatArrayListAll.add(wChats);
        //notifyItemInserted(chatArrayList.size());

        notifyDataSetChanged();
    }

    public void updateData(ArrayList <ChatWith> chatArrayList2){

        final ChatDiffCallback chatDiffCallback = new ChatDiffCallback (chatArrayList2, chatArrayList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(chatDiffCallback);
        diffResult.dispatchUpdatesTo(this);
        chatArrayList.clear();
        chatArrayList.addAll(chatArrayList2);


    }



}
