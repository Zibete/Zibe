package com.zibete.proyecto1.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.adapters.AdapterGroups;
import com.zibete.proyecto1.FixedSwipeRefreshLayout;
import com.zibete.proyecto1.model.Groups;
import com.zibete.proyecto1.R;

import java.util.ArrayList;
import java.util.Collections;

import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupData;


public class GruposFragment extends Fragment implements SearchView.OnQueryTextListener{

    View view;
    FixedSwipeRefreshLayout group_swipe_refresh;
    static RecyclerView rv_groups;
    ProgressBar progressbar;
    static AdapterGroups adapter;
    GridLayoutManager mLayoutManager;
    FloatingActionButton fab_new_group;
    public final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    public final ArrayList<Groups> groupsArrayList = new ArrayList<>();
    public final ArrayList<Groups> originalGroupsArrayList = new ArrayList<>();
    public final ArrayList<Groups> groupsArrayList2 = new ArrayList<>();


    public GruposFragment(){
        //Constructor vacío
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
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){

        view = inflater.inflate(R.layout.fragment_grupos,container,false);

        setHasOptionsMenu(true);

        progressbar = view.findViewById(R.id.progressbar);
        mLayoutManager = new GridLayoutManager(getContext(), 2);
        mLayoutManager.setReverseLayout(false);
        //mLayoutManager.setStackFromEnd(true);
        rv_groups = view.findViewById(R.id.rv_groups);
        rv_groups.setLayoutManager(mLayoutManager);
        fab_new_group = view.findViewById(R.id.fab_new_group);
        final Context context = getContext();
        adapter = new AdapterGroups(groupsArrayList, originalGroupsArrayList, getContext());
        rv_groups.setAdapter(adapter);

        group_swipe_refresh = view.findViewById(R.id.group_swipe_refresh);
        group_swipe_refresh.setRecyclerView(rv_groups);



        group_swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                loadGroups("refresh");
                group_swipe_refresh.setRefreshing(false);

            }
        });

        fab_new_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                adapter.goNewGroup();

            }
        });

        loadGroups("load");

        return view;
    }



    public void loadGroups(final String flag) {

        progressbar.setVisibility(View.VISIBLE);

        refGroupData.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    if (flag.equals("load")) {
                        groupsArrayList.removeAll(groupsArrayList);
                    } else {
                        groupsArrayList2.removeAll(groupsArrayList2);
                    }

                    originalGroupsArrayList.removeAll(originalGroupsArrayList);



                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        final Groups group = snapshot.getValue(Groups.class);

                        String nombre = snapshot.child("name").getValue(String.class);

                        refGroupUsers.child(nombre).addListenerForSingleValueEvent(new ValueEventListener() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                long counter = dataSnapshot.getChildrenCount();
                                group.setUsers((int) counter);
                                //Toast.makeText(getContext(), counter+"", Toast.LENGTH_SHORT).show();

                                if (flag.equals("load")) {

                                    adapter.addGroup(group);
                                } else {
                                    groupsArrayList2.add(group);
                                }


                                Collections.sort(groupsArrayList2);
                                Collections.sort(groupsArrayList);
                                Collections.sort(originalGroupsArrayList);

                                if (flag.equals("load")) {
                                    adapter.notifyDataSetChanged();
                                } else {
                                    adapter.updateDataGroups(groupsArrayList2);

                                }

                                setScrollbar();

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });




                    }



                } else {
                    progressbar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No hay Grupos", Toast.LENGTH_SHORT).show();
                }


                progressbar.setVisibility(View.GONE);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });






    }

    public static void setScrollbar(){
        rv_groups.scrollToPosition(0);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_search = menu.findItem(R.id.action_search);
        final MenuItem action_settings = menu.findItem(R.id.action_settings);
        final MenuItem action_desbloq_usuers = menu.findItem(R.id.action_unlock);
        final MenuItem action_favorites = menu.findItem(R.id.action_favorites);
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setVisible(false);
        action_search.setVisible(true);
        action_settings.setVisible(true);
        action_desbloq_usuers.setVisible(true);
        action_favorites.setVisible(true);

        SearchView searchView = (SearchView) action_search.getActionView();

        searchView.setOnQueryTextListener(this);

    }

}
