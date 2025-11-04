package com.zibete.proyecto1.utils;

import static com.zibete.proyecto1.Constants.Empty;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.utils.FirebaseRefs.auth;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_cuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;
import static com.zibete.proyecto1.utils.FirebaseRefs.user;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.location.Location;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.model.ChatWith;
import com.zibete.proyecto1.model.State;
import com.zibete.proyecto1.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

// Clase que maneja la comunicación con Firebase para datos de usuario
// (repository = repositorio → capa de acceso a datos)
public class UserRepository {


    // ------------------------------------------------------------
    // setUserOnline → establece al usuario como conectado
    // (StateOnLine = poner en línea)
    // ------------------------------------------------------------
    public static void setUserOnline(Context context, String id_user) {

        // 🔸 Crear el objeto Estado con los textos de recursos
        // Estado = clase de modelo que representa el estado de conexión
        State currentState = new State(
                context.getString(R.string.conectado), // "connected" → conectado
                "",
                ""
        );

        // 🔸 Guardar el estado en Firebase (dos ubicaciones distintas)
        ref_datos.child(id_user).child("Estado").setValue(currentState);
        ref_cuentas.child(id_user).child("estado").setValue(true);
    }

    public static void setUserOffline(Context context, String id_user){

        if (user != null) {
            final Calendar c = Calendar.getInstance();
            final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            final State cState = new State(
                    context.getString(R.string.ultVez),
                    dateFormat.format(c.getTime()),
                    timeFormat.format(c.getTime()));

            ref_datos.child(id_user).child("Estado").setValue(cState);
            ref_cuentas.child(id_user).child("estado").setValue(false);
        }

    }

