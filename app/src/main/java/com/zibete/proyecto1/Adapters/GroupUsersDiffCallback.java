package com.zibete.proyecto1.Adapters;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.zibete.proyecto1.model.UserGroup;

import java.util.ArrayList;
import java.util.List;

public class GroupUsersDiffCallback extends DiffUtil.Callback {

    private ArrayList<UserGroup> mNewList;
    private ArrayList<UserGroup> mOldList;


    public GroupUsersDiffCallback(List<UserGroup> newlist, List<UserGroup> oldlist) {
        this.mNewList = (ArrayList<UserGroup>) newlist;
        this.mOldList = (ArrayList<UserGroup>) oldlist;
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
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        UserGroup newItem = mNewList.get(newItemPosition);
        UserGroup oldItem = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();


        if(!newItem.getUser_name().equals(oldItem.getUser_name())){
            diff.putString("diff", newItem.getUser_name());
        }


        else{

            diff.putString("other", newItem.getUser_name());

        }


        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}



