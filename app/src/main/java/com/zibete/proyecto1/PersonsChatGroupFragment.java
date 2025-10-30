package com.zibete.proyecto1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.Adapters.AdapterGroupUsers;
import com.zibete.proyecto1.POJOS.UserGroup;

import java.util.ArrayList;

import static com.zibete.proyecto1.utils.FirebaseRefs.ref_group_users;
import static com.zibete.proyecto1.ui.Usuarios.UsuariosFragment.groupName;

public class PersonsChatGroupFragment extends Fragment implements SearchView.OnQueryTextListener{

    RecyclerView rv_group_users;
    ProgressBar progressbar;
    ImageView img_cancel_dialog;
    static AdapterGroupUsers adapter;
    final ArrayList<UserGroup> groupUsersList = new ArrayList<>();
    final ArrayList<UserGroup> groupOriginalUsersList = new ArrayList<>();

    public PersonsChatGroupFragment() {
        //...
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.getFilter().filter(newText);
        return false;
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_list_users_group, container, false);

        setHasOptionsMenu(true);

        rv_group_users = view.findViewById(R.id.rv_group_users);

        progressbar = view.findViewById(R.id.progressbar);
        img_cancel_dialog = view.findViewById(R.id.img_cancel_dialog);

        progressbar.setVisibility(View.VISIBLE);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());

        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        rv_group_users.setLayoutManager(mLayoutManager);


        adapter = new AdapterGroupUsers(groupUsersList, groupOriginalUsersList, getContext());

        rv_group_users.setAdapter(adapter);

        //groupUsersList.clear();

//CREAR LISTA



        ref_group_users.child(groupName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    groupUsersList.clear();
                    groupOriginalUsersList.clear();
                    progressbar.setVisibility(View.GONE);
                    rv_group_users.setVisibility(View.VISIBLE);

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                        UserGroup groupUser = snapshot.getValue(UserGroup.class);
                        adapter.addUser(groupUser);


                    }


                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        /*
        ref_group_users.child(groupName).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


                if (dataSnapshot.exists()) {


                    progressbar.setVisibility(View.GONE);
                    rv_group_users.setVisibility(View.VISIBLE);
                    UserGroup groupUser = dataSnapshot.getValue(UserGroup.class);
                    adapter.addUser(groupUser);


                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    UserGroup groupUser = dataSnapshot.getValue(UserGroup.class);
                    adapter.removeUser(groupUser);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

         */








        return view;
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
