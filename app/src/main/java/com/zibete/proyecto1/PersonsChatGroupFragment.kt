package com.zibete.proyecto1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterGroupUsers
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers

class PersonsChatGroupFragment : Fragment(), SearchView.OnQueryTextListener {
    var rv_group_users: RecyclerView? = null
    var progressbar: ProgressBar? = null
    var img_cancel_dialog: ImageView? = null
    val groupUsersList: ArrayList<UserGroup?> = ArrayList<UserGroup?>()
    val groupOriginalUsersList: ArrayList<UserGroup?> = ArrayList<UserGroup?>()

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

        rv_group_users = view.findViewById(R.id.rv_group_users)

        progressbar = view.findViewById(R.id.progressbar)
        img_cancel_dialog = view.findViewById(R.id.img_cancel_dialog)

        progressbar!!.visibility = View.VISIBLE

        val mLayoutManager = LinearLayoutManager(context)

        mLayoutManager.setReverseLayout(true)
        mLayoutManager.setStackFromEnd(true)

        rv_group_users!!.setLayoutManager(mLayoutManager)


        adapter = AdapterGroupUsers(
            groupUsersList as MutableList<UserGroup>,
            groupOriginalUsersList as MutableList<UserGroup>, requireContext())

        rv_group_users!!.setAdapter(adapter)


        //groupUsersList.clear();

//CREAR LISTA
        refGroupUsers.child(UsuariosFragment.groupName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        groupUsersList.clear()
                        groupOriginalUsersList.clear()
                        progressbar!!.setVisibility(View.GONE)
                        rv_group_users!!.setVisibility(View.VISIBLE)

                        for (snapshot in dataSnapshot.getChildren()) {
                            val groupUser = snapshot.getValue<UserGroup?>(UserGroup::class.java)
                            adapter!!.addUser(groupUser!!)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


        /*
        ref_group_users.child(groupName).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {


                if (dataSnapshot.exists()) {


                    progressbar.setVisibility(View.GONE);
                    rv_group_users.setVisibility(View.VISIBLE);
                    UserGroup groupUser = dataSnapshot.getValue(UserGroup.class);
                    adapter.addUser(groupUser);


                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    UserGroup groupUser = dataSnapshot.getValue(UserGroup.class);
                    adapter.removeUser(groupUser);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

         */
        return view
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

        val searchView = action_search.getActionView() as SearchView?

        searchView!!.setOnQueryTextListener(this)
    }

    companion object {
        var adapter: AdapterGroupUsers? = null
    }
}
