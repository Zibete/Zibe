package com.zibete.proyecto1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.zibete.proyecto1.model.Users;

import java.util.List;

public class BroadcastReciver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "notify_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final List<Users> usersList = null; // reservado si se requiere en el futuro

    @Override
    public void onReceive(Context context, Intent intent) {
        // 🚀 Arranque seguro del servicio Notify
        Intent serviceIntent = new Intent(context, Notify.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getApplicationContext().startForegroundService(serviceIntent);
        } else {
            context.getApplicationContext().startService(serviceIntent);
        }

        // ✅ Notificación opcional si se pasan datos en el intent
        if (intent.getExtras() != null) {
            int noVistos = intent.getIntExtra("noVistos", 0);
            String ult_msg = intent.getStringExtra("ult_msg");
            String nombre = intent.getStringExtra("getNombre");

            if (noVistos > 0 && nombre != null && ult_msg != null) {
                showNotification(context, nombre, ult_msg, noVistos);
            }
        }
    }

    private void showNotification(Context context, String nombre, String ult_msg, int noVistos) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 🔔 Crear canal de notificación si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones de mensajes",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Canal para mensajes entrantes");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 📨 Construcción de la notificación
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_chat_24)
                .setContentTitle(noVistos + " mensajes de " + nombre)
                .setContentText(ult_msg)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
}
