package com.zibete.proyecto1.ui.chatlist

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.FacebookSdk.getApplicationContext
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.ChatListGroupsFragment
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterChatLista
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.UserMessageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : Fragment(), SearchView.OnQueryTextListener {


    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer
    @Inject lateinit var userSessionManager: UserSessionManager
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var userRepository: UserRepository

    private val myUid = userSessionManager.user.uid
    private val chatListViewModel: ChatListViewModel by viewModels()


    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapterChatLista: AdapterChatLista
    private lateinit var layoutManager: LinearLayoutManager
    private val chatsArrayList: ArrayList<ChatWith> = ArrayList()
    private var chatListChildListener: ChildEventListener? = null
    private var emptyStateListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatListViewModel.events.collect { event ->
                    when (event) {
                        is ChatListUiEvent.ConfirmHideChat -> {
                            UserMessageUtils.confirm(
                                context = getApplicationContext(),
                                title = "Ocultar chat",
                                message = "¿Ocultar chat con ${event.name}?",
                                onConfirm = event.onConfirm
                            )
                        }
                        is ChatListUiEvent.ShowChatHidden -> {
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = "Se ha ocultado el chat",
                                duration = Snackbar.LENGTH_SHORT,
                                iconRes = R.drawable.ic_info_24
                            )
                        }
                        else -> {}
                    }
                }
            }
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
        val ref = FirebaseRefs.refDatos.child(myUid).child(Constants.CHATWITH)

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
        list.sort()
    }

    // ---------- Empty state / onboarding ----------

    private fun setupEmptyStateListener() {
        val ref = FirebaseRefs.refDatos.child(myUid).child(Constants.CHATWITH)

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val b = _binding ?: return

                if (dataSnapshot.exists()) {
                    var count = 0L

                    for (snapshot in dataSnapshot.children) {
                        val state = snapshot.child("estado").getValue(String::class.java)
                        val photo = snapshot.child("wUserPhoto").getValue(String::class.java)

                        if (photo != Constants.EMPTY &&
                            (state == Constants.CHATWITH || state == "silent")
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
            val type = Constants.CHATWITH
            val wChat = chatsArrayList[item.order]
            runItemSelected(item, type, wChat.userId, wChat.userName)
        }

        // Chat unknown / grupos
        if (item.groupId == Constants.FRAGMENT_ID_CHATGROUPLIST) {
            val wChat = ChatListGroupsFragment.Companion.chatsGroupArrayList[item.order]
            val type = Constants.CHATWITHUNKNOWN
            runItemSelected(item, type, wChat.userId, wChat.userName)
        }

        return true
    }

    private fun runItemSelected(
        item: MenuItem,
        type: String,
        idUser: String,
        nameUser: String
    ) {
        val action = when (item.itemId) {
            1 -> ChatMenuAction.MarkAsReadChat
            2 -> ChatMenuAction.SilentUser
            3 -> ChatMenuAction.BlockUser
            4 -> ChatMenuAction.HideChat
            5 -> ChatMenuAction.DeleteChat
            else -> return
        }

        chatListViewModel.onChatItemMenuAction(action, idUser, type, nameUser)
    }

    // ---------- SearchView ----------

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unlock)
        val actionExit = menu.findItem(R.id.action_exit)

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

        val ref = FirebaseRefs.refDatos.child(myUid).child(Constants.CHATWITH)
        chatListChildListener?.let { ref.removeEventListener(it) }
        emptyStateListener?.let { ref.removeEventListener(it) }

        chatListChildListener = null
        emptyStateListener = null
        _binding = null
        super.onDestroyView()
    }
}