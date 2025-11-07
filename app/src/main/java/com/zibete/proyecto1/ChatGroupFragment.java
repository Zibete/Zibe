package com.zibete.proyecto1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.activity.result.ActivityResultLauncher;

import com.canhub.cropper.CropImageContractOptions;


import com.zibete.proyecto1.adapters.AdapterChatGroup;
import com.zibete.proyecto1.model.ChatsGroup;
import com.zibete.proyecto1.utils.CropHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.zibete.proyecto1.Constants.CAMERA_SELECTED;
import static com.zibete.proyecto1.Constants.MSG;
import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_SELECTED;
import static com.zibete.proyecto1.Constants.maxChatSize;
import static com.zibete.proyecto1.MainActivity.badgeDrawableGroup;
import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupChat;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.MainActivity.toolbar;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.inGroup;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userDate;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userName;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.userType;

public class ChatGroupFragment extends Fragment {
    EditText msg;
    ImageView btnCamera;
    private ImageView btnSendMsg;
    public static ChildEventListener listenerGroupChat;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ArrayList<ChatsGroup> chatsArrayList;
    AdapterChatGroup adapter;
    FloatingActionButton buttonScrollBack;
    LinearLayoutManager mLayoutManager;
    LinearLayout layoutChat, linear_photo_view, linearBack;

    MaterialCardView card_edit_delete;
    ProgressDialog progress;
    ProgressBar loadingPhoto;
    LinearLayout linearOnBoardingGroupChat;
    private String stringMsg;
    private int msgType;
    LinearLayout linear_photo, layout_chat_group;
    RecyclerView rvMsg;
    ImageView photo, cancel_action, img_cancel_dialog;
    MaterialCardView cameraSelection, gallerySelection;
    TextView tv_title;
    ProgressBar progressbar;

