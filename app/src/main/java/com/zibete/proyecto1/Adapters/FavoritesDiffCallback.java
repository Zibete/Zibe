package com.zibete.proyecto1.Adapters;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.zibete.proyecto1.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FavoritesDiffCallback extends DiffUtil.Callback {

    private ArrayList<String> mNewList;
    private ArrayList<String> mOldList;


    public FavoritesDiffCallback(List<String> newlist, List<String> oldlist) {
        this.mNewList = (ArrayList<String>) newlist;
        this.mOldList = (ArrayList<String>) oldlist;
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




        return mOldList.get(oldItemPosition).getBytes()==mNewList.get(newItemPosition).getBytes();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        String newItem = mNewList.get(newItemPosition);
        String oldItem = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();


        if(!Objects.equals(newItem.getBytes(), oldItem.getBytes())){
            diff.putString("id", Arrays.toString(newItem.getBytes()));
        }


        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}



