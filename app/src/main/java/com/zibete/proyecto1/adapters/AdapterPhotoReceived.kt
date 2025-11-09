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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.adapters.AdapterPhotoReceived.ViewHolderAdapterPhoto

class AdapterPhotoReceived(
    var photoList: ArrayList<String>,
    private val maxSize: Int,
    var context: Context
) : RecyclerView.Adapter<ViewHolderAdapterPhoto>() {

    fun addString(photo: String) {
        if (photoList.size > maxSize) {
            photoList.removeAt(0)
            notifyItemRemoved(0)
        }

        photoList.add(photo)
        notifyItemInserted(photoList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAdapterPhoto {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_photos, parent, false)
        val holder = ViewHolderAdapterPhoto(v)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolderAdapterPhoto, position: Int) {
        //final ArrayList <String> photos = photoList.get(position);

        val metrics = DisplayMetrics()
        val windowManager = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthPixels = metrics.widthPixels

        val layoutParams = LinearLayout.LayoutParams(
            widthPixels / 3, widthPixels / 3
        )

        holder.linearPhotoView.layoutParams = layoutParams

        holder.linearPhotoView.visibility = View.VISIBLE

        Glide.with(holder.itemView).load(photoList[position]).into(holder.photoView)

        holder.photoView.setOnClickListener { v ->
            val intent = Intent(context, SlidePhotoActivity::class.java)
            intent.putExtra("photoList", photoList)
            intent.putExtra("position", position)
            intent.putExtra("rotation", 180)
            v.context.startActivity(intent)
        }
    } //Fin del onBindViewHolder

    override fun getItemCount(): Int {
        return photoList.size
    }

    class ViewHolderAdapterPhoto(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var photoView: ImageView = itemView.findViewById(R.id.photoView)
        var linearPhotoView: LinearLayout = itemView.findViewById(R.id.linear_photo_view)
    }
}
