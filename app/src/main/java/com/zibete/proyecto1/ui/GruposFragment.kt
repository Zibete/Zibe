package com.zibete.proyecto1.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.PageAdapterGroup
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterGroups
import com.zibete.proyecto1.databinding.DialogGoGroupBinding
import com.zibete.proyecto1.databinding.DialogGoNewGroupBinding
import com.zibete.proyecto1.databinding.FragmentGruposBinding
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupData
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import com.zibete.proyecto1.utils.Utils.repo
import java.text.SimpleDateFormat
import java.util.*

class GruposFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentGruposBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterGroups
    private lateinit var layoutManager: GridLayoutManager

    private val groupsArrayList = ArrayList<Groups>()
    private val originalGroupsArrayList = ArrayList<Groups>()
    private val groupsArrayList2 = ArrayList<Groups>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGruposBinding.inflate(inflater, container, false)

        setupRecycler()
        setupSwipeRefresh()
        setupFab()
        loadGroups("load")

        return binding.root
    }

    // ---------- UI / Recycler ----------

    private fun setupRecycler() = with(binding) {
        layoutManager = GridLayoutManager(requireContext(), 2).apply {
            reverseLayout = false
        }

        adapter = AdapterGroups(groupsArrayList, originalGroupsArrayList, requireContext()) { groupSelected ->
            // Aquí recibes el grupo clickeado y llamas a tu función local
            onGroupClicked(groupSelected)
        }

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
            showCreateGroupDialog()
        }
    }

    // ---------- Carga de grupos ----------

    fun loadGroups(flag: String) {
        val b = _binding ?: return
        b.progressbar.isVisible = true

        refGroupData.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val binding = _binding ?: return  // vista destruida, salimos

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

                // Por cada grupo en DB
                for (snapshot in dataSnapshot.children) {
                    val group = snapshot.getValue(Groups::class.java) ?: continue
                    val nombre = snapshot.child("name").getValue(String::class.java) ?: continue

                    // Obtener cantidad de usuarios del grupo
                    refGroupUsers.child(nombre)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            @SuppressLint("SetTextI18n")
                            override fun onDataChange(usersSnap: DataSnapshot) {
                                val bindingInner = _binding ?: return

                                val counter = usersSnap.childrenCount.toInt()
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
                                bindingInner.progressbar.isVisible = false
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Si falla este hijo, ocultamos progress solo si sigue visible
                                _binding?.progressbar?.isVisible = false
                            }
                        })
                }

                // Si no hay hijos o ya terminó el loop sin callbacks extra
                binding.progressbar.isVisible = false
            }

            override fun onCancelled(error: DatabaseError) {
                _binding?.progressbar?.isVisible = false
            }
        })
    }

    private fun sortGroups() {
        Collections.sort(groupsArrayList)
        Collections.sort(groupsArrayList2)
        Collections.sort(originalGroupsArrayList)
    }

    private fun setScrollbar() {
        val b = _binding ?: return
        if (::adapter.isInitialized && adapter.itemCount > 0) {
            b.rvGroups.scrollToPosition(0)
        }
    }

    // 1. Lógica para manejar el click en un grupo existente (viene del Adapter)
    private fun onGroupClicked(group: Groups) {
        showJoinGroupDialog(group)
    }

    // 2. Diálogo para unirse a grupo público (Refactorizado de goPublicGroup)
    private fun showJoinGroupDialog(group: Groups) {
        val binding = DialogGoGroupBinding.inflate(layoutInflater)
        val currentUser = FirebaseAuth.getInstance().currentUser // O usa tu variable global 'user'

        binding.nameUser.text = currentUser?.displayName
        binding.tvChat.text = group.name
        Glide.with(requireContext()).load(currentUser?.photoUrl).into(binding.imageUser)
        binding.btnStartChat2.isEnabled = false

        binding.edtNick.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnStartChat2.isEnabled = binding.edtNick.text?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.AlertDialogApp))
            .setView(binding.root)
            .setCancelable(true)
            .create()

        // Opción 1: Entrar con nombre real
        binding.btnStartChat1.setOnClickListener {
            joinGroupAndNavigate(dialog, group.name, currentUser?.displayName ?: "", 1)
        }

        // Opción 2: Entrar con Nickname
        binding.btnStartChat2.setOnClickListener { v ->
            FirebaseRefs.refGroupUsers.child(group.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            val name = child.child("user_name").getValue(String::class.java)
                            if (name == binding.edtNick.text.toString()) {
                                Toast.makeText(requireContext(), "${binding.edtNick.text} está en uso", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        joinGroupAndNavigate(dialog, group.name, binding.edtNick.text.toString(), 0)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 3. Diálogo para Crear Nuevo Grupo (Refactorizado de goNewGroup)
// LLAMA A ESTA FUNCIÓN DESDE TU FAB (Boton flotante) O MENU
    fun showCreateGroupDialog() {
        val binding = DialogGoNewGroupBinding.inflate(layoutInflater)
        val currentUser = FirebaseAuth.getInstance().currentUser

        binding.nameUser.text = currentUser?.displayName
        Glide.with(requireContext()).load(currentUser?.photoUrl).into(binding.imageUser)
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

        val dialog = AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.AlertDialogApp))
            .setView(binding.root)
            .setCancelable(true)
            .create()

        binding.btnCreateNewChat.setOnClickListener {
            val name = binding.edtNameNewGroup.text.toString()
            val data = binding.edtDataNewGroup.text.toString()

            FirebaseRefs.refGroupData.child(name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(requireContext(), "El nombre ya está en uso", Toast.LENGTH_SHORT).show()
                            return
                        }

                        @SuppressLint("SimpleDateFormat")
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())
                        val group = Groups(name, data, currentUser!!.uid, Constants.PUBLIC_GROUP, 0, date)

                        FirebaseRefs.refGroupData.child(name).setValue(group)

                        // Entramos directamente al grupo creado
                        joinGroupAndNavigate(dialog, name, currentUser.displayName ?: "", Constants.PUBLIC_GROUP)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.imgCancelDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 4. Lógica central de navegación y guardado (Refactorizado de goGroup)
    private fun joinGroupAndNavigate(dialog: AlertDialog?, groupName: String, userName: String, type: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        @SuppressLint("SimpleDateFormat")
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS").format(Date())

        // --- REPO: Guardado automático ---
        repo.userName = userName
        repo.groupName = groupName
        repo.inGroup = true
        repo.userType = type
        repo.userDate = date

        // --- FIREBASE LOGIC ---
        val chatMsg = ChatsGroup("se unió a la sala", date, userName, currentUser.uid, 0, type)
        FirebaseRefs.refGroupChat.child(groupName).push().setValue(chatMsg)

        MainActivity.listenerGroupBadge?.let { listener ->
            FirebaseRefs.refGroupChat.child(groupName).addValueEventListener(listener)
        }

        val query: Query = FirebaseRefs.refDatos.child(currentUser.uid)
            .child(Constants.CHATWITHUNKNOWN)
            .orderByChild("noVisto").startAt(1.0)

        MainActivity.listenerMsgUnreadBadge?.let { listener ->
            query.addValueEventListener(listener)
        }

        // --- UI UPDATES ---
        (activity as? MainActivity)?.invalidateOptionsMenu()

        // --- NAVIGATION ---
        // NOTA: Intenta usar NavController en el futuro. Por ahora mantenemos tu lógica de transacciones.
        val newFragment = PageAdapterGroup()
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, newFragment)
            .commit()

        // Registrar usuario en nodo del grupo
        val userGroup = UserGroup(currentUser.uid, userName, type)
        FirebaseRefs.refGroupUsers.child(groupName).child(currentUser.uid).setValue(userGroup)

        dialog?.dismiss()
    }

    // ---------- SearchView ----------

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        if (::adapter.isInitialized) {
            adapter.filter.filter(newText)
        }
        return true
    }

    // ---------- Menú ----------

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

    // ---------- Ciclo de vida ----------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
