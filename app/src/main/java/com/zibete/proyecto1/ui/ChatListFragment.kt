package com.zibete.proyecto1.ui

import android.R
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.Constants
import com.zibete.proyecto1.adapters.AdapterChatLista
import com.zibete.proyecto1.adapters.ChatListGroupsFragment
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections

class ChatListFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatLista: AdapterChatLista
    private lateinit var layoutManager: LinearLayoutManager

    private val chatsArrayList: ArrayList<ChatWith> = ArrayList()

    // Listeners para limpiarlos correctamente
    private var chatListChildListener: ChildEventListener? = null
    private var emptyStateListener: ValueEventListener? = null

    private val currentUser
        get() = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        val u = currentUser
        if (u == null) {
            setupInitialUi()
            showOnBoarding()
            return binding.root
        }

        setupRecycler()
        setupInitialUi()
        setupChatListListener(u.uid)
        setupEmptyStateListener(u.uid)
        setupAdapterObserver()

        return binding.root
    }

    // ---------- UI inicial ----------

    private fun setupInitialUi() = with(binding) {
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

        adapterChatLista = AdapterChatLista(chatsArrayList, requireContext())

        binding.rv.apply {
            layoutManager = this@ChatListFragment.layoutManager
            adapter = adapterChatLista
        }

        registerForContextMenu(binding.rv)
    }

    // ---------- Listener principal de lista de chats ----------

    private fun setupChatListListener(userId: String) {
        val ref = FirebaseRefs.refDatos.child(userId).child(Constants.chatWith)

        val listener = object : ChildEventListener {
            // Nuevo chat
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!snapshot.exists()) return

                val chat = snapshot.getValue(ChatWith::class.java) ?: return
                adapterChatLista.addChats(chat)
                updateDatesAndSort(chatsArrayList)
            }

            // Cambios en chats
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            _binding?.progressbar2?.isVisible = false
                            return
                        }

                        val updatedList = ArrayList<ChatWith>()
                        for (child in dataSnapshot.children) {
                            val chat = child.getValue(ChatWith::class.java)
                            if (chat != null) updatedList.add(chat)
                        }

                        updateDatesAndSort(updatedList)
                        adapterChatLista.updateData(updatedList)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            // Chat eliminado
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val chat = snapshot.getValue(ChatWith::class.java) ?: return
                adapterChatLista.deleteChat(chat)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        chatListChildListener = listener
        ref.addChildEventListener(listener)
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDatesAndSort(list: MutableList<ChatWith>) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        list.forEach { chat ->
            try {
                chat.date = format.parse(chat.dateTime)
            } catch (_: ParseException) {
            }
        }
        Collections.sort(list)
    }

    // ---------- Empty state / onboarding ----------

    private fun setupEmptyStateListener(userId: String) {
        val ref = FirebaseRefs.refDatos.child(userId).child(Constants.chatWith)

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val b = _binding ?: return

                if (dataSnapshot.exists()) {
                    var count = 0L

                    for (snapshot in dataSnapshot.children) {
                        val state = snapshot.child("estado").getValue(String::class.java)
                        val photo = snapshot.child("wUserPhoto").getValue(String::class.java)

                        if (photo != Constants.Empty &&
                            (state == Constants.chatWith || state == "silent")
                        ) {
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

                b.progressbar2.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        emptyStateListener = listener
        ref.addValueEventListener(listener)
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

    // ---------- Observer para scroll y progress ----------

    private fun setupAdapterObserver() {
        adapterChatLista.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
                _binding?.progressbar2?.isVisible = false
            }
        })
    }

    private fun setScrollbar() {
        val b = _binding ?: return
        if (::adapterChatLista.isInitialized && adapterChatLista.itemCount > 0) {
            b.rv.scrollToPosition(adapterChatLista.itemCount - 1)
        }
    }

    // ---------- Context menu acciones ----------

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Chat individual
        if (item.groupId == Constants.FRAGMENT_ID_CHATLIST) {
            val type = Constants.chatWith
            val wChat = chatsArrayList[item.order]
            runItemSelected(item, type, wChat.userId, wChat.userName)
        }

        // Chat unknown / grupos
        if (item.groupId == Constants.FRAGMENT_ID_CHATGROUPLIST) {
            val wChat = ChatListGroupsFragment.chatsGroupArrayList[item.order]
            val type = Constants.chatWithUnknown
            runItemSelected(item, type, wChat.userId, wChat.userName)
        }

        return true
    }

    private fun runItemSelected(
        item: MenuItem,
        type: String,
        idUser: String,
        nameUser: String?
    ) {
        val view = requireActivity().findViewById<View>(R.id.content)

        when (item.itemId) {
            1 -> UserRepository.setNoLeido(idUser, type)
            2 -> UserRepository.Silent(nameUser, idUser, type)
            3 -> UserRepository.setBlockUser(requireContext(), nameUser, idUser, view, type)
            4 -> Constants().UnhiddenChat(requireContext(), idUser, nameUser, view, type)
            5 -> Constants().DeleteChat(requireContext(), idUser, nameUser, view, type)
        }
    }

    // ---------- SearchView ----------

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(com.zibete.proyecto1.R.id.action_search)
        val actionUnlock = menu.findItem(com.zibete.proyecto1.R.id.action_unlock)
        val actionExit = menu.findItem(com.zibete.proyecto1.R.id.action_exit)

        actionExit.isVisible = false
        actionSearch.isVisible = true
        actionUnlock.isVisible = true

        val searchView = actionSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapterChatLista.filter.filter(newText)
        return true
    }

    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        val u = currentUser
        if (u != null) {
            val ref = FirebaseRefs.refDatos.child(u.uid).child(Constants.chatWith)
            chatListChildListener?.let { ref.removeEventListener(it) }
            emptyStateListener?.let { ref.removeEventListener(it) }
        }

        chatListChildListener = null
        emptyStateListener = null
        _binding = null
        super.onDestroyView()
    }
}
