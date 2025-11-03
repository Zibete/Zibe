package com.zibete.proyecto1.Adapters;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.zibete.proyecto1.model.Groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupsDiffCallback extends DiffUtil.Callback {

    private ArrayList<Groups> mNewList;
    private ArrayList<Groups> mOldList;


    public GroupsDiffCallback(List<Groups> newlist, List<Groups> oldlist) {
        this.mNewList = (ArrayList<Groups>) newlist;
        this.mOldList = (ArrayList<Groups>) oldlist;
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
        return mOldList.get(oldItemPosition).getUsers()==mNewList.get(newItemPosition).getUsers();
    }


    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Groups newItem = mNewList.get(newItemPosition);
        Groups oldItem = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();

        if(!Objects.equals(newItem.getUsers(), oldItem.getUsers())){

            diff.putString("users", String.valueOf(newItem.getUsers()));

        }

        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}



