package com.zibete.proyecto1.adapters

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.Constants
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections

class GroupUsersDiffCallback(
    private val newList: ArrayList<UserGroup>,
    private val oldlist: ArrayList<UserGroup>
    ) :    DiffUtil.Callback() {

        override fun getOldListSize() = oldlist.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldlist[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldlist[oldItemPosition] == newList[newItemPosition]

        override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
            val o = oldlist[oldPos]
            val n = newList[newPos]

            return Bundle().apply {
                if (o.userName != n.userName) putString("userName", n.userName)
            }.takeIf { !it.isEmpty }
    }

}


class ChatListFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatLista: AdapterChatLista
    private lateinit var layoutManager: LinearLayoutManager

    private val chatsArrayList: java.util.ArrayList<ChatWith> = java.util.ArrayList()
    private val user: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        if (user == null) {
            // Si no hay usuario logueado, mostramos onboarding y salimos.
            setupInitialUi()
            showOnBoarding()
            return binding.root
        }

        setupRecycler()
        setupInitialUi()
        setupChatListListener()
        setupEmptyStateListener()
        setupAdapterObserver()

        return binding.root
    }

    // ---------- UI inicial ----------

    private fun setupInitialUi() = with(binding) {
        // Estado inicial: cargando lista
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

    private fun setupChatListListener() {
        val u = user ?: return

        FirebaseRefs.refDatos.child(u.uid).child(Constants.chatWith)
            .addChildEventListener(object : ChildEventListener {
                // Nuevo chat
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!snapshot.exists()) return

                    val chat = snapshot.getValue(ChatWith::class.java) ?: return
                    adapterChatLista.addChats(chat)
                    updateDatesAndSort(chatsArrayList)
                }

                // Cambios en chats
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val listener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (!dataSnapshot.exists()) {
                                binding.progressbar2.isVisible = false
                                return
                            }

                            val updatedList = java.util.ArrayList<ChatWith>()
                            for (child in dataSnapshot.children) {
                                val chat = child.getValue(ChatWith::class.java)
                                if (chat != null) updatedList.add(chat)
                            }

                            updateDatesAndSort(updatedList)
                            adapterChatLista.updateData(updatedList)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }

                    FirebaseRefs.refDatos.child(u.uid).child(Constants.chatWith)
                        .addListenerForSingleValueEvent(listener)
                }

                // Chat eliminado
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val chat = snapshot.getValue(ChatWith::class.java) ?: return
                    adapterChatLista.deleteChat(chat)
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
        Collections.sort(list)
    }

    // ---------- Empty state / onboarding ----------

    private fun setupEmptyStateListener() {
        val u = user ?: return

        FirebaseRefs.refDatos.child(u.uid).child(Constants.chatWith)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        var count = 0L

                        for (snapshot in dataSnapshot.children) {
                            val state = snapshot.child("estado").getValue(String::class.java)
                            val photo =
                                snapshot.child("wUserPhoto").getValue(String::class.java)

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

    // ---------- Observer para scroll y progress ----------

    private fun setupAdapterObserver() {
        adapterChatLista.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
                binding.progressbar2.isVisible = false
            }
        })
    }

    private fun setScrollbar() {
        if (adapterChatLista.itemCount > 0) {
            binding.rv.scrollToPosition(adapterChatLista.itemCount - 1)
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
            val wChat = ChatListGroupsFragment.Companion.chatsGroupArrayList[item.order]
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
        super.onDestroyView()
        _binding = null
    }
}