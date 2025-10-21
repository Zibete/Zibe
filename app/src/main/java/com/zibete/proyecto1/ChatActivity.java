package com.zibete.proyecto1;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.canhub.cropper.CropImageContractOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.zibete.proyecto1.Adapters.AdapterChat;
import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.POJOS.Chats;
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.utils.CropHelper;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.zibete.proyecto1.Adapters.AdapterChat.mediaPlayer;
import static com.zibete.proyecto1.Constants.AUDIO;
import static com.zibete.proyecto1.Constants.AUDIO_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.AUDIO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.CAMERA_SELECTED;
import static com.zibete.proyecto1.Constants.MIC_SELECTED;
import static com.zibete.proyecto1.Constants.MSG;
import static com.zibete.proyecto1.Constants.MSG_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.MSG_SENDER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO_SELECTED;
import static com.zibete.proyecto1.Constants.PHOTO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.chat;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.Constants.maxChatSize;
import static com.zibete.proyecto1.Constants.storageReference;
import static com.zibete.proyecto1.Constants.unknown;
import static com.zibete.proyecto1.MainActivity.ref_chat_path;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;
import static com.zibete.proyecto1.MainActivity.ref_datos;
import static com.zibete.proyecto1.MainActivity.ref_group_users;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userType;





public class ChatActivity extends AppCompatActivity {

    public static ArrayList <Chats> msgSelected = new ArrayList<>();
    ArrayList <Chats> chatsArrayList;
    ArrayList <String> photoList;
    ArrayList <Users> extraUserList = new ArrayList<>();

    CircleImageView img_user;
    TextView nameUser, tv_estado, tv_title;
    EditText msg;

    ImageView icon_conectado, icon_desconectado, img_cancel_dialog;
    ImageView btnCamera, btnSendMsg;
    LottieAnimationView btnMic, trashAnimated2;

    Button btnDesbloq;
    LottieAnimationView trashAnimated;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    DatabaseReference ref_actual = ref_datos.child(user.getUid()).child("ChatList").child("Actual");

    Chronometer timer;
    AdapterChat adapter;
    FloatingActionButton buttonScrollBack;
    LinearLayoutManager mLayoutManager;

    LinearLayout layoutChat, layoutBloq, linearNameUser, linear_edit_delete, linear_photo_view;
    LinearLayout linear_photo, linearCardMsg, linearBack, linearLottie, linearDate;

    ProgressDialog progress;
    ProgressBar loadingPhoto;

    private String suActual, id_user, name_user_final, estadoUser, estadoYo, token;
    private Integer noVisto, miNoVisto;
    private String stringMsg, nameAudio;
    private int msgType;

    RecyclerView rvMsg;
    ImageView cameraSelected, storageSelected, photo, cancel_action;

    static TextView countDeleteMsg, tv_date;
    static LinearLayout linearDeleteMsg, linear_timer;

    private ContentValues values;
    private Uri imageUri, resultUri;

    private MediaRecorder mediaRecorder;
    private String recorder = null;

    DatabaseReference startedByMe;
    DatabaseReference startedByHim;

    private String unknownName, idUserUnknown;

    ValueEventListener listenerChatUnknown;

    StorageReference refYourReceiverData, refMyReceiverData;
    Vibrator vibrator;

    private Bitmap thumbnail;
    String imageurl, refChat, refChatWith;

    public static String myPhoto, yourPhoto;

    byte [] thumb_byte;

    private float firstTouchX, dX, scale;
    private String id_user_final;
    private String myName;

    private CardView cardview_title;
    private TextView tv_chat_title;

    public static void selectedDeleteMsg(Chats chats) {

        msgSelected.add(chats);

        if (msgSelected.size() > 1) {
            linearDeleteMsg.setVisibility(View.VISIBLE);
            countDeleteMsg.setText(String.valueOf(msgSelected.size()));
        }
    }

    public static void setDate(String date) { tv_date.setText(date); }

    public static void notSelectedDeleteMsg(Chats chats) {

        int index = msgSelected.indexOf(chats);
        if (index != -1) {
            msgSelected.remove(index);

            if (msgSelected.isEmpty()) {
                linearDeleteMsg.setVisibility(View.GONE);
            }
            countDeleteMsg.setText(String.valueOf(msgSelected.size()));
        }
    }


    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cropImageLauncher = CropHelper.registerLauncherForActivity(
                this,                     // Activity
                refMyReceiverData,        // tu StorageReference de envío en 1 a 1
                linear_photo_view,
                linear_photo,
                photo,
                loadingPhoto,
                msg,
                btnCamera,
                btnSendMsg,
                uri -> { /* opcional: usar la Uri recortada si necesitás */ }
        );



        setContentView(R.layout.activity_chat);

