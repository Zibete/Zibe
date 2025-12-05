package com.zibete.proyecto1.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.data.UserRepository
import javax.inject.Inject

class SliderProfileAdapter @Inject constructor(
    private val profileUiBinder: ProfileUiBinder,
    private val userRepository: UserRepository,
    private val context: Context,
    private val userList: MutableList<Users>,
    private val rotation: Int
) : RecyclerView.Adapter<SliderProfileAdapter.VH>() {

    // --- ViewHolder ---
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val floatingActionMenu: FloatingActionMenu = itemView.findViewById(R.id.floatingActionMenu)
        val subMenuChatWith: FloatingActionButton = itemView.findViewById(R.id.menu_go_chat)
        val subMenuChatWithUnknown: FloatingActionButton = itemView.findViewById(R.id.menu_go_chat_group)

        val recyclerPhotos: RecyclerView = itemView.findViewById(R.id.recyclerPhotos)
        val linearImageActivity: LinearLayout = itemView.findViewById(R.id.linearImageActivity)
        val linearPhotos: LinearLayout = itemView.findViewById(R.id.linearPhotos)
        val distanceUser: TextView = itemView.findViewById(R.id.distanceUser)
        val ftPerfil: ImageView = itemView.findViewById(R.id.ftPerfil)
        val nameUser: TextView = itemView.findViewById(R.id.nameUser)
        val desc: TextView = itemView.findViewById(R.id.desc)
        val age: TextView = itemView.findViewById(R.id.edad)
        val tvEstado: TextView = itemView.findViewById(R.id.tv_status)
        val iconConectado: ImageView = itemView.findViewById(R.id.icon_connected)
        val iconDesconectado: ImageView = itemView.findViewById(R.id.icon_disconnected)
        val perfilFavoriteOff: ImageView = itemView.findViewById(R.id.perfil_favorite_off)
        val perfilFavoriteOn: ImageView = itemView.findViewById(R.id.perfil_favorite_on)
        val perfilBloq: ImageView = itemView.findViewById(R.id.perfil_bloq)
        val perfilBloqMe: ImageView = itemView.findViewById(R.id.perfil_bloq_me)
        val coordinatorLayoutPhoto: CoordinatorLayout = itemView.findViewById(R.id.coordinatorLayoutPhoto)
    }

    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.adapter_profile, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = userList[position]

        // FAB menu
        holder.floatingActionMenu.setClosedOnTouchOutside(true)

        // Carrusel de “fotos recibidas”
        val receivedPhotos = ArrayList<String>()
        val adapterPhotoReceived = AdapterPhotoReceived(receivedPhotos, Constants.MAXCHATSIZE, context)
        holder.recyclerPhotos.apply {
            val lm = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true).apply { stackFromEnd = true }
            layoutManager = lm
            adapter = adapterPhotoReceived
        }

        // Nombre
        holder.nameUser.text = user.name

        // Alto dinámico del contenedor de imagen principal (75% pantalla aprox.)
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val height = metrics.heightPixels
        holder.linearImageActivity.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            height - (height / 4)
        )

        // Favoritos ON/OFF
        holder.perfilFavoriteOff.setOnClickListener {
            currentUser?.uid?.let { uid ->
                FirebaseRefs.refDatos.child(uid).child("FavoriteList").child(user.id).setValue(user.id)
                Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
            }
        }
        holder.perfilFavoriteOn.setOnClickListener {
            currentUser?.uid?.let { uid ->
                FirebaseRefs.refDatos.child(uid).child("FavoriteList").child(user.id).removeValue()
                Toast.makeText(context, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
            }
        }

        // Descripción visible/oculta
        if (user.description.isNotEmpty()) {
            holder.desc.text = user.description
            holder.desc.visibility = View.VISIBLE
        } else {
            holder.desc.visibility = View.GONE
        }

        // ProgressBar centrado para la foto de perfil
        val progressBar = ProgressBar(context).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.CENTER }
            isIndeterminate = true
        }
        holder.coordinatorLayoutPhoto.addView(progressBar)

        // Foto de perfil (c/ esquinas redondeadas)
        Glide.with(context)
            .load(user.profilePhoto)
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable?> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    progressBar.visibility = View.GONE
                    return false                }


            })
            .into(holder.ftPerfil)

        // Abrir foto en SlidePhotoActivity (full screen + zoom) — usa la misma lógica que fotos recibidas
        holder.linearImageActivity.setOnClickListener {
            val photoList = arrayListOf(user.profilePhoto)
            val intent = Intent(context, SlidePhotoActivity::class.java).apply {
                putStringArrayListExtra("photoList", photoList)
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            it.context.startActivity(intent)
        }

        // Estado online/offline + texto
        UserRepository.stateUser(
            context,
            user.id,
            holder.iconConectado,
            holder.iconDesconectado,
            holder.tvEstado,
            Constants.NODE_CURRENT_CHAT
        )
        userRepository.setUserOnline(context, user.id)

        // Binders auxiliares
        profileUiBinder.setFavorite(user.id, holder.perfilFavoriteOn, holder.perfilFavoriteOff)
        profileUiBinder.getBloqMe(user.id, holder.perfilBloqMe)
        profileUiBinder.getAge(user.id, holder.age)

        profileUiBinder.getDistanceToUser(user.id) { distanceText ->
            holder.distanceUser.text = distanceText
        }

        profileUiBinder.addPhotoReceived(user.id, adapterPhotoReceived, holder.linearPhotos)
        profileUiBinder.setMenuProfile(context, user.id, holder.subMenuChatWithUnknown, holder.subMenuChatWith)
    }
}
