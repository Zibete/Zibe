package com.zibete.proyecto1;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import static com.zibete.proyecto1.utils.Constants.CALLING;
import static com.zibete.proyecto1.utils.Constants.RINGING;
import static com.zibete.proyecto1.utils.FirebaseRefs.refCuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;

public class CallingActivity extends AppCompatActivity {

    ImageView img_user;
    TextView tv_user_name;
    CardView accept_call, decline_call;
    String id_user;

    @Override
    public void onStart(){
        super.onStart();

        refDatos.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(CALLING) && !dataSnapshot.hasChild(RINGING)) {

                    HashMap <String,Object> callingInfo = new HashMap<>();

                    callingInfo.put("Uid", id_user);



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }




    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_calling);

        img_user = findViewById(R.id.img_user);
        tv_user_name = findViewById(R.id.tv_user_name);
        accept_call = findViewById(R.id.accept_call);
        decline_call = findViewById(R.id.decline_call);

        id_user = getIntent().getExtras().getString("id_user");

        refCuentas.child(id_user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    String photo = dataSnapshot.child("foto").getValue(String.class);
                    String name = dataSnapshot.child("nombre").getValue(String.class);

                    Glide.with(CallingActivity.this).load(photo).into(img_user);
                    tv_user_name.setText(name);

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });



    }
}
