package com.zibete.proyecto1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zibete.proyecto1.POJOS.Users;

import java.util.List;
import java.util.Objects;

public class BroadcastReciver extends BroadcastReceiver {

    Context context;
    Service service;
    private static String CHANNEL_ID;
    final private static int NOTIFICATION_ID = 0;
    List<Users> usersList;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    DatabaseReference ref_datos = database.getReference("Usuarios").child("Datos");
    Notification notification, notificationAña;

    @Override
    public void onReceive(Context context1, Intent intent) {

        context.startService(new Intent(context, Notify.class));

        /*
        final int noVistos = (int) Objects.requireNonNull(intent.getExtras()).get("noVistos");
        final String ult_msg = (String) intent.getExtras().get("ult_msg");
        final String nombre = (String) intent.getExtras().get("getNombre");
        final String ID = (String) intent.getExtras().get("getID");
        final String foto = (String) intent.getExtras().get("getFoto");

        Intent intent1 = new Intent(context1,Notify.class);

        intent1.putExtra("getNombre",nombre);
        intent1.putExtra("getFoto",foto);
        intent1.putExtra("getID",ID);
        intent1.putExtra("noVistos",noVistos);
        intent1.putExtra("ult_msg",ult_msg);


        CHANNEL_ID = user.getUid();

        notification = new NotificationCompat.Builder(context1, CHANNEL_ID)
                //.setGroup(id_user)
                .setSmallIcon(R.drawable.ic_baseline_chat_24)

                .setContentTitle(noVistos + " mensajes de " + nombre)
                .setContentText(ult_msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();





        //startForeground(1,notificationAña);
        NotificationManager notificationManager = (NotificationManager) context1.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);











/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context1.getApplicationContext().startService(intent1);
        }
        Toast.makeText(context1, "SERVICIO INICIADO", Toast.LENGTH_SHORT).show();

 */



    }





}
