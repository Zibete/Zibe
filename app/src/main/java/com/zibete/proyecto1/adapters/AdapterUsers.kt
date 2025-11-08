package com.zibete.proyecto1.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterUsers.ViewHolderAdapter
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.Constants
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlideProfileActivity
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.EditProfileFragment.UsuariosFragment
import com.zibete.proyecto1.utils.DateUtils
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.GlassEffect
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.utils.UserRepository
import eightbitlab.com.blurview.BlurView
import java.math.BigDecimal
import java.math.RoundingMode

class AdapterUsers(
    private val usersList: MutableList<Users>,
    private val usersListAll: MutableList<Users>,
    private val context: Context
) : RecyclerView.Adapter<ViewHolderAdapter?>(), Filterable {
    private val user = FirebaseAuth.getInstance().getCurrentUser()

    // --------------------- Filtro --------------------- //
    override fun getFilter(): Filter {
        return filterChats
    }

    private val filterChats = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val search = constraint?.toString()?.lowercase()?.trim().orEmpty()

            val filtered = if (search.isEmpty()) {
                usersListAll.toList()
            } else {
                usersListAll
                    .filter { it.name?.lowercase()?.contains(search) == true }
            }

            return FilterResults().apply { values = filtered }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            usersList.clear()
            usersList.addAll(results.values as List<Users>)
            notifyDataSetChanged()
            UsuariosFragment.setScrollbar()
        }
    }

    // --------------------- ViewHolder --------------------- //
    class ViewHolderAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_usuario1: TextView
        var tv_edad: TextView
        var distance: TextView
        var tv_desc: TextView
        var tv_estado: TextView
        var img_user: ImageView
        var icon_conectado: ImageView
        var icon_desconectado: ImageView
        var goChat: ImageView
        var favorite_on: ImageView
        var bloq_me: ImageView
        var bloq: ImageView
        var linear_desc: LinearLayout
        var cardView: CardView
        var blurView: BlurView?
        var glowBorder: View?

        init {
            tv_usuario1 = itemView.findViewById<TextView>(R.id.tv_usuario1)
            tv_edad = itemView.findViewById<TextView>(R.id.tv_edad)
            distance = itemView.findViewById<TextView>(R.id.distance)
            tv_desc = itemView.findViewById<TextView>(R.id.tv_desc)
            tv_estado = itemView.findViewById<TextView>(R.id.tv_estado)
            img_user = itemView.findViewById<ImageView>(R.id.image_user1)
            icon_conectado = itemView.findViewById<ImageView>(R.id.icon_conectado)
            icon_desconectado = itemView.findViewById<ImageView>(R.id.icon_desconectado)
            goChat = itemView.findViewById<ImageView>(R.id.goChat)
            favorite_on = itemView.findViewById<ImageView>(R.id.favorite_on)
            bloq_me = itemView.findViewById<ImageView>(R.id.bloq_me)
            bloq = itemView.findViewById<ImageView>(R.id.bloq)
            linear_desc = itemView.findViewById<LinearLayout>(R.id.linear_desc)
            cardView = itemView.findViewById<CardView>(R.id.cardviewUsers)
            blurView = itemView.findViewById<BlurView?>(R.id.blur_view)
            glowBorder = itemView.findViewById<View?>(R.id.glow_border)

            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }
    }

    // --------------------- Create --------------------- //
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAdapter {
        val v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.row_usuarios, parent, false)
        return ViewHolderAdapter(v)
    }

    // --------------------- Bind con payloads --------------------- //
    override fun onBindViewHolder(
        holder: ViewHolderAdapter,
        position: Int,
        payloads: MutableList<Any?>
    ) {
        val u = usersList[position]

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            loadUserCard(holder, u)
        } else {
            val o = payloads[0] as Bundle
            if (o.containsKey("distance")) loadUserCard(holder, u)
        }

        // Acciones
        holder.goChat.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(v!!.context, ChatActivity::class.java)
            intent.putExtra("id_user", u.id)
            v.context.startActivity(intent)
        })

        holder.cardView.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(context, SlideProfileActivity::class.java)
            val extra = ArrayList<Users?>(usersList)
            extra.reverse()
            intent.putExtra("userList", extra)
            intent.putExtra("position", extra.indexOf(u))
            intent.putExtra("rotation", 0)
            v?.context?.startActivity(intent)
        })
    }

    override fun onBindViewHolder(holder: ViewHolderAdapter, position: Int) { /* manejado arriba */

    }

    // --------------------- Datos por card --------------------- //
    @SuppressLint("SetTextI18n")
    fun loadUserCard(h: ViewHolderAdapter, users: Users) {
        Glide.with(context).load(users.profilePhoto).into(h.img_user)
        h.tv_usuario1.text = users.name

        // Distancia
        val dist = ProfileUiBinder.getDistanceMeters(
            UserRepository.latitude,
            UserRepository.longitude,
            users.latitude,
            users.longitude
        )
        if (dist > 10000) {
            h.distance.text = "A " + BigDecimal(dist / 1000).setScale(
                0,
                RoundingMode.HALF_UP
            ) + " km"
        } else if (dist > 1000) {
            h.distance.text = "A " + BigDecimal(dist / 1000).setScale(
                1,
                RoundingMode.HALF_UP
            ) + " km"
        } else {
            h.distance.text = "A " + BigDecimal(dist).setScale(0, RoundingMode.HALF_UP) + " m"
        }

        // Edad
        val edad = DateUtils.calcularEdad(users.birthDay)
        h.tv_edad.text = edad.toString()


        // Descripción
        if (users.description != null && !users.description!!.isEmpty()) {
            h.tv_desc.setText(users.description)
            h.linear_desc.visibility = View.VISIBLE
        } else {
            h.linear_desc.visibility = View.GONE
        }

        // Estado on/off
        UserRepository.stateUser(
            context,
            users.id,
            h.icon_conectado,
            h.icon_desconectado,
            h.tv_estado,
            Constants.chatWith
        )

        // Favoritos
        FirebaseRefs.refDatos.child(user!!.uid).child("FavoriteList").child(users.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    h.favorite_on.isVisible = snap.exists()
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Bloqueado
        FirebaseRefs.refDatos.child(user.uid).child(Constants.chatWith).child(users.id)
            .child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    h.bloq.isVisible = (snap.getValue(String::class.java) == "bloq")
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Me bloqueó
        FirebaseRefs.refDatos.child(users.id).child(Constants.chatWith).child(user.uid)
            .child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val blocked = snap.getValue(String::class.java) == "bloq"
                    h.bloq_me.isVisible = blocked
                    h.goChat.isVisible = !blocked
                }
                override fun onCancelled(error: DatabaseError) {}
            })

    }

    // --------------------- Utilidades de lista --------------------- //
    fun addUser(u: Users) {
        usersList.add(u)
        usersListAll.add(u)
        notifyItemInserted(usersList.size) //-1? --> ListAdapter en el futuro
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
