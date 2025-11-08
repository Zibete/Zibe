package com.zibete.proyecto1.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.ChatActivity;
import com.zibete.proyecto1.model.UserGroup;
import com.zibete.proyecto1.PerfilActivity;
import com.zibete.proyecto1.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupData;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.groupName;

public class AdapterGroupUsers extends RecyclerView.Adapter<AdapterGroupUsers.viewHolderAdapter> implements Filterable {

    List <UserGroup> groupUsersList;
    List <UserGroup> groupOriginalUsersList;
    //ArrayList<UserGroup>groupUsersArrayList2;
    Context context;
    String search;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    public AdapterGroupUsers(ArrayList<UserGroup> groupUsersList, List<UserGroup>groupOriginalUsersList, Context context) {
        this.groupUsersList = groupUsersList;
        this.groupOriginalUsersList = groupOriginalUsersList;
        //this.groupUsersArrayList2 = new ArrayList<>();
        this.context = context;

    }

    @Override
    public Filter getFilter() {
        return filterGroups;
    }

    Filter filterGroups = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            ArrayList<UserGroup> groupUserListFiltered = new ArrayList<>();

            if (constraint.toString().isEmpty()){

                groupUserListFiltered.addAll(groupOriginalUsersList);

            }else{

                for (UserGroup u : groupOriginalUsersList){

                    String name = u.getUserName().toLowerCase(Locale.ROOT).trim();
                    search = constraint.toString().toLowerCase(Locale.ROOT).trim();

                    if (name.contains(search)) {

                        groupUserListFiltered.add(u);

                    }

                }

            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = groupUserListFiltered;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {



            groupUsersList.clear();
            groupUsersList.addAll((Collection<? extends UserGroup>) filterResults.values);
            notifyDataSetChanged();
            //PersonsChatGroupFragment.setScrollbar();

        }
    };


    public class viewHolderAdapter extends RecyclerView.ViewHolder {

        CircleImageView image_user_group;
        TextView name_user_group;
        CardView cardviewUserGroup;
        ImageView masked, creator;
        LinearLayout linearCardPerson;

        public viewHolderAdapter(@NonNull View itemView) {
            super(itemView);

            image_user_group = itemView.findViewById(R.id.image_user_group);
            name_user_group = itemView.findViewById(R.id.name_user_group);
            cardviewUserGroup = itemView.findViewById(R.id.cardviewUserGroup);
            masked = itemView.findViewById(R.id.masked);
            creator = itemView.findViewById(R.id.creator);
            linearCardPerson = itemView.findViewById(R.id.linearCardPerson);
        }


    }

    @NonNull
    @Override
    public viewHolderAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_groups_users, parent, false);
        viewHolderAdapter holder = new viewHolderAdapter(v);
        return holder;

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, final int position, List <Object> payloads) {

        final UserGroup groupUser = groupUsersList.get(position);
/*
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

            loadUserCard(holder, groupUser.getUser_name());

        } else {

            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("diff")) {

                    loadUserCard(holder, groupUser.getUser_name());

                }
            }
        }

 */


        if (groupUser.getUserId().equals(user.getUid())){
            holder.cardviewUserGroup.setCardBackgroundColor(context.getResources().getColor(R.color.accent_transparent));
        }else{
            holder.cardviewUserGroup.setCardBackgroundColor(context.getResources().getColor(R.color.zibe_night_start));
        }


        refGroupData.child(groupName).child("id_creator").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String creator = dataSnapshot.getValue(String.class);

                if (groupUser.getUserId().equals(creator)){

                    holder.creator.setVisibility(View.VISIBLE);

                }else{

                    holder.creator.setVisibility(View.GONE);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });







        refCuentas.child(groupUser.getUserId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    String name = dataSnapshot.child("nombre").getValue(String.class);
                    String foto = dataSnapshot.child("foto").getValue(String.class);


                    if (groupUser.getType() == 0) {

                        Glide.with(context.getApplicationContext()).load(context.getString(R.string.URL_PHOTO_DEF)).into(holder.image_user_group);
                        holder.name_user_group.setText(groupUser.getUserName());
                        holder.masked.setVisibility(View.VISIBLE);

                    }else{

                    Glide.with(context.getApplicationContext()).load(foto).into(holder.image_user_group);
                    holder.name_user_group.setText(name);
                    holder.masked.setVisibility(View.GONE);

                    }



                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





        final GestureDetector gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                if (!groupUser.getUserId().equals(user.getUid())) {

                    Intent intent = new Intent(context, ChatActivity.class);

                    intent.putExtra("unknownName", groupUser.getUserName()); //Nombre incógnito o UID
                    intent.putExtra("idUserUnknown", groupUser.getUserId()); //Su UID

                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    context.startActivity(intent);

                }

                return true;
            }


            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {

                if (!groupUser.getUserId().equals(user.getUid())) {

                    if (groupUser.getType() == 1){

                        Intent intent = new Intent(context, PerfilActivity.class);
                        intent.putExtra("id_user", groupUser.getUserId());
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        context.startActivity(intent);




                    }else{
                        Toast.makeText(context, "Usuario incógnito", Toast.LENGTH_SHORT).show();
                    }
                }

                return false;
            }



        });

        holder.linearCardPerson.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                //...

                return false;
            }
        });

       holder.linearCardPerson.setOnTouchListener(new View.OnTouchListener() {
           @Override
           public boolean onTouch(View v, MotionEvent event) {

               gd.onTouchEvent(event);

               return false;
           }
       });





    }



    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, int position) {

        //final Users userss = usersList.get(position);

    }//Fin del onBindViewHolder

    public void addUser(UserGroup groupUser){

        groupUsersList.add(groupUser);
        Collections.sort(groupUsersList);
        notifyDataSetChanged();

        groupOriginalUsersList.add(groupUser);
        notifyItemInserted(groupUsersList.size());
        notifyItemInserted(groupOriginalUsersList.size());


    }


    public void removeUser (UserGroup groupUser) {

        int indice = groupUsersList.indexOf(groupUser);

        if (indice != -1) {

            groupUsersList.set(indice, groupUser);

            notifyDataSetChanged();
            groupUsersList.remove(indice);
            notifyItemRemoved(indice);

        }


    }


    @Override
    public int getItemCount() {
        return groupUsersList.size();
    }

/*
    public void updateDataGroupUsers(ArrayList <UserGroup> groupUsersArrayList2){

        final GroupUsersDiffCallback groupUsersDiffCallback = new GroupUsersDiffCallback (groupUsersArrayList2, groupUsersArrayList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(groupUsersDiffCallback);
        diffResult.dispatchUpdatesTo(this);
        groupUsersArrayList.clear();
        groupUsersArrayList.addAll(groupUsersArrayList2);

        groupOriginalUsersList.clear();
        groupOriginalUsersList.addAll(groupUsersArrayList2);



    }

 */




}


