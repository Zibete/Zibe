package com.zibete.proyecto1.adapters

import android.content.Context
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
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.favorites.FavoriteUserUi

class AdapterFavoriteUsers(
    private val favorites: MutableList<FavoriteUserUi>,
    private val context: Context,
    private val onUserClicked: (FavoriteUserUi) -> Unit
) : RecyclerView.Adapter<AdapterFavoriteUsers.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_favorite_user)
        val tvAge: TextView = itemView.findViewById(R.id.tv_favorite_age)
        val imgUser: ImageView = itemView.findViewById(R.id.image_favorite_user)
        val card: CardView = itemView.findViewById(R.id.cardview_favorites)
        val iconOnline: ImageView = itemView.findViewById(R.id.icon_connected)
        val iconOffline: ImageView = itemView.findViewById(R.id.icon_disconnected)
        val container: LinearLayout = itemView.findViewById(R.id.linearCardFavorites)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_favorites, parent, false)
        return FavoriteViewHolder(v)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favorites[position]

        adjustItemHeight(holder)
        bindUserCard(holder, favorite)

        holder.card.setOnClickListener {
            onUserClicked(favorite)
        }
    }

    override fun getItemCount(): Int = favorites.size

    // ========== Public API ==========

    fun updateDataUsers(newList: List<FavoriteUserUi>) {
        val diffCallback = FavoriteUsersDiffCallback(newList, favorites)
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

    private fun bindUserCard(holder: FavoriteViewHolder, favorite: FavoriteUserUi) {
        holder.tvName.text = favorite.name
        holder.tvAge.text = favorite.age.toString()

        Glide.with(context)
            .load(favorite.profilePhoto)
            .placeholder(R.drawable.ic_person_24)
            .error(R.drawable.ic_person_24)
            .into(holder.imgUser)

        if (favorite.isOnline) {
            showOnline(holder)
        } else {
            showOffline(holder)
        }
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

// DiffUtil específico para FavoriteUserUi
class FavoriteUsersDiffCallback(
    private val newList: List<FavoriteUserUi>,
    private val oldList: List<FavoriteUserUi>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}
