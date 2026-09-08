package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_DATA
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_USERS
import com.zibete.proyecto1.core.utils.GlassEffect
import com.zibete.proyecto1.databinding.RowGroupBinding
import com.zibete.proyecto1.domain.roomsv2.RoomV2DirectoryItem
import eightbitlab.com.blurview.BlurView

class AdapterGroups(
    private val onGroupClicked: (RoomV2DirectoryItem) -> Unit
) : ListAdapter<RoomV2DirectoryItem, AdapterGroups.ViewHolder>(GroupsDiffCallback) {

    private var originalList: List<RoomV2DirectoryItem> = emptyList()

    fun submitOriginal(list: List<RoomV2DirectoryItem>) {
        originalList = list
        submitList(list)
    }

    fun filterByName(query: String?) {
        val q = query.orEmpty().trim().lowercase()
        if (q.isEmpty()) {
            submitList(originalList)
            return
        }

        submitList(
            originalList.filter { it.name.lowercase().contains(q) }
        )
    }

    inner class ViewHolder(val binding: RowGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val blurView: BlurView = binding.blurView
        val glowBorder: View = binding.glowBorder
        val card = binding.cardviewGroups

        init {
            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }

        fun bind(item: RoomV2DirectoryItem) = with(binding) {
            tvTitle.text = item.name
            tvDataGroup.text = item.description
            tvDataGroup.isSelected = true
            tvNumberPersons.text = item.memberCount.toString()

            card.setOnClickListener {
                onGroupClicked(item)
            }
        }

        fun bindPayload(payload: Any, item: RoomV2DirectoryItem) = with(binding) {
            val changes = payload as? Set<String> ?: run {
                bind(item)
                return
            }

            if (PAYLOAD_GROUPS_USERS in changes) {
                tvNumberPersons.text = item.memberCount.toString()
            }

            if (PAYLOAD_GROUPS_DATA in changes) {
                tvDataGroup.text = item.description
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        val payload = payloads.firstOrNull()
        if (payload != null) holder.bindPayload(payload, item) else holder.bind(item)
    }
}
