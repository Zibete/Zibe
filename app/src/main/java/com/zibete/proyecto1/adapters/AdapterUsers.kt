package com.zibete.proyecto1.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.LocationRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.GlassEffect
import com.zibete.proyecto1.utils.ProfileUiBinder
import eightbitlab.com.blurview.BlurView
import java.math.BigDecimal
import java.math.RoundingMode

class AdapterUsers(
    private val context: Context,
    private val locationRepository: LocationRepository,
    private val onChatClicked: (String) -> Unit,
    private val onProfileClicked: (Users) -> Unit,
) : RecyclerView.Adapter<AdapterUsers.ViewHolderAdapter>(), Filterable {

    /** Lista actual mostrada */
    private var usersList: MutableList<Users> = mutableListOf()

    /** Lista original para filtro */
    private var originalList: MutableList<Users> = mutableListOf()

    // ----------------------- FILTER ----------------------- //
    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.lowercase()?.trim() ?: ""

            val filtered = if (query.isEmpty()) {
                originalList
            } else {
                originalList.filter { it.name.lowercase().contains(query) }
            }

            return FilterResults().apply {
                values = filtered
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            usersList = (results.values as List<Users>).toMutableList()
            notifyDataSetChanged()
        }
    }

    // ----------------------- VIEW HOLDER ----------------------- //
    class ViewHolderAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsuario1: TextView = itemView.findViewById(R.id.tv_usuario1)
        val tvEdad: TextView = itemView.findViewById(R.id.tv_edad)
        val distance: TextView = itemView.findViewById(R.id.distance)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        val imgUser: ImageView = itemView.findViewById(R.id.image_user1)

        val goChat: ImageView = itemView.findViewById(R.id.goChat)
        val linearDesc: LinearLayout = itemView.findViewById(R.id.linear_desc)
        val cardView: CardView = itemView.findViewById(R.id.cardviewUsers)

        val blurView: BlurView? = itemView.findViewById(R.id.blur_view)
        val glowBorder: View? = itemView.findViewById(R.id.glow_border)

        init {
            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }
    }

    // ----------------------- CREATE ----------------------- //
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAdapter {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_usuarios, parent, false)
        return ViewHolderAdapter(v)
    }

    // ----------------------- BIND ----------------------- //
    override fun onBindViewHolder(holder: ViewHolderAdapter, position: Int) {
        val u = usersList[position]
        loadUserCard(holder, u)

        holder.goChat.setOnClickListener { onChatClicked(u.id) }
        holder.cardView.setOnClickListener { onProfileClicked(u) }
    }

    override fun getItemCount(): Int = usersList.size

    // ----------------------- CARD DATA ----------------------- //
    private fun loadUserCard(h: ViewHolderAdapter, u: Users) {
        Glide.with(context).load(u.profilePhoto).into(h.imgUser)

        h.tvUsuario1.text = u.name
        h.tvEdad.text = u.age.toString()

        // Distancia ya viene calculada en UsersViewModel como distanceMeters
        h.distance.text = locationRepository.formatDistance(u.distanceMeters)

        if (u.description.isNotEmpty()) {
            h.tvDesc.text = u.description
            h.linearDesc.isVisible = true
        } else {
            h.linearDesc.isVisible = false
        }
    }

    // ----------------------- PUBLIC UPDATE ----------------------- //
    fun updateDataUsers(newList: List<Users>) {
        usersList = newList.toMutableList()
        originalList = newList.toMutableList()
        notifyDataSetChanged()
    }
}