        tv_chat_title = findViewById(R.id.tv_chat_title);
        cardview_title = findViewById(R.id.cardview_title);
        vibrator = (Vibrator) ChatActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
        scale = getResources().getDisplayMetrics().density;
        linearCardMsg = findViewById(R.id.linearCardMsg);
        tv_date = findViewById(R.id.tv_date);
        progress = new ProgressDialog(ChatActivity.this, R.style.AlertDialogApp);
        loadingPhoto = findViewById(R.id.loadingPhoto);
        photoList = new ArrayList<>();
        buttonScrollBack = findViewById(R.id.buttonScrollBack);
        linearBack = findViewById(R.id.linearBack);
        layoutChat = findViewById(R.id.layoutChat);
        layoutBloq = findViewById(R.id.layoutBloq);
        btnDesbloq = findViewById(R.id.btnDesbloq);
        cancel_action = findViewById(R.id.cancel_action);
        linearNameUser = findViewById(R.id.linearNameUser);
        linearLottie = findViewById(R.id.linearLottie);
        btnSendMsg = findViewById(R.id.btnSendMsg);
        btnCamera = findViewById(R.id.btnCamera);
        btnMic = findViewById(R.id.btnMic);
        btnMic.setVisibility(View.VISIBLE);
        btnCamera.setVisibility(View.VISIBLE);
        btnSendMsg.setVisibility(View.GONE);
        trashAnimated = findViewById(R.id.trashAnimated);
        linear_photo_view = findViewById(R.id.linear_photo_view);
        linear_photo = findViewById(R.id.linear_photo);
        linear_timer = findViewById(R.id.linear_timer);
        linearDate = findViewById(R.id.linearDate);
        photo = findViewById(R.id.photo);
        timer = findViewById(R.id.timer);
        linearDeleteMsg = findViewById(R.id.linearDeleteMsg);
        countDeleteMsg = findViewById(R.id.countDeleteMsg);
        trashAnimated2 = findViewById(R.id.trashAnimated2);

        img_user = findViewById(R.id.image_user);
        nameUser = findViewById(R.id.nameUser);
        tv_estado = findViewById(R.id.tv_estado);
        icon_conectado = findViewById(R.id.icon_conectado);
        icon_desconectado = findViewById(R.id.icon_desconectado);
        msg = findViewById(R.id.msg);


        rvMsg = findViewById(R.id.rvMsg);
        //rvMsg.setHasFixedSize(true);


        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        rvMsg.setLayoutManager(mLayoutManager);

        chatsArrayList = new ArrayList<>();
        adapter = new AdapterChat(chatsArrayList, maxChatSize, getApplicationContext());

        rvMsg.setAdapter(adapter);


        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        msgSelected.clear();



        id_user = getIntent().getExtras().getString("id_user");
        unknownName = getIntent().getExtras().getString("unknownName");
        idUserUnknown = getIntent().getExtras().getString("idUserUnknown");


//CHAT
        if (id_user != null){
            cardview_title.setVisibility(View.GONE);
            id_user_final = id_user;
            refChat = chat;
            refChatWith = chatWith;
            myPhoto = user.getPhotoUrl().toString();
            myName = user.getDisplayName();

            //Nombre y foto del user
            ref_cuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {

                        Users this_user = dataSnapshot.getValue(Users.class);
                        extraUserList.add(this_user);

                        yourPhoto = dataSnapshot.child("foto").getValue(String.class);
                        name_user_final = dataSnapshot.child("nombre").getValue(String.class);

                        nameUser.setText(name_user_final);
                        Glide.with(ChatActivity.this).load(yourPhoto).into(img_user);
                    }else{

                        ref_datos.child(user.getUid()).child(chatWith).child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                yourPhoto = snapshot.child("wUserPhoto").getValue(String.class);
                                name_user_final = snapshot.child("wUserName").getValue(String.class);

                                nameUser.setText(name_user_final);
                                Glide.with(ChatActivity.this).load(yourPhoto).into(img_user);

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




//CHAT_UNKNOWN
        } else {

            cardview_title.setVisibility(View.VISIBLE);
            tv_chat_title.setText("Chat privado en " + groupName);

            id_user_final = idUserUnknown;
            name_user_final = unknownName;
            refChat = unknown;
            refChatWith = chatWithUnknown;
            myName = userName;

            //Nombre y foto del user
            nameUser.setText(name_user_final);


            ref_group_users.child(groupName).child(idUserUnknown).child("type").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()){

                        int type = dataSnapshot.getValue(int.class);

                        if (type == 0){

                            yourPhoto = getApplicationContext().getString(R.string.URL_PHOTO_DEF);
                            Glide.with(ChatActivity.this).load(yourPhoto).into(img_user);

                        }else{

                            ref_cuentas.child(idUserUnknown).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if (dataSnapshot.exists()) {

                                        yourPhoto = dataSnapshot.child("foto").getValue(String.class);
                                        Glide.with(ChatActivity.this).load(yourPhoto).into(img_user);
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });



            //Mi foto
            if (userType == 0){
                myPhoto = this.getString(R.string.URL_PHOTO_DEF);
            }else{
                myPhoto = user.getPhotoUrl().toString();
            }


            listenerChatUnknown = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (!dataSnapshot.exists() || !Objects.equals(dataSnapshot.child("user_name").getValue(String.class), name_user_final)) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ChatActivity.this, R.style.AlertDialogApp));
                        builder.setMessage("Lo sentimos, " + name_user_final + " ya no esá disponible");
                        builder.setCancelable(false);
                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                finish();

                            }
                        });
                        builder.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            ref_group_users.child(groupName).child(id_user_final).addValueEventListener(listenerChatUnknown);




        }

        ref_cuentas.child(id_user_final).child("token").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                token = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });




        refYourReceiverData = storageReference.child(refChatWith + "/" + id_user_final + "/");
        refMyReceiverData = storageReference.child(refChatWith + "/" + user.getUid() + "/");


        startedByMe = ref_chat_path.child(refChat).child(user.getUid() + " <---> " + id_user_final).child("Mensajes");
        startedByHim = ref_chat_path.child(refChat).child(id_user_final + " <---> " + user.getUid()).child("Mensajes");



        setMicButton();

        linearDeleteMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                trashAnimated.playAnimation();

                DeleteMsgs();

            }

        });


        trashAnimated2.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                trashAnimated2.setVisibility(View.GONE);
            }
        });

        trashAnimated.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                linearDeleteMsg.setVisibility(View.GONE);
            }
        });


        TextView tv_cancel_audio = findViewById(R.id.tv_cancel_audio);

        final Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(300);
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        tv_cancel_audio.startAnimation(anim);



        //Si bloqueaste al usuario
        ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    if (snapshot.getValue(String.class).equals("bloq")) {

                        layoutBloq.setVisibility(View.VISIBLE);
                        layoutChat.setVisibility(View.GONE);
                        linearLottie.setVisibility(View.GONE);
                    } else {
                        layoutBloq.setVisibility(View.GONE);
                        layoutChat.setVisibility(View.VISIBLE);
                        linearLottie.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutBloq.setVisibility(View.GONE);
                    layoutChat.setVisibility(View.VISIBLE);
                    linearLottie.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                photoList.add(stringMsg);
                Intent intent = new Intent(ChatActivity.this, SlidePhotoActivity.class);
                intent.putExtra("photoList", photoList);
                intent.putExtra("position", 0);
                intent.putExtra("rotation", 180);
                startActivity(intent);

            }
        });

        cancel_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cancelSendPhoto();

            }
        });


        btnDesbloq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View view = findViewById(android.R.id.content);

                new Constants().desBloquear(ChatActivity.this, id_user_final, name_user_final, view, refChatWith);


            }
        });


