package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.zibete.proyecto1.databinding.ItemSlidePhotoBinding

class SliderPhotoAdapter(
    private val urls: List<String>,
    private val onLoadStart: (position: Int) -> Unit,
    private val onLoadEnd: (position: Int) -> Unit
) : RecyclerView.Adapter<SliderPhotoAdapter.VH>() {

    inner class VH(val binding: ItemSlidePhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        val photoView get() = binding.photoView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemSlidePhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = urls.getOrNull(position).orEmpty()

        holder.photoView.load(url) {
            crossfade(true)
            scale(Scale.FIT)

            listener(
                onStart = { onLoadStart(position) },
                onSuccess = { _, _ -> onLoadEnd(position) },
                onError = { _, _ -> onLoadEnd(position) },
                onCancel = { onLoadEnd(position) }
            )
        }
    }
}

