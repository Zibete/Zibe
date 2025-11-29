package com.zibete.proyecto1.adapters

import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.PerfilActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.Utils.calcAge
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos

class AdapterFavoriteUsers(
    private val favorites: MutableList<String>,
    private val context: Context
) : RecyclerView.Adapter<AdapterFavoriteUsers.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_favorite_user)
        val tvAge: TextView = itemView.findViewById(R.id.tv_favorite_age)
        val imgUser: ImageView = itemView.findViewById(R.id.image_favorite_user)
        val card: CardView = itemView.findViewById(R.id.cardview_favorites)
        val iconOnline: ImageView = itemView.findViewById(R.id.`@+id/icon_connected`)
        val iconOffline: ImageView = itemView.findViewById(R.id.`@+id/icon_disconnected`)
        val container: LinearLayout = itemView.findViewById(R.id.linearCardFavorites)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_favorites, parent, false)
        return FavoriteViewHolder(v)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favoriteUserId = favorites[position]

        // Ajuste de alto proporcional (grid 3 columnas estilo card cuadrado-ish)
        adjustItemHeight(holder)

        // Cargar info del usuario
        bindUserCard(holder, favoriteUserId)

        // Click -> Perfil
        holder.card.setOnClickListener { v ->
            val intent = Intent(v.context, PerfilActivity::class.java)
            intent.putExtra("id_user", favoriteUserId)
            v.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = favorites.size

    // ========== Public API ==========

    fun addUser(userId: String) {
        favorites.add(userId)
        notifyItemInserted(favorites.size - 1)
    }

    fun updateDataUsers(newList: List<String>) {
        val diffCallback = FavoritesDiffCallback(newList, favorites)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        favorites.clear()
        favorites.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    // ========== Internals ==========

    private fun adjustItemHeight(holder: FavoriteViewHolder) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)

        val width = metrics.widthPixels
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            width / 3 // altura = 1/3 ancho (3 columnas)
        )
        holder.container.layoutParams = params
    }

    private fun bindUserCard(holder: FavoriteViewHolder, userId: String) {
        // Datos del usuario
        refCuentas.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val u = snapshot.getValue(Users::class.java) ?: return

                    holder.tvAge.text = calcAge(u.birthDay).toString()
                    holder.tvName.text = u.name

                    Glide.with(context)
                        .load(u.profilePhoto)
                        .placeholder(R.drawable.ic_person_24)
                        .error(R.drawable.ic_person_24)
                        .into(holder.imgUser)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Estado online / offline
        refDatos.child(userId).child("Estado")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showOffline(holder)
                        return
                    }

                    val estado = snapshot.child("estado").getValue(String::class.java)
                    if (estado == context.getString(R.string.online)) {
                        showOnline(holder)
                    } else {
                        showOffline(holder)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showOffline(holder)
                }
            })
    }

    private fun showOnline(holder: FavoriteViewHolder) {
        holder.iconOnline.visibility = View.VISIBLE
        holder.iconOffline.visibility = View.GONE
    }

    private fun showOffline(holder: FavoriteViewHolder) {
        holder.iconOnline.visibility = View.GONE
        holder.iconOffline.visibility = View.VISIBLE
    }
}
