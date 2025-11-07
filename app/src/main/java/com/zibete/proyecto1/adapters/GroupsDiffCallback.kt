package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Groups

class GroupsDiffCallback(
    private val newList: List<Groups>,
    private val oldList: List<Groups>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].users == newList[newItemPosition].users

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]

        return Bundle().apply {
            if (oldItem.users != newItem.users) putString("users", newItem.users.toString())
        }.takeIf { !it.isEmpty }

    }
}



