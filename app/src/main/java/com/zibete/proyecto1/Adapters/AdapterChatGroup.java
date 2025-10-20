package com.zibete.proyecto1.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.ChatActivity;
import com.zibete.proyecto1.POJOS.ChatsGroup;
import com.zibete.proyecto1.PerfilActivity;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.zibete.proyecto1.Constants.MSG;
import static com.zibete.proyecto1.Constants.MSG_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.MSG_SENDER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;
import static com.zibete.proyecto1.MainActivity.ref_datos;
import static com.zibete.proyecto1.MainActivity.ref_group_users;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;

public class AdapterChatGroup extends RecyclerView.Adapter<AdapterChatGroup.viewHolderAdapterChat> implements View.OnCreateContextMenuListener{


    List<ChatsGroup> msgList;
    ArrayList <String> photoList = new ArrayList<>();
    Context context;
    private int position;
    private String menu1;
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    public static final int MSG_TYPE_MID = 2;
    private Integer maxSize;

    String id_user;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterChatGroup(ArrayList<ChatsGroup> msgList, Integer maxSize, Context context) {
        this.msgList = msgList;
        this.maxSize = maxSize;
        this.context = context;

    }


    public void addChat(final ChatsGroup chats) {

        if (msgList.size() > maxSize) {
            msgList.remove(0);
            notifyItemRemoved(0);
        }
        msgList.add(chats);
        notifyItemInserted(msgList.size());

        if (chats.getType_msg() == PHOTO | chats.getType_msg() == PHOTO_RECEIVER_DLT | chats.getType_msg() == PHOTO_SENDER_DLT) {

            if (photoList.size() > maxSize) {
                photoList.remove(0);
                notifyItemRemoved(0);
            }
            photoList.add(chats.getMensaje());

        }


    }


