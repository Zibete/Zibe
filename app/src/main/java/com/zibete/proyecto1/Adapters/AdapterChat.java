package com.zibete.proyecto1.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zibete.proyecto1.model.Chats;
import com.zibete.proyecto1.R;
import com.zibete.proyecto1.SlidePhotoActivity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.zibete.proyecto1.ChatActivity.msgSelected;
import static com.zibete.proyecto1.ChatActivity.myPhoto;
import static com.zibete.proyecto1.ChatActivity.notSelectedDeleteMsg;
import static com.zibete.proyecto1.ChatActivity.selectedDeleteMsg;
import static com.zibete.proyecto1.ChatActivity.setDate;
import static com.zibete.proyecto1.ChatActivity.yourPhoto;
import static com.zibete.proyecto1.Constants.AUDIO;
import static com.zibete.proyecto1.Constants.AUDIO_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.AUDIO_SENDER_DLT;
import static com.zibete.proyecto1.Constants.FRAGMENT_ID_CHATLIST;
import static com.zibete.proyecto1.Constants.INFO;
import static com.zibete.proyecto1.Constants.MSG;
import static com.zibete.proyecto1.Constants.MSG_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.MSG_SENDER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO;
import static com.zibete.proyecto1.Constants.PHOTO_RECEIVER_DLT;
import static com.zibete.proyecto1.Constants.PHOTO_SENDER_DLT;


public class AdapterChat extends RecyclerView.Adapter<AdapterChat.viewHolderAdapterChat> implements View.OnCreateContextMenuListener {

    List<Chats> msgList;
    ArrayList <String> photoList = new ArrayList<>();
    Context context;
    private int position;

    public static MediaPlayer mediaPlayer;

    public Handler handler;

    public Runnable moveSeekBarThread;

    public MediaPlayer.OnCompletionListener onCompletionListener;

    public SeekBar.OnSeekBarChangeListener seekBarChangeListener;

    public String stringAudio = null;

    private long mediaSelected;
    private int mediaPos;
    private int mediaMax;

    public static final int TYPE_INFO = 0;
    public static final int MSG_TYPE_LEFT = 1;
    public static final int MSG_TYPE_RIGHT = 2;

    private Integer maxSize;

    Vibrator vibrator;

    private float scale;

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public AdapterChat(ArrayList<Chats> msgList, Integer maxSize, Context context) {
        this.msgList = msgList;
        this.maxSize = maxSize;
        this.context = context;
    }


    @Override
    public int getItemViewType(int position) {

        if (msgList.get(position).getType() == INFO){
            return TYPE_INFO;
        }else {
            if (msgList.get(position).getSender().equals(user.getUid())) {
                return MSG_TYPE_RIGHT;
            } else {
                return MSG_TYPE_LEFT;
            }
        }
    }

    public void addChat(Chats chats) {

        if (msgList.size() > maxSize) {
            msgList.remove(0);
            notifyItemRemoved(0);
        }

        if (msgList.size() > 0){

            Calendar c = Calendar.getInstance();

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);

            int lastItem = msgList.size()-1;

            String thisDate = chats.getDate().substring(0,10);
            String lastDate = msgList.get(lastItem).getDate().substring(0, 10);

            if (!thisDate.equals(lastDate)){ //Si nueva fecha no es igual a la anterior

                if (thisDate.equals(dateFormat.format(c.getTime()))) {  //chequeamos...Si esta fecha es hoy

                    msgList.add(new Chats(context.getString(R.string.hoy),thisDate,null,INFO,0));//Ponemos HOY

                } else if (thisDate.equals(dateFormat.format(calendar.getTime()))) {

                    msgList.add(new Chats(context.getString(R.string.ayer), thisDate, null, INFO, 0));//ayer

                } else {

                    msgList.add(new Chats(thisDate,thisDate,null,INFO,0));
                }
            }
        }


        msgList.add(chats);
        notifyItemInserted(msgList.size());

