package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.databinding.RowGroupBinding
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.core.constants.Constants
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_CATEGORY
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_DATA
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_GROUPS_USERS
import com.zibete.proyecto1.core.utils.GlassEffect
import eightbitlab.com.blurview.BlurView

class AdapterGroups(
    private val onGroupClicked: (Groups) -> Unit
) : ListAdapter<Groups, AdapterGroups.ViewHolder>(GroupsDiffCallback) {

    private var originalList: List<Groups> = emptyList()

    fun submitOriginal(list: List<Groups>) {
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

        fun bind(item: Groups) = with(binding) {
            tvTitle.text = item.name
            tvDataGroup.text = item.description
            tvDataGroup.isSelected = true
            tvNumberPersons.text = item.users.toString()

            card.setOnClickListener {
                if (item.type == Constants.PUBLIC_GROUP) {
                    onGroupClicked(item)
                }
            }
        }

        fun bindPayload(payload: Any, item: Groups) = with(binding) {
            val changes = payload as? Set<String> ?: run {
                bind(item)
                return
            }

            if (PAYLOAD_GROUPS_USERS in changes) {
                tvNumberPersons.text = item.users.toString()
            }

            if (PAYLOAD_GROUPS_DATA in changes) {
                tvDataGroup.text = item.description
            }

            if (PAYLOAD_GROUPS_CATEGORY in changes) {
                // si cambia categoría, puede afectar el click habilitado
                card.setOnClickListener {
                    if (item.type == Constants.PUBLIC_GROUP) {
                        onGroupClicked(item)
                    }
                }
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
