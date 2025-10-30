package com.zibete.proyecto1.Adapters;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.MainActivity;
import com.zibete.proyecto1.POJOS.ChatsGroup;
import com.zibete.proyecto1.POJOS.Groups;
import com.zibete.proyecto1.POJOS.UserGroup;
import com.zibete.proyecto1.PageAdapterGroup;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.ui.GruposFragment;
import com.zibete.proyecto1.ui.Usuarios.UsuariosFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import eightbitlab.com.blurview.BlurView;

import static com.zibete.proyecto1.Constants.PUBLIC_GROUP;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.Constants.listenerGroupBadge;
import static com.zibete.proyecto1.Constants.listenerMsgUnreadBadge;
import static com.zibete.proyecto1.MainActivity.layoutSettings;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_group_chat;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_group_users;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_groups;
import static com.zibete.proyecto1.MainActivity.toolbar;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.editor;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.inGroup;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userDate;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userType;
import static com.zibete.proyecto1.utils.GlassEffect.applyGlassEffect;
import static com.zibete.proyecto1.utils.GlassEffect.startGlowIfAny;

public class AdapterGroups extends RecyclerView.Adapter<AdapterGroups.viewHolderAdapter> implements Filterable {

    List<Groups> groupsList;
    List<Groups> originalGroupsArrayList;
    Context context;
    BlurView blurView;
    View glowBorder;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterGroups(ArrayList<Groups> groupsArrayList, List<Groups> originalGroupsArrayList, Context context) {
        this.groupsList = groupsArrayList;
        this.originalGroupsArrayList = originalGroupsArrayList;
        this.context = context;
    }

    @Override
    public Filter getFilter() {
        return filterGroups;
    }