/*
        ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
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

 */

        rvMsg.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                int totalItemCount = adapter.getItemCount();
                int lastVisible = mLayoutManager.findLastVisibleItemPosition();
                int firstVisible = mLayoutManager.findFirstVisibleItemPosition();

                boolean endHasBeenReached = lastVisible + 1 >= totalItemCount;
                if (totalItemCount >= 0 && endHasBeenReached) {
                    linearBack.setVisibility(View.GONE);
                    linearDate.setVisibility(View.GONE);
                } else {

                    linearBack.setVisibility(View.VISIBLE);
                    linearDate.setVisibility(View.VISIBLE);

                    Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        public void run() {

                            linearDate.setVisibility(View.GONE);

                        }
                    };
                    handler.postDelayed(runnable, 2000);


                    adapter.getDate(firstVisible);
                }
            }
        });


        buttonScrollBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScrollbar();
            }
        });



        msg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (msg.length() == 0) {
                    btnCamera.setVisibility(View.VISIBLE);
                    btnMic.setVisibility(View.VISIBLE);
                    btnSendMsg.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (msg.length() != 0) {
                    btnCamera.setVisibility(View.GONE);
                    btnMic.setVisibility(View.GONE);
                    btnSendMsg.setVisibility(View.VISIBLE);
                    ref_datos.child(user.getUid()).child("Estado").child("estado").setValue(getString(R.string.escribiendo));
                    ref_cuentas.child(user.getUid()).child("estado").setValue(true);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (msg.length() == 0) {
                    btnCamera.setVisibility(View.VISIBLE);
                    btnMic.setVisibility(View.VISIBLE);
                    btnSendMsg.setVisibility(View.GONE);
                    new Constants().StateOnLine(getApplicationContext(), user.getUid());
                }

            }
        });




        ref_datos.child(id_user_final).child("ChatList").child("Actual").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) suActual = dataSnapshot.getValue(String.class);
                else suActual = "";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });



