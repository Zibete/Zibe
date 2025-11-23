package com.zibete.proyecto1.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.PerfilActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupData
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Collections

class AdapterGroupUsers(
    groupUsersList: MutableList<UserGroup>,
    groupOriginalUsersList: MutableList<UserGroup>,
    private val context: Context
) : RecyclerView.Adapter<AdapterGroupUsers.viewHolderAdapter>(), Filterable {

    var groupUsersList: MutableList<UserGroup> = groupUsersList
        private set

    var groupOriginalUsersList: MutableList<UserGroup> = groupOriginalUsersList
        private set

    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private var search: String? = null

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

            return FilterResults().apply {
                values = filteredList
            }
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
    inner class viewHolderAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageUserGroup: CircleImageView =
            itemView.findViewById(R.id.image_user_group)
        val nameUserGroup: TextView =
            itemView.findViewById(R.id.name_user_group)
        val cardviewUserGroup: CardView =
            itemView.findViewById(R.id.cardviewUserGroup)
        val masked: ImageView =
            itemView.findViewById(R.id.masked)
        val creator: ImageView =
            itemView.findViewById(R.id.creator)
        val linearCardPerson: LinearLayout =
            itemView.findViewById(R.id.linearCardPerson)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolderAdapter {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_groups_users, parent, false)
        return viewHolderAdapter(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: viewHolderAdapter, position: Int) {
        val groupUser = groupUsersList[position]
        val currentUserId = user?.uid

        // Fondo distinto si es el usuario actual
        val colorRes = if (groupUser.userId == currentUserId) {
            R.color.accent_transparent
        } else {
            R.color.zibe_night_start
        }
        holder.cardviewUserGroup.setCardBackgroundColor(
            ContextCompat.getColor(context, colorRes)
        )

        // Mostrar icono de "creator" si corresponde
        refGroupData.child(UsuariosFragment.groupName)
            .child("id_creator")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val creatorId = dataSnapshot.getValue(String::class.java)
                    holder.creator.visibility =
                        if (groupUser.userId == creatorId) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // No-op
                }
            })

        // Cargar nombre y foto desde la cuenta
        refCuentas.child(groupUser.userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) return

                    val name = dataSnapshot.child("nombre").getValue(String::class.java)
                    val foto = dataSnapshot.child("foto").getValue(String::class.java)

                    if (groupUser.type == 0) {
                        // Usuario incógnito
                        Glide.with(context.applicationContext)
                            .load(context.getString(R.string.URL_PHOTO_DEF))
                            .into(holder.imageUserGroup)

                        holder.nameUserGroup.text = groupUser.userName
                        holder.masked.visibility = View.VISIBLE
                    } else {
                        // Usuario normal
                        Glide.with(context.applicationContext)
                            .load(foto)
                            .into(holder.imageUserGroup)

                        holder.nameUserGroup.text = name
                        holder.masked.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // No-op
                }
            })

        // Gestos: doble tap → Chat, single tap → Perfil o Toast
        val gestureDetector = GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (groupUser.userId != currentUserId) {
                        val intent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("unknownName", groupUser.userName)
                            putExtra("idUserUnknown", groupUser.userId)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        context.startActivity(intent)
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (groupUser.userId != currentUserId) {
                        if (groupUser.type == 1) {
                            val intent = Intent(context, PerfilActivity::class.java).apply {
                                putExtra("id_user", groupUser.userId)
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Usuario incógnito", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return false
                }
            }
        )

        holder.linearCardPerson.setOnLongClickListener {
            // TODO: comportamiento en long click si lo necesitás
            false
        }

        holder.linearCardPerson.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    override fun getItemCount(): Int = groupUsersList.size

    // --- Helpers para modificar lista ---

    fun addUser(groupUser: UserGroup) {
        groupUsersList.add(groupUser)
        groupUsersList.sort()
        groupOriginalUsersList.add(groupUser)
        notifyDataSetChanged()
    }

    fun removeUser(groupUser: UserGroup) {
        val index = groupUsersList.indexOf(groupUser)
        if (index != -1) {
            groupUsersList.removeAt(index)
            notifyItemRemoved(index)
        }

        val originalIndex = groupOriginalUsersList.indexOf(groupUser)
        if (originalIndex != -1) {
            groupOriginalUsersList.removeAt(originalIndex)
        }
    }
}
