package com.zibete.proyecto1.ui.ChatList;

import android.annotation.SuppressLint;
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
import com.zibete.proyecto1.adapters.AdapterChatLista;
import com.zibete.proyecto1.Constants;
import com.zibete.proyecto1.model.ChatWith;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.utils.UserRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import static com.zibete.proyecto1.Constants.Empty;
import static com.zibete.proyecto1.Constants.FRAGMENT_ID_CHATGROUPLIST;
import static com.zibete.proyecto1.Constants.FRAGMENT_ID_CHATLIST;
import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.Constants.chatWithUnknown;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.ui.ChatList.ChatListGroupsFragment.chatsGroupArrayList;

public class ChatListFragment extends Fragment implements SearchView.OnQueryTextListener{

    LottieAnimationView lottieChatRight, lottieChatLeft;
    ProgressBar progressbar;
    RecyclerView rv;
    ArrayList <ChatWith> chatsArrayList = new ArrayList<>();
    LinearLayout linearOnBoardingChatList;
    LinearLayout tv_linearOnBoardingChatList;

    ArrayList<ChatWith> getSorted(ArrayList<ChatWith> chatsArrayList2) {

        ArrayList<ChatWith> list = chatsArrayList2;

        Collections.sort(list, new Comparator<ChatWith>() {
            @Override
            public int compare(ChatWith o1, ChatWith o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });

        return list;
    }


    private AdapterChatLista adapterChatLista;
    LinearLayoutManager mLayoutManager;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public ChatListFragment() {
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
        adapterChatLista = new AdapterChatLista(chatsArrayList, getContext());
        rv.setAdapter(adapterChatLista);
        registerForContextMenu(rv);

        lottieChatRight = view.findViewById(R.id.lottieChatRight);
        lottieChatLeft = view.findViewById(R.id.lottieChatLeft);




/*
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels; //Ancho
        int heightPixels = metrics.heightPixels; //Alto

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, widthPixels/2);

        linearOnBoardingChatList.setLayoutParams(layoutParams);

 */











//CHAT LISTA
//CREAR LISTA

        refDatos.child(user.getUid()).child(chatWith).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


                if (dataSnapshot.exists()) {


                        ChatWith chat = dataSnapshot.getValue(ChatWith.class);
                        adapterChatLista.addChats(chat);



                    @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    for (ChatWith wChat : chatsArrayList) {


                        try {
                            Date date = dateFormat3.parse(wChat.getDateTime());
                            wChat.setDate(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                }
                Collections.sort(chatsArrayList);


            }

            //CADA VEZ QUE CAMBIA ALGO
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


                ValueEventListener listenerChangeChatList = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {

                            ArrayList <ChatWith> chatsArrayList2 = new ArrayList<>();
                            chatsArrayList2.clear();


                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                ChatWith chat = snapshot.getValue(ChatWith.class);
                                chatsArrayList2.add(chat);

                            }

                            @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            for (ChatWith wChat : chatsArrayList2){

                                try {
                                    Date date = dateFormat3.parse(wChat.getDateTime());
                                    wChat.setDate(date);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                            }

                            Collections.sort(chatsArrayList2);

                            adapterChatLista.updateData(chatsArrayList2);

                        } else {

                            progressbar.setVisibility(View.GONE);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };

                refDatos.child(user.getUid()).child(chatWith).addValueEventListener(listenerChangeChatList);

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {


                ChatWith chatWith = dataSnapshot.getValue(ChatWith.class);
                adapterChatLista.deleteChat(chatWith);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });






/*
        ref_datos.child(user.getUid()).child("ChatList").child("nuevoMensaje").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    final Integer newChats = dataSnapshot.getValue(Integer.class);
                    if (newChats > 0) {
                        dataSnapshot.getRef().setValue(0);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

 */




        adapterChatLista.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setScrollbar();
                progressbar.setVisibility(View.GONE);
            }
        });





        //Mostrar NO HAS CHATEADO
        refDatos.child(user.getUid()).child(chatWith).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    long count = 0;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        String state = snapshot.child("estado").getValue(String.class);
                        String photo = snapshot.child("wUserPhoto").getValue(String.class);

                        if (!photo.equals(Empty)) {

                            if (state.equals(chatWith) || state.equals("silent")) {

                                count = count + 1;

                            }
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
        rv.scrollToPosition(adapterChatLista.getItemCount()-1);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {


        if(item.getGroupId() == FRAGMENT_ID_CHATLIST) {

            String type = chatWith;
            final ChatWith wChat = chatsArrayList.get(item.getOrder());
            String id_user = wChat.getUserId();
            String name_user = wChat.getUserName();
            RunItemSelected(item, type, id_user, name_user);
        }

        if(item.getGroupId() == FRAGMENT_ID_CHATGROUPLIST) {
            final ChatWith wChat = chatsGroupArrayList.get(item.getOrder());
            String id_user = wChat.getUserId();
            String name_user = wChat.getUserName();
            String type = chatWithUnknown;
            RunItemSelected(item, type, id_user, name_user);
        }



        return true;
    }

    public void RunItemSelected(MenuItem item, String type, String id_user, String name_user) {

        View view = getActivity().findViewById(android.R.id.content);
        switch (item.getItemId()) {
            case 1: //Marcar leído, no leído

                UserRepository.setNoLeido(id_user,type);
                break;

            case 2: //Silenciar notificaciones

                UserRepository.Silent(name_user, id_user, type);

                break;

            case 3: //Bloquear


                UserRepository.setBlockUser(getContext(), name_user, id_user, view, type);

                break;


            case 4: //Ocultar

                new Constants().UnhiddenChat(getContext(), id_user, name_user, view, type);

                break;


            case 5: //Eliminar

                new Constants().DeleteChat(getContext(), id_user, name_user, view, type);

                break;
        }
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapterChatLista.getFilter().filter(newText);
        return false;
    }

/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

 */

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_search = menu.findItem(R.id.action_search);
        final MenuItem action_settings2 = menu.findItem(R.id.action_unlock);
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setVisible(false);
        action_search.setVisible(true);
        action_settings2.setVisible(true);

        SearchView searchView = (SearchView) action_search.getActionView();

        searchView.setOnQueryTextListener(this);


    }




}