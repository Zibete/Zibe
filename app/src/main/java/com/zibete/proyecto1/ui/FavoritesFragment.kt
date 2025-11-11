package com.zibete.proyecto1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.FixedSwipeRefreshLayout
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterFavoriteUsers
import com.zibete.proyecto1.utils.FirebaseRefs
import java.util.Collections

class FavoritesFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var favoritesSwipeRefresh: FixedSwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterFavoriteUsers
    private lateinit var layoutManager: GridLayoutManager

    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val favoritesList = mutableListOf<String>()
    private val favoritesTemp = mutableListOf<String>() // para refresh

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        setHasOptionsMenu(true)

        if (user == null) return view

        progressBar = view.findViewById(R.id.progressbar)
        recyclerView = view.findViewById(R.id.rv_favorites)
        favoritesSwipeRefresh = view.findViewById(R.id.favorite_swipe_refresh)

        layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = layoutManager

        adapter = AdapterFavoriteUsers(favoritesList, requireContext())
        recyclerView.adapter = adapter

        favoritesSwipeRefresh.setRecyclerView(recyclerView)
        favoritesSwipeRefresh.setOnRefreshListener {
            loadFavorites("refresh")
        }

        loadFavorites("load")

        return view
    }

    private fun loadFavorites(flag: String) {
        progressBar.visibility = View.VISIBLE

        if (flag == "load") {
            favoritesList.clear()
        } else {
            favoritesTemp.clear()
        }

        FirebaseRefs.refDatos.child(user!!.uid).child("FavoriteList")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        progressBar.visibility = View.GONE
                        adapter.updateDataUsers(emptyList())
                        Toast.makeText(context, "No hay favoritos", Toast.LENGTH_SHORT).show()
                        favoritesSwipeRefresh.isRefreshing = false
                        return
                    }

                    val pending = snapshot.childrenCount
                    if (pending == 0L) {
                        progressBar.visibility = View.GONE
                        favoritesSwipeRefresh.isRefreshing = false
                        return
                    }

                    var processed = 0L

                    for (child in snapshot.children) {
                        val favUserId = child.getValue(String::class.java)

                        if (favUserId.isNullOrEmpty()) {
                            checkComplete(++processed, pending, flag)
                            continue
                        }

                        // Verificamos que el usuario siga existiendo
                        FirebaseRefs.refCuentas.child(favUserId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    if (userSnap.exists()) {
                                        if (flag == "load") {
                                            favoritesList.add(favUserId)
                                        } else {
                                            favoritesTemp.add(favUserId)
                                        }
                                    } else {
                                        // Limpieza: si ya no existe el user, borramos de favoritos
                                        child.ref.removeValue()
                                    }

                                    checkComplete(++processed, pending, flag)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    checkComplete(++processed, pending, flag)
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    favoritesSwipeRefresh.isRefreshing = false
                }
            })
    }

    private fun checkComplete(
        processed: Long,
        total: Long,
        flag: String
    ) {
        if (processed < total) return

        // Orden alfabético por id (como ya tenías)
        if (flag == "load") {
            favoritesList.sort()
            adapter.updateDataUsers(favoritesList)
        } else {
            favoritesTemp.sort()
            adapter.updateDataUsers(favoritesTemp)
        }

        progressBar.visibility = View.GONE
        favoritesSwipeRefresh.isRefreshing = false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.action_search)?.isVisible = false
        menu.findItem(R.id.action_unlock)?.isVisible = false
        menu.findItem(R.id.action_favorites)?.isVisible = false
        menu.findItem(R.id.action_exit)?.isVisible = false
    }
}
