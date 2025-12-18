package com.zibete.proyecto1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.github.chrisbanes.photoview.PhotoView
import com.zibete.proyecto1.databinding.ItemSlidePhotoBinding

class SliderPhotoAdapter(
    private val urls: List<String>,
    private val onLoadStart: () -> Unit,
    private val onLoadEnd: () -> Unit
) : RecyclerView.Adapter<SliderPhotoAdapter.VH>() {

    inner class VH(val binding: ItemSlidePhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        val photoView: PhotoView get() = binding.photoView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSlidePhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = urls.getOrNull(position).orEmpty()
        onLoadStart()
        holder.photoView.load(url) {
            crossfade(true)
            scale(Scale.FIT)
            listener(
                onSuccess = { _, _ -> onLoadEnd() },
                onError = { _, _ -> onLoadEnd() }
            )
        }
    }
}
