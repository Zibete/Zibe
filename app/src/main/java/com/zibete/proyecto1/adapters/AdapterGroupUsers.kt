package com.zibete.proyecto1.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class AdapterGroupUsers(
    private var groupUsersList: MutableList<UserGroup>,
    private var groupOriginalUsersList: MutableList<UserGroup>,
    private val context: Context,
    // --- NUEVAS DEPENDENCIAS ---
    private var creatorId: String = "", // Se setea desde fuera
    private val onUserSingleTap: (UserGroup) -> Unit,
    private val onUserDoubleTap: (UserGroup) -> Unit
) : RecyclerView.Adapter<AdapterGroupUsers.ViewHolderAdapter>(), Filterable {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // --- Filter ---
    override fun getFilter(): Filter = filterGroups

    private val filterGroups: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.lowercase()?.trim() ?: ""
            val filteredList: MutableList<UserGroup> = if (query.isEmpty()) {
                groupOriginalUsersList.toMutableList()
            } else {
                groupOriginalUsersList.filter { u ->
                    u.userName.lowercase().trim().contains(query)
                }.toMutableList()
            }
            return FilterResults().apply { values = filteredList }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            val resultList = results.values as? List<UserGroup> ?: emptyList()
            groupUsersList.clear()
            groupUsersList.addAll(resultList)
            notifyDataSetChanged()
        }
    }

    // --- ViewHolder ---
    inner class ViewHolderAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageUserGroup: CircleImageView = itemView.findViewById(R.id.image_user_group)
        val nameUserGroup: TextView = itemView.findViewById(R.id.name_user_group)
        val cardviewUserGroup: CardView = itemView.findViewById(R.id.cardviewUserGroup)
        val masked: ImageView = itemView.findViewById(R.id.masked)
        val creator: ImageView = itemView.findViewById(R.id.creator)
        val linearCardPerson: LinearLayout = itemView.findViewById(R.id.linearCardPerson)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAdapter {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_groups_users, parent, false)
        return ViewHolderAdapter(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolderAdapter, position: Int) {
        val groupUser = groupUsersList[position]

        // 1. Color de fondo (UI Logic)
        val colorRes = if (groupUser.userId == currentUserId) R.color.accent_transparent else R.color.zibe_night_start
        holder.cardviewUserGroup.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))

        // 2. Icono Creator (Ahora usamos la variable local, no llamamos a Firebase aquí)
        holder.creator.visibility = if (groupUser.userId == creatorId && creatorId.isNotEmpty()) View.VISIBLE else View.GONE

        // 3. Cargar datos visuales (Firebase Listener para UI updates)
        // NOTA: Idealmente esto debería venir en el objeto UserGroup, pero lo mantenemos para no romper tu flujo actual.
        refCuentas.child(groupUser.userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) return

                val name = dataSnapshot.child("nombre").getValue(String::class.java)
                val foto = dataSnapshot.child("foto").getValue(String::class.java)

                if (groupUser.type == 0) { // Incógnito
                    loadUserImage(context.getString(R.string.URL_PHOTO_DEF), holder.imageUserGroup)
                    holder.nameUserGroup.text = groupUser.userName
                    holder.masked.visibility = View.VISIBLE
                } else { // Normal
                    loadUserImage(foto, holder.imageUserGroup)
                    holder.nameUserGroup.text = name
                    holder.masked.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Gestos (Delegamos la acción)
        val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (groupUser.userId != currentUserId) {
                    onUserDoubleTap(groupUser)
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (groupUser.userId != currentUserId) {
                    onUserSingleTap(groupUser)
                }
                return false
            }
        })

        holder.linearCardPerson.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // IMPORTANTE: Retornar false permite que el scroll del RecyclerView funcione
            false // Antes retornabas false solo al final, aquí nos aseguramos.
        }
    }

    private fun loadUserImage(url: String?, view: ImageView) {
        try {
            Glide.with(context.applicationContext).load(url).into(view)
        } catch (_: Exception) {}
    }

    override fun getItemCount(): Int = groupUsersList.size

    // --- Public API ---

    fun setCreatorId(id: String) {
        this.creatorId = id
        notifyDataSetChanged()
    }

    fun addUser(groupUser: UserGroup) {
        groupUsersList.add(groupUser)
        groupUsersList.sort() // Asegúrate que UserGroup implemente Comparable
        groupOriginalUsersList.add(groupUser)
        notifyDataSetChanged()
    }

    fun removeUser(groupUser: UserGroup) {
        val index = groupUsersList.indexOf(groupUser)
        if (index != -1) {
            groupUsersList.removeAt(index)
            notifyItemRemoved(index)
        }
        groupOriginalUsersList.remove(groupUser)
    }
}