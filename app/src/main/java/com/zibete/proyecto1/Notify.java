package com.zibete.proyecto1;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zibete.proyecto1.model.Users;

import java.util.List;

public class Notify extends Service {


    private static String CHANNEL_ID;
    final private static int NOTIFICATION_ID = 0;
    List<Users> usersList;
    Context context;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    DatabaseReference ref_datos = database.getReference("Usuarios").child("Datos");
    Notification notification, notificationAña;
    public static final FirebaseAuth mAuth = FirebaseAuth.getInstance();


    public final DatabaseReference ref_estado = database.getReference("Usuarios").child("Datos").child(user.getUid()).child("Estado");


    @Override
    public void onCreate() {
        super.onCreate();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {


        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

/*
        Estado cState = new Estado(
                getString(R.string.conectado),
                "",
                "");
        ref_estado.setValue(cState);


        Toast.makeText(this, "SERVICIO INICIADO", Toast.LENGTH_SHORT).show();

 */


        return START_NOT_STICKY;
    }


public void onDestroy (){
        /*

    final Calendar c = Calendar.getInstance();
    final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    Estado cState = new Estado(
            getString(R.string.ultVez),
            dateFormat.format(c.getTime()),
            timeFormat.format(c.getTime()));
    ref_estado.setValue(cState);

         */


}



/*
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Toast.makeText(this, "SERVICIO INICIADO", Toast.LENGTH_SHORT).show();

        final int noVistos = (int) Objects.requireNonNull(intent.getExtras()).get("noVistos");
        final String ult_msg = (String) intent.getExtras().get("ult_msg");
        final String nombre = (String) intent.getExtras().get("getNombre");
        final String ID = (String) intent.getExtras().get("getID");
        final String foto = (String) intent.getExtras().get("getFoto");

        final int service = (int) intent.getExtras().get("service");


        snoozeIntent.putExtra("getNombre",userss.getNombre());
        snoozeIntent.putExtra("getFoto",userss.getFoto());
        snoozeIntent.putExtra("getID",userss.getID());
        snoozeIntent.putExtra("noVistos",noVistos);
        snoozeIntent.putExtra("ult_msg",holder.ult_msg.getText());



        CHANNEL_ID = user.getUid();
        notificationAña = new NotificationCompat.Builder(this, CHANNEL_ID)

                .build();


        startForeground(1, notificationAña);
        stopForeground(true);





        Intent snoozeIntent = new Intent(this, BroadcastReciver.class);
        snoozeIntent.putExtra("getNombre",nombre);
        snoozeIntent.putExtra("getFoto",foto);
        snoozeIntent.putExtra("getID",ID);
        snoozeIntent.putExtra("noVistos",noVistos);
        snoozeIntent.putExtra("ult_msg",ult_msg);

        snoozeIntent.addFlags (Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        snoozeIntent.setAction("com.zibete.action.SERVICE");
        this.sendBroadcast(snoozeIntent);




        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setGroup(id_user)
                .setSmallIcon(R.drawable.ic_baseline_chat_24)

                .setContentTitle(noVistos + " mensajes de " + nombre)
                .setContentText(ult_msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();





        //startForeground(1,notificationAña);
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);

        //stopSelf();
        //stopForeground(true);






    }

 */




/*
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(context.getApplicationContext(), "SERVICIO INICIADO", Toast.LENGTH_SHORT).show();
        CHANNEL_ID = user.getUid();
        final int noVistos = (int) Objects.requireNonNull(intent.getExtras()).get("noVistos");
        final String ult_msg = (String) intent.getExtras().get("ult_msg");
        final String nombre = (String) intent.getExtras().get("getNombre");
        final String ID = (String) intent.getExtras().get("getID");
        final String foto = (String) intent.getExtras().get("getFoto");

//        final int service = (int) intent.getExtras().get("service");


        snoozeIntent.putExtra("getNombre",userss.getNombre());
        snoozeIntent.putExtra("getFoto",userss.getFoto());
        snoozeIntent.putExtra("getID",userss.getID());
        snoozeIntent.putExtra("noVistos",noVistos);
        snoozeIntent.putExtra("ult_msg",holder.ult_msg.getText());









        Intent snoozeIntent = new Intent(context.getApplicationContext(), BroadcastReciver.class);
        snoozeIntent.putExtra("getNombre",nombre);
        snoozeIntent.putExtra("getFoto",foto);
        snoozeIntent.putExtra("getID",ID);
        snoozeIntent.putExtra("noVistos",noVistos);
        snoozeIntent.putExtra("ult_msg",ult_msg);

        snoozeIntent.addFlags (Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        snoozeIntent.setAction("com.zibete.action.SERVICE");
        context.getApplicationContext().sendBroadcast(snoozeIntent);


        //NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(NOTIFICATION_ID, notification);

        //stopSelf();
        stopForeground(true);









        final int noVistos = (int) Objects.requireNonNull(intent.getExtras()).get("noVistos");
        final String ult_msg = (String) intent.getExtras().get("ult_msg");
        final String nombre = (String) intent.getExtras().get("getNombre");
        final String ID = (String) intent.getExtras().get("getID");
        final String foto = (String) intent.getExtras().get("getFoto");




        //final Users userss = usersList.get(position);
        //final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        CHANNEL_ID = user.getUid();


        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setGroup(id_user)
                .setSmallIcon(R.drawable.ic_baseline_chat_24)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_chat_24))
                .setContentTitle(noVistos + " mensajes de " + nombre)
                .setContentText(ult_msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();


        notificationAña = new NotificationCompat.Builder(this, CHANNEL_ID)


                .build();



        //startForeground(1,notificationAña);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);

        //stopSelf();
        //stopForeground(true);




        return START_NOT_STICKY;
    }
    */





/*
    @Override
    public void onTaskRemoved( Intent rootIntent ) {
        Intent intent = new Intent( this, Notify.class );
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
    }

 */


   // @Override
    //public void onDestroy() {


/*
        Intent intent = new Intent("com.zibete.action.SERVICE");
        intent.putExtra("yourvalue", "torestore");
        sendBroadcast(intent);






        Intent intent = new Intent(getApplicationContext(), BroadcastReciver.class);

        intent.putExtra("getNombre","nombre");
        intent.putExtra("getFoto","foto");
        intent.putExtra("getID","id");
        intent.putExtra("noVistos","noVistos");
        intent.putExtra("ult_msg","holder.ult_msg.getText()");

        Toast.makeText(getApplicationContext(), "SERVICIO RE-INICIADO", Toast.LENGTH_SHORT).show();
        getApplicationContext().sendBroadcast(intent);







        final int noVistos = (int) Objects.requireNonNull(intent.getExtras()).get("noVistos");
        final String ult_msg = (String) intent.getExtras().get("ult_msg");
        final String nombre = (String) intent.getExtras().get("getNombre");
        final String ID = (String) intent.getExtras().get("getID");
        final String foto = (String) intent.getExtras().get("getFoto");

        Intent intent1 = new Intent(this,Notify.class);


        intent1.putExtra("getNombre",nombre);
        intent1.putExtra("getFoto",foto);
        intent1.putExtra("getID",ID);
        intent1.putExtra("noVistos",noVistos);
        intent1.putExtra("ult_msg",ult_msg);

 */




//        this.getApplicationContext().startService(intent);
  //      Toast.makeText(this, "SERVICIO RE INICIADO", Toast.LENGTH_SHORT).show();



   // }





}
