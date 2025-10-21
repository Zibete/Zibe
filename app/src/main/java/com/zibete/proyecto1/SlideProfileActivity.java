package com.zibete.proyecto1;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.Adapters.SliderProfileAdapter;
import com.zibete.proyecto1.POJOS.Users;

import java.util.ArrayList;
import java.util.Objects;

import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.MainActivity.ref_datos;

public class SlideProfileActivity extends AppCompatActivity {

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ProgressBar progressbarImage;

    ArrayList <Users> userList;
    ViewPager viewPager;

    public SlideProfileActivity(){
        //Constructor vacío
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slide_activity);




        progressbarImage = findViewById(R.id.progressbarImage);
        progressbarImage.setVisibility(View.GONE);
        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userList = (ArrayList<Users>) getIntent().getExtras().getSerializable("userList");

        final int position = getIntent().getExtras().getInt("position");
        final int rotation = getIntent().getExtras().getInt("rotation");




        LinearLayout linearSlide = findViewById(R.id.linearSlide);

        viewPager = new ViewPager(this);
        viewPager.setId(View.generateViewId());


        viewPager.setAdapter(new SliderProfileAdapter(this, userList, rotation));



        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        //viewPager.setRotationY(180);
        //viewPager.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);


        //linearSlide.setRotationY(180);


        viewPager.setLayoutParams(params);



        linearSlide.addView(viewPager, viewPager.getLayoutParams());
        viewPager.setCurrentItem(position);



    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

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

        Users users = userList.get(viewPager.getCurrentItem());
        //Para el menu1
        ref_datos.child(user.getUid()).child(chatWith).child(users.getID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    String state = dataSnapshot.child("estado").getValue(String.class);

                    if(Objects.equals(state, "silent")){
                        action_silent.setVisible(false);
                        action_notif.setVisible(true);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if(Objects.equals(state, chatWith)){
                        action_silent.setVisible(true);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(false);
                        action_bloq.setVisible(true);
                    }
                    if(Objects.equals(state, "bloq")){
                        action_silent.setVisible(false);
                        action_notif.setVisible(false);
                        action_desbloq.setVisible(true);
                        action_bloq.setVisible(false);
                    }
                    if(Objects.equals(state, "delete")){
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





/*
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if(item.getGroupId() == 1) {

            final Users users = userList.get(item.getOrder());
            String id_user = users.getID();
            String name_user = users.getNombre();



            View view = findViewById(android.R.id.content);

            if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
                return true;
            }





            switch (item.getItemId()) {
                case R.id.action_silent: //Silenciar notificaciones
                    new Constants().Silenciar(id_user);
                    Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();

                    break;

                case R.id.action_notif:

                    new Constants().Silenciar(id_user);
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();

                    break;

                case R.id.action_bloq: //Bloquear


                    new Constants().Bloquear(this, id_user, name_user, view);

                    break;

                case R.id.action_desbloq: //DESBloquear

                    new Constants().desBloquear(this, id_user, name_user, view);

                    break;


                case R.id.action_delete: //Eliminar

                    View vista = getLayoutInflater().inflate(R.layout.dialog_delete,null);
                    new Constants().Eliminar(this, id_user, name_user, vista);

                    break;
            }
        }
        return true;
    }

*/












    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Users users = userList.get(viewPager.getCurrentItem());
        View view = findViewById(android.R.id.content);
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();

        } else if (id == R.id.action_silent) { // Silenciar notificaciones
            new Constants().Silent(users.getNombre(), users.getID(), chatWith);
            Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_notif) {
            new Constants().Silent(users.getNombre(), users.getID(), chatWith);
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.action_bloq) { // Bloquear
            new Constants().Block(this, users.getNombre(), users.getID(), view, chatWith);

        } else if (id == R.id.action_desbloq) { // Desbloquear
            new Constants().desBloquear(this, users.getID(), users.getNombre(), view, chatWith);

        } else if (id == R.id.action_delete) { // Eliminar
            new Constants().DeleteChat(this, users.getID(), users.getNombre(), view, chatWith);
        }

        return super.onOptionsItemSelected(item);
    }















    @Override
    protected void onPause() {
        super.onPause();
        new Constants().StateOffLine(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Constants().StateOnLine(getApplicationContext(), user.getUid());
    }

}
