package com.zibete.proyecto1.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.Constants;
import com.zibete.proyecto1.POJOS.Users;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;
import com.zibete.proyecto1.utils.UserRepository;

import java.util.ArrayList;

import static com.zibete.proyecto1.Constants.chatWith;
import static com.zibete.proyecto1.Constants.maxChatSize;
import static com.zibete.proyecto1.utils.FirebaseRefs.ref_datos;

public class SliderProfileAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater layoutInflater;
    ArrayList <String> photoList;
    PhotoView photo;
    int rotation;

    ImageView ft_perfil;
    ImageView icon_conectado, icon_desconectado;
    TextView nameUser, tv_estado, desc, age, distanceUser;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    LinearLayout linearImageActivity, linearPhotos, linear_desc;
    RecyclerView recyclerPhotos;
    String unknownName;
    CoordinatorLayout coordinatorLayoutPhoto;
    ArrayList <String> receivedPhotos;
    AdapterPhotoReceived adapterPhotoReceived;
    ImageView perfil_favorite_on, perfil_favorite_off, perfil_bloq, perfil_bloq_me;
    ProgressBar loadingPhoto;
    FloatingActionMenu floatingActionMenu;
    FloatingActionButton subMenu_chatWith, subMenu_chatWithUnknown;
    ArrayList <Users> userList;


    public SliderProfileAdapter(Context context, ArrayList <Users> userList, int rotation){
        this.context = context;
        this.userList = userList;
        this.rotation = rotation;
    }



    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = layoutInflater.inflate(R.layout.adapter_profile, container, false);

        floatingActionMenu = view.findViewById(R.id.floatingActionMenu);

        subMenu_chatWith = view.findViewById(R.id.subMenu_chatWith);
        subMenu_chatWithUnknown = view.findViewById(R.id.subMenu_chatWithUnknown);

        floatingActionMenu.setClosedOnTouchOutside(true);


        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true);
        layoutManager.setStackFromEnd(true);
        recyclerPhotos = view.findViewById(R.id.recyclerPhotos);
        recyclerPhotos.setLayoutManager(layoutManager);

        photoList = new ArrayList<>();
        receivedPhotos = new ArrayList<>();
        adapterPhotoReceived = new AdapterPhotoReceived(receivedPhotos, maxChatSize, context);

        recyclerPhotos.setAdapter(adapterPhotoReceived);

        linearImageActivity = view.findViewById(R.id.linearImageActivity);
        linearPhotos = view.findViewById(R.id.linearPhotos);
        distanceUser = view.findViewById(R.id.distanceUser);
        ft_perfil = view.findViewById(R.id.ftPerfil);
        nameUser = view.findViewById(R.id.nameUser);
        desc = view.findViewById(R.id.desc);
        age = view.findViewById(R.id.edad);
        tv_estado = view.findViewById(R.id.tv_estado);
        icon_conectado = view.findViewById(R.id.icon_conectado);
        icon_desconectado = view.findViewById(R.id.icon_desconectado);

        perfil_favorite_off = view.findViewById(R.id.perfil_favorite_off);
        perfil_favorite_on = view.findViewById(R.id.perfil_favorite_on);
        perfil_bloq = view.findViewById(R.id.perfil_bloq);
        perfil_bloq_me = view.findViewById(R.id.perfil_bloq_me);
        loadingPhoto = view.findViewById(R.id.loadingPhoto);
        coordinatorLayoutPhoto = view.findViewById(R.id.coordinatorLayoutPhoto);
        linear_desc = view.findViewById(R.id.linear_desc);

        final Users users = userList.get(position);

        nameUser.setText(users.getNombre());


        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);



        int height = metrics.heightPixels;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height-(height/4));

        linearImageActivity.setLayoutParams(layoutParams);



        //FAVORITE LIST
        perfil_favorite_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                ref_datos.child(user.getUid()).child("FavoriteList").child(users.getID()).setValue(users.getID());

                Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show();

            }
        });

        perfil_favorite_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                ref_datos.child(user.getUid()).child("FavoriteList").child(users.getID()).removeValue();

                Toast.makeText(context, "Quitado de favoritos", Toast.LENGTH_SHORT).show();

            }
        });


//Set Data
        if (!users.getDescripcion().isEmpty()) {
            linear_desc.setVisibility(View.VISIBLE);
            desc.setText(users.getDescripcion());
        }else{
            linear_desc.setVisibility(View.GONE);
        }


        final ProgressBar progressBar = new ProgressBar(context);

        CoordinatorLayout.LayoutParams pbParam = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pbParam.gravity = Gravity.CENTER;

        progressBar.setLayoutParams(pbParam);

        coordinatorLayoutPhoto.addView(progressBar);

        Glide.with(context)
                .load(users.getFoto())
                .apply(new RequestOptions().transform( new CenterCrop(), new RoundedCorners(35)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                })
                .into(ft_perfil);

        

        linearImageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                photoList.add(users.getFoto());
                Intent intent = new Intent(context, SlidePhotoActivity.class);
                intent.putExtra("photoList",photoList);
                intent.putExtra("position",0);
                intent.putExtra("rotation",180);
                view.getContext().startActivity(intent);
            }
        });


        UserRepository.stateUser(context, users.getID(), icon_conectado, icon_desconectado, tv_estado, chatWith);

        UserRepository.setUserOnline(context, users.getID());


        new Constants().setFavorite(users.getID(), perfil_favorite_on, perfil_favorite_off);

        new Constants().setBloq(users.getID(), perfil_bloq);

        new Constants().getBloqMe(users.getID(), perfil_bloq_me);

        new Constants().getAge(users.getID(), age);

        new Constants().getDistanceToUser(users.getID(), distanceUser);

        new Constants().addPhotoReceived(users.getID(), adapterPhotoReceived, linearPhotos);

        new Constants().setMenuProfile(context, users.getID(), subMenu_chatWithUnknown,subMenu_chatWith);



        container.addView(view);
        return view;
    }




    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (CoordinatorLayout) object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((CoordinatorLayout)object);
    }




}
