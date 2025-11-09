package com.zibete.proyecto1;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rahimlis.badgedtablayout.BadgedTabLayout;
import com.zibete.proyecto1.adapters.ChatListGroupsFragment;

import java.util.ArrayList;
import java.util.List;

import static com.zibete.proyecto1.utils.Constants.CHATWITHUNKNOWN;
import static com.zibete.proyecto1.utils.FirebaseRefs.refDatos;
import static com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers;
import static com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment.groupName;

public class PageAdapterGroup extends Fragment {

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    public ViewPager viewPager;
    private final List <Fragment> fragments = new ArrayList<>();
    public static ValueEventListener valueEventListenerTitle;
    LinearLayout linearProgressBar;
    ProgressBar progressBar;

    public PageAdapterGroup() {
        //...
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_groups_chat, container, false);

        viewPager = view.findViewById(R.id.viewPager);
        linearProgressBar = view.findViewById(R.id.linearProgressBar);
        progressBar = view.findViewById(R.id.progressBar);
        linearProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        FragmentManager cfManager = getChildFragmentManager();

        final MyPagerAdapter viewPagerAdapter = new MyPagerAdapter(cfManager);


        fragments.add(new PersonsChatGroupFragment());
        fragments.add(new ChatGroupFragment());
        fragments.add(new ChatListGroupsFragment());


        viewPager.setAdapter(viewPagerAdapter);

        final BadgedTabLayout tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(2);

        start(container);


        final Query newQuery = refDatos.child(user.getUid()).child(CHATWITHUNKNOWN).orderByChild("noVisto").startAt(1);
        newQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    int countMsgUnread = 0;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        int unRead = snapshot.child("noVisto").getValue(int.class);

                        countMsgUnread = countMsgUnread + unRead;

                    }

                    tabLayout.setBadgeText(2, String.valueOf(countMsgUnread));


                }else{
                    tabLayout.setBadgeText(2, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        return view;

    }

    public void start(final ViewGroup container) {
        linearProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable(){
            public void run(){

                ConnectivityManager connectivityManager = (ConnectivityManager) container.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    // Si hay conexión a Internet en este momento

                    viewPager.setCurrentItem(1);

                    new Handler().postDelayed(new Runnable() {
                        public void run() {

                            linearProgressBar.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                        }
                        },200);

                } else {
                    progressBar.setVisibility(View.GONE);
                    linearProgressBar.setVisibility(View.VISIBLE);
                    final Snackbar snack = Snackbar.make(viewPager, "No hay conexión a Internet en este momento", Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("Reintentar", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                            start(container);
                        }
                    });
                    snack.setBackgroundTint(getResources().getColor(R.color.colorC));
                    TextView tv = snack.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    snack.show();

                    // No hay conexión a Internet en este momento
                }


            }
        }, 800);
    }


    public class MyPagerAdapter extends FragmentPagerAdapter {

        private int foundItems;

        public void updateTitleData(  int foundItems  ) {
            this.foundItems = foundItems;

            notifyDataSetChanged();
        }

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            valueEventListenerTitle = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    long count = dataSnapshot.getChildrenCount();

                    if (isAdded()) {

                        updateTitleData((int) count);

                    }



                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };

            refGroupUsers.child(groupName).addValueEventListener(valueEventListenerTitle);


        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }


        @Override
        public CharSequence getPageTitle (int position) {

            switch (position){
                case 0:
                    return "(" + foundItems + ") " + getContext().getString(R.string.menu_usuarios);
                case 1:
                    return groupName;
                case 2:
                    return getResources().getString(R.string.menu_chat);
                default:
                    return null;
            }
        }
    }
}