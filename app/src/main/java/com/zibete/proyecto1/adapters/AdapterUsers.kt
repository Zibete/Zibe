package com.zibete.proyecto1.adapters

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.GlassEffect
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import eightbitlab.com.blurview.BlurView
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@AndroidEntryPoint
class AdapterUsers @Inject constructor(
    private val profileUiBinder: ProfileUiBinder,
    private val usersList: MutableList<Users>,
    private val usersListAll: MutableList<Users>,
    private val context: Context,
    // --- ACCIONES (Callbacks) ---
    private val onChatClicked: (String) -> Unit,       // Pasamos el ID del usuario
    private val onProfileClicked: (Users) -> Unit,     // Pasamos el objeto usuario completo
    private val onListUpdated: () -> Unit              // Para notificar scroll o cambios
) : RecyclerView.Adapter<AdapterUsers.ViewHolderAdapter>(), Filterable {


    // --------------------- Filtro --------------------- //
    override fun getFilter(): Filter = filterChats

    private val filterChats = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val search = constraint?.toString()?.lowercase()?.trim().orEmpty()
            val filtered = if (search.isEmpty()) {
                usersListAll.toList()
            } else {
                usersListAll.filter { it.name.lowercase().contains(search) }
            }
            return FilterResults().apply { values = filtered }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            usersList.clear()
            usersList.addAll(results.values as MutableList<Users>)
            notifyDataSetChanged()
            onListUpdated() // Reemplaza a UsuariosFragment.setScrollbar()
        }
    }

    // --------------------- ViewHolder --------------------- //
    class ViewHolderAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsuario1: TextView = itemView.findViewById(R.id.tv_usuario1)
        val tvEdad: TextView = itemView.findViewById(R.id.tv_edad)
        val distance: TextView = itemView.findViewById(R.id.distance)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        val tvEstado: TextView = itemView.findViewById(R.id.`@+id/tv_status`)
        val imgUser: ImageView = itemView.findViewById(R.id.image_user1)
        val iconConectado: ImageView = itemView.findViewById(R.id.`@+id/icon_connected`)
        val iconDesconectado: ImageView = itemView.findViewById(R.id.`@+id/icon_disconnected`)
        val goChat: ImageView = itemView.findViewById(R.id.goChat)
        val favoriteOn: ImageView = itemView.findViewById(R.id.favorite_on)
        val bloqMe: ImageView = itemView.findViewById(R.id.bloq_me)
        val bloq: ImageView = itemView.findViewById(R.id.bloq)
        val linearDesc: LinearLayout = itemView.findViewById(R.id.linear_desc)
        val cardView: CardView = itemView.findViewById(R.id.cardviewUsers)
        val blurView: BlurView? = itemView.findViewById(R.id.blur_view)
        val glowBorder: View? = itemView.findViewById(R.id.glow_border)

        init {
            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }
    }

    // --------------------- Create --------------------- //
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAdapter {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_usuarios, parent, false)
        return ViewHolderAdapter(v)
    }

    // --------------------- Bind --------------------- //
    override fun onBindViewHolder(holder: ViewHolderAdapter, position: Int, payloads: MutableList<Any?>) {
        val u = usersList[position]

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            loadUserCard(holder, u)
        } else {
            val o = payloads.firstOrNull() as? android.os.Bundle
            if (o?.containsKey("distance") == true) loadUserCard(holder, u)
        }

        // Acciones delegadas al Fragmento
        holder.goChat.setOnClickListener {
            onChatClicked(u.id)
        }

        holder.cardView.setOnClickListener {
            onProfileClicked(u)
        }
    }

    override fun onBindViewHolder(holder: ViewHolderAdapter, position: Int) {
        // Manejado en la versión con payloads
    }

    // --------------------- Datos por card --------------------- //
    @SuppressLint("SetTextI18n")
    private fun loadUserCard(h: ViewHolderAdapter, users: Users) {
        Glide.with(context).load(users.profilePhoto).into(h.imgUser)
        h.tvUsuario1.text = users.name

        // Distancia
        val dist = profileUiBinder.getDistanceMeters(
            userRepository.latitude,
            UserRepository.longitude,
            users.latitude,
            users.longitude
        )
        h.distance.text = formatDistance(dist)

        // Edad
        h.tvEdad.text = Utils.calcAge(users.birthDay).toString()

        // Descripción
        if (users.description.isNotEmpty()) {
            h.tvDesc.text = users.description
            h.linearDesc.visibility = View.VISIBLE
        } else {
            h.linearDesc.visibility = View.GONE
        }

        // Estado (Online/Offline)
        UserRepository.stateUser(context, users.id, h.iconConectado, h.iconDesconectado, h.tvEstado, Constants.CHATWITH)

        // Listeners Visuales (Favorito, Bloqueado, Me Bloqueó)
        currentUserUid?.let { uid ->
            // Favorito
            FirebaseRefs.refDatos.child(uid).child("FavoriteList").child(users.id)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) { h.favoriteOn.isVisible = snap.exists() }
                    override fun onCancelled(error: DatabaseError) {}
                })

            // Bloqueado (Yo lo bloqueé)
            FirebaseRefs.refDatos.child(uid).child(Constants.CHATWITH).child(users.id).child("estado")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        h.bloq.isVisible = (snap.getValue(String::class.java) == "bloq")
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            // Me bloqueó (Él me bloqueó)
            FirebaseRefs.refDatos.child(users.id).child(Constants.CHATWITH).child(uid).child("estado")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val blocked = snap.getValue(String::class.java) == "bloq"
                        h.bloqMe.isVisible = blocked
                        h.goChat.isVisible = !blocked
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    // --------------------- Helpers --------------------- //

    private fun formatDistance(dist: Double): String {
        return when {
            dist > 10000 -> "A ${BigDecimal(dist / 1000.0).setScale(0, RoundingMode.HALF_UP)} km"
            dist > 1000 -> "A ${BigDecimal(dist / 1000.0).setScale(1, RoundingMode.HALF_UP)} km"
            else -> "A ${BigDecimal(dist).setScale(0, RoundingMode.HALF_UP)} m"
        }
    }

    fun addUser(u: Users) {
        usersList.add(u)
        usersListAll.add(u)
        notifyItemInserted(usersList.size - 1)
    }

    override fun getItemCount() = usersList.size

    fun updateDataUsers(newList: List<Users>) {
        val diff = UsersDiffCallback(newList, usersList)
        val result = DiffUtil.calculateDiff(diff)
        result.dispatchUpdatesTo(this)
        usersList.clear()
        usersList.addAll(newList)
        usersListAll.clear()
        usersListAll.addAll(newList)
    }
}