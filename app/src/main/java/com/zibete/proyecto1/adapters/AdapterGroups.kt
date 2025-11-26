package com.zibete.proyecto1.adapters
import com.zibete.proyecto1.R


import android.animation.ValueAnimator
import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.zibete.proyecto1.*
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.databinding.DialogGoGroupBinding
import com.zibete.proyecto1.databinding.DialogGoNewGroupBinding
import com.zibete.proyecto1.databinding.RowGroupBinding
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.GlassEffect
import eightbitlab.com.blurview.BlurView
import java.text.SimpleDateFormat
import java.util.*

class AdapterGroups(
    private val groupsList: MutableList<Groups>,
    private val originalGroupsArrayList: MutableList<Groups>,
    private val context: Context
) : RecyclerView.Adapter<AdapterGroups.ViewHolder>(), Filterable {

    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // ---------- FILTRO ----------
    override fun getFilter(): Filter = filterGroups

    private val filterGroups = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filtered = ArrayList<Groups>()
            val search = constraint.toString().lowercase(Locale.getDefault()).trim()

            if (search.isEmpty()) {
                filtered.addAll(originalGroupsArrayList)
            } else {
                for (group in originalGroupsArrayList) {
                    if (group.name.lowercase(Locale.getDefault()).contains(search)) {
                        filtered.add(group)
                    }
                }
            }
            return FilterResults().apply { values = filtered }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            groupsList.clear()
            groupsList.addAll(results.values as List<Groups>)
            notifyDataSetChanged()
        }
    }

    // ---------- VIEW HOLDER ----------
    inner class ViewHolder(val binding: RowGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        val blurView: BlurView = binding.blurView
        val glowBorder: View = binding.glowBorder
        val card = binding.cardviewGroups

        init {
            GlassEffect.applyGlassEffect(blurView, itemView)
            GlassEffect.startGlowIfAny(glowBorder)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowGroupBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = groupsList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any?>) {
        val group = groupsList[position]
        if (payloads.isEmpty()) bindGroup(holder, group)
        else (payloads[0] as? Bundle)?.takeIf { it.containsKey("users") }?.let { bindGroup(holder, group) }

        holder.card.setOnClickListener {
            if (group.category == Constants.PUBLIC_GROUP) goPublicGroup(group)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    // ---------- BIND ----------
    private fun bindGroup(holder: ViewHolder, group: Groups) = with(holder.binding) {
        tvTitle.text = group.name
        tvDataGroup.text = group.data
        tvDataGroup.isSelected = true

        FirebaseRefs.refGroupUsers.child(group.name).addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                tvNumberPersons.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------- CREAR NUEVO GRUPO ----------
    @SuppressLint("InflateParams")
    fun goNewGroup() {
        val binding = DialogGoNewGroupBinding.inflate(LayoutInflater.from(context))

        binding.nameUser.text = user?.displayName
        Glide.with(context).load(user?.photoUrl).into(binding.imageUser)
        binding.btnCreateNewChat.isEnabled = false

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnCreateNewChat.isEnabled =
                    binding.edtNameNewGroup.text?.isNotEmpty() == true &&
                            binding.edtDataNewGroup.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.edtNameNewGroup.addTextChangedListener(watcher)
        binding.edtDataNewGroup.addTextChangedListener(watcher)

        // 🔹 Se crea el diálogo con el tema correcto y se asigna la vista root del binding
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogApp))
        builder.setView(binding.root)
        val dialog = builder.create()
        dialog.setCancelable(true)
        dialog.show()

        // 🔹 Acción del botón Crear
        binding.btnCreateNewChat.setOnClickListener {
            val name = binding.edtNameNewGroup.text.toString()
            val data = binding.edtDataNewGroup.text.toString()

            FirebaseRefs.refGroupData.child(name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(context, "El nombre ya está en uso", Toast.LENGTH_SHORT).show()
                            return
                        }

                        @SuppressLint("SimpleDateFormat")
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())
                        val group = Groups(name, data, user!!.uid, Constants.PUBLIC_GROUP, 0, date)

                        FirebaseRefs.refGroupData.child(name).setValue(group)
                        goGroup(it, dialog, name, user.displayName ?: "", Constants.PUBLIC_GROUP)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // 🔹 Acción de cerrar
        binding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
    }




    // ---------- ENTRAR A GRUPO EXISTENTE ----------
    @SuppressLint("InflateParams")
    fun goPublicGroup(group: Groups) {
        val binding = DialogGoGroupBinding.inflate(inflater)

        binding.nameUser.text = user?.displayName
        binding.tvChat.text = group.name
        Glide.with(context).load(user?.photoUrl).into(binding.imageUser)
        binding.btnStartChat2.isEnabled = false

        binding.edtNick.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnStartChat2.isEnabled = binding.edtNick.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogApp))
            .setView(binding.root)
            .setCancelable(true)
            .show()

        binding.btnStartChat1.setOnClickListener {
            goGroup(it, dialog, group.name, user?.displayName ?: "", 1)
        }

        binding.btnStartChat2.setOnClickListener { v ->
            FirebaseRefs.refGroupUsers.child(group.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val name = child.child("user_name").getValue(String::class.java)
                            if (name == binding.edtNick.text.toString()) {
                                Toast.makeText(context, "${binding.edtNick.text} está en uso", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        goGroup(v, dialog, group.name, binding.edtNick.text.toString(), 0)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
    }


    // ---------- ENTRAR AL CHAT ----------
    fun goGroup(v: View, dialog: AlertDialog, groupName: String, userName: String, type: Int) {
        @SuppressLint("SimpleDateFormat")
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS").format(Date())

        UsuariosFragment.userName = userName
        UsuariosFragment.groupName = groupName
        UsuariosFragment.inGroup = true
        UsuariosFragment.userType = type
        UsuariosFragment.userDate = date

        UsuariosFragment.editor.putBoolean("inGroup", true)
        UsuariosFragment.editor.putString("groupName", groupName)
        UsuariosFragment.editor.putString("userName", userName)
        UsuariosFragment.editor.putInt("userType", type)
        UsuariosFragment.editor.putString("userDate", date)
        UsuariosFragment.editor.apply()

        val chatMsg = ChatsGroup("se unió a la sala", date, userName, user!!.uid, 0, type)
        FirebaseRefs.refGroupChat.child(groupName).push().setValue(chatMsg)

        MainActivity.listenerGroupBadge?.let { listener ->
            FirebaseRefs.refGroupChat.child(groupName).addValueEventListener(listener)
        }


        val query: Query = FirebaseRefs.refDatos.child(user.uid)
            .child(Constants.CHATWITHUNKNOWN)
            .orderByChild("noVisto").startAt(1.0)

        MainActivity.listenerMsgUnreadBadge?.let { listener ->
            query.addValueEventListener(listener)
        }

//        MainActivity.toolbar?.isVisible = true
//        MainActivity.layoutSettings?.isVisible = false
        (context as MainActivity).invalidateOptionsMenu()

        val newFragment = PageAdapterGroup()
        val activity = v.context as AppCompatActivity
        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, newFragment)
//        MainActivity.toolbar?.title = groupName
        context.invalidateOptionsMenu()
        transaction.commit()

        val userGroup = UserGroup(user.uid, userName, type)
        FirebaseRefs.refGroupUsers.child(groupName).child(user.uid).setValue(userGroup)

        MainActivity.listenerGroupBadge?.let { listener ->
            FirebaseRefs.refGroupChat.child(groupName).addValueEventListener(listener)
        }

        dialog.dismiss()
    }

    // ---------- UTILIDADES ----------
    fun addGroup(group: Groups) {
        groupsList.add(group)
        originalGroupsArrayList.add(group)
        notifyItemInserted(groupsList.size - 1)
    }

    fun updateDataGroups(newList: ArrayList<Groups>) {
        val diff = GroupsDiffCallback(newList, groupsList)
        val result = DiffUtil.calculateDiff(diff)
        result.dispatchUpdatesTo(this)
        groupsList.clear()
        groupsList.addAll(newList)
    }

    override fun getItemViewType(position: Int): Int = Constants.PUBLIC_GROUP
}