    FrameLayout frameSendMsg;
    private ContentValues values;
    private Uri imageUri;
    private Uri resultUri;
    ArrayList <String> photoList;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    StorageReference refSendImages = storageReference.child("Chats/" + user.getUid() + "/");
    static TextView countDeleteMsg;
    // 1️⃣ Registrar launcher
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }





    public ChatGroupFragment() {
        //Constructor vacío
    }







    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_group, container, false);


        setHasOptionsMenu(true);

        badgeDrawableGroup.setVisible(false);






        refGroupChat.child(groupName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (toolbar.getTitle().equals(groupName)) {
                    if(!groupName.equals("")) {
                        int count = (int) snapshot.getChildrenCount();
                        refDatos.child(user.getUid()).child("ChatList").child("msgReadGroup").setValue(count);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });







        linearOnBoardingGroupChat = view.findViewById(R.id.linearOnBoardingGroupChat);
        layout_chat_group = view.findViewById(R.id.layout_chat_group);
        progress = new ProgressDialog(getContext(), R.style.AlertDialogApp);
        loadingPhoto = view.findViewById(R.id.loadingPhoto);
       // group_name = getArguments().getString("group_name");
       // getUid = getArguments().getString("getUid");
        photoList = new ArrayList<>();
        buttonScrollBack = view.findViewById(R.id.buttonScrollBack);
        linearBack = view.findViewById(R.id.linearBack);
        layoutChat = view.findViewById(R.id.layoutChat);
        frameSendMsg = view.findViewById(R.id.frameSendMsg);
        progressbar = view.findViewById(R.id.progressbar2);

        cancel_action = view.findViewById(R.id.cancel_action);

        btnCamera = view.findViewById(R.id.btnCamera);
        linear_photo_view = view.findViewById(R.id.linear_photo_view);
        linear_photo = view.findViewById(R.id.linear_photo);
        photo = view.findViewById(R.id.photo);


        countDeleteMsg = view.findViewById(R.id.countDeleteMsg);








        getContext();
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();



                photo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        photoList.add(stringMsg);
                        Intent intent = new Intent(getContext(), SlidePhotoActivity.class);
                        intent.putExtra("photoList", photoList);
                        intent.putExtra("position", 0);
                        intent.putExtra("rotation", 180);
                        startActivity(intent);

                    }
                });

        cancel_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linear_photo_view.setVisibility(View.GONE);
                msg.setVisibility(View.VISIBLE);
                btnCamera.setVisibility(View.VISIBLE);
                btnSendMsg.setVisibility(View.GONE);
                msgType = MSG;

            }
        });



        layout_chat_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //...
            }
        });


        msg = view.findViewById(R.id.msg);
        btnSendMsg = view.findViewById(R.id.btnSendMsg);

        rvMsg = view.findViewById(R.id.rvMsg);
        //rvMsg.setHasFixedSize(true);


        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        rvMsg.setLayoutManager(mLayoutManager);

        chatsArrayList = new ArrayList<>();
        adapter = new AdapterChatGroup(chatsArrayList, maxChatSize, getContext());

        rvMsg.setAdapter(adapter);




        rvMsg.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                int totalItemCount = adapter.getItemCount();
                int lastVisible = mLayoutManager.findLastVisibleItemPosition();

                boolean endHasBeenReached = lastVisible + 1 >= totalItemCount;
                if (totalItemCount >= 0 && endHasBeenReached) {
                    linearBack.setVisibility(View.GONE);
                }else{

                    linearBack.setVisibility(View.VISIBLE);

                }
            }
        });


        buttonScrollBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setScrollbar();
            }
        });



        btnCamera.setVisibility(View.VISIBLE);
        btnSendMsg.setVisibility(View.GONE);
        msg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (msg.length() == 0) {
                    btnCamera.setVisibility(View.VISIBLE);
                    btnSendMsg.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (msg.length() != 0) {
                    btnCamera.setVisibility(View.GONE);
                    btnSendMsg.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (msg.length() == 0) {
                    btnCamera.setVisibility(View.VISIBLE);
                    btnSendMsg.setVisibility(View.GONE);
                }
            }
        });



        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendPhoto();
            }
        });


        //Enviar mensaje:
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                sendMessage();


            }
        });

        refGroupChat.child(groupName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    progressbar.setVisibility(View.GONE);
                    linearOnBoardingGroupChat.setVisibility(View.GONE);
                    rvMsg.setVisibility(View.VISIBLE);
                }else{
                    progressbar.setVisibility(View.GONE);
                    linearOnBoardingGroupChat.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        listenerGroupChat = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                if (dataSnapshot.exists()) {

                    if (inGroup) {

                        //final Calendar c = Calendar.getInstance();
                        @SuppressLint("SimpleDateFormat") final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");

                        //DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss:SS");
                        ChatsGroup chat = dataSnapshot.getValue(ChatsGroup.class);

                        Date dateChat = null;
                        try {
                            dateChat = fmt.parse(chat.getDateTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Date dateUser = null;
                        try {
                            dateUser = fmt.parse(userDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }


                        if (dateChat != null) {
                            if (dateChat.after(dateUser)) {
                                adapter.addChat(chat);
                            }
                        }
                    }
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };


        refGroupChat.child(groupName).addChildEventListener(listenerGroupChat);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setScrollbar();


            }
        });

        cropImageLauncher = CropHelper.registerLauncherForFragment(
                this,
                refSendImages,
                linear_photo_view,
                linear_photo,
                photo,
                loadingPhoto,
                loadingPhoto,
                frameSendMsg,
                msg,
                btnCamera,
                btnSendMsg,
                uri -> { /* opcional */ }
        );

        return view;
    }// FIN OnCreate

    public void sendPhoto() {


        final View viewFilter = getLayoutInflater().inflate(R.layout.select_source_pic, null);

        img_cancel_dialog = viewFilter.findViewById(R.id.img_cancel_dialog);
        cameraSelection = viewFilter.findViewById(R.id.cameraSelection);
        gallerySelection = viewFilter.findViewById(R.id.gallerySelection);
        tv_title = viewFilter.findViewById(R.id.tv_title);
        card_edit_delete = viewFilter.findViewById(R.id.card_edit_delete);

        tv_title.setText(getResources().getString(R.string.enviar_desde));
        card_edit_delete.setVisibility(View.GONE);


        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogApp));
        builder.setView(viewFilter);
        builder.setCancelable(true);

        final AlertDialog dialog = builder.create();
        dialog.show();


        cameraSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                int ReadStoragePermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.READ_EXTERNAL_STORAGE);
                int WriteStoragePermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE);
                int cameraPermission = ContextCompat.checkSelfPermission(getContext(),Manifest.permission.CAMERA);

                if (cameraPermission != PackageManager.PERMISSION_GRANTED || WriteStoragePermission != PackageManager.PERMISSION_GRANTED ||
                        ReadStoragePermission!= PackageManager.PERMISSION_GRANTED) {


                    requestPermissions(permissions, CAMERA_SELECTED);


                }else {

                    startCamera();

                }

                dialog.dismiss();
            }

        });

        gallerySelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PHOTO_SELECTED);


                }else{

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

    public void sendMessage() {

        final String textSuChatw;

        if (msgType == PHOTO){

            textSuChatw = getResources().getString(R.string.photo_received);
            linear_photo_view.setVisibility(View.GONE);
            msg.setVisibility(View.VISIBLE);
            btnCamera.setVisibility(View.VISIBLE);

        }else{
            msgType = MSG;
            stringMsg = msg.getText().toString();
            textSuChatw = stringMsg;
        }


        if (!stringMsg.equals("")) {

            final Calendar c = Calendar.getInstance();
            final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS");


            final ChatsGroup chatmsg = new ChatsGroup(
                    stringMsg,
                    dateFormat3.format(c.getTime()),
                    userName,
                    user.getUid(),
                    msgType,
                    userType);
            refGroupChat.child(groupName).push().setValue(chatmsg);



            refGroupUsers.child(groupName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        String user_id = snapshot.child("user_id").getValue(String.class);

                        if (!user_id.equals(user.getUid())) {

                            refCuentas.child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    String token = snapshot.child("token").getValue(String.class);

                                    if (!token.equals("")) {

                                        RequestQueue myrequest = Volley.newRequestQueue(getContext());
                                        JSONObject json = new JSONObject();
                                        try {

                                            json.put("to", token);

                                            JSONObject notification = new JSONObject();

                                            notification.put("novistos", "");
                                            notification.put("user", userName);
                                            notification.put("msg", textSuChatw);
                                            notification.put("id_user", user.getUid());
                                            notification.put("type", groupName);

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


            msg.setText("");
            //stringMsg = null;
            msgType = MSG;
        }
    }






    private void setScrollbar(){
        rvMsg.scrollToPosition(adapter.getItemCount()-1);
    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_search = menu.findItem(R.id.action_search);
        final MenuItem action_desbloqUsers = menu.findItem(R.id.action_unlock);
        final MenuItem action_favoritos = menu.findItem(R.id.action_favorites);
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        action_exit.setVisible(true);
        action_search.setVisible(false);
        action_desbloqUsers.setVisible(true);
        action_favoritos.setVisible(true);
    }






    public void startCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, System.currentTimeMillis());

        imageUri = getContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        startActivityForResult(intent, CAMERA_SELECTED);
    }







    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        progress.setMessage("Espere...");
        progress.show();
        progress.setCanceledOnTouchOutside(false);


        CropHelper.launchCrop(cropImageLauncher, imageUri);


        progress.dismiss();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_SELECTED){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera();
            }
        }

        if (requestCode == PHOTO_SELECTED){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PHOTO_SELECTED);
            }
        }
    }







}