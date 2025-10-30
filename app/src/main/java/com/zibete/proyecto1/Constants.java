package com.zibete.proyecto1;

import static com.zibete.proyecto1.MainActivity.latitud;
import static com.zibete.proyecto1.MainActivity.longitud;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_chat;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_chat_path;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_cuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_group_users;
import static com.zibete.proyecto1.utils.FirebaseRefs.user;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.toRadians;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.zibete.proyecto1.Adapters.AdapterPhotoReceived;
import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.POJOS.Chats;
import com.zibete.proyecto1.utils.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class Constants extends Application {

    public static final int REQUEST_LOCATION = 0;
    public static int FRAGMENT_ID_CHATLIST = 1;
    public static int FRAGMENT_ID_CHATGROUPLIST = 2;
    public static int CAMERA_SELECTED = 22;
    public static int PHOTO_SELECTED = 33;
    public static int MIC_SELECTED = 44;
    public static int PERMISSIONS_EDIT_PROFILE = 11;
    public static int maxChatSize = 10000;
    public static int INFO = 111;
    public static int MSG = 100;
    public static int MSG_SENDER_DLT = 101;
    public static int MSG_RECEIVER_DLT = 102;
    public static int PHOTO = 200;
    public static int PHOTO_SENDER_DLT = 201;
    public static int PHOTO_RECEIVER_DLT = 202;
    public static int AUDIO = 300;
    public static int AUDIO_SENDER_DLT = 301;
    public static int AUDIO_RECEIVER_DLT = 302;
    public static int PRIVATE_GROUP = 2;
    public static int PUBLIC_GROUP = 1;
    public static String chat = "Chats";
    public static String unknown = "Unknown";
    public static String chatWith = "ChatWith";
    public static String chatWithUnknown = "ChatWithUnknown";
    public static String Empty = "Empty";
    public static String Calling = "Calling";
    public static String Ringing = "Ringing";
    public static FirebaseStorage storage = FirebaseStorage.getInstance();
    public static StorageReference storageReference = storage.getReference();
    public static ValueEventListener listenerToken;
    public static ValueEventListener listenerGroupBadge;
    public static ValueEventListener listenerMsgUnreadBadge;

















//
//    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

//    public void StateOnLine(Context context, final String id_user){
//
//        Estado cState = new Estado(
//                context.getString(R.string.conectado),
//                "",
//                "");
//        ref_datos.child(id_user).child("Estado").setValue(cState);
//        ref_cuentas.child(id_user).child("estado").setValue(true);
//
//    }
//    public void StateOffLine(Context context, String id_user){
//
//        if (user != null) {
//            final Calendar c = Calendar.getInstance();
//            final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
//            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
//
//            final Estado cState = new Estado(
//                    context.getString(R.string.ultVez),
//                    dateFormat.format(c.getTime()),
//                    timeFormat.format(c.getTime()));
//
//            ref_datos.child(id_user).child("Estado").setValue(cState);
//            ref_cuentas.child(id_user).child("estado").setValue(false);
//        }
//
//    }
//    public void StateUser (final Context context, final String id_user, final ImageView icon_conectado, final ImageView icon_desconectado, final TextView tv_estado, final String type){
//
//        ref_datos.child(id_user).child("Estado").addValueEventListener(new ValueEventListener() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
//
//                if (dataSnapshot.exists()) {
//
//                    Calendar c = Calendar.getInstance();
//                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
//                    final String estado = dataSnapshot.child("estado").getValue(String.class);
//                    String fecha = dataSnapshot.child("fecha").getValue(String.class);
//                    String hora = dataSnapshot.child("hora").getValue(String.class);
//
//                    if (estado.equals(context.getString(R.string.conectado))) {
//                        //Conectado
//                        icon_conectado.setVisibility(View.VISIBLE);
//                        icon_desconectado.setVisibility(View.GONE);
//                        tv_estado.setText(context.getString(R.string.enlinea));
//                        tv_estado.setTypeface(null, Typeface.NORMAL);
//                        tv_estado.setTextColor(context.getResources().getColor(R.color.colorClaro));
//                    } else {
//                        if (estado.equals(context.getString(R.string.escribiendo)) || estado.equals(context.getString(R.string.grabando))) {
//                            //Escribiendo
//
//
//                            ref_datos.child(id_user).child("ChatList").child("Actual").addValueEventListener(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                                    if (dataSnapshot.exists()) {
//
//                                        if (dataSnapshot.getValue(String.class).equals(user.getUid() + type)) {
//                                            icon_conectado.setVisibility(View.VISIBLE);
//                                            icon_desconectado.setVisibility(View.GONE);
//                                            tv_estado.setText(estado);
//                                            tv_estado.setTypeface(null, Typeface.ITALIC);
//                                            tv_estado.setTextColor(context.getResources().getColor(R.color.accent));
//                                        } else {
//                                            icon_conectado.setVisibility(View.VISIBLE);
//                                            icon_desconectado.setVisibility(View.GONE);
//                                            tv_estado.setText(context.getString(R.string.enlinea));
//                                            tv_estado.setTypeface(null, Typeface.NORMAL);
//                                            tv_estado.setTextColor(context.getResources().getColor(R.color.colorClaro));
//
//                                        }
//                                    }
//                                }
//                                @Override
//                                public void onCancelled(@NonNull DatabaseError error) {
//                                }
//                            });
//
//                        }else {
//                            //últ vez
//                            icon_conectado.setVisibility(View.GONE);
//                            icon_desconectado.setVisibility(View.VISIBLE);
//                            tv_estado.setTypeface(null, Typeface.NORMAL);
//                            tv_estado.setTextColor(context.getResources().getColor(R.color.colorClaro));
//
//                            if (fecha.equals(dateFormat.format(c.getTime()))) {
//                                tv_estado.setText(context.getString(R.string.ultVez) + " " + context.getString(R.string.hoy) + " " + context.getString(R.string.a_las) + " " + hora);
//                            } else {
//
//                                Calendar calendar = Calendar.getInstance();
//                                calendar.add(Calendar.DATE, -1);
//
//                                if (fecha.equals(dateFormat.format(calendar.getTime()))) {
//                                    tv_estado.setText(context.getString(R.string.ultVez) + " " + context.getString(R.string.ayer) + " " + context.getString(R.string.a_las) + " " + hora);
//                                } else {
//                                    tv_estado.setText(context.getString(R.string.ultVez) + " " + fecha + " " + context.getString(R.string.a_las) + " " + hora);
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    icon_conectado.setVisibility(View.GONE);
//                    icon_desconectado.setVisibility(View.VISIBLE);
//                    tv_estado.setText(context.getString(R.string.desconectado));
//                }
//
//            }
//            @Override public void onCancelled(@NonNull DatabaseError error) {
//            }
//        });
//
//    }

//    public static void NoLeido(String id_user, String type){
//
//
//        ref_datos.child(user.getUid()).child(type).child(id_user).child("noVisto").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull final DataSnapshot dataVistos) {
//                if (dataVistos.exists()) {
//                    final Integer noVistos = dataVistos.getValue(Integer.class);
//
//                    ref_datos.child(user.getUid()).child("ChatList").child("msgNoLeidos").addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataLeidos) {
//                            if (dataLeidos.exists()) {
//                                final Integer noLeidos = dataLeidos.getValue(Integer.class);
//                                Integer count = noLeidos - noVistos;
//
//                                if ((noVistos > 0)) {
//                                    dataVistos.getRef().setValue(0);
//                                    dataLeidos.getRef().setValue(count);
//
//                                } else {
//
//                                    dataVistos.getRef().setValue(1);
//                                    dataLeidos.getRef().setValue(noLeidos + 1);
//
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        });
//
//
//    }

