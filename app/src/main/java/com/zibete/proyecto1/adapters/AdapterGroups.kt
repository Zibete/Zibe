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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.databinding.RowGroupBinding
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.GlassEffect
import eightbitlab.com.blurview.BlurView
import java.util.*

class AdapterGroups(
    private val groupsList: MutableList<Groups>,
    private val originalGroupsArrayList: MutableList<Groups>,
    private val context: Context,
    // Lambda: El fragmento nos dirá qué hacer cuando se toque un grupo
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
    inner class ViewHolder(val binding: RowGroupBinding) : RecyclerView.ViewHolder(binding.root) {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any?>) {
        val group = groupsList[position]
        if (payloads.isEmpty()) bindGroup(holder, group)
        else (payloads[0] as? Bundle)?.takeIf { it.containsKey("users") }?.let { bindGroup(holder, group) }

        holder.card.setOnClickListener {
            // Solo pasamos el evento, no decidimos nada aquí
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

        // Nota: Idealmente esto debería venir en el modelo Groups para evitar llamadas en onBind
        FirebaseRefs.refGroupUsers.child(group.name).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvNumberPersons.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
    }

    override fun getItemViewType(position: Int): Int = Constants.PUBLIC_GROUP
}