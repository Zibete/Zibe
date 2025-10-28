package com.zibete.proyecto1.ui.Usuarios;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zibete.proyecto1.Adapters.AdapterUsers;
import com.zibete.proyecto1.FixedSwipeRefreshLayout;
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.R;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;
import static com.zibete.proyecto1.Constants.getDistanceMeters;
import static com.zibete.proyecto1.MainActivity.filter;
import static com.zibete.proyecto1.MainActivity.latitud;
import static com.zibete.proyecto1.MainActivity.longitud;
import static com.zibete.proyecto1.MainActivity.ref_cuentas;
import static com.zibete.proyecto1.MainActivity.refresh;

public class UsuariosFragment extends Fragment implements SearchView.OnQueryTextListener{

    View view;
    ProgressBar progressbar;
    ImageButton goChat;
    static RecyclerView rv;
    static AdapterUsers adapter;
    LinearLayoutManager mLayoutManager;
    FixedSwipeRefreshLayout swipe_refresh;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();



    @SuppressLint("RestrictedApi")
    public static SharedPreferences preferences = getApplicationContext().getSharedPreferences("FilterUsers", Context.MODE_PRIVATE);
    public static SharedPreferences.Editor editor = preferences.edit();

    public static boolean filterPrefs = preferences.getBoolean("filterPrefs", false);
    public static boolean checkPref = preferences.getBoolean("checkPref", false);
    public static boolean edadPref = preferences.getBoolean("edadPref", false);
    public static int desdePref = preferences.getInt("desdePref",0);
    public static int hastaPref = preferences.getInt("hastaPref",0);

    public static boolean inGroup = preferences.getBoolean("inGroup",false);
    public static String groupName = preferences.getString("groupName","");
    public static String userName = preferences.getString("userName","");
    public static int userType = preferences.getInt("userType",2);
    public static int readGroupMsg = preferences.getInt("readGroupMsg",0);
    public static String userDate = preferences.getString("userDate","");
    public static int countGroupBadge;

    public static boolean individualNotifications = preferences.getBoolean("individualNotifications",true);
    public static boolean groupNotifications = preferences.getBoolean("groupNotifications",true);


    public final ArrayList<Users>usersArrayList = new ArrayList<>();
    public final ArrayList<Users>originalUsersList = new ArrayList<>();
    public final ArrayList<Users>usersArrayList2 = new ArrayList<>();


    public UsuariosFragment() {
        //Constructor vacío
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){

        view = inflater.inflate(R.layout.fragment_usuarios,container,false);

        setHasOptionsMenu(true);

        progressbar = view.findViewById(R.id.progressbar);
        goChat = view.findViewById(R.id.goChat);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        rv = view.findViewById(R.id.rv);
        rv.setLayoutManager(mLayoutManager);
        // Sugerencias de performance para listas con blur
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setDrawingCacheEnabled(true);
        rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);



        //Verificar si esta en True o False:
        if (!filterPrefs) {

            editor.putBoolean("checkPref", false);
            editor.putBoolean("edadPref", false);
            editor.putInt("desdePref", 0);
            editor.putInt("hastaPref", 0);

            editor.apply();
            filter.setColorFilter(getContext().getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);

        }else{
            filter.setColorFilter(getContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);

        }


        adapter = new AdapterUsers(usersArrayList, originalUsersList, getContext());
        rv.setAdapter(adapter);

        swipe_refresh = view.findViewById(R.id.swipe_refresh);
        swipe_refresh.setRecyclerView(rv);




        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {


                loadUsers(checkPref, desdePref, hastaPref, "refresh");


                swipe_refresh.setRefreshing(false);
            }
        });




//BOTON Refresh
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                loadUsers(checkPref, desdePref, hastaPref, "refresh");


            }
        });




