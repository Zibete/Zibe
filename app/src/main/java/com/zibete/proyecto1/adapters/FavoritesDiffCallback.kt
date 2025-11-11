package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil

class FavoritesDiffCallback(
    private val newList: List<String>,
    private val oldList: List<String>
) :  DiffUtil.Callback() {

    override fun getOldListSize() = newList.size

    override fun getNewListSize() = oldList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun getChangePayload(oldPos: Int, newPos: Int): Any? = null // innecesario para String
}



