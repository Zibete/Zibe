package com.zibete.proyecto1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterGroupUsers
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupData
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import com.zibete.proyecto1.utils.Utils.repo

class GroupUsersFragment : Fragment(), SearchView.OnQueryTextListener {
    var rvGroupUsers: RecyclerView? = null
    var progressbar: ProgressBar? = null
    var imgCancelDialog: ImageView? = null
    val groupUsersList: ArrayList<UserGroup?> = ArrayList()
    val groupOriginalUsersList: ArrayList<UserGroup?> = ArrayList()

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter!!.getFilter().filter(newText)
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_list_users_group, container, false)

        setHasOptionsMenu(true)

        rvGroupUsers = view.findViewById(R.id.rv_group_users)

        progressbar = view.findViewById(R.id.progressbar)
        imgCancelDialog = view.findViewById(R.id.img_cancel_dialog)

        progressbar!!.visibility = View.VISIBLE

        val mLayoutManager = LinearLayoutManager(context)

        mLayoutManager.setReverseLayout(true)
        mLayoutManager.setStackFromEnd(true)

        rvGroupUsers!!.setLayoutManager(mLayoutManager)

        adapter = AdapterGroupUsers(
            groupUsersList = groupUsersList as MutableList<UserGroup>,
            groupOriginalUsersList = groupOriginalUsersList as MutableList<UserGroup>,
            context = requireContext(),
            onUserSingleTap = { userGroup -> handleUserSingleTap(userGroup) },
            onUserDoubleTap = { userGroup -> handleUserDoubleTap(userGroup) }
        )
        rvGroupUsers!!.adapter = adapter

        fetchGroupCreatorId()

        //groupUsersList.clear();

//CREAR LISTA
        refGroupUsers.child(repo.groupName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        groupUsersList.clear()
                        groupOriginalUsersList.clear()
                        progressbar!!.visibility = View.GONE
                        rvGroupUsers!!.visibility = View.VISIBLE

                        for (snapshot in dataSnapshot.getChildren()) {
                            val groupUser = snapshot.getValue<UserGroup?>(UserGroup::class.java)
                            adapter!!.addUser(groupUser!!)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        return view
    }

    // 1. Obtener ID del creador (Reemplaza la llamada repetitiva en onBind)
    private fun fetchGroupCreatorId() {
        // Usamos repo.groupName (tu nueva arquitectura)
        val groupName = repo.groupName

        refGroupData.child(groupName).child("id_creator")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val creatorId = snapshot.getValue(String::class.java) ?: ""
                    // Actualizamos el adapter
                    adapter?.setCreatorId(creatorId)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 2. Single Tap Logic (Perfil o Toast)
    private fun handleUserSingleTap(groupUser: UserGroup) {
        if (groupUser.type == 1) {
            val intent = Intent(requireContext(), PerfilActivity::class.java).apply {
                putExtra("id_user", groupUser.userId)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Usuario incógnito", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Double Tap Logic (Ir al Chat Privado)
    private fun handleUserDoubleTap(groupUser: UserGroup) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("unknownName", groupUser.userName)
            putExtra("idUserUnknown", groupUser.userId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val action_search = menu.findItem(R.id.action_search)
        val action_desbloqUsers = menu.findItem(R.id.action_unlock)
        val action_favoritos = menu.findItem(R.id.action_favorites)
        val action_exit = menu.findItem(R.id.action_exit)

        action_exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        action_exit.isVisible = true
        action_search.isVisible = true
        action_desbloqUsers.isVisible = true
        action_favoritos.isVisible = true

        val searchView = action_search.actionView as SearchView?

        searchView!!.setOnQueryTextListener(this)
    }

    companion object {
        var adapter: AdapterGroupUsers? = null
    }
}
