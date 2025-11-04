package com.zibete.proyecto1.Adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil

class FavoritesDiffCallback(
    private val newList: ArrayList<String>,
    private val oldList: ArrayList<String>
) :  DiffUtil.Callback() {

    override fun getOldListSize() = newList.size

    override fun getNewListSize() = oldList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun getChangePayload(oldPos: Int, newPos: Int): Any? = null // innecesario para String
}



