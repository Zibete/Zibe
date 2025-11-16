package com.zibete.proyecto1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.zibete.proyecto1.ui.splash.SplashActivity;
import com.zibete.proyecto1.ui.UsuariosFragment;

import static com.zibete.proyecto1.utils.Constants.CHAT;
import static com.zibete.proyecto1.utils.Constants.CHATWITH;
import static com.zibete.proyecto1.utils.Constants.UNKNOWN;
import static com.zibete.proyecto1.utils.FirebaseRefs.refChats;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.MainActivity.toolbar;

public class FCM extends FirebaseMessagingService {

    private static String CHANNEL_ID;
    final private static int NOTIFICATION_ID = 0;
    public final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

    }

    public void onMessageReceived(final RemoteMessage remoteMessage) {

        if (user != null) {
            final String novistos = remoteMessage.getData().get("novistos");
            final String userName = remoteMessage.getData().get("user");
            final String msg = remoteMessage.getData().get("msg");
            final String id_user = remoteMessage.getData().get("id_user");
            final String type = remoteMessage.getData().get("type");

            final String ref;

            if (type.equals(CHATWITH)) {
                ref = CHAT;
            } else {
                ref = UNKNOWN;
            }


            if (!type.equals(UsuariosFragment.groupName)) {

                if (UsuariosFragment.individualNotifications) {

                    if (remoteMessage.getData().size() > 0) {

                        if (userName != null) {

                            final Query newQuery = refDatos.child(user.getUid()).child(type).orderByChild("noVisto").startAt(1);
                            newQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {

                                        final long childrenCount = dataSnapshot.getChildrenCount();//Cantidad de chats con mensajes no vistos

                                        int countMsgUnread = 0;

                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                            int unRead = snapshot.child("noVisto").getValue(int.class);

                                            countMsgUnread = countMsgUnread + unRead;

                                        }

                                        if (childrenCount > 1) {

                                            final String title = countMsgUnread + " mensajes de " + childrenCount + " chats";
                                            final String text = userName + ": " + msg;

                                            msgNotify(title, text, id_user, type, ref);

                                        } else {

                                            final String text = msg;
                                            final String title;

                                            if (novistos.equals("1")) {

                                                title = "Nuevo mensaje de " + userName;

                                            } else {

                                                title = novistos + " mensajes de " + userName;

                                            }
                                            msgNotify(title, text, id_user, type, ref);

                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }


                } else {

                    DoubleCheck(id_user, type, ref);
                }

            } else {

                if (UsuariosFragment.groupNotifications) {

                    if (!remoteMessage.getData().isEmpty()) {

                        if (userName != null) {

                            if (toolbar != null) {

                                if (!toolbar.getTitle().equals(UsuariosFragment.groupName)) {

                                    String title = "Nuevo mensaje de " + type;
                                    String text = userName + ": " + msg;
                                    msgNotify(title, text, id_user, type, ref);

                                }

                            } else {

                                String title = "Nuevo mensaje de " + type;
                                String text = userName + ": " + msg;
                                msgNotify(title, text, id_user, type, ref);

                            }
                        }
                    }
                }
            }
        }
    }

    public void msgNotify (String title, String text, final String id_user, final String type, final String ref){

        CHANNEL_ID = "mensaje";
        CharSequence name = getString(R.string.channel_name);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        builder.setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(text)
                .setContentInfo(name)
                .setContentIntent(pendingIntent())
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 300, 200, 300})
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);


        notificationManager.notify(NOTIFICATION_ID,builder.build());

        if (!type.equals(UsuariosFragment.groupName)) {
            DoubleCheck(id_user, type, ref);
        }

    }

    public void DoubleCheck(final String id_user, final String type, final String ref) {

        refDatos.child(user.getUid()).child(type).child(id_user).child("wVisto").setValue(2);

        refDatos.child(user.getUid()).child(type).child(id_user).child("noVisto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                final Integer noVistos = dataSnapshot.getValue(Integer.class);

                if (noVistos > 0) {

                    refChats.child(ref).child(user.getUid() + " <---> " + id_user).child("Mensajes").limitToLast(noVistos).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            setDoubleCheck(dataSnapshot);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                    refChats.child(ref).child(id_user + " <---> " + user.getUid()).child("Mensajes").limitToLast(noVistos).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            setDoubleCheck(dataSnapshot);
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

    public void setDoubleCheck(@NonNull DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                if (snapshot.hasChild("envia")) {
                    String envia = snapshot.child("envia").getValue(String.class);
                    if (!envia.equals(user.getUid())) {
                        if (snapshot.hasChild("visto")) {

                            snapshot.getRef().child("visto").setValue(2);

                        }
                    }
                }
            }
        }
    }

    public PendingIntent pendingIntent (){

        Intent intent = new Intent (getApplicationContext(), SplashActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(this,0,intent,0);

    }

}