        if (chats.getType() == PHOTO | chats.getType() == PHOTO_RECEIVER_DLT | chats.getType() == PHOTO_SENDER_DLT) {

            photoList.add(chats.getMessage());

        }
    }

    public void actualizeMsg (Chats chats){

        int index = msgList.indexOf(chats);

        if (index != -1) {
            msgList.set(index, chats);

            if (chats.getSender().equals(user.getUid())) {

                if (chats.getType() == MSG_SENDER_DLT || chats.getType() == PHOTO_SENDER_DLT || chats.getType() == AUDIO_SENDER_DLT) {
                    msgList.remove(index);
                }

            } else {

                if (chats.getType() == MSG_RECEIVER_DLT || chats.getType() == PHOTO_RECEIVER_DLT || chats.getType() == AUDIO_RECEIVER_DLT) {
                    msgList.remove(index);
                }

            }
        }

        notifyDataSetChanged();

    }

    public void deleteMsg (Chats chats) {

        int index = msgList.indexOf(chats);

        if (index != -1) {

            msgList.set(index, chats);

            notifyDataSetChanged();
            msgList.remove(index);
            notifyItemRemoved(index);

        }
    }


    @NotNull
    @Override
    public viewHolderAdapterChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;

        if (viewType == TYPE_INFO) {

            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_date_chat, parent, false);

        } else if (viewType == MSG_TYPE_RIGHT) {

            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_msg_right, parent, false);

        } else {

            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_msg_left, parent, false);

        }

        view.setOnCreateContextMenuListener(this);
        return new viewHolderAdapterChat(view);
    }



    @Override
    public void onBindViewHolder(@NonNull final viewHolderAdapterChat holder, final int position) {


        final Chats chats = msgList.get(position);


        if (chats.getType() == INFO){

            holder.bindView(chats);

        }else {

            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            holder.bindView(chats);


            int index = msgSelected.indexOf(chats);

            if (index != -1) {

                holder.selectedItem.setBackgroundColor(context.getResources().getColor(R.color.accent_transparent));

            } else {

                holder.selectedItem.setBackgroundColor(context.getResources().getColor(R.color.transparent));

            }


            holder.img_pic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (msgSelected.isEmpty()) {

                        Intent intent = new Intent(context, SlidePhotoActivity.class);
                        intent.putExtra("photoList", photoList);
                        intent.putExtra("position", photoList.indexOf(chats.getMessage()));
                        intent.putExtra("rotation", 0);
                        v.getContext().startActivity(intent);

                    } else {

                        OnClick(holder, chats);
                    }
                }
            });

            Checked(holder, chats);