//    public void Silent(final String name_user, final String id_user, final String type){
//
//
//        ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                if (dataSnapshot.exists()) {
//
//                    String state = dataSnapshot.child("estado").getValue(String.class);
//                    String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);
//
//                    if (photo.equals(Empty)){
//
//                        dataSnapshot.getRef().removeValue();
//
//                    }else{
//
//                        if (state.equals("silent")) {
//
//                            dataSnapshot.getRef().child("estado").setValue(type);
//
//                        }else{
//
//                            dataSnapshot.getRef().child("estado").setValue("silent");
//
//                        }
//                    }
//
//                }else {
//
//                    newChatWith(dataSnapshot, id_user, name_user, "silent");
//
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        });
//
//
//    }

//    public void newChatWith(@NonNull DataSnapshot dataSnapshot, String id_user, String name_user, String state) {
//        final Calendar c = Calendar.getInstance();
//        final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");
//        final ChatWith newChat = new ChatWith(
//                "",
//                dateFormat3.format(c.getTime()),
//                null,
//                "",
//                id_user,
//                name_user,
//                Empty,
//                state,
//                0,
//                1);
//
//        dataSnapshot.getRef().setValue(newChat);
//    }

//    public void Block(final Context context, final String name_user, final String id_user, final View view, final String type){
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
//        builder.setTitle("Bloquear");
//        builder.setMessage("¿Desea bloquear a " + name_user + "?");
//
//        builder.setCancelable(false);
//        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
//
//            public void onClick(DialogInterface builder, int id) {
//
//                ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
//
//                        if (dataSnapshot.exists()){
//
//                            dataSnapshot.getRef().child("estado").setValue("bloq");
//
//                        } else {
//
//                            newChatWith(dataSnapshot, id_user, name_user, "bloq");
//
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                    }
//                });
//
//                final Snackbar snack = Snackbar.make(view, "Bloqueaste a " + name_user +", podrás desbloquearlo cuando desees", Snackbar.LENGTH_INDEFINITE);
//                snack.setAction("OK", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        snack.dismiss();
//                    }
//                });
//                snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
//                TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
//                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                snack.show();
//
//            }
//        });
//        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface builder, int id) {
//                return;
//            }
//        });
//        builder.show();
//
//    }

