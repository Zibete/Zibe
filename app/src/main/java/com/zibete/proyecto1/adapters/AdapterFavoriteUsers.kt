package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.adapters.FavoritesDiffCallback.PayloadFavoriteUser
import com.zibete.proyecto1.databinding.RowFavoritesBinding
import com.zibete.proyecto1.ui.favorites.FavoriteUserUi
import com.zibete.proyecto1.core.utils.ZibeApp
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.extensions.bindStatusIndicator
import com.zibete.proyecto1.ui.extensions.loadAvatar

class AdapterFavoriteUsers(
    private val onUserClicked: (FavoriteUserUi) -> Unit
) : ListAdapter<FavoriteUserUi, AdapterFavoriteUsers.FavoriteViewHolder>(FavoritesDiffCallback) {

    private val itemHeight: Int = (ZibeApp.ScreenUtils.widthPx / 3).coerceAtLeast(1)

    inner class FavoriteViewHolder(
        val binding: RowFavoritesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            adjustItemHeight()
        }

        fun bind(item: FavoriteUserUi) = with(binding) {
            favoriteUserName.text = item.name
            favoriteUserAge.text = item.age.toString()
            imageFavoriteUser.loadAvatar(item.profilePhoto)
            bindOnlineIndicator(this, item.isOnline)
            bindClicks(binding, item)
        }

        fun bindPayload(payload: Any, item: FavoriteUserUi) = with(binding) {
            val changes = payload as? Set<*> ?: run { bind(item); return }

            if (PayloadFavoriteUser.NAME in changes) favoriteUserName.text = item.name
            if (PayloadFavoriteUser.AGE in changes) favoriteUserAge.text = item.age.toString()
            if (PayloadFavoriteUser.PHOTO_URL in changes) imageFavoriteUser.loadAvatar(item.profilePhoto)
            if (PayloadFavoriteUser.IS_ONLINE in changes) bindOnlineIndicator(this, item.isOnline)

            bindClicks(binding, item)
        }

        fun bindClicks(binding: RowFavoritesBinding, item: FavoriteUserUi) =
            binding.cardviewFavorites.setOnClickListener { onUserClicked(item) }

        private fun adjustItemHeight() = with(binding) {
            val params = cardviewFavorites.layoutParams
            params.height = itemHeight
            cardviewFavorites.layoutParams = params
        }
    }

    fun submitOriginal(list: List<FavoriteUserUi>) {
        val safeList = list.toList()
        submitList(safeList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = RowFavoritesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: FavoriteViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        val payload = payloads.firstOrNull()
        if (payload != null) holder.bindPayload(payload, item) else holder.bind(item)
    }

    override fun onViewRecycled(holder: FavoriteViewHolder) {
        Glide.with(holder.binding.root).clear(holder.binding.imageFavoriteUser)
        holder.binding.imageFavoriteUser.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    private fun bindOnlineIndicator(b: RowFavoritesBinding, isOnline: Boolean) {
        b.statusIndicator.isVisible = true
        val status = if (isOnline) UserStatus.Online else UserStatus.Offline
        b.statusIndicator.bindStatusIndicator(b.root.context, status)
    }

}
