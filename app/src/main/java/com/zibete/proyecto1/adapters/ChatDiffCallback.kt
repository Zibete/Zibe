package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.ChatWith

class ChatDiffCallback(
    private val newList: ArrayList<ChatWith>,
    private val oldList: ArrayList<ChatWith>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].userId === newList[newItemPosition].userId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
         oldList[oldItemPosition] == newList[newItemPosition]

    override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
        val oldItem = oldList[oldPos]
        val newItem = newList[newPos]

        return Bundle().apply {
            if (oldItem.userId != newItem.userId)
                putString("id", newItem.userId)

            if (oldItem.noSeen != newItem.noSeen)
                putString("date", newItem.date.toString())
        }.takeIf { !it.isEmpty }
    }

}



