package com.zibete.proyecto1.Adapters;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.zibete.proyecto1.POJOS.ChatWith;
import com.zibete.proyecto1.POJOS.Users;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UsersDiffCallback extends DiffUtil.Callback {

    private ArrayList<Users> mNewList;
    private ArrayList<Users> mOldList;


    public UsersDiffCallback(List<Users> newlist, List<Users> oldlist) {
        this.mNewList = (ArrayList<Users>) newlist;
        this.mOldList = (ArrayList<Users>) oldlist;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).getDistance()==mNewList.get(newItemPosition).getDistance();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Users newItem = mNewList.get(newItemPosition);
        Users oldItem = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();


        if(!Objects.equals(newItem.getDistance(), oldItem.getDistance())){
            diff.putString("distance", String.valueOf(newItem.getDistance()));
        }


        else{




            if(!newItem.getAge().equals(oldItem.getAge())){
                diff.putString("age", String.valueOf(newItem.getAge()));
            }


            diff.putString("other", newItem.getNombre());





        }



        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}



