package com.zibete.proyecto1

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterChatGroupsLista
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import kotlin.collections.forEach

class ChatListGroupsFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatGroupsLista: AdapterChatGroupsLista
    private lateinit var layoutManager: LinearLayoutManager

    private val user get() = currentUser!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        if (user == null) {
            setupInitialUi()
            showOnBoarding()
            return binding.root
        }

        setupRecycler()
        setupInitialUi()
        setupGroupChatListListener()
        setupEmptyStateListener()
        setupAdapterObserver()

        return binding.root
    }

    // ---------- UI inicial ----------

    private fun setupInitialUi() = with(binding) {
        // Estado inicial: cargando
        rv.isVisible = true
        linearOnBoardingChatList.isVisible = false
        progressbar2.isVisible = true

        lottieChatLeft.cancelAnimation()
        lottieChatRight.cancelAnimation()
    }

    private fun setupRecycler() {
        layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        // Usamos la lista compartida del companion como fuente inicial
        adapterChatGroupsLista = AdapterChatGroupsLista(chatsGroupArrayList, requireContext())

        binding.rv.apply {
            layoutManager = this@ChatListGroupsFragment.layoutManager
            adapter = adapterChatGroupsLista
        }

        registerForContextMenu(binding.rv)
    }

    // ---------- Listener principal: chatWithUnknown ----------

    private fun setupGroupChatListListener() {

        // Reset lista compartida
        chatsGroupArrayList.clear()

        refDatos.child(user.uid).child(Constants.CHATWITHUNKNOWN)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!snapshot.exists()) return

                    // Solo agregamos si tiene foto (misma lógica original)
                    val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()
                    if (photo.isNotEmpty()) {
                        val chat = snapshot.getValue(ChatWith::class.java) ?: return
                        adapterChatGroupsLista.addChats(chat)
                        updateDatesAndSort(chatsGroupArrayList)
                    }

                    chatsGroupArrayList.sort()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    refDatos.child(user.uid).child(Constants.CHATWITHUNKNOWN)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (!dataSnapshot.exists()) {
                                    binding.progressbar2.isVisible = false
                                    return
                                }

                                val updatedList = ArrayList<ChatWith>()
                                for (child in dataSnapshot.children) {
                                    val chat = child.getValue(ChatWith::class.java)
                                    if (chat != null) updatedList.add(chat)
                                }

                                updateDatesAndSort(updatedList)
                                updatedList.sort()

                                adapterChatGroupsLista.updateData(updatedList)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val chat = snapshot.getValue(ChatWith::class.java) ?: return
                    adapterChatGroupsLista.deleteChat(chat)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDatesAndSort(list: MutableList<ChatWith>) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        list.forEach { chat ->
            try {
                val parsed = format.parse(chat.dateTime)
                chat.date = parsed
            } catch (_: ParseException) {
            }
        }
        list.sort()
    }

    // ---------- Empty state / Onboarding ----------

    private fun setupEmptyStateListener() {

        refDatos.child(user.uid).child(Constants.CHATWITHUNKNOWN)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        var count = 0L
                        for (snapshot in dataSnapshot.children) {
                            val state =
                                snapshot.child("estado").getValue(String::class.java).orEmpty()
                            if (state == Constants.CHATWITHUNKNOWN || state == "silent") {
                                count++
                            }
                        }
                        if (count == 0L) {
                            showOnBoarding()
                        } else {
                            showChatList()
                        }
                    } else {
                        showOnBoarding()
                    }
                    binding.progressbar2.isVisible = false
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showOnBoarding() = with(binding) {
        rv.isVisible = false
        linearOnBoardingChatList.isVisible = true

        lottieChatLeft.playAnimation()
        Handler(Looper.getMainLooper()).postDelayed({
            lottieChatRight.playAnimation()
        }, 100)
    }

    private fun showChatList() = with(binding) {
        rv.isVisible = true
        linearOnBoardingChatList.isVisible = false

        lottieChatLeft.cancelAnimation()
        lottieChatRight.cancelAnimation()
    }

    // ---------- Observer para scroll / progreso ----------

    private fun setupAdapterObserver() {
        adapterChatGroupsLista.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
                binding.progressbar2.isVisible = false
            }
        })
    }

    private fun setScrollbar() {
        if (adapterChatGroupsLista.itemCount > 0) {
            binding.rv.scrollToPosition(adapterChatGroupsLista.itemCount - 1)
        }
    }

    // ---------- SearchView ----------

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unlock)
        val actionFavorites = menu.findItem(R.id.action_favorites)
        val actionExit = menu.findItem(R.id.action_exit)

        actionExit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        actionExit.isVisible = true
        actionSearch.isVisible = true
        actionUnlock.isVisible = true
        actionFavorites.isVisible = true

        val searchView = actionSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapterChatGroupsLista.filter.filter(newText)
        return true
    }

    // ---------- Companion (lista compartida para otros usos, ej. menú) ----------

    companion object {
        // Mantengo esta lista como fuente compartida (se pasa por referencia al adapter)
        val chatsGroupArrayList: ArrayList<ChatWith> = ArrayList()
    }

    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}