/*
            final Handler handler = new Handler();
            final Runnable mLongPressed = new Runnable() {
                public void run() {

                    int index = msgSelected.indexOf(chats);

                    vibrator.vibrate(75);

                    if (index == -1) {

                        Select(holder, chats);

                    }else{
                        notSelectedChat(holder, chats);
                    }
                }
            };

 */

            /*

            View.OnTouchListener onTouchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    int action = MotionEventCompat.getActionMasked(event);

                    switch (action) {

                        case (MotionEvent.ACTION_DOWN):

                            if (!msgSelected.isEmpty()){

                                int index = msgSelected.indexOf(chats);
                                vibrator.vibrate(75);

                                if (index == -1) {

                                    Select(holder, chats);

                                }else{

                                    holder.selectedItem.setBackgroundColor(context.getResources().getColor(R.color.fui_transparent));
                                    notSelectedDeleteMsg(chats);

                                }

                            }

                            handler.postDelayed(mLongPressed, ViewConfiguration.getLongPressTimeout());

                            return true;

                        case (MotionEvent.ACTION_UP):


                            return true;

                        case (MotionEvent.ACTION_MOVE):



                            return true;

                    }

                     return true;
                }
            };


            holder.linearCardMsg.setOnTouchListener(onTouchListener);
            holder.img_pic.setOnTouchListener(onTouchListener);




             */




            holder.linearCardMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    OnClick(holder, chats);
                }
            });


            holder.linearCardMsg.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    LongClick(holder, chats);

                    return false;
                }
            });

            holder.img_pic.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    LongClick(holder, chats);

                    return false;
                }
            });




        }

    }//Fin del onBindViewHolder

    public void OnClick(@NonNull viewHolderAdapterChat holder, Chats chats) {


        if (!msgSelected.isEmpty()){

            int index = msgSelected.indexOf(chats);
            vibrator.vibrate(75);

            if (index == -1) {
                Select(holder, chats);
            }else{
                notSelectedChat(holder, chats);
            }

        }
    }

    public void LongClick(@NonNull viewHolderAdapterChat holder, Chats chats) {


        vibrator.vibrate(75);

        if (!msgSelected.isEmpty()){

            int index = msgSelected.indexOf(chats);

            if (index == -1) {

                Select(holder, chats);
            }else{
                notSelectedChat(holder, chats);
            }

        }else{
            Select(holder, chats);
        }

        setPosition(holder.getAdapterPosition());
    }

    public void notSelectedChat(@NonNull viewHolderAdapterChat holder, Chats chats) {

        holder.selectedItem.setBackgroundColor(context.getResources().getColor(R.color.transparent));
        notSelectedDeleteMsg(chats);
    }

    public void Select(@NonNull viewHolderAdapterChat holder, Chats chats) {

        holder.selectedItem.setBackgroundColor(context.getResources().getColor(R.color.accent_transparent));
        selectedDeleteMsg(chats);

    }

    public void Checked(@NonNull viewHolderAdapterChat holder, Chats chats) {

        if (chats.getSender().equals(user.getUid())) {

            switch (chats.getSeen()) {

                case 1:
                    holder.checked.setVisibility(View.VISIBLE);
                    holder.checked.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
                    holder.checked2.setVisibility(View.GONE);

                    break;

                case 2:
                    holder.checked.setVisibility(View.VISIBLE);
                    holder.checked2.setVisibility(View.VISIBLE);
                    holder.checked.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);
                    holder.checked2.setColorFilter(context.getResources().getColor(R.color.blanco), PorterDuff.Mode.SRC_IN);

                    break;

                case 3:

                    holder.checked.setVisibility(View.VISIBLE);
                    holder.checked2.setVisibility(View.VISIBLE);
                    holder.checked.setColorFilter(context.getResources().getColor(R.color.visto), PorterDuff.Mode.SRC_IN);
                    holder.checked2.setColorFilter(context.getResources().getColor(R.color.visto), PorterDuff.Mode.SRC_IN);

                    break;

            }
        }
    }

    public void getDate(int position) {

        Calendar c = Calendar.getInstance();


        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);

        String fecha = msgList.get(position).getDate().substring(0,10);

        if (fecha.equals(dateFormat.format(c.getTime()))) { //Si esta fecha es hoy

            //Ponemos HOY
            setDate(context.getString(R.string.hoy));

        } else {

            if (fecha.equals(dateFormat.format(calendar.getTime()))) {

                setDate(context.getString(R.string.ayer));

            } else {

                setDate(fecha);

            }
        }
    }

    public void setPosition(int position) {
        this.position = position;

    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        menu.add(FRAGMENT_ID_CHATLIST, 1, position, R.string.eliminar);

    }



    public class viewHolderAdapterChat extends RecyclerView.ViewHolder {

        LinearLayout linear_mensaje_msg, linear_mensaje_pic, linear_mensaje_audio;
        CardView linearCardMsg;
        CoordinatorLayout selectedItem;
        ImageView img_pic, ic_play_pause;
        TextView hora, tv_msg;
        ImageView checked;
        ImageView checked2;
        ProgressBar loadingPhoto;
        SeekBar seekBar;
        Chronometer tv_timer;
        TextView tv_info;
        CircleImageView circleImgAudio;
        TextView tv_notFound;

        private int stateMediaPlayer;
        private final int notStarted = 0;
        private final int play = 1;
        private final int pause = 2;


        private final View item;

        private Long chronStateSave = 0L;


        public viewHolderAdapterChat(@NonNull View itemView) {
            super(itemView);

            this.item = itemView;

        }




        public void bindView(final Chats chats){

            selectedItem = itemView.findViewById(R.id.selectedItem);
            linearCardMsg = itemView.findViewById(R.id.linearCardMsg);
            loadingPhoto = itemView.findViewById(R.id.loadingPhoto);
            img_pic = itemView.findViewById(R.id.img_pic);
            linear_mensaje_msg = itemView.findViewById(R.id.linear_mensaje_msg);
            linear_mensaje_pic = itemView.findViewById(R.id.linear_mensaje_pic);
            linear_mensaje_audio = itemView.findViewById(R.id.linear_mensaje_audio);
            tv_msg = itemView.findViewById(R.id.tv_msg);
            hora = itemView.findViewById(R.id.hora_msg);
            checked = itemView.findViewById(R.id.checked);
            checked2 = itemView.findViewById(R.id.checked2);
            ic_play_pause = itemView.findViewById(R.id.ic_play_pause);
            tv_timer = itemView.findViewById(R.id.tv_timer);
            tv_info = itemView.findViewById(R.id.tv_info);
            seekBar = itemView.findViewById(R.id.seekBar);
            circleImgAudio = itemView.findViewById(R.id.circleImgAudio);
            tv_notFound = itemView.findViewById(R.id.tv_notFound);


            scale = context.getResources().getDisplayMetrics().density;
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);



            int dp_15 = (int) (15 * scale + 0.5f);
            int widthPixels = metrics.widthPixels;

            if (chats.getType() == INFO){

                tv_info.setText(chats.getMessage());

            }

            if (chats.getType() == PHOTO | chats.getType() == PHOTO_RECEIVER_DLT | chats.getType() == PHOTO_SENDER_DLT){

                hora.setText(chats.getDate().substring(11,16));

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        widthPixels/2, widthPixels/2);

                linear_mensaje_pic.setLayoutParams(layoutParams);


                linear_mensaje_pic.setVisibility(View.VISIBLE);
                linear_mensaje_msg.setVisibility(View.GONE);
                linear_mensaje_audio.setVisibility(View.GONE);


                tv_notFound.setVisibility(View.GONE);
                loadingPhoto.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chats.getMessage())
                        .apply(new RequestOptions().transform( new CenterCrop(), new RoundedCorners(dp_15)))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                tv_notFound.setVisibility(View.VISIBLE);
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                loadingPhoto.setVisibility(View.GONE);
                                return false;
                            }

                        })
                        .into(img_pic);

            }

            if (chats.getType() == MSG | chats.getType() == MSG_RECEIVER_DLT | chats.getType() == MSG_SENDER_DLT){

                hora.setText(chats.getDate().substring(11,16));

                linear_mensaje_pic.setVisibility(View.GONE);
                linear_mensaje_msg.setVisibility(View.VISIBLE);
                linear_mensaje_audio.setVisibility(View.GONE);
                tv_msg.setText(chats.getMessage());

            }

            if (chats.getType() == AUDIO | chats.getType() == AUDIO_RECEIVER_DLT | chats.getType() == AUDIO_SENDER_DLT){

                hora.setText(chats.getDate().substring(11,16));

                linear_mensaje_pic.setVisibility(View.GONE);
                linear_mensaje_msg.setVisibility(View.GONE);
                linear_mensaje_audio.setVisibility(View.VISIBLE);


                LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(
                        (widthPixels/5*2), ViewGroup.LayoutParams.WRAP_CONTENT);
                seekBarParams.topMargin = dp_15;
                seekBar.setLayoutParams(seekBarParams);


                tv_timer.setText(chats.getDate().substring(23,28));

                stateMediaPlayer = notStarted;

                if (chats.getSender().equals(user.getUid())){
                    Glide.with(context).load(myPhoto).into(circleImgAudio);
                }else{
                    Glide.with(context).load(yourPhoto).into(circleImgAudio);
                }



/*
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (fromUser) {

                            mediaSelected = progress;

                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

 */




/*

                Uri uri = Uri.parse(chats.getMensaje());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(chats.getMensaje());


                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int millSecond = Integer.parseInt(durationStr);
                String time1 = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millSecond),
                        TimeUnit.MILLISECONDS.toSeconds(millSecond) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millSecond))
                );


                tv_timer.setText(time1);

 */




/*
                String time = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(mediaMax),
                        TimeUnit.MILLISECONDS.toSeconds(mediaMax) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mediaMax))
                );

                tv_timer.setText(time);

 */




/*
                if (chats.getEnvia().equals(user.getUid())){

                    if (myPhoto == null) {

                        ref_cuentas.child(user.getUid()).child("foto").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()){

                                    myPhoto = dataSnapshot.getValue(String.class);
                                    Glide.with(context).load(myPhoto).into(circleImgAudio);

                                }

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    } else{
                        Glide.with(context).load(myPhoto).into(circleImgAudio);
                    }

                }else{

                    if (yourPhoto == null) {

                        ref_cuentas.child(chats.getEnvia()).child("foto").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()){

                                    yourPhoto = dataSnapshot.getValue(String.class);
                                    Glide.with(context).load(yourPhoto).into(circleImgAudio);

                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });

                    } else{
                        Glide.with(context).load(yourPhoto).into(circleImgAudio);
                    }
                }


 */



                ic_play_pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        switch (stateMediaPlayer){

                            case notStarted:

                                PlayAudio(chats);

                                break;

                            case play:

                                PauseAudio(chats);

                                break;

                            case pause:

                                ContinueAudio();

                                break;
                        }
                    }
                });
            }
        }

        public void PlayAudio(final Chats chats) {

            if (mediaPlayer != null) {

                onCompletionListener.onCompletion(mediaPlayer);

            }


            stateMediaPlayer = play;

            stringAudio = chats.getMessage();


            mediaPlayer = new MediaPlayer();

            handler = new Handler();

            moveSeekBarThread = new Runnable() {

                public void run() {

                    if (mediaPlayer !=null) {

                        mediaPos = mediaPlayer.getCurrentPosition();
                        mediaMax = mediaPlayer.getDuration();
                        seekBar.setMax(mediaMax);
                        seekBar.setProgress(mediaPos);

                        handler.postDelayed(this, 10);
                    }
                }
            };




            onCompletionListener = new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    if (mediaPlayer != null) {
                        stateMediaPlayer = notStarted;
                        ic_play_pause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                        tv_timer.stop();
                        tv_timer.setText(chats.getDate().substring(23, 28));
                        handler.removeCallbacks(moveSeekBarThread);

                        seekBarChangeListener = null;
                        seekBar.setOnSeekBarChangeListener(null);
                        seekBar.setProgress(0);

                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                        mediaSelected = 0;
                    }

                }
            };


            mediaPlayer.setOnCompletionListener(onCompletionListener);

            seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    if (fromUser) {

                        mediaSelected = progress;

                        if (mediaPlayer != null) {

                            mediaPlayer.seekTo(progress);
                            tv_timer.setBase( SystemClock.elapsedRealtime() -  progress);

                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

            seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    ic_play_pause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                    mediaPlayer.seekTo((int)mediaSelected);

                    mediaPlayer.start();

                    tv_timer.setBase(SystemClock.elapsedRealtime() - mediaSelected);
                    tv_timer.start();
                    moveSeekBarThread.run();


                }
            });

            try {
                mediaPlayer.setDataSource(chats.getMessage());
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public void ContinueAudio() {

            stateMediaPlayer = play;

            if (mediaSelected != 0){

                tv_timer.setBase( SystemClock.elapsedRealtime() -  mediaSelected);

            }else{

                long intervalOnPause = (SystemClock.elapsedRealtime() - chronStateSave);
                tv_timer.setBase(tv_timer.getBase() + intervalOnPause - mediaSelected);

            }


            tv_timer.start();

            mediaPlayer.start();

            ic_play_pause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baseline_pause_24));

        }

        public void PauseAudio(Chats chats) {
            stateMediaPlayer = pause;

            mediaSelected = 0;

            chronStateSave = SystemClock.elapsedRealtime();
            tv_timer.stop();
            tv_timer.setText(chats.getDate().substring(23,28));
            mediaPlayer.pause();

            mediaPos = mediaPlayer.getCurrentPosition();
            mediaMax = mediaPlayer.getDuration();

            seekBar.setProgress(mediaPos);

            ic_play_pause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
        }
    }
}