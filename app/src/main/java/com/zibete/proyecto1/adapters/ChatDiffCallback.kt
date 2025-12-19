package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Conversation

class ChatDiffCallback(
    private val newList: List<Conversation>,
    private val oldList: List<Conversation>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].otherId === newList[newItemPosition].otherId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
         oldList[oldItemPosition] == newList[newItemPosition]

    override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
        val oldItem = oldList[oldPos]
        val newItem = newList[newPos]

        return Bundle().apply {
            if (oldItem.otherId != newItem.otherId)
                putString("id", newItem.otherId)

            if (oldItem.unreadCount != newItem.unreadCount)
                putString("date", newItem.date.toString())
        }.takeIf { !it.isEmpty }
    }

}



