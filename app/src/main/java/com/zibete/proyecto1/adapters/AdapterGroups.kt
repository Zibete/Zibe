package com.zibete.proyecto1.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.databinding.RowGroupBinding
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.GlassEffect
import eightbitlab.com.blurview.BlurView
import java.util.Locale
import java.util.ArrayList

class AdapterGroups(
    private val groupsList: MutableList<Groups>,
    private val originalGroupsArrayList: MutableList<Groups>,
    private val context: Context,
    private val onGroupClicked: (Groups) -> Unit
) : RecyclerView.Adapter<AdapterGroups.ViewHolder>(), Filterable {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // ---------- FILTRO ----------
    override fun getFilter(): Filter = filterGroups

    private val filterGroups = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filtered = ArrayList<Groups>()
            val search = constraint.toString().lowercase(Locale.getDefault()).trim()

            if (search.isEmpty()) {
                filtered.addAll(originalGroupsArrayList)
            } else {
                for (group in originalGroupsArrayList) {
                    if (group.name.lowercase(Locale.getDefault()).contains(search)) {
                        filtered.add(group)
                    }
                }
            }
            return FilterResults().apply { values = filtered }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            groupsList.clear()
            @Suppress("UNCHECKED_CAST")
            groupsList.addAll(results.values as List<Groups>)
            notifyDataSetChanged()
        }
    }

    // ---------- VIEW HOLDER ----------
    inner class ViewHolder(val binding: RowGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val blurView: BlurView = binding.blurView
        val glowBorder: View = binding.glowBorder
        val card = binding.cardviewGroups

        init {
            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowGroupBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = groupsList.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any?>
    ) {
        val group = groupsList[position]
        if (payloads.isEmpty()) {
            bindGroup(holder, group)
        } else {
            (payloads[0] as? Bundle)
                ?.takeIf { it.containsKey("users") }
                ?.let { bindGroup(holder, group) }
        }

        holder.card.setOnClickListener {
            if (group.category == Constants.PUBLIC_GROUP) {
                onGroupClicked(group)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    private fun bindGroup(holder: ViewHolder, group: Groups) = with(holder.binding) {
        tvTitle.text = group.name
        tvDataGroup.text = group.data
        tvDataGroup.isSelected = true

        // Ahora usamos directamente el valor ya seteado en el modelo
        tvNumberPersons.text = group.users.toString()
    }

    // ---------- UTILIDADES ----------
    fun addGroup(group: Groups) {
        groupsList.add(group)
        originalGroupsArrayList.add(group)
        notifyItemInserted(groupsList.size - 1)
    }

    fun updateDataGroups(newList: ArrayList<Groups>) {
        val diff = GroupsDiffCallback(newList, groupsList)
        val result = DiffUtil.calculateDiff(diff)
        result.dispatchUpdatesTo(this)
        groupsList.clear()
        groupsList.addAll(newList)

        // Mantenemos también el original actualizado para el filtro
        originalGroupsArrayList.clear()
        originalGroupsArrayList.addAll(newList)
    }

    override fun getItemViewType(position: Int): Int = Constants.PUBLIC_GROUP
}
