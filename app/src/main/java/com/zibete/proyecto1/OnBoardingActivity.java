package com.zibete.proyecto1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.zibete.proyecto1.Adapters.SliderAdapter;

public class OnBoardingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ViewPager slideViewPager;
    private TabLayout dotsTabLayout;
    private SliderAdapter sliderAdapter;
    private Button btnBack, btnNext;
    private int currentPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        mAuth = FirebaseAuth.getInstance();
        dotsTabLayout = findViewById(R.id.dots);
        slideViewPager = findViewById(R.id.viewPager);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);

        sliderAdapter = new SliderAdapter(this);
        slideViewPager.setAdapter(sliderAdapter);
        dotsTabLayout.setupWithViewPager(slideViewPager);
        setUpDots();

        slideViewPager.addOnPageChangeListener(viewListener);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage == (sliderAdapter.getCount()-1)){
                    finish();
                }else{
                    slideViewPager.setCurrentItem(currentPage+1);
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slideViewPager.setCurrentItem(currentPage-1);
            }
        });
    }

    public void onStart() {
        super.onStart();



    }




    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            currentPage = position;
            if (position == 0){
                btnNext.setEnabled(true);
                btnNext.setText("Siguiente");
                btnBack.setVisibility(View.GONE);
            }else{
                if (position == sliderAdapter.getCount()-1){
                    btnNext.setEnabled(true);
                    btnNext.setText("Continuar!");
                    btnBack.setEnabled(true);
                    btnBack.setVisibility(View.VISIBLE);
                    btnBack.setText("Atrás");
                }else{
                    btnNext.setEnabled(true);
                    btnNext.setText("Siguiente");
                    btnBack.setEnabled(true);
                    btnBack.setVisibility(View.VISIBLE);
                    btnBack.setText("Atrás");
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };



    private void setUpDots(){
        ViewGroup tabStrip = (ViewGroup) dotsTabLayout.getChildAt(0);
        for (int i = 0 ; i<tabStrip.getChildCount(); i++){
            View tabView = tabStrip.getChildAt(i);
            if (tabView !=null){
                int paddingStart = tabView.getPaddingStart();
                int paddingTop = tabView.getPaddingTop();
                int paddingEnd = tabView.getPaddingEnd();
                int paddingBottom = tabView.getPaddingBottom();
                ViewCompat.setBackground(tabView, AppCompatResources.getDrawable(tabView.getContext(), R.drawable.tab_color));
                ViewCompat.setPaddingRelative(tabView, paddingStart, paddingTop, paddingEnd, paddingBottom);
            }

        }
    }

}