    Filter filterGroups = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Groups> groupsArrayListFiltered = new ArrayList<>();
            if (constraint.toString().isEmpty()) {
                groupsArrayListFiltered.addAll(originalGroupsArrayList);
            } else {
                for (Groups groups : originalGroupsArrayList) {
                    String name = groups.getName().toLowerCase(Locale.ROOT).trim();
                    String search = constraint.toString().toLowerCase(Locale.ROOT).trim();
                    if (name.contains(search)) {
                        groupsArrayListFiltered.add(groups);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = groupsArrayListFiltered;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            groupsList.clear();
            //noinspection unchecked
            groupsList.addAll((Collection<? extends Groups>) filterResults.values);
            notifyDataSetChanged();
            GruposFragment.setScrollbar();
        }
    };

    public class viewHolderAdapter extends RecyclerView.ViewHolder {
        CardView cardview_groups;              // Puede ser MaterialCardView en runtime
        LinearLayout linearCardGroup;
        TextView tv_title, tv_data_group, tv_number_persons;

        // Animación
        ValueAnimator borderAnimator;          // se guarda por ViewHolder para evitar duplicados
        GradientDrawable fallbackStrokeDrawable; // si no es MaterialCardView, usamos este borde

        public viewHolderAdapter(@NonNull View itemView) {
            super(itemView);
            tv_number_persons = itemView.findViewById(R.id.tv_number_persons);
            tv_data_group = itemView.findViewById(R.id.tv_data_group);
            tv_title = itemView.findViewById(R.id.tv_title);
            cardview_groups = itemView.findViewById(R.id.cardview_groups);
            linearCardGroup = itemView.findViewById(R.id.linearCardGroup);
            glowBorder = itemView.findViewById(R.id.glow_border);
            blurView = itemView.findViewById(R.id.blur_view);

            applyGlassEffect(blurView, itemView);
            startGlowIfAny(glowBorder);
        }

    }

    @NonNull
    @Override
    public viewHolderAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group, parent, false);
        return new viewHolderAdapter(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, int position, List<Object> payloads) {
        final Groups group = groupsList.get(position);

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            if (group.getCategory() == PUBLIC_GROUP) {
                loadGroupCard(holder, group);
            }
        } else {
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("users")) {
                    if (group.getCategory() == PUBLIC_GROUP) {
                        loadGroupCard(holder, group);
                    }
                }
            }
        }

        holder.cardview_groups.setOnClickListener(v -> {
            if (group.getCategory() == PUBLIC_GROUP) {
                goPublicGroup(group);
            }
        });

    }

    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapter holder, int position) {
        // vacío a propósito (usamos el de payloads)
    }

    // Utilidades
    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private int safeGetColor(int resId, int fallback) {
        try {
            return context.getColor(resId);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Integer[] toObjectArray(int[] colors) {
        Integer[] arr = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) arr[i] = colors[i];
        return arr;
    }

    // ---------------------------------------------
    // Carga de tarjeta
    // ---------------------------------------------
    public void loadGroupCard(@NonNull final viewHolderAdapter holder, Groups group) {
        holder.tv_title.setText(group.getName());
        holder.tv_data_group.setText(group.getData());
        holder.tv_data_group.setSelected(true);

        ref_group_users.child(group.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.tv_number_persons.setText(dataSnapshot.getChildrenCount() + "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ---------------------------------------------
    // Flujos existentes (sin cambios en lógica)
    // ---------------------------------------------
    public void goNewGroup() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewFilter = inflater.inflate(R.layout.dialog_go_new_group, null);

        ImageView img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);
        final Button btn_create_new_chat = viewFilter.findViewById(R.id.btn_create_new_chat);

        TextView name_user = viewFilter.findViewById(R.id.name_user);
        CircleImageView image_user = viewFilter.findViewById(R.id.image_user);
        final EditText edt_name_new_group = viewFilter.findViewById(R.id.edt_name_new_group);
        final EditText edt_data_new_group = viewFilter.findViewById(R.id.edt_data_new_group);

        name_user.setText(user.getDisplayName());
        Glide.with(context).load(user.getPhotoUrl()).into(image_user);
        btn_create_new_chat.setEnabled(false);

        edt_name_new_group.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_name_new_group.length() == 0) btn_create_new_chat.setEnabled(false);
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edt_name_new_group.length() != 0 && edt_data_new_group.length() != 0) {
                    btn_create_new_chat.setEnabled(true);
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (edt_name_new_group.length() == 0) btn_create_new_chat.setEnabled(false);
            }
        });

        edt_data_new_group.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_data_new_group.length() == 0) btn_create_new_chat.setEnabled(false);
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edt_data_new_group.length() != 0 && edt_name_new_group.length() != 0) {
                    btn_create_new_chat.setEnabled(true);
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (edt_data_new_group.length() == 0) btn_create_new_chat.setEnabled(false);
            }
        });

        final AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
        builder.setView(viewFilter);
        builder.setCancelable(true);

        final AlertDialog alertDialog = builder.show();
        btn_create_new_chat.setOnClickListener(v -> {
            final String newGroupName = edt_name_new_group.getText().toString();
            final String newGroupData = edt_data_new_group.getText().toString();

            ref_groups.child(newGroupName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Toast.makeText(context, "El nombre ya está en uso", Toast.LENGTH_SHORT).show();
                    } else {
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        Groups group = new Groups(
                                newGroupName,
                                newGroupData,
                                user.getUid(),
                                PUBLIC_GROUP,
                                0,
                                dateFormat.format(Calendar.getInstance().getTime()));
                        ref_groups.child(newGroupName).setValue(group);
                        goGroup(v, alertDialog, newGroupName, user.getDisplayName(), PUBLIC_GROUP);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        });

        img_cancel_dialog.setOnClickListener(v -> alertDialog.dismiss());
    }

    public void goPublicGroup(final Groups group) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewFilter = inflater.inflate(R.layout.dialog_go_group, null);

        ImageView img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);
        Button btn_start_chat1 = viewFilter.findViewById(R.id.btn_start_chat1);
        final Button btn_start_chat2 = viewFilter.findViewById(R.id.btn_start_chat2);
        TextView name_user = viewFilter.findViewById(R.id.name_user);
        TextView tv_chat = viewFilter.findViewById(R.id.tv_chat);
        CircleImageView image_user = viewFilter.findViewById(R.id.image_user);
        final EditText edt_nick = viewFilter.findViewById(R.id.edt_nick);

        name_user.setText(user.getDisplayName());
        Glide.with(context).load(user.getPhotoUrl()).into(image_user);
        tv_chat.setText(group.getName());
        btn_start_chat2.setEnabled(false);

        edt_nick.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (edt_nick.length() == 0) btn_start_chat2.setEnabled(false);
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edt_nick.length() != 0) btn_start_chat2.setEnabled(true);
            }
            @Override public void afterTextChanged(Editable s) {
                if (edt_nick.length() == 0) btn_start_chat2.setEnabled(false);
            }
        });

        final AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
        builder.setView(viewFilter);
        builder.setCancelable(true);

        final AlertDialog alertDialog = builder.show();
        btn_start_chat1.setOnClickListener(v -> goGroup(v, alertDialog, group.getName(), user.getDisplayName(), 1));

        btn_start_chat2.setOnClickListener(v -> ref_group_users.child(group.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String name = snapshot.child("user_name").getValue(String.class);
                    if (name != null && name.equals(edt_nick.getText().toString())) {
                        Toast.makeText(context, edt_nick.getText().toString() + " está en uso", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                goGroup(v, alertDialog, group.getName(), edt_nick.getText().toString(), 0);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        }));

        img_cancel_dialog.setOnClickListener(v -> alertDialog.dismiss());
    }

    public void goGroup(View v, AlertDialog alertDialog, final String groupName, String getUser, int type) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");

        userName = getUser;
        UsuariosFragment.groupName = groupName;
        inGroup = true;
        userType = type;
        userDate = dateFormat3.format(Calendar.getInstance().getTime());

        editor.putBoolean("inGroup", true);
        editor.putString("groupName", groupName);
        editor.putString("userName", getUser);
        editor.putInt("userType", type);
        editor.putString("userDate", dateFormat3.format(Calendar.getInstance().getTime()));
        editor.apply();

        final ChatsGroup chatmsg = new ChatsGroup(
                "se unió a la sala",
                userDate,
                userName,
                user.getUid(),
                0,
                userType);
        ref_group_chat.child(groupName).push().setValue(chatmsg);

        ref_group_chat.child(UsuariosFragment.groupName).addValueEventListener(listenerGroupBadge);

        final Query query = ref_datos.child(user.getUid()).child(chatWithUnknown).orderByChild("noVisto").startAt(1);
        query.addValueEventListener(listenerMsgUnreadBadge);

        toolbar.setVisibility(View.VISIBLE);
        layoutSettings.setVisibility(View.GONE);

        ((MainActivity) context).invalidateOptionsMenu();

        PageAdapterGroup newFragment = new PageAdapterGroup();
        AppCompatActivity activity = (AppCompatActivity) v.getContext();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, newFragment);

        toolbar.setTitle(groupName);

        ((MainActivity) context).invalidateOptionsMenu();
        Bundle data = new Bundle();
        newFragment.setArguments(data);

        transaction.commit();

        UserGroup userGroup = new UserGroup(user.getUid(), getUser, type);
        ref_group_users.getRef().child(groupName).child(user.getUid()).setValue(userGroup);
        ref_group_chat.child(groupName).addValueEventListener(listenerGroupBadge);

        alertDialog.dismiss();
    }

    @Override
    public int getItemCount() {
        return groupsList.size();
    }

    public void addGroup(Groups group) {
        groupsList.add(group);
        originalGroupsArrayList.add(group);
        notifyItemInserted(groupsList.size());
        notifyItemInserted(originalGroupsArrayList.size());
    }

    public void updateDataGroups(ArrayList<Groups> groupsArrayList2) {
        final GroupsDiffCallback groupsDiffCallback = new GroupsDiffCallback(groupsArrayList2, groupsList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(groupsDiffCallback);
        diffResult.dispatchUpdatesTo(this);
        groupsList.clear();
        groupsList.addAll(groupsArrayList2);

        groupsList.clear();
        groupsList.addAll(groupsArrayList2);
    }

    @Override
    public int getItemViewType(int position) {
        return PUBLIC_GROUP;
    }
}
