package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.UserGroup

class GroupUsersDiffCallback(
    private val newList: ArrayList<UserGroup>,
    private val oldlist: ArrayList<UserGroup>
    ) :    DiffUtil.Callback() {

        override fun getOldListSize() = oldlist.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldlist[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldlist[oldItemPosition] == newList[newItemPosition]

        override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
            val o = oldlist[oldPos]
            val n = newList[newPos]

            return Bundle().apply {
                if (o.userName != n.userName) n.userName?.let { putString("userName", it) }
            }.takeIf { !it.isEmpty }
    }

}



