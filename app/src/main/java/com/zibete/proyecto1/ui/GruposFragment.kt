package com.zibete.proyecto1.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterGroups
import com.zibete.proyecto1.databinding.FragmentGruposBinding
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupData
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import java.util.*

class GruposFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentGruposBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterGroups
    private lateinit var layoutManager: GridLayoutManager
    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val groupsArrayList = ArrayList<Groups>()
    private val originalGroupsArrayList = ArrayList<Groups>()
    private val groupsArrayList2 = ArrayList<Groups>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGruposBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        setupRecycler()
        setupSwipeRefresh()
        setupFab()
        loadGroups("load")

        return binding.root
    }

    // --- Inicialización UI ---

    private fun setupRecycler() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 2).apply {
            reverseLayout = false
        }

        adapter = AdapterGroups(groupsArrayList, originalGroupsArrayList, requireContext())
        rvGroups.layoutManager = layoutManager
        rvGroups.adapter = adapter
    }

    private fun setupSwipeRefresh() = with(binding) {
        groupSwipeRefresh.setRecyclerView(rvGroups)
        groupSwipeRefresh.setOnRefreshListener {
            loadGroups("refresh")
            groupSwipeRefresh.isRefreshing = false
        }
    }

    private fun setupFab() = with(binding) {
        fabNewGroup.setOnClickListener {
            adapter.goNewGroup()
        }
    }

    // --- Lógica principal de carga de grupos ---

    fun loadGroups(flag: String) {
        binding.progressbar.isVisible = true

        refGroupData.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    binding.progressbar.isVisible = false
                    Toast.makeText(requireContext(), "No hay Grupos", Toast.LENGTH_SHORT).show()
                    return
                }

                when (flag) {
                    "load" -> groupsArrayList.clear()
                    "refresh" -> groupsArrayList2.clear()
                }
                originalGroupsArrayList.clear()

                for (snapshot in dataSnapshot.children) {
                    val group = snapshot.getValue(Groups::class.java) ?: continue
                    val nombre = snapshot.child("name").getValue(String::class.java) ?: continue

                    refGroupUsers.child(nombre)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            @SuppressLint("SetTextI18n")
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val counter = dataSnapshot.childrenCount.toInt()
                                group.users = counter

                                if (flag == "load") {
                                    adapter.addGroup(group)
                                } else {
                                    groupsArrayList2.add(group)
                                }

                                sortGroups()

                                if (flag == "load") {
                                    adapter.notifyDataSetChanged()
                                } else {
                                    adapter.updateDataGroups(groupsArrayList2)
                                }

                                setScrollbar()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                binding.progressbar.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sortGroups() {
        Collections.sort(groupsArrayList)
        Collections.sort(groupsArrayList2)
        Collections.sort(originalGroupsArrayList)
    }

    private fun setScrollbar() {
        if (adapter.itemCount > 0) {
            binding.rvGroups.scrollToPosition(0)
        }
    }

    // --- Filtro de búsqueda ---

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.filter.filter(newText)
        return true
    }

    // --- Menú superior ---

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionSettings = menu.findItem(R.id.action_settings)
        val actionUnlock = menu.findItem(R.id.action_unlock)
        val actionFavorites = menu.findItem(R.id.action_favorites)
        val actionExit = menu.findItem(R.id.action_exit)

        actionExit.isVisible = false
        actionSearch.isVisible = true
        actionSettings.isVisible = true
        actionUnlock.isVisible = true
        actionFavorites.isVisible = true

        val searchView = actionSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
