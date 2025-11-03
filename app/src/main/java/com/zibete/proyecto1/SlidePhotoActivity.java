package com.zibete.proyecto1;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.Adapters.SliderPhotoAdapter;
import com.zibete.proyecto1.utils.UserRepository;

import java.util.ArrayList;

public class SlidePhotoActivity extends AppCompatActivity {

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ProgressBar progressbarImage;

    public SlidePhotoActivity(){
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

        final ArrayList <String> photoList = (ArrayList<String>) getIntent().getExtras().getSerializable("photoList");
        final int position = getIntent().getExtras().getInt("position");
        final int rotation = getIntent().getExtras().getInt("rotation");


        LinearLayout linearSlide = findViewById(R.id.linearSlide);

        ViewPager viewPager = new ViewPager(this);
        viewPager.setId(View.generateViewId());

        viewPager.setAdapter(new SliderPhotoAdapter(this, photoList, rotation));


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        viewPager.setRotationY(rotation);
        viewPager.setLayoutParams(params);


        linearSlide.addView(viewPager, viewPager.getLayoutParams());
        viewPager.setCurrentItem(position);



    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UserRepository.setUserOffline(getApplicationContext(),user.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserRepository.setUserOnline(getApplicationContext(), user.getUid());
    }

}
