package com.zibete.proyecto1.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.Adapters.AdapterFavoriteUsers;
import com.zibete.proyecto1.FixedSwipeRefreshLayout;
import com.zibete.proyecto1.R;

import java.util.ArrayList;
import java.util.Collections;

import static com.zibete.proyecto1.utils.FirebaseRefs.ref_cuentas;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;


public class FavoritesFragment extends Fragment {

    View view;
    FixedSwipeRefreshLayout favorite_swipe_refresh;
    static RecyclerView rv_favorites;
    ProgressBar progressbar;
    static AdapterFavoriteUsers adapter;
    GridLayoutManager mLayoutManager;
    public final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


    public final ArrayList<String> favoritesArrayList = new ArrayList<>();
    public final ArrayList<String> originalFavoritesArrayList = new ArrayList<>();
    public final ArrayList<String> favoritesArrayList2 = new ArrayList<>();



    public FavoritesFragment(){
        //Constructor vacío
    }




    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){

        view = inflater.inflate(R.layout.fragment_favorites,container,false);


        setHasOptionsMenu(true);



        progressbar = view.findViewById(R.id.progressbar);
        mLayoutManager = new GridLayoutManager(getContext(), 3);
        mLayoutManager.setReverseLayout(false);
        //mLayoutManager.setStackFromEnd(true);
        rv_favorites = view.findViewById(R.id.rv_favorites);
        rv_favorites.setLayoutManager(mLayoutManager);



        adapter = new AdapterFavoriteUsers(favoritesArrayList, getContext());
        rv_favorites.setAdapter(adapter);


        favorite_swipe_refresh = view.findViewById(R.id.favorite_swipe_refresh);
        favorite_swipe_refresh.setRecyclerView(rv_favorites);



        favorite_swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {


                loadUsers("refresh");


                favorite_swipe_refresh.setRefreshing(false);
            }
        });



        loadUsers("load");


        return view;
    }



    public void loadUsers(final String flag) {

        progressbar.setVisibility(View.VISIBLE);

        if (flag.equals("load")){
            favoritesArrayList.removeAll(favoritesArrayList);
        }else{
            favoritesArrayList2.removeAll(favoritesArrayList2);
        }

        originalFavoritesArrayList.removeAll(originalFavoritesArrayList);



        ref_datos.child(user.getUid()).child("FavoriteList").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        final String user = snapshot.getValue(String.class);

                        ref_cuentas.child(user).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                if (dataSnapshot1.exists()){

                                    if (flag.equals("load")){

                                        adapter.addUser(user);
                                    }else{
                                        favoritesArrayList2.add(user);
                                    }

                                    Collections.sort(favoritesArrayList2);
                                    Collections.sort(favoritesArrayList);


                                    if (flag.equals("load")){
                                        adapter.notifyDataSetChanged();
                                    }else {
                                        adapter.updateDataUsers(favoritesArrayList2);
                                    }

                                }else{

                                    dataSnapshot.getRef().child(user).removeValue();

                                }

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });

                    }



                }else{
                    progressbar.setVisibility(View.GONE);

                    Toast.makeText(getContext(), "No hay favoritos", Toast.LENGTH_SHORT).show();

                }


                progressbar.setVisibility(View.GONE);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final MenuItem action_search = menu.findItem(R.id.action_search);
        final MenuItem action_settings2 = menu.findItem(R.id.action_unlock);
        final MenuItem action_favoritos = menu.findItem(R.id.action_favorites);
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setVisible(false);
        action_search.setVisible(false);
        action_settings2.setVisible(false);
        action_favoritos.setVisible(false);

    }

}
