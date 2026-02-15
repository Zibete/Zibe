package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.RowFavoritesBinding
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_AGE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_NAME
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_ONLINE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_PHOTO_URL
import com.zibete.proyecto1.ui.favorites.FavoriteUserUi
import com.zibete.proyecto1.core.utils.ZibeApp

class AdapterFavoriteUsers(
    private val onUserClicked: (FavoriteUserUi) -> Unit
) : ListAdapter<FavoriteUserUi, AdapterFavoriteUsers.FavoriteViewHolder>(FavoritesDiffCallback) {

    private var originalList: List<FavoriteUserUi> = emptyList()
    private val itemHeight: Int = (ZibeApp.ScreenUtils.widthPx / 3).coerceAtLeast(1)

    inner class FavoriteViewHolder(
        private val binding: RowFavoritesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            adjustItemHeight()
        }

        fun bind(item: FavoriteUserUi) = with(binding) {
            tvFavoriteUser.text = item.name
            tvFavoriteAge.text = item.age.toString()

            Glide.with(root)
                .load(item.profilePhoto)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .into(imageFavoriteUser)

            bindOnlinePayload(item.isOnline)

            cardviewFavorites.setOnClickListener { onUserClicked(item) }
        }

        fun bindPayload(payload: Any, item: FavoriteUserUi) = with(binding) {
            val changes = payload as? Set<*> ?: run {
                bind(item)
                return
            }

            if (PAYLOAD_ONLINE in changes) {
                iconConnected.isVisible = item.isOnline
                iconDisconnected.isVisible = !item.isOnline
            }

            if (PAYLOAD_NAME in changes) {
                tvFavoriteUser.text = item.name
            }

            if (PAYLOAD_AGE in changes) {
                tvFavoriteAge.text = item.age.toString()
            }

            if (PAYLOAD_PHOTO_URL in changes) {
                Glide.with(root)
                    .load(item.profilePhoto)
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(imageFavoriteUser)
            }
        }

        private fun bindOnlinePayload(isOnline: Boolean) = with(binding) {
            iconConnected.isVisible = isOnline
            iconDisconnected.isVisible = !isOnline
        }

        private fun adjustItemHeight() = with(binding) {
            val params = linearCardFavorites.layoutParams
            params.height = itemHeight
            linearCardFavorites.layoutParams = params
        }
    }

    fun submitOriginal(list: List<FavoriteUserUi>) {
        val safeList = list.toList()
        originalList = safeList
        submitList(safeList)
    }

    fun filterByName(query: String?) {
        val q = query.orEmpty().trim()
        if (q.isEmpty()) {
            submitList(originalList)
        } else {
            submitList(originalList.filter { it.name.contains(q, ignoreCase = true) })
        }
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

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        val payload = payloads.firstOrNull()
        if (payload != null) holder.bindPayload(payload, item) else holder.bind(item)
    }

}
