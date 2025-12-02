package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Users

class UsersDiffCallback(
    private val newlist: List<Users>,
    private val oldlist: List<Users>
) : DiffUtil.Callback() {

    override fun getNewListSize(): Int { return newlist.size }
    override fun getOldListSize(): Int { return oldlist.size }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldlist[oldItemPosition].distanceMeters == newlist[newItemPosition].distanceMeters

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldlist[oldItemPosition] == newlist[newItemPosition]

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newlist[newItemPosition]
        val oldItem = oldlist[oldItemPosition]
        val diff = Bundle()

        if (oldItem.distanceMeters != newItem.distanceMeters)
            diff.putDouble("distance", newItem.distanceMeters)

        if (oldItem.age != newItem.age)
            newItem.age?.let { diff.putInt("age", it) }

        if (oldItem.name != newItem.name)
            diff.putString("name", newItem.name)

        return if (diff.isEmpty) null else diff
    }
}