//ESTADO
        new Constants().StateUser(getApplicationContext(), id_user_final, icon_conectado, icon_desconectado, tv_estado, refChatWith);





        linearNameUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent intent = new Intent(ChatActivity.this, PerfilActivity.class);
                intent.putExtra("id_user", id_user);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                 */


                if (extraUserList.size() == 1) {

                    Intent intent = new Intent(ChatActivity.this, SlideProfileActivity.class);

                    Collections.reverse(extraUserList);

                    intent.putExtra("userList", extraUserList);
                    intent.putExtra("position", 0);
                    intent.putExtra("rotation", 0);
                    v.getContext().startActivity(intent);
                }



            }
        });





        ref_datos.child(id_user_final).child(refChatWith).child(user.getUid()).child("noVisto").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    noVisto = 0; //Si nunca chateamos, seteo en 0
                } else {
                    noVisto = dataSnapshot.getValue(Integer.class); // Hubo un mensaje, extraigo y seteo
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        vistos();


        ref_datos.child(id_user_final).child(refChatWith).child(user.getUid()).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot wEstadoUser) {
                if (!wEstadoUser.exists()) {
                    estadoUser = refChatWith; //Si nunca chateamos, seteo en chat
                } else {
                    // CÓMO ME TIENE?: Si existe, seteo el estado
                    if (wEstadoUser.getValue(String.class).equals("delete")) { //Si borró mi chat
                        estadoUser = refChatWith; //Lo mostramos de nuevo
                    } else {
                        estadoUser = wEstadoUser.getValue(String.class);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot mEstadoUser) {
                if (!mEstadoUser.exists()) {
                    estadoYo = refChatWith; // Si nunca chateamos, seteo como chat
                } else {

                    if (mEstadoUser.getValue(String.class).equals("delete")) { //Si borré el chat
                        estadoYo = refChatWith; //Lo mostramos de nuevo
                    } else {
                        estadoYo = mEstadoUser.getValue(String.class);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendPhoto();
            }
        });






        btnMic.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (linear_photo_view.getVisibility() != View.VISIBLE) {

                    if (firstTouchX == 0.0) {
                        firstTouchX = btnMic.getX();

                    }

                    final SpringAnimation xAnimation = new SpringAnimation(btnMic, SpringAnimation.X);

                    SpringForce springForce = new SpringForce()
                            .setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
                            .setStiffness(SpringForce.STIFFNESS_HIGH)
                            .setFinalPosition(firstTouchX);
                    xAnimation.setSpring(springForce);

                    int action = MotionEventCompat.getActionMasked(event);

                    DisplayMetrics metrics = new DisplayMetrics();
                    WindowManager windowManager = (WindowManager) ChatActivity.this.getSystemService(Context.WINDOW_SERVICE);
                    windowManager.getDefaultDisplay().getMetrics(metrics);

                    int widthPixels = metrics.widthPixels / 2;

                    switch (action) {

                        case (MotionEvent.ACTION_DOWN):

                            sendAudio();
                            xAnimation.cancel();
                            vibrator.vibrate(80);
                            dX = v.getX() - event.getRawX() - (int) (75 * scale + 0.5f);

                            return true;

                        case (MotionEvent.ACTION_UP):

                            xAnimation.start();
                            stopRecordAudio();

                            return true;

                        case (MotionEvent.ACTION_MOVE):

                            if (linear_timer.getVisibility() == View.VISIBLE) {

                                if (event.getRawX() > widthPixels) {


                                    float move = event.getRawX() + dX;

                                    if (btnMic.isAnimating()) {

                                        btnMic.animate()
                                                .x(move)
                                                .setDuration(0)
                                                .start();
                                    }

                                } else {

                                    cancelRecordAudio();
                                    xAnimation.start();

                                }
                                return true;
                            }

                        default:
                            return true;
                    }
                }
                return true;
            }
        });



        //Enviar mensaje:
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                sendMessage(null);


            }
        });


        startedByMe.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                addChat(dataSnapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                actualizeChat(dataSnapshot);
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                deleteChat(dataSnapshot);
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        startedByHim.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                addChat(dataSnapshot);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                actualizeChat(dataSnapshot);
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                deleteChat(dataSnapshot);
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setScrollbar();
            }
        });


    }// FIN OnCreate

    public void cancelSendPhoto() {
        linear_photo_view.setVisibility(View.GONE);
        msg.setVisibility(View.VISIBLE);
        btnCamera.setVisibility(View.VISIBLE);
        btnMic.setVisibility(View.VISIBLE);
        btnSendMsg.setVisibility(View.GONE);
        msgType = MSG;
    }

    private void sendAudio() {

        if (estadoUser.equals("bloq")) {

            final Snackbar snack = Snackbar.make(ChatActivity.this.findViewById(android.R.id.content), "Mensaje no grabado: Estás bloqueado por el usuario", Snackbar.LENGTH_SHORT);
            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
            TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            snack.show();

        } else {

            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
            int RecordAudioPermission = ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.RECORD_AUDIO);
            int WriteStoragePermission = ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (RecordAudioPermission != PackageManager.PERMISSION_GRANTED || WriteStoragePermission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(ChatActivity.this, permissions, MIC_SELECTED);

            } else {

                startRecordAudio();

            }
        }
    }

    private void startRecordAudio() {

        if (mediaRecorder == null) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
            nameAudio = "AUDIO_" + simpleDateFormat.format(Calendar.getInstance().getTime()) + ".mp3";

            recorder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + nameAudio;
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(recorder);


            try {

                ref_datos.child(user.getUid()).child("Estado").child("estado").setValue(getString(R.string.grabando));
                ref_cuentas.child(user.getUid()).child("estado").setValue(true);

                mediaRecorder.prepare();
                mediaRecorder.start();

                setMicAnimated();

                msg.setVisibility(View.GONE);
                btnCamera.setVisibility(View.GONE);

                linear_timer.setVisibility(View.VISIBLE);
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();

            } catch (IOException e) {
                e.printStackTrace();

                cancelRecordAudio();
                Toast.makeText(this, "Se produjo un error", Toast.LENGTH_SHORT).show();
            }

        }

    }

    private void cancelRecordAudio() {

        if (mediaRecorder != null) {

            vibrator.vibrate(80);

            mediaRecorder.release();
            mediaRecorder = null;

            setMicButton();

            btnMic.cancelAnimation();
            btnMic.clearAnimation();

            trashAnimated2.setVisibility(View.VISIBLE);
            trashAnimated2.playAnimation();

            timer.stop();
            linear_timer.setVisibility(View.GONE);

            msg.setVisibility(View.VISIBLE);
            btnCamera.setVisibility(View.VISIBLE);

            new Constants().StateOnLine(getApplicationContext(), user.getUid());

        }
    }

    private void stopRecordAudio() {

        final String timerText = timer.getText().toString();

        if (!timerText.equals("00:00")) {

            if (mediaRecorder != null) {

                try {

                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;

                    InputStream stream = new FileInputStream(new File(recorder));

                    final UploadTask uploadTask = refYourReceiverData.child(nameAudio).putStream(stream);
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if (taskSnapshot.getMetadata() != null) {
                                if (taskSnapshot.getMetadata().getReference() != null) {
                                    Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            stringMsg = uri.toString();
                                            msgType = AUDIO;

                                            sendMessage(timerText);

                                        }
                                    });
                                }
                            }
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();

                    Toast.makeText(ChatActivity.this, "Hubo un error", Toast.LENGTH_SHORT).show();
                }
            }

            vibrator.vibrate(80);

            timer.stop();
            linear_timer.setVisibility(View.GONE);

            btnMic.cancelAnimation();
            btnMic.clearAnimation();

            setMicButton();

            msg.setVisibility(View.VISIBLE);
            btnCamera.setVisibility(View.VISIBLE);

            new Constants().StateOnLine(getApplicationContext(), user.getUid());

        }else{

            cancelRecordAudio();

        }

    }

    private void setMicAnimated() {

        int dp_200 = (int) (200 * scale + 0.5f);

        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(dp_200, dp_200);
        params2.gravity = Gravity.END|Gravity.BOTTOM;
        btnMic.setLayoutParams(params2);
        btnMic.setImageResource(android.R.color.transparent);
        btnMic.setBackground(getResources().getDrawable(R.color.fui_transparent));
        btnMic.setAnimation(R.raw.lf30_editor_24iqgref);

        btnMic.playAnimation();

        int dp_65 = (int) (65 * scale + 0.5f);

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.END|Gravity.BOTTOM;
        params.setMargins(0,0,-dp_65,-dp_65);
        linearLottie.setLayoutParams(params);


    }

    private void setMicButton() {
        btnMic.setBackground(getResources().getDrawable(R.drawable.marco_color_b_round));
        btnMic.setImageResource(R.drawable.ic_baseline_mic_24);

        int dp_10 = (int) (10 * scale + 0.5f);
        int dp_13 = (int) (13 * scale + 0.5f);
        int dp_50 = (int) (50 * scale + 0.5f);

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        linearLottie.setLayoutParams(params);

        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(dp_50, dp_50);
        params2.gravity = Gravity.END|Gravity.BOTTOM;
        btnMic.setLayoutParams(params2);
        params2.setMargins(dp_10,dp_10,dp_10,dp_10);
        btnMic.setPadding(dp_13,dp_13,dp_13,dp_13);
    }

    public void sendPhoto() {
        if (estadoUser.equals("bloq")) {

            final Snackbar snack = Snackbar.make(ChatActivity.this.findViewById(android.R.id.content), "Mensaje no enviado: Estás bloqueado por el usuario", Snackbar.LENGTH_SHORT);
            snack.setBackgroundTint(getResources().getColor(R.color.colorC));
            TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            snack.show();


        } else {


            final View viewFilter = getLayoutInflater().inflate(R.layout.profile_photo_layout, null);

            img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);
            cameraSelected = viewFilter.findViewById(R.id.cameraSelected);
            storageSelected = viewFilter.findViewById(R.id.storageSelected);
            tv_title = viewFilter.findViewById(R.id.tv_title);
            linear_edit_delete = viewFilter.findViewById(R.id.linear_edit_delete);

            tv_title.setText(getResources().getString(R.string.enviar_desde));
            linear_edit_delete.setVisibility(View.GONE);


            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ChatActivity.this, R.style.AlertDialogApp));
            builder.setView(viewFilter);
            builder.setCancelable(true);

            final AlertDialog dialog = builder.create();
            dialog.show();


            cameraSelected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                    int ReadStoragePermission = ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    int WriteStoragePermission = ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    int cameraPermission = ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.CAMERA);

                    if (cameraPermission != PackageManager.PERMISSION_GRANTED || WriteStoragePermission != PackageManager.PERMISSION_GRANTED ||
                            ReadStoragePermission != PackageManager.PERMISSION_GRANTED) {


                        ActivityCompat.requestPermissions(ChatActivity.this, permissions, CAMERA_SELECTED);

                    } else {

                        startCamera();

                    }

                    dialog.dismiss();
                }

            });

            storageSelected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PHOTO_SELECTED);

                    } else {

                        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(gallery, PHOTO_SELECTED);

                    }

                    dialog.dismiss();

                }
            });

            img_cancel_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        }
    }

    public void startCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());

        imageUri = getApplicationContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        startActivityForResult(intent, CAMERA_SELECTED);
    }

    public void sendMessage(String timerText) {

        String textMiChatw, textSuChatw;


        if (msgType == PHOTO) {

            textMiChatw = getResources().getString(R.string.photo_send);
            textSuChatw = getResources().getString(R.string.photo_received);

            linear_photo_view.setVisibility(View.GONE);
            msg.setVisibility(View.VISIBLE);
            btnCamera.setVisibility(View.VISIBLE);
            btnMic.setVisibility(View.VISIBLE);

/*
            if (thumb_byte != null) {

                File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Zibe");
                String state = Environment.getExternalStorageState();
                if (state.equals(Environment.MEDIA_MOUNTED)) {
                    if(!folder.mkdirs()){
                        Log.e("TAG", "Error: No se creo el directorio privado");

                    }
                }


                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyyHHmm");
                String name = "IMG_" + simpleDateFormat.format(Calendar.getInstance().getTime());


                File file = new File(folder, name + ".jpg");

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }


 */

        } else if (msgType == AUDIO) {

            textMiChatw = getResources().getString(R.string.audio_send);
            textSuChatw = getResources().getString(R.string.audio_received);


        }else{
            msgType = MSG;
            stringMsg = msg.getText().toString();
            textSuChatw = stringMsg;
            textMiChatw = stringMsg;
        }


        if (!stringMsg.equals("")) {


            if (estadoUser.equals("bloq")) {


                final Snackbar snack = Snackbar.make(ChatActivity.this.findViewById(android.R.id.content), "Mensaje no enviado: Estás bloqueado por el usuario", Snackbar.LENGTH_SHORT);
                snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snack.show();


            } else {

                int visto;
                if (!suActual.equals(user.getUid() + refChatWith) || suActual == null) {
                    visto = 1;
                } else {
                    visto = 3;
                }

                final Calendar c = Calendar.getInstance();
                final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");


                String date;
                if (timerText == null){
                    date = dateFormat3.format(c.getTime());

                }else{
                    date = dateFormat3.format(c.getTime()) + " " + timerText;

                }


                final Chats chatmsg = new Chats(
                        stringMsg,
                        date,
                        user.getUid(),
                        msgType,
                        visto);



                startedByMe.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull final DataSnapshot startedByMe) {
                        if (!startedByMe.exists()) { //Si No existe chat que yo haya iniciado

                            startedByHim.addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot startedByHim) {
                                    if (!startedByHim.exists()) { //Si no existe chat que él haya iniciado
                                        startedByMe.getRef().push().setValue(chatmsg);// Entonces soy el primero, creo uno
                                    } else {
                                        startedByHim.getRef().push().setValue(chatmsg);// Utilizo el que inició él
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });

                        } else {
                            startedByMe.getRef().push().setValue(chatmsg); //Utilizo el que inicié
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });


                final ChatWith miChat = new ChatWith(
                        textMiChatw,
                        date,
                        null,
                        user.getUid(),
                        id_user_final,
                        name_user_final,
                        yourPhoto,
                        estadoYo,
                        miNoVisto,
                        0);

                ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).setValue(miChat);


                //CardView


                if (!suActual.equals(user.getUid() + refChatWith) || suActual.equals(null)) { //Si no está chateando conmigo


                    final Integer count = noVisto + 1;


                    final ChatWith suChat = new ChatWith(
                            textSuChatw,
                            date,
                            null,
                            user.getUid(),
                            user.getUid(),
                            myName,
                            myPhoto,
                            estadoUser,
                            count,
                            1);
                    ref_datos.child(id_user_final).child(refChatWith).child(user.getUid()).setValue(suChat);

/*
                    ref_datos.child(id_user).child("ChatList").child("msgNoLeidos").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot msgNoLeidos) {
                            if (msgNoLeidos.exists()) {

                                final Integer newChats = msgNoLeidos.getValue(Integer.class);
                                final Integer contador = newChats + 1;
                                msgNoLeidos.getRef().setValue(contador);
                            } else {
                                msgNoLeidos.getRef().setValue(1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });

 */


                    //Notifications

/*
                    ref_datos.child(id_user).child("Settings").child("individualNotifications").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                            //boolean individualNotifications = dataSnapshot.getValue(boolean.class);

 */


                    //if (individualNotifications){

                    if (!estadoUser.equals("silent")) {


                        RequestQueue myrequest = Volley.newRequestQueue(getApplicationContext());
                        JSONObject json = new JSONObject();
                        try {

                            json.put("to", token);

                            JSONObject notification = new JSONObject();

                            notification.put("novistos", count);
                            notification.put("user", myName);
                            notification.put("msg", suChat.getwMsg());
                            notification.put("id_user", chatmsg.getEnvia());
                            notification.put("type", refChatWith);

                            json.put("priority", "high");

                            json.put("data", notification);

                            String URL = "https://fcm.googleapis.com/fcm/send";

                            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, json, null, null) {
                                @Override
                                public Map<String, String> getHeaders() {
                                    Map<String, String> header = new HashMap<>();

                                    header.put("content-type", "application/json");
                                    header.put("authorization", "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me");
                                    return header;

                                }
                            };

                            myrequest.add(request);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    //}
/*
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });

 */

                } else { //Está en mi mismo chat

                    final ChatWith suChat = new ChatWith(
                            textSuChatw,
                            date,
                            null,
                            user.getUid(),
                            user.getUid(),
                            myName,
                            myPhoto,
                            estadoUser,
                            0,
                            3);
                    ref_datos.child(id_user_final).child(refChatWith).child(user.getUid()).setValue(suChat);



                }

            }
            msg.setText("");
            stringMsg = null;
            msgType = MSG;
        }
    }

    private void actualizeChat(@NonNull DataSnapshot dataSnapshot) {
        Chats chat = dataSnapshot.getValue(Chats.class);
        adapter.actualizeMsg(chat);
    }

    private void deleteChat(@NonNull DataSnapshot dataSnapshot) {
        Chats chat = dataSnapshot.getValue(Chats.class);
        adapter.deleteMsg(chat);
    }

    private void addChat(@NonNull DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {

            Chats chat = dataSnapshot.getValue(Chats.class);
            adapter.addChat(chat);



        }
    }

    private void vistos() {

        ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot daataSnapshot) {

                miNoVisto = 0; //Exista o no, seteo en 0 porque entré y ví

                if (daataSnapshot.exists()) {

                    final Integer msgDescontar = daataSnapshot.child("noVisto").getValue(Integer.class); // Msg que no había leído, para descontar

                    if (msgDescontar > 0) {

                         startedByMe.orderByChild("date").limitToLast(msgDescontar).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                setDoubleCheck(dataSnapshot);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });

                        startedByHim.orderByChild("date").limitToLast(msgDescontar).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                setDoubleCheck(dataSnapshot);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    daataSnapshot.getRef().child("wVisto").setValue(3);// Seteo en VISTO el mensaje
                    daataSnapshot.getRef().child("noVisto").setValue(0); //Seteo en 0 mensajes no vistos

                    /*
                    ref_datos.child(user.getUid()).child("ChatList").child("msgNoLeidos").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {
                                Integer msgNoLeidos = dataSnapshot.getValue(Integer.class); //Si existe un mensaje, y existe msgNoLeidos: A msgNoLeidos le resto los no vistos
                                Integer result = msgNoLeidos - msgDescontar;
                                dataSnapshot.getRef().setValue(result);

                            } else {
                                dataSnapshot.getRef().setValue(0); //Si msgNoLeidos no existe, creo y seteo en 0
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });

                     */

                }
            }

            public void setDoubleCheck(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.hasChild("visto")) {
                            snapshot.getRef().child("visto").setValue(3);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setScrollbar() {
        rvMsg.scrollToPosition(adapter.getItemCount() - 1);
    }


    @Override
    protected void onPause() {
        super.onPause();

        new Constants().StateOffLine(getApplicationContext(), user.getUid());

        ref_actual.getRef().setValue("");

    }


    @Override
    protected void onResume() {
        super.onResume();

        new Constants().StateOnLine(getApplicationContext(), user.getUid());

        ref_actual.setValue(id_user_final + refChatWith);

    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_activity, menu);


        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_silent = menu.findItem(R.id.action_silent);
        final MenuItem action_notif = menu.findItem(R.id.action_notif);
        final MenuItem action_bloq = menu.findItem(R.id.action_bloq);
        final MenuItem action_desbloq = menu.findItem(R.id.action_desbloq);
        final MenuItem action_delete = menu.findItem(R.id.action_delete);


        action_delete.setVisible(true);


        //Para el menu1
        ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).child("estado").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    String state = dataSnapshot.getValue(String.class);

                    if (Objects.equals(state, "silent")) {
                        action_silent.setVisible(false);
                        action_notif.setVisible(true);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if (Objects.equals(state, refChatWith)) {
                        action_silent.setVisible(true);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if (Objects.equals(state, "bloq")) {
                        action_silent.setVisible(false);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(true);
                        action_bloq.setVisible(false);
                    }
                    if (Objects.equals(state, "delete")) {
                        action_silent.setVisible(true);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                }else{
                    action_silent.setVisible(true);
                    action_notif.setVisible(false);
                    action_desbloq.setVisible(false);
                    action_bloq.setVisible(true);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        View view = findViewById(android.R.id.content);
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;

        } else if (id == R.id.action_silent) { // Silenciar notificaciones
            new Constants().Silent(name_user_final, id_user_final, refChatWith);
            Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_notif) {
            new Constants().Silent(name_user_final, id_user_final, refChatWith);
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_bloq) { // Bloquear
            new Constants().Block(this, name_user_final, id_user_final, view, refChatWith);

        } else if (id == R.id.action_desbloq) { // Desbloquear
            new Constants().desBloquear(this, id_user_final, name_user_final, view, refChatWith);

        } else if (id == R.id.action_delete) { // Eliminar
            new Constants().DeleteChat(this, id_user_final, name_user_final, view, refChatWith);
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        if (refChatWith.equals(chatWithUnknown)) {
            ref_group_users.child(groupName).child(id_user_final).removeEventListener(listenerChatUnknown);
        }
        ref_actual.getRef().setValue("");
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);


        CropHelper.launchCrop(cropImageLauncher, imageUri);





//        if (requestCode == CAMERA_SELECTED && resultCode == RESULT_OK) {
//
//
//            CropImage.activity(imageUri)
//                    .setGuidelines(CropImageView.Guidelines.ON)
//                    .setRequestedSize(1920, 1080)
//                    .start(ChatActivity.this);
//
//        }
//
//
//        if (requestCode == PHOTO_SELECTED && resultCode == RESULT_OK) {
//
//            Uri imgUri = CropImage.getPickImageResultUri(getApplicationContext(), data);
//
//            CropImage.activity(imgUri)
//                    .setGuidelines(CropImageView.Guidelines.ON)
//                    .setRequestedSize(1920, 1080)
//                    .start(ChatActivity.this);
//
//        }

//
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//
//            if (resultCode == RESULT_OK) {
//
//                resultUri = result.getUri();
//                msgType = PHOTO;
//                linear_photo_view.setVisibility(View.VISIBLE);
//                msg.setVisibility(View.GONE);
//
//                btnCamera.setVisibility(View.GONE);
//
//                DisplayMetrics metrics = new DisplayMetrics();
//                getWindowManager().getDefaultDisplay().getMetrics(metrics);
//                int widthPixels = metrics.widthPixels;
//
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                        widthPixels / 3, widthPixels / 3);
//
//                linear_photo.setLayoutParams(layoutParams);
//
//                //layoutChat.setLayoutParams(new LinearLayout.LayoutParams(
//                 //       ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//
//                //refSendImages.child(resultUri.getLastPathSegment());
//                final StorageReference refPic = refYourReceiverData.child(resultUri.getLastPathSegment());
//
//
//                refPic.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        if (taskSnapshot.getMetadata() != null) {
//                            if (taskSnapshot.getMetadata().getReference() != null) {
//                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
//                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
//                                    @Override
//                                    public void onSuccess(Uri uri) {
//
//                                        stringMsg = uri.toString();
//                                        int dp_15 = (int) (15 * scale + 0.5f);
//                                        loadingPhoto.setVisibility(View.VISIBLE);
//                                        Glide.with(ChatActivity.this)
//                                                .load(stringMsg)
//                                                .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(dp_15)))
//                                                .listener(new RequestListener<Drawable>() {
//                                                    @Override
//                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//
//                                                        cancelSendPhoto();
//
//                                                        return false;
//                                                    }
//
//                                                    @Override
//                                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                                                        loadingPhoto.setVisibility(View.GONE);
//                                                        btnSendMsg.setVisibility(View.VISIBLE);
//                                                        btnMic.setVisibility(View.GONE);
//                                                        return false;
//                                                    }
//
//                                                })
//                                                .into(photo);
//
//
//                                    }
//                                });
//                            }
//                        }
//                    }
//                });
//
//
//                try {
//                    thumbnail = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), resultUri);
//                    imageurl = getRealPathFromURI(resultUri);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//
//
//
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//                thumb_byte = byteArrayOutputStream.toByteArray();
//
//
//
//
//
//            }
//        }

        progress.dismiss();
    }


    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = ChatActivity.this.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_SELECTED) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera();
            }
        }

        if (requestCode == PHOTO_SELECTED) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PHOTO_SELECTED);
            }
        }

        if (requestCode == MIC_SELECTED) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startRecordAudio();
            }
        }

    }


    public boolean onContextItemSelected(MenuItem item) {


        if (item.getGroupId() == 1) {

            DeleteMsgs();

        }
        return true;
    }


    public void RemoveChatWith(final int countList) {

        startedByMe.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    deleteChatWith(dataSnapshot);
                } else {
                    startedByHim.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                deleteChatWith(dataSnapshot);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            public void deleteChatWith(@NonNull DataSnapshot dataSnapshot) {

                long messages = dataSnapshot.getChildrenCount();
                final ArrayList<String> senderDelete = new ArrayList<>();
                final ArrayList<String> receiverDelete = new ArrayList<>();

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

                long count = messages - (senderDelete.size() + receiverDelete.size() + countList);

                if (count == 0) {
                    ref_datos.child(user.getUid()).child(refChatWith).child(id_user_final).removeValue();
                    onBackPressed();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void DeleteMsgs() {


        for (final Chats chat : msgSelected) {

            startedByMe.orderByChild("date").equalTo(chat.getDate()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                        iterator(dataSnapshot);
                    } else {
                        startedByHim.orderByChild("date").equalTo(chat.getDate()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()) {
                                    iterator(dataSnapshot);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }

                public void iterator(@NonNull DataSnapshot dataSnapshot) {

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
                                int startString = chat.getMensaje().indexOf(id_user_final) + id_user_final.length() + 3;
                                int endString = chat.getMensaje().indexOf(".jpg") + 4;
                                refYourReceiverData.child(chat.getMensaje().substring(startString, endString)).delete();
                                snapshot.getRef().removeValue();
                            }
                            if (type == AUDIO) {
                                snapshot.child("type").getRef().setValue(AUDIO_SENDER_DLT);
                            }
                            if (type == AUDIO_RECEIVER_DLT) {
                                int startString = chat.getMensaje().indexOf(id_user_final) + id_user_final.length() + 3;
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

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });


        }


        final int count = msgSelected.size();
        if (count > 1) {
            Toast.makeText(this, count + " mensajes eliminados", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,  "mensaje eliminado", Toast.LENGTH_SHORT).show();
        }


        RemoveChatWith(count);

        msgSelected.removeAll(msgSelected);

        countDeleteMsg.setText(String.valueOf(msgSelected.size()));
    }


}