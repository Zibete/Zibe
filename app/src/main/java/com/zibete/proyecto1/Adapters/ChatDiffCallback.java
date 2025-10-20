package com.zibete.proyecto1.Adapters;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.zibete.proyecto1.POJOS.ChatWith;

import java.util.ArrayList;
import java.util.List;

public class ChatDiffCallback extends DiffUtil.Callback {

    private ArrayList<ChatWith> mNewList;
    private ArrayList<ChatWith> mOldList;


    public ChatDiffCallback(List<ChatWith> newlist, List<ChatWith> oldlist) {
        this.mNewList = (ArrayList<ChatWith>) newlist;
        this.mOldList = (ArrayList<ChatWith>) oldlist;
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
        return mOldList.get(oldItemPosition).getwUserID()==mNewList.get(newItemPosition).getwUserID();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        ChatWith newItem = mNewList.get(newItemPosition);
        ChatWith oldItem = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();


        if(!newItem.getwUserID().equals(oldItem.getwUserID())){
            diff.putString("id", newItem.getwUserID());
        }


        else{




            if(!newItem.getNoVisto().equals(oldItem.getNoVisto())){
                diff.putString("date", newItem.getwDate());
            }


            diff.putString("other", newItem.getwUserID());





        }



        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}