    @NotNull
    @Override
    public viewHolderAdapterChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == MSG_TYPE_MID) {

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notif,parent,false);
            viewHolderAdapterChat holder = new viewHolderAdapterChat(v);
            v.setOnCreateContextMenuListener(this);

            return holder;

        }else{

            if (viewType == MSG_TYPE_RIGHT){

                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_msg_right,parent,false);
                viewHolderAdapterChat holder = new viewHolderAdapterChat(v);
                v.setOnCreateContextMenuListener(this);

                return holder;
            }else{

                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_msg_left,parent,false);
                viewHolderAdapterChat holder = new viewHolderAdapterChat(v);
                v.setOnCreateContextMenuListener(this);

                return holder;
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapterChat holder, final int position) {


        final ChatsGroup chats = msgList.get(position);


        holder.bindView(chats);




        if (chats.getType_msg() != 0) {
            final GestureDetector gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {

                    if (!chats.getID().equals(user.getUid())) {

                        ref_group_users.child(groupName).child(chats.getID()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()) {//Si el ID está en el chat

                                    String thisname = dataSnapshot.child("user_name").getValue(String.class);

                                    if (dataSnapshot.child("type").getValue(int.class) == 0) {

                                        if (thisname.equals(chats.getName())) {//Si el nombre es el mismo

                                            Intent intent = new Intent(context, ChatActivity.class);
                                            intent.putExtra("unknownName", chats.getName()); //Nombre incógnito o UID
                                            intent.putExtra("idUserUnknown", chats.getID()); //Su UID

                                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                            context.startActivity(intent);

                                        } else {

                                            noDisponible(chats);

                                        }


                                    } else {

                                        if (chats.getType_user() == 1) {

                                            Intent intent = new Intent(context, ChatActivity.class);
                                            intent.putExtra("unknownName", chats.getName()); //Nombre incógnito o UID
                                            intent.putExtra("idUserUnknown", chats.getID()); //Su UID

                                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                            context.startActivity(intent);
                                        } else {

                                            noDisponible(chats);

                                        }

                                    }

                                } else {

                                    noDisponible(chats);

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    return true;
                }

                public void noDisponible(final ChatsGroup chats) {

                    String text;

                    if (chats.getType_user() == 0) {
                        text = "Lo sentimos, " + chats.getName() + " ya no está disponible";
                    } else {
                        text = "Lo sentimos, " + chats.getName() + " ya no está en este chat. Encuéntralo en Personas";
                    }


                    final Snackbar snack = Snackbar.make(holder.linear_mensaje_msg, text, Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();


                    ref_datos.child(user.getUid()).child(chatWithUnknown).child(chats.getID()).removeValue();
                }


                @Override
                public boolean onSingleTapConfirmed(final MotionEvent event) {

                    if (chats.getType_user() == 0) {

                        final Snackbar snack = Snackbar.make(holder.linear_mensaje_msg, "Perfil incógnito", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();

                    } else {

                        if (!chats.getID().equals(user.getUid())) {
                            Intent intent = new Intent(context, PerfilActivity.class);
                            intent.putExtra("id_user", chats.getID());
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            context.startActivity(intent);
                        }


                    }

                    return false;
                }

            });


            holder.linearCardMsg.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    return gd.onTouchEvent(event);
                }
            });


            holder.linearCardMsg.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    //...

                    return false;
                }
            });

        }

    }//Fin del onBindViewHolder





    public void setPosition(int position) {
        this.position = position;

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        //menu.add(UNIQUE_FRAGMENT_GROUP_ID, 1, position, R.string.eliminar);

    }



    @Override
    public int getItemCount() {
        return msgList.size();
    }




    public class viewHolderAdapterChat extends RecyclerView.ViewHolder {


        LinearLayout linear_mensaje_msg, linear_mensaje_pic;
        CardView linearCardMsg;
        CoordinatorLayout selectedItem;
        ImageView img_pic;
        TextView hora, tv_msg, name_user;
        CircleImageView img_user;
        ImageView checked;
        ImageView checked2;
        ProgressBar loadingPhoto;


        private final View item;




        public viewHolderAdapterChat(@NonNull View itemView) {
            super(itemView);

            this.item = itemView;

        }




        public void bindView(final ChatsGroup chats){

            if (chats.getType_msg() != 0){

                checked = itemView.findViewById(R.id.checked);
                checked2 = itemView.findViewById(R.id.checked2);
                selectedItem = itemView.findViewById(R.id.selectedItem);
                loadingPhoto = itemView.findViewById(R.id.loadingPhoto);
                img_pic = itemView.findViewById(R.id.img_pic);
                linear_mensaje_msg = itemView.findViewById(R.id.linear_mensaje_msg);
                linear_mensaje_pic = itemView.findViewById(R.id.linear_mensaje_pic);

            }


            tv_msg = itemView.findViewById(R.id.tv_msg);
            linearCardMsg = itemView.findViewById(R.id.linearCardMsg);
            hora = itemView.findViewById(R.id.hora_msg);
            img_user = itemView.findViewById(R.id.img_user);
            name_user = itemView.findViewById(R.id.name_user);
            hora.setText(chats.getDate().substring(11,16));

            if (chats.getType_msg() == PHOTO | chats.getType_msg() == PHOTO_RECEIVER_DLT | chats.getType_msg() == PHOTO_SENDER_DLT){

                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) context
                        .getSystemService(Context.WINDOW_SERVICE);
                windowManager.getDefaultDisplay().getMetrics(metrics);

                int widthPixels = metrics.widthPixels;

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        widthPixels/4, widthPixels/4);

                linear_mensaje_pic.setLayoutParams(layoutParams);


                linear_mensaje_pic.setVisibility(View.VISIBLE);
                linear_mensaje_msg.setVisibility(View.GONE);

                Glide.with(context).load(chats.getMensaje()).into(img_pic);


                loadingPhoto.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chats.getMensaje())
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
                        .into(img_pic);


                img_pic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {



                        Intent intent = new Intent(context, SlidePhotoActivity.class);
                        intent.putExtra("photoList", photoList);
                        intent.putExtra("position", photoList.indexOf(chats.getMensaje()));
                        intent.putExtra("rotation", 0);
                        v.getContext().startActivity(intent);


                    }
                });



            }

            if (chats.getType_msg() == MSG | chats.getType_msg() == MSG_RECEIVER_DLT | chats.getType_msg() == MSG_SENDER_DLT){
                linear_mensaje_pic.setVisibility(View.GONE);
                linear_mensaje_msg.setVisibility(View.VISIBLE);
                tv_msg.setText(chats.getMensaje());
            }

            if (chats.getType_msg() == 0){
                tv_msg.setText(chats.getMensaje());
            }

            if (chats.getType_user() == 0){

                if (chats.getType_msg() == 0){
                    name_user.setText(chats.getName());
                }else {
                    name_user.setText(chats.getName() + ":");
                }
                Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(img_user);

            }else{

                ref_cuentas.child(chats.getID()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            String name = dataSnapshot.child("nombre").getValue(String.class);
                            String foto = dataSnapshot.child("foto").getValue(String.class);

                            if (chats.getType_msg() == 0){
                                name_user.setText(name);
                            }else {
                                name_user.setText(name + ":");
                            }
                            Glide.with(context).load(foto).into(img_user);
                        }else{
                            name_user.setText(chats.getName() + ":");
                            Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(img_user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }


        }

        public View getItem() {
            return item;
        }
    }




    @Override
    public int getItemViewType(int position) {

        if(msgList.get(position).getType_msg() == 0){
            return MSG_TYPE_MID;
        }else {
            if (msgList.get(position).getID().equals(user.getUid())) {
                return MSG_TYPE_RIGHT;
            } else {
                return MSG_TYPE_LEFT;
            }
        }
    }

}

