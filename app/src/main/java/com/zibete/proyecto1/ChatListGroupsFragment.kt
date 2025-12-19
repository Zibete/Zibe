package com.zibete.proyecto1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.AdapterChatGroupsList
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs.refChatUnknown
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import dagger.hilt.android.AndroidEntryPoint
import java.text.ParseException
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class ChatListGroupsFragment : Fragment(), SearchView.OnQueryTextListener {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var userRepository: UserRepository

    private val myUid: String get() = userRepository.myUid

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterChatGroupsList: AdapterChatGroupsList
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

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
        binding.rvChatlist.isVisible = true
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

        adapterChatGroupsList = AdapterChatGroupsList(
            chatList = chatsGroupArrayList,
            context = requireContext(),
            onChatClicked = { chat -> handleChatClick(chat) },
            onChatSeen = { chat -> markChatAsSeen(chat) },
            onMarkAsRead = { chat -> processDoubleCheckLogic(chat) }
        )

        binding.rvChatlist
.apply {
            layoutManager = this@ChatListGroupsFragment.layoutManager
            adapter = adapterChatGroupsList
        }

        registerForContextMenu(binding.rvChatlist
)
    }

    // ---------- Listener principal: chatWithUnknown ----------

    private fun setupGroupChatListListener() {

        // Reset lista compartida
        chatsGroupArrayList.clear()

        refDatos.child(myUid).child(Constants.NODE_GROUP_PRIVATE_DM)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (!snapshot.exists()) return

                    // Solo agregamos si tiene foto (misma lógica original)
                    val photo = snapshot.child("wUserPhoto").getValue(String::class.java).orEmpty()
                    if (photo.isNotEmpty()) {
                        val chat = snapshot.getValue(Conversation::class.java) ?: return
                        adapterChatGroupsList.addChats(chat)
                        updateDatesAndSort(chatsGroupArrayList)
                    }

                    chatsGroupArrayList.sort()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    refDatos.child(myUid).child(Constants.NODE_GROUP_PRIVATE_DM)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (!dataSnapshot.exists()) {
                                    binding.progressbar2.isVisible = false
                                    return
                                }

                                val updatedList = ArrayList<Conversation>()
                                for (child in dataSnapshot.children) {
                                    val chat = child.getValue(Conversation::class.java)
                                    if (chat != null) updatedList.add(chat)
                                }

                                updateDatesAndSort(updatedList)
                                updatedList.sort()

                                adapterChatGroupsList.updateData(updatedList)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val chat = snapshot.getValue(Conversation::class.java) ?: return
                    adapterChatGroupsList.deleteChat(chat)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDatesAndSort(list: MutableList<Conversation>) {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        list.forEach { chat ->
            try {
                val parsed = format.parse(chat.lastDate)
                chat.date = parsed
            } catch (_: ParseException) {
            }
        }
        list.sort()
    }

    // ---------- Empty state / Onboarding ----------

    private fun setupEmptyStateListener() {

        refDatos.child(myUid).child(Constants.NODE_GROUP_PRIVATE_DM)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        var count = 0L
                        for (snapshot in dataSnapshot.children) {
                            val state =
                                snapshot.child("estado").getValue(String::class.java).orEmpty()
                            if (state == Constants.NODE_GROUP_PRIVATE_DM || state == "silent") {
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
        rvChatlist.isVisible = false
        linearOnBoardingChatList.isVisible = true

        lottieChatLeft.playAnimation()
        Handler(Looper.getMainLooper()).postDelayed({
            lottieChatRight.playAnimation()
        }, 100)
    }

    private fun showChatList() = with(binding) {
        binding.rvChatlist.isVisible = true
        linearOnBoardingChatList.isVisible = false

        lottieChatLeft.cancelAnimation()
        lottieChatRight.cancelAnimation()
    }

    // ---------- Observer para scroll / progreso ----------

    private fun setupAdapterObserver() {
        adapterChatGroupsList.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setScrollbar()
                binding.progressbar2.isVisible = false
            }
        })
    }

    private fun setScrollbar() {
        if (adapterChatGroupsList.itemCount > 0) {
            binding.rvChatlist.scrollToPosition(adapterChatGroupsList.itemCount - 1)
        }
    }

    // 1. Manejo de Click Principal (Valida si sigue en grupo y navega)
    private fun handleChatClick(chat: Conversation) {
        // Usamos el repo para saber el grupo actual
        val groupName = userPreferencesRepository.groupName

        refGroupUsers.child(groupName).child(chat.otherId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                            putExtra("unknownName", chat.otherName)
                            putExtra("idUserUnknown", chat.otherId)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Lo sentimos, ${chat.otherName} ya no está disponible",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Eliminamos el chat si el usuario ya no existe
                        refDatos.child(myUid)
                            .child(Constants.NODE_GROUP_PRIVATE_DM)
                            .child(chat.otherId)
                            .removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 2. Marcar chat como visto (wVisto = 2)
    private fun markChatAsSeen(chat: Conversation) {
        refDatos.child(myUid)
            .child(Constants.NODE_GROUP_PRIVATE_DM)
            .child(chat.otherId)
            .child("wVisto")
            .setValue(2)
    }

    // 3. Lógica compleja de Doble Check (setMyDoubleCheck)
    private fun processDoubleCheckLogic(chat: Conversation) {
        refDatos.child(myUid)
            .child(Constants.NODE_GROUP_PRIVATE_DM)
            .child(chat.otherId)
            .child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val noVistos = snapshot.getValue(Int::class.java) ?: return
                    if (noVistos <= 0) return

                    fun markMessagesInNode(ds: DataSnapshot) {
                        for (msg in ds.children) {
                            msg.ref.child("visto").setValue(2)
                        }
                    }

                    // Intenta ruta 1: YO <--> EL
                    val path1 = "${myUid} <---> ${chat.otherId}"
                    refChatUnknown.child(path1).child("Mensajes")
                        .limitToLast(noVistos)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ds: DataSnapshot) {
                                if (ds.exists()) {
                                    markMessagesInNode(ds)
                                } else {
                                    // Intenta ruta 2: EL <--> YO
                                    val path2 = "${chat.otherId} <---> ${myUid}"
                                    refChatUnknown.child(path2).child("Mensajes")
                                        .limitToLast(noVistos)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(ds2: DataSnapshot) {
                                                if (ds2.exists()) markMessagesInNode(ds2)
                                            }
                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ---------- SearchView ----------

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unblock)
        val actionFavorites = menu.findItem(R.id.action_favorites)
        val actionExit = menu.findItem(R.id.action_exit_group)

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
        adapterChatGroupsList.filter.filter(newText)
        return true
    }

    // ---------- Companion (lista compartida para otros usos, ej. menú) ----------

    companion object {
        // Mantengo esta lista como fuente compartida (se pasa por referencia al adapter)
        val chatsGroupArrayList: ArrayList<Conversation> = ArrayList()
    }

    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}