//BOTON Filtro
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                final View viewFilter = getLayoutInflater().inflate(R.layout.filter_layout,null);
                final SwitchCompat switch_edad = viewFilter.findViewById(R.id.switch_edad);
                final SwitchCompat switch_online = viewFilter.findViewById(R.id.switch_online);
                final Spinner spinnerAge = viewFilter.findViewById(R.id.spinner_age);
                final Spinner spinnerAge2 = viewFilter.findViewById(R.id.spinner_age2);

                final Integer[] edades = new Integer[]  {18,19,20,21,22,23,24,25,26,27,28,29,30,31,
                        32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,
                        57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,
                        82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99};


                ArrayAdapter <Integer> adapter1 = new ArrayAdapter<>(getContext(),
                        R.layout.tv_spinner_selected,edades);
                adapter1.setDropDownViewResource(R.layout.tv_spinner_lista);
                spinnerAge.setAdapter(adapter1);
                spinnerAge2.setAdapter(adapter1);

                if (filterPrefs){
                    spinnerAge.setSelection(desdePref-18);
                    spinnerAge2.setSelection(hastaPref-18);
                    switch_online.setChecked(checkPref);
                    switch_edad.setChecked(edadPref);
                }


                if (switch_edad.isChecked()) {
                    spinnerAge.setEnabled(true);
                    spinnerAge2.setEnabled(true);
                }else{
                    spinnerAge.setEnabled(false);
                    spinnerAge2.setEnabled(false);
                }





                spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(spinnerAge2.getSelectedItemPosition() < spinnerAge.getSelectedItemPosition()) {
                            spinnerAge2.setSelection(position);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) { }
                });
                spinnerAge2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(spinnerAge.getSelectedItemPosition() > spinnerAge2.getSelectedItemPosition()) {
                            spinnerAge.setSelection(position);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) { }
                });


                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertDialogApp));

                builder.setView(viewFilter);
                builder.setPositiveButton("Filtrar", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface builder, int selectedIndex) {

                        filterPrefs = true;
                        editor.putBoolean("filterPrefs", filterPrefs);

                        checkPref = switch_online.isChecked();
                        editor.putBoolean("checkPref", switch_online.isChecked());

                        edadPref = switch_edad.isChecked();
                        editor.putBoolean("edadPref", switch_edad.isChecked());

                        if (switch_edad.isChecked()){

                            desdePref = edades[spinnerAge.getSelectedItemPosition()];
                            hastaPref = edades[spinnerAge2.getSelectedItemPosition()];
                            editor.putInt("desdePref", edades[spinnerAge.getSelectedItemPosition()]);
                            editor.putInt("hastaPref", edades[spinnerAge2.getSelectedItemPosition()]);

                        }else{
                            desdePref = 0;
                            hastaPref = 0;
                            editor.putInt("desdePref", 0);
                            editor.putInt("hastaPref", 0);
                        }


                        editor.apply();

                        loadUsers(checkPref, desdePref, hastaPref, "filter");
                        filter.setColorFilter(getContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);

                    }
                });

                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                return;

                            }
                        });


                builder.setNeutralButton("Quitar filtro", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface builder, int id) {

                                DeletePreferences();

                                loadUsers(checkPref, desdePref, hastaPref, "filter");

                                return;

                            }
                        });


                builder.setCancelable(true);
                final AlertDialog dialog = builder.create();


                switch_online.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                        if (isChecked) {
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));
                        }else{
                            if (!switch_edad.isChecked()){
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                            }
                        }
                    }
                });



                switch_edad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                        if (isChecked) {
                            spinnerAge.setEnabled(true);
                            spinnerAge2.setEnabled(true);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));

                        }else{
                            spinnerAge.setEnabled(false);
                            spinnerAge2.setEnabled(false);
                            if (!switch_online.isChecked()){
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);

                            }
                        }
                    }
                });



                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        if(!filterPrefs){
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.GRAY);
                        }
                        if(!switch_online.isChecked() & !switch_edad.isChecked()){
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GRAY);
                        }else{
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));

                        }

                    }
                });

                dialog.show();
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getContext().getResources().getColor(R.color.blanco));
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getContext().getResources().getColor(R.color.blanco));
            }
        });

















        loadUsers(checkPref, desdePref, hastaPref, "load");


        return view;
    }

    @SuppressLint("RestrictedApi")
    public static void DeletePreferences() {
        filterPrefs = false;
        checkPref = false;
        edadPref = false;
        desdePref = 0;
        hastaPref = 0;

        inGroup = false;
        groupName = "";
        userName = "";
        userType = 2;

        editor.putBoolean("inGroup", false);
        editor.putString("groupName", "");
        editor.putString("userName", "");
        editor.putInt("userType", 2);

        editor.putBoolean("filterPrefs", false);
        editor.putBoolean("checkPref", false);
        editor.putBoolean("edadPref", false);
        editor.putInt("desdePref", 0);
        editor.putInt("hastaPref", 0);

        editor.apply();

        filter.setColorFilter(getApplicationContext().getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
    }






    public void loadUsers(final boolean check, final Integer desde, final Integer hasta, final String flag) {

        progressbar.setVisibility(View.VISIBLE);

        ref_cuentas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    if (flag.equals("load")){
                        usersArrayList.removeAll(usersArrayList);
                    }else{
                        usersArrayList2.removeAll(usersArrayList2);
                    }

                    originalUsersList.removeAll(originalUsersList);


                    if (edadPref){

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            String key = snapshot.getKey();
                            String usuario = user.getUid();

                            if (!key.equals(usuario)){

                                Users users = snapshot.getValue(Users.class);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    String edad1 = users.getBirthDay();
                                    if (!edad1.isEmpty()) {
                                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                        LocalDate fechaNac = LocalDate.parse(users.getBirthDay(), fmt);
                                        LocalDate ahora = LocalDate.now();
                                        Period periodo = Period.between(fechaNac, ahora);
                                        Integer edad = periodo.getYears();
                                        users.setAge(edad);
                                    }
                                }

                                Double distanceMeters = getDistanceMeters(latitud, longitud, users.getLatitud(), users.getLongitud());
                                users.setDistance(distanceMeters);

                                if (users.getAge() >= desde & users.getAge() <= hasta) {

                                    if (check) {

                                        if (users.getEstado()) {

                                            if (flag.equals("load")) {

                                                adapter.addUser(users);
                                            } else {
                                                usersArrayList2.add(users);
                                            }

                                        }
                                    } else {

                                        if (flag.equals("load")) {
                                            adapter.addUser(users);
                                        } else {
                                            usersArrayList2.add(users);
                                        }
                                    }
                                }
                            }
                        }
                    }else{


                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            String key = snapshot.getKey();
                            String usuario = user.getUid();

                            if (!key.equals(usuario)){

                                Users users = snapshot.getValue(Users.class);

                                Double distanceMeters = getDistanceMeters(latitud, longitud, users.getLatitud(), users.getLongitud());
                                users.setDistance(distanceMeters);

                                if (check) {

                                    if (users.getEstado()) {
                                        if (flag.equals("load")) {
                                            adapter.addUser(users);
                                        } else {
                                            usersArrayList2.add(users);
                                        }
                                    }
                                } else {

                                    if (flag.equals("load")) {
                                        adapter.addUser(users);
                                    } else {
                                        usersArrayList2.add(users);
                                    }
                                }

                            }
                        }
                    }

                    Collections.sort(usersArrayList2);
                    Collections.sort(usersArrayList);
                    Collections.sort(originalUsersList);


                    if (flag.equals("load")){

                        adapter.notifyDataSetChanged();

                    }else {

                        adapter.updateDataUsers(usersArrayList2);
                        //adapter.updateDataUsers(originalUsersList);
                    }
                    setScrollbar();

                } else {
                    progressbar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No existen usuarios", Toast.LENGTH_SHORT).show();
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
        final MenuItem action_exit = menu.findItem(R.id.action_exit);

        action_exit.setVisible(false);
        action_search.setVisible(true);
        action_settings2.setVisible(true);

        SearchView searchView = (SearchView) action_search.getActionView();

        searchView.setOnQueryTextListener(this);

    }


    public static void setScrollbar(){
        rv.scrollToPosition(adapter.getItemCount()-1);
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
}