//    public void desBloquear (final Context context, final String id_user, final String name_user, final View view, final String type) {
//
//        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp))
//
//                .setMessage("¿Desea desbloquear a " + name_user + "?")
//                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface builder, int id) {
//
//                        ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);
//
//                                if (photo.equals(Empty)){
//                                    dataSnapshot.getRef().removeValue();
//
//                                }else{
//                                    dataSnapshot.getRef().child("estado").setValue(type);
//
//                                }
//
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError error) {
//
//                            }
//                        });
//
//
//                        Snackbar snack = Snackbar.make(view, "Desbloqueaste a " + name_user, Snackbar.LENGTH_SHORT);
//                        snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
//                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
//                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                        snack.show();
//
//                    }
//
//
//                })
//                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
//
//                    public void onClick(DialogInterface builder, int id) {
//
//                        return;
//                    }
//                })
//                .show();
//
//    }

    public void UnhiddenChat(final Context context, final String id_user, String name_user, final View view, final String type) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
        builder.setTitle("Ocultar Chat con " + name_user);

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int selectedIndex) {

                ref_datos.child(user.getUid()).child(type).child(id_user).child("estado").setValue("delete");

                Snackbar snack = Snackbar.make(view, "Se ha ocultado el chat", Snackbar.LENGTH_SHORT);
                snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();

            }
        });


        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int id) {
                return;
            }
        });


        builder.setCancelable(false);

        builder.show();

    }

    public void DeleteChat(final Context context, final String id_user, final String name_user, final View view, final String type){

        final String ref;

        if (type.equals(chatWith)){ ref = chat; }else{ ref = unknown; }

        final StorageReference refYourReceiverData = storageReference.child(type + "/" + id_user + "/");
        final StorageReference refMyReceiverData = storageReference.child(type + "/" + user.getUid() + "/");
        final DatabaseReference startedByMe = ref_chat_path.child(ref).child(user.getUid() + " <---> " + id_user).child("Mensajes");
        final DatabaseReference startedBeHim = ref_chat_path.child(ref).child(id_user + " <---> " + user.getUid()).child("Mensajes");



        startedByMe.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    iterator(dataSnapshot, id_user);
                } else {

                    startedBeHim.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                iterator(dataSnapshot, id_user);
                            }else{

                                Snackbar snack = Snackbar.make(view, "Chat vacío", Snackbar.LENGTH_SHORT);
                                snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
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
            }

            public void iterator(@NonNull final DataSnapshot dataSnapshot, final String id_user) {

                //long messages = dataSnapshot.getChildrenCount();

                //final ArrayList<String> receiverDelete = new ArrayList<>();
                final ArrayList <Chats> messages = new ArrayList<>();

/*
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chats chat = snapshot.getValue(Chats.class);

                    if (chat.getEnvia().equals(user.getUid())) {
                        if (chat.getType() == MSG_SENDER_DLT) {
                            String key = snapshot.getKey();
                            senderDelete.add(key);
                        }
                        if (chat.getType() == PHOTO_SENDER_DLT) {
                            String key = snapshot.getKey();
                            senderDelete.add(key);
                        }
                        if (chat.getType() == AUDIO_SENDER_DLT) {
                            String key = snapshot.getKey();
                            senderDelete.add(key);
                        }
                    } else {
                        if (chat.getType() == MSG_RECEIVER_DLT) {
                            String key = snapshot.getKey();
                            receiverDelete.add(key);
                        }
                        if (chat.getType() == PHOTO_RECEIVER_DLT) {
                            String key = snapshot.getKey();
                            receiverDelete.add(key);
                        }
                        if (chat.getType() == AUDIO_RECEIVER_DLT) {
                            String key = snapshot.getKey();
                            receiverDelete.add(key);
                        }
                    }
                }


 */


                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chats chat = snapshot.getValue(Chats.class);

                    if (chat.getEnvia().equals(user.getUid())) {
                        if (chat.getType() == MSG | chat.getType() == MSG_RECEIVER_DLT) {
                            messages.add(chat);
                        }
                        if (chat.getType() == PHOTO | chat.getType() == PHOTO_RECEIVER_DLT) {
                            messages.add(chat);
                        }
                        if (chat.getType() == AUDIO | chat.getType() == AUDIO_RECEIVER_DLT) {
                            messages.add(chat);
                        }
                    } else {
                        if (chat.getType() == MSG | chat.getType() == MSG_SENDER_DLT) {
                            messages.add(chat);
                        }
                        if (chat.getType() == PHOTO | chat.getType() == PHOTO_SENDER_DLT) {
                            messages.add(chat);
                        }
                        if (chat.getType() == AUDIO | chat.getType() == AUDIO_SENDER_DLT) {
                            messages.add(chat);
                        }
                    }
                }


                final long count = messages.size();

                //deleteChat(id_user, name_user, count, context, view, type, ref, chat, dataSnapshot, messages);

                //long count = messages - (senderDelete.size() + receiverDelete.size());

                //deleteChat(id_user, name_user, count, context, view, type, ref);


                if (count == 0){
                    Snackbar snack = Snackbar.make(view, "Chat vacío", Snackbar.LENGTH_SHORT);
                    snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();

                }else{



                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
                    builder.setTitle("Eliminar Chat con " + name_user);

                    final String[] titles;

                    if (count == 1) {
                        titles = new String[]{"Ocultar chat", "Eliminar " + count + " mensaje"};
                    }else{
                        titles = new String[]{"Ocultar chat", "Eliminar " + count + " mensajes"};
                    }

                    final int[] itemSelected = {0};

                    builder.setSingleChoiceItems(titles, itemSelected[0], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                            itemSelected[0] = selectedIndex;

                        }
                    });


                    builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface builder, int selectedIndex) {


                            if (titles[itemSelected[0]].equals("Ocultar chat")) {

                                ref_datos.child(user.getUid()).child(type).child(id_user).child("estado").setValue("delete");

                                Snackbar snack = Snackbar.make(view, "Se ha ocultado el chat", Snackbar.LENGTH_SHORT);
                                snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                                TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                snack.show();


                            } else {

                                for (final Chats chat : messages) {

                                    dataSnapshot.getRef().orderByChild("date").equalTo(chat.getDate()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            if (dataSnapshot.exists()) {

                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                                    Integer type = snapshot.child("type").getValue(Integer.class);
                                                    String sender = snapshot.child("envia").getValue(String.class);

                                                    if (sender.equals(user.getUid())) {

                                                        if (type == MSG) {
                                                            snapshot.child("type").getRef().setValue(MSG_SENDER_DLT);
                                                        }
                                                        if (type == MSG_RECEIVER_DLT) {
                                                            snapshot.getRef().removeValue();
                                                        }
                                                        if (type == PHOTO) {
                                                            snapshot.child("type").getRef().setValue(PHOTO_SENDER_DLT);
                                                        }
                                                        if (type == PHOTO_RECEIVER_DLT) {
                                                            int startString = chat.getMensaje().indexOf(id_user) + id_user.length() + 3;
                                                            int endString = chat.getMensaje().indexOf(".jpg") + 4;
                                                            refYourReceiverData.child(chat.getMensaje().substring(startString, endString)).delete();
                                                            snapshot.getRef().removeValue();
                                                        }
                                                        if (type == AUDIO) {
                                                            snapshot.child("type").getRef().setValue(AUDIO_SENDER_DLT);
                                                        }
                                                        if (type == AUDIO_RECEIVER_DLT) {
                                                            int startString = chat.getMensaje().indexOf(id_user) + id_user.length() + 3;
                                                            int endString = chat.getMensaje().indexOf(".mp3") + 4;
                                                            refYourReceiverData.child(chat.getMensaje().substring(startString, endString)).delete();
                                                            snapshot.getRef().removeValue();
                                                        }

                                                    } else {

                                                        if (type == MSG) {
                                                            snapshot.child("type").getRef().setValue(MSG_RECEIVER_DLT);
                                                        }
                                                        if (type == MSG_SENDER_DLT) {
                                                            snapshot.getRef().removeValue();
                                                        }
                                                        if (type == PHOTO) {
                                                            snapshot.child("type").getRef().setValue(PHOTO_RECEIVER_DLT);
                                                        }
                                                        if (type == PHOTO_SENDER_DLT) {
                                                            int startString = chat.getMensaje().indexOf(user.getUid()) + user.getUid().length() + 3;
                                                            int endString = chat.getMensaje().indexOf(".jpg") + 4;
                                                            refMyReceiverData.child(chat.getMensaje().substring(startString, endString)).delete();
                                                            snapshot.getRef().removeValue();
                                                        }
                                                        if (type == AUDIO) {
                                                            snapshot.child("type").getRef().setValue(AUDIO_RECEIVER_DLT);
                                                        }
                                                        if (type == AUDIO_SENDER_DLT) {
                                                            int startString = chat.getMensaje().indexOf(user.getUid()) + user.getUid().length() + 3;
                                                            int endString = chat.getMensaje().indexOf(".mp3") + 4;
                                                            refMyReceiverData.child(chat.getMensaje().substring(startString, endString)).delete();
                                                            snapshot.getRef().removeValue();
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }

                                ref_datos.child(user.getUid()).child(type).child(id_user).removeValue();

                                if (count == 1) {
                                    Toast.makeText(context, count + " mensaje eliminado", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(context, count + " mensajes eliminados", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                    builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface builder, int id) {
                            return;
                        }
                    });

                    builder.setCancelable(false);
                    builder.show();

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


    }

    public static Double getDistanceMeters(double milatitud, double milongitud, double sulatitud, double sulongitud) {

        double l1 = toRadians(milatitud);
        double l2 = toRadians(sulatitud);
        double g1 = toRadians(milongitud);
        double g2 = toRadians(sulongitud);

        double dist = acos(sin(l1) * sin(l2) + cos(l1) * cos(l2) * cos(g1 - g2));
        if(dist < 0) {
            dist = dist + Math.PI;
        }

        return (double) Math.round(dist * 6378100);
    }

    public void getAge (final String id_user, final TextView age){

        ref_cuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public void getDistanceToUser (final String id_user, final TextView distanceUser){

        ref_cuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Double sulatitud = dataSnapshot.child("latitud").getValue(Double.class);
                Double sulongitud = dataSnapshot.child("longitud").getValue(Double.class);

                Double distanceMeters = getDistanceMeters(latitud, longitud, sulatitud, sulongitud);

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

    public void setMenuProfile(final Context context, final String id_user, final FloatingActionButton subMenu_chatWithUnknown, FloatingActionButton subMenu_chatWith){

        ref_group_users.child(groupName).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public void setFavorite (String user_id, final ImageView profile_favorite_on, final ImageView profile_favorite_off){

        ref_datos.child(user.getUid()).child("FavoriteList").child(user_id).addValueEventListener(new ValueEventListener() {
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

    public void setBloq (String user_id, final ImageView profile_bloc){

        ref_datos.child(user.getUid()).child(chatWith).child(user_id).child("estado").addValueEventListener(new ValueEventListener() {
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

    public void getBloqMe (String user_id, final ImageView profile_bloc_me){

        //Me Bloqueó
        ref_datos.child(user_id).child(chatWith).child(user.getUid()).child("estado").addValueEventListener(new ValueEventListener() {
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

    public void addPhotoReceived (String id_user, final AdapterPhotoReceived adapterPhotoReceived, final LinearLayout linearPhotos){

        ref_chat.child(user.getUid() + " <---> " + id_user).child("Mensajes").addChildEventListener(new ChildEventListener() {
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

        ref_chat.child(id_user + " <---> " + user.getUid()).child("Mensajes").addChildEventListener(new ChildEventListener() {
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

    public void addPhoto(@NonNull DataSnapshot dataSnapshot, AdapterPhotoReceived adapterPhotoReceived, LinearLayout linearPhotos) {
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


}
