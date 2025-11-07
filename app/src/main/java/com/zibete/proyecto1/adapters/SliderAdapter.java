package com.zibete.proyecto1.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.airbnb.lottie.LottieAnimationView;
import com.zibete.proyecto1.R;

public class SliderAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater layoutInflater;
    private float scale;

    public SliderAdapter (Context context){
        this.context = context;
    }

    public int[] slide_images = {
            /*
            R.drawable.ic_logo,
            R.drawable.ic_logo,
            R.drawable.ic_logo,
             */
            R.raw.chat_right,
            R.raw.lf30_editor_miibzys8,
            R.raw.onboarding_persons,


    };

    public String [] slide_title = {
            "Chatea",
            "Descubre",
            "Socializa",
    };

    public String [] slide_parrafo = {
            "Chatea con familiares y amigos, cuando quieras, en tiempo real!",
            "Encuentra personas cercanas a tu ubicación. Haz nuevos amigos!",
            "Únete a las salas de chat existentes, o crea una a tu medida!",
    };

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);

        scale = context.getResources().getDisplayMetrics().density;

        LottieAnimationView slideImageView = view.findViewById(R.id.imageView);
        TextView slideTitle = view.findViewById(R.id.title);
        TextView slideParrafo = view.findViewById(R.id.parrafo);


        slideImageView.setAnimation(slide_images[position]);
        slideTitle.setText(slide_title[position]);
        slideTitle.setTextSize(40);
        slideParrafo.setText(slide_parrafo[position]);

        container.addView(view);
        int dp_250 = (int) (250 * scale + 0.5f);
        int dp_50 = (int) (50 * scale + 0.5f);
/*
        if (slideTitle.getText().toString().equals("Socializa")){
            slideImageView.setPadding(-dp_50,0,0,-dp_50);

        }else{
            slideImageView.setPadding(0,0,0,0);

        }

 */
        if (slideTitle.getText().toString().equals("Socializa")){

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp_250);
            params.gravity = Gravity.CENTER;
            slideImageView.setLayoutParams(params);

        }else{
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp_250,dp_250);
            params.gravity = Gravity.CENTER;
            slideImageView.setLayoutParams(params);

        }

        return view;
    }

    @Override
    public int getCount() {
        return slide_images.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (LinearLayout) object;

    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout)object);
    }
}
