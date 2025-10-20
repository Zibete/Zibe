package com.zibete.proyecto1.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AdapterPhotoReceived extends RecyclerView.Adapter<AdapterPhotoReceived.viewHolderAdapterPhoto> {


    ArrayList <String> photoList;
    Context context;
    private Integer maxSize;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterPhotoReceived(ArrayList <String> photoList, Integer maxSize, Context context) {
        this.photoList = photoList;
        this.maxSize = maxSize;
        this.context = context;
    }



    public void addString(String photo) {

        if (photoList.size() > maxSize) {
            photoList.remove(0);
            notifyItemRemoved(0);
        }

        photoList.add(photo);
        notifyItemInserted(photoList.size());
    }



    @NotNull
    @Override
    public viewHolderAdapterPhoto onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_photos,parent,false);
        viewHolderAdapterPhoto holder = new viewHolderAdapterPhoto(v);

        return holder;

    }



    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapterPhoto holder, final int position) {

        //final ArrayList <String> photos = photoList.get(position);


        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                widthPixels/3, widthPixels/3);

        holder.linear_photo_view.setLayoutParams(layoutParams);

        holder.linear_photo_view.setVisibility(View.VISIBLE);

        Glide.with(context).load(photoList.get(position)).into(holder.photoView);


        holder.photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, SlidePhotoActivity.class);
                intent.putExtra("photoList",photoList);
                intent.putExtra("position",position);
                intent.putExtra("rotation",180);
                v.getContext().startActivity(intent);

            }
        });


    }//Fin del onBindViewHolder


    @Override
    public int getItemCount() {
        return photoList.size();

    }

    public class viewHolderAdapterPhoto extends RecyclerView.ViewHolder {

        ImageView photoView;
        LinearLayout linear_photo_view;

        public viewHolderAdapterPhoto(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
            linear_photo_view =  itemView.findViewById(R.id.linear_photo_view);
        }
    }

}
