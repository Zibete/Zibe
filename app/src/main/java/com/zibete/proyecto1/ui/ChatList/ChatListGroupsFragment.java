package com.zibete.proyecto1.ui.ChatList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.Adapters.AdapterChatGroupsLista;
import com.zibete.proyecto1.Constants;
import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.MainActivity.ref_datos;

public class ChatListGroupsFragment extends Fragment implements SearchView.OnQueryTextListener{

    ProgressBar progressbar;
    RecyclerView rv;
    static ArrayList <ChatWith> chatsGroupArrayList = new ArrayList<>();
    LinearLayout linearOnBoardingChatList;
    LinearLayout tv_linearOnBoardingChatList;
    Context context;
    LottieAnimationView lottieChatRight, lottieChatLeft;
    private AdapterChatGroupsLista adapterChatGroupsLista;
    LinearLayoutManager mLayoutManager;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public ChatListGroupsFragment() {
        //Constructor vacío
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        setHasOptionsMenu(true);

        progressbar = view.findViewById(R.id.progressbar2);
        linearOnBoardingChatList = view.findViewById(R.id.linearOnBoardingChatList);
        mLayoutManager = new LinearLayoutManager(getContext());

        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        rv = view.findViewById(R.id.rv);
        rv.setLayoutManager(mLayoutManager);
        adapterChatGroupsLista = new AdapterChatGroupsLista(chatsGroupArrayList, getContext());
        rv.setAdapter(adapterChatGroupsLista);
        registerForContextMenu(rv);
        lottieChatRight = view.findViewById(R.id.lottieChatRight);
        lottieChatLeft = view.findViewById(R.id.lottieChatLeft);

//CREAR LISTA
        chatsGroupArrayList.clear();
        ref_datos.child(user.getUid()).child(chatWithUnknown).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


                if (dataSnapshot.exists()){



                    if (!dataSnapshot.child("wUserPhoto").equals("")) {
                        ChatWith chat = dataSnapshot.getValue(ChatWith.class);
                        adapterChatGroupsLista.addChats(chat);
                    }

                    @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    for (ChatWith wChat : chatsGroupArrayList){


                        try {
                            Date date = dateFormat3.parse(wChat.getwDate());
                            wChat.setDateDate(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }

                }

                Collections.sort(chatsGroupArrayList);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


//CADA VEZ QUE CAMBIA ALGO
                ref_datos.child(user.getUid()).child(chatWithUnknown).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {

                            ArrayList<ChatWith> chatsArrayList2 = new ArrayList<>();
                            chatsArrayList2.clear();


                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                ChatWith chat = snapshot.getValue(ChatWith.class);
                                chatsArrayList2.add(chat);

                            }


                            @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            for (ChatWith wChat : chatsArrayList2){


                                try {
                                    Date date = dateFormat3.parse(wChat.getwDate());
                                    wChat.setDateDate(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                            }

                            Collections.sort(chatsArrayList2);

                            adapterChatGroupsLista.updateData(chatsArrayList2);

                        } else {
                            progressbar.setVisibility(View.GONE);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                ChatWith chatWith = dataSnapshot.getValue(ChatWith.class);
                adapterChatGroupsLista.deleteChat(chatWith);

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        adapterChatGroupsLista.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setScrollbar();
                progressbar.setVisibility(View.GONE);
            }
        });





        //Mostrar NO HAS CHATEADO
        ref_datos.child(user.getUid()).child(chatWithUnknown).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    long count = 0;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("estado").getValue(String.class).equals(chatWithUnknown) | snapshot.child("estado").getValue(String.class).equals("silent")) {
                            count = count + 1;
                        }
                    }

                    if (count == 0) {
                        rv.setVisibility(View.GONE);
                        linearOnBoardingChatList.setVisibility(View.VISIBLE);
                        lottieChatLeft.playAnimation();
                        new Handler().postDelayed(new Runnable(){
                            public void run(){
                                lottieChatRight.playAnimation();
                            }
                        }, 100);

                    }else{
                        rv.setVisibility(View.VISIBLE);
                        linearOnBoardingChatList.setVisibility(View.GONE);
                        lottieChatLeft.cancelAnimation();
                        lottieChatRight.cancelAnimation();
                    }
                }else{
                    rv.setVisibility(View.GONE);
                    linearOnBoardingChatList.setVisibility(View.VISIBLE);
                    lottieChatLeft.playAnimation();
                    new Handler().postDelayed(new Runnable(){
                        public void run(){
                            lottieChatRight.playAnimation();
                        }
                    }, 100);
                }
                progressbar.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
            }
        });



        rv.setVisibility(View.VISIBLE);


        return view;
    }


    private void setScrollbar(){
        rv.scrollToPosition(adapterChatGroupsLista.getItemCount()-1);
    }






    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapterChatGroupsLista.getFilter().filter(newText);
        return false;
    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_search = menu.findItem(R.id.action_search);
        final MenuItem action_desbloqUsers = menu.findItem(R.id.action_unlock);
        final MenuItem action_favoritos = menu.findItem(R.id.action_favorites);
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        action_exit.setVisible(true);
        action_search.setVisible(true);
        action_desbloqUsers.setVisible(true);
        action_favoritos.setVisible(true);

        SearchView searchView = (SearchView) action_search.getActionView();

        searchView.setOnQueryTextListener(this);


    }


}