    public static void stateUser(
            final Context context,
            final String userId,                    // id_user → usuario a mostrar
            final ImageView iconConnected,          // icon_conectado
            final ImageView iconDisconnected,       // icon_desconectado
            final TextView tvStatus,                // tv_estado
            final String type                       // type → ej. "chatWith"
    ) {
        ref_datos.child(userId).child("Estado").addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

                    final String estado = dataSnapshot.child("estado").getValue(String.class);   // estado → "conectado", "escribiendo", etc.
                    final String fecha = dataSnapshot.child("fecha").getValue(String.class);     // fecha última vez
                    final String hora = dataSnapshot.child("hora").getValue(String.class);       // hora última vez

                    if (estado != null && estado.equals(context.getString(R.string.conectado))) {
                        // ✅ Conectado (online)
                        iconConnected.setVisibility(View.VISIBLE);
                        iconDisconnected.setVisibility(View.GONE);
                        tvStatus.setText(context.getString(R.string.enlinea)); // "en línea"
                        tvStatus.setTypeface(null, Typeface.NORMAL);
                        tvStatus.setTextColor(context.getResources().getColor(R.color.colorClaro));

                    } else {
                        // ⚠️ Puede estar escribiendo / grabando / desconectado

                        if (estado != null &&
                                (estado.equals(context.getString(R.string.escribiendo)) ||
                                        estado.equals(context.getString(R.string.grabando)))) {

                            // 📝 Escribiendo / 🎙 Grabando
                            // Tenemos que chequear si el chat actual es conmigo
                            ref_datos.child(userId)
                                    .child("ChatList")
                                    .child("Actual")
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dsChat) {
                                            if (dsChat.exists()) {

                                                String currentChat = dsChat.getValue(String.class);
                                                // usuario actual logueado
                                                String myUid = auth.getCurrentUser() != null
                                                        ? auth.getCurrentUser().getUid()
                                                        : null;

                                                // compara: if (Actual == userLogged + type)
                                                if (myUid != null && currentChat != null
                                                        && currentChat.equals(myUid + type)) {

                                                    // ✅ Mostrar “escribiendo” o “grabando”
                                                    iconConnected.setVisibility(View.VISIBLE);
                                                    iconDisconnected.setVisibility(View.GONE);
                                                    tvStatus.setText(estado);
                                                    tvStatus.setTypeface(null, Typeface.ITALIC);
                                                    tvStatus.setTextColor(context.getResources().getColor(R.color.accent));

                                                } else {
                                                    // está escribiendo pero NO conmigo → mostrar online normal
                                                    iconConnected.setVisibility(View.VISIBLE);
                                                    iconDisconnected.setVisibility(View.GONE);
                                                    tvStatus.setText(context.getString(R.string.enlinea));
                                                    tvStatus.setTypeface(null, Typeface.NORMAL);
                                                    tvStatus.setTextColor(context.getResources().getColor(R.color.colorClaro));
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            // opcional: log
                                        }
                                    });

                        } else {
                            // ⏱ Última vez conectado
                            iconConnected.setVisibility(View.GONE);
                            iconDisconnected.setVisibility(View.VISIBLE);
                            tvStatus.setTypeface(null, Typeface.NORMAL);
                            tvStatus.setTextColor(context.getResources().getColor(R.color.colorClaro));

                            if (fecha != null && fecha.equals(dateFormat.format(c.getTime()))) {
                                // hoy
                                tvStatus.setText(
                                        context.getString(R.string.ultVez) + " " +
                                                context.getString(R.string.hoy) + " " +
                                                context.getString(R.string.a_las) + " " +
                                                hora
                                );
                            } else {
                                // quizá ayer
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(Calendar.DATE, -1);

                                if (fecha != null && fecha.equals(dateFormat.format(calendar.getTime()))) {
                                    tvStatus.setText(
                                            context.getString(R.string.ultVez) + " " +
                                                    context.getString(R.string.ayer) + " " +
                                                    context.getString(R.string.a_las) + " " +
                                                    hora
                                    );
                                } else {
                                    // fecha cualquiera
                                    tvStatus.setText(
                                            context.getString(R.string.ultVez) + " " +
                                                    fecha + " " +
                                                    context.getString(R.string.a_las) + " " +
                                                    hora
                                    );
                                }
                            }
                        }
                    }
                } else {
                    // no hay nodo "Estado" → mostrar desconectado
                    iconConnected.setVisibility(View.GONE);
                    iconDisconnected.setVisibility(View.VISIBLE);
                    tvStatus.setText(context.getString(R.string.desconectado)); // "disconnected" → desconectado
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // opcional: log
            }
        });
    }

    public static void setNoLeido(String id_user, String type){

        ref_datos.child(user.getUid()).child(type).child(id_user).child("noVisto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataVistos) {
                if (dataVistos.exists()) {
                    final Integer noVistos = dataVistos.getValue(Integer.class);

                    ref_datos.child(user.getUid()).child("ChatList").child("msgNoLeidos").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataLeidos) {
                            if (dataLeidos.exists()) {
                                final Integer noLeidos = dataLeidos.getValue(Integer.class);
                                Integer count = noLeidos - noVistos;
                                if ((noVistos > 0)) {
                                    dataVistos.getRef().setValue(0);
                                    dataLeidos.getRef().setValue(count);
                                } else {
                                    dataVistos.getRef().setValue(1);
                                    dataLeidos.getRef().setValue(noLeidos + 1);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public static void Silent(final String name_user, final String id_user, final String type){


        ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    String state = dataSnapshot.child("estado").getValue(String.class);
                    String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);

                    if (photo.equals(Empty)){

                        dataSnapshot.getRef().removeValue();

                    }else{

                        if (state.equals("silent")) {

                            dataSnapshot.getRef().child("estado").setValue(type);

                        }else{

                            dataSnapshot.getRef().child("estado").setValue("silent");

                        }
                    }

                }else {

                    newChatWith(dataSnapshot, id_user, name_user, "silent");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


    }

    public static void newChatWith(@NonNull DataSnapshot dataSnapshot, String id_user, String name_user, String state) {
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");
        final ChatWith newChat = new ChatWith(
                "",
                dateFormat3.format(c.getTime()),
                null,
                "",
                id_user,
                name_user,
                Empty,
                state,
                0,
                1);

        dataSnapshot.getRef().setValue(newChat);
    }

    public static void setBlockUser(
            final Context context,
            final String name_user,
            final String id_user,
            final View view,
            final String type)
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp));
        builder.setTitle("Bloquear");
        builder.setMessage("¿Desea bloquear a " + name_user + "?");

        builder.setCancelable(false);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface builder, int id) {

                ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()){

                            dataSnapshot.getRef().child("estado").setValue("bloq");

                        } else {

                            newChatWith(dataSnapshot, id_user, name_user, "bloq");

                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

                final Snackbar snack = Snackbar.make(view, "Bloqueaste a " + name_user +", podrás desbloquearlo cuando desees", Snackbar.LENGTH_INDEFINITE);
                snack.setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snack.dismiss();
                    }
                });
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
        builder.show();

    }

    public static void setUnBlockUser (
            final Context context,
            final String id_user,
            final String name_user,
            final View view,
            final String type) {

        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogApp))

                .setMessage("¿Desea desbloquear a " + name_user + "?")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface builder, int id) {

                        ref_datos.child(user.getUid()).child(type).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                String photo = dataSnapshot.child("wUserPhoto").getValue(String.class);
                                if (photo.equals(Empty)){
                                    dataSnapshot.getRef().removeValue();
                                }else{
                                    dataSnapshot.getRef().child("estado").setValue(type);

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        Snackbar snack = Snackbar.make(view, "Desbloqueaste a " + name_user, Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(context.getResources().getColor(R.color.colorC));
                        TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        snack.show();

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface builder, int id) {
                        return;
                    }
                })
                .show();

    }




    public static void bindBlockStatus (
            String user_id,
            final ImageView profile_bloc)

    { // bindBlockStatus = vincular estado de bloqueo

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

    public static Double latitude;
    public static Double longitude;
    public static void updateLocationUI(Location mLastLocation) {
        if (mLastLocation == null) return;

        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        ref_cuentas.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    FirebaseRefs.ref_cuentas.child(user.getUid()).child("latitud").setValue(latitude);
                    FirebaseRefs.ref_cuentas.child(user.getUid()).child("longitud").setValue(longitude);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }





}
