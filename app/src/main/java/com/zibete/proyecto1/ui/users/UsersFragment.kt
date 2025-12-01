package com.zibete.proyecto1.ui.users

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.FixedSwipeRefreshLayout
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlideProfileActivity
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.FragmentGroupBinding
import com.zibete.proyecto1.databinding.FragmentUsersBinding
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.extensions.getColorCompat
import com.zibete.proyecto1.ui.main.MainActivity
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class UsersFragment : Fragment(), SearchView.OnQueryTextListener {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var firebaseRefsContainer: FirebaseRefsContainer
    @Inject lateinit var userSessionManager: UserSessionManager
    @Inject lateinit var userRepository: UserRepository

    @Inject lateinit var profileUiBinder: ProfileUiBinder

    private val myUid = userSessionManager.user.uid


    private lateinit var _binding: FragmentUsersBinding
    private val binding get() = _binding


    private val usersViewModel: UsersViewModel by viewModels()






    // UI Elements
    private var root: View? = null
    private var progressbar: ProgressBar? = null
    private var goChat: ImageButton? = null
    private var rv: RecyclerView? = null
    private var swipeRefresh: FixedSwipeRefreshLayout? = null

    // Data & Adapters
    private var adapterUsers: AdapterUsers? = null
    private val usersArrayList = mutableListOf<Users>()
    private val originalUsersList = mutableListOf<Users>()




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        val view = binding.root

        setHasOptionsMenu(true)

        initViews()
        setupRecyclerView()
        setupSwipeRefresh()

        // Configuración inicial del Toolbar
        updateToolbarState()

        // Carga inicial
        loadUsers(isRefresh = false)



        binding.refreshButton.setOnClickListener {
            usersViewModel.onRefreshRequested()
        }

        binding.filterButton.setOnClickListener {
            usersViewModel.onFilterClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.filterActive.collect { active ->
                val color = if (active) R.color.accent else R.color.blanco
                binding.filterButton.setColorFilter(
                    requireContext().getColorCompat(color),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
        return view
    }


    private fun initViews() {
        progressbar = root!!.findViewById(R.id.progressbar)
        goChat = root!!.findViewById(R.id.goChat)
    }

    private fun setupRecyclerView() {
        rv = root!!.findViewById(R.id.rv)
        val mLayoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        rv?.apply {
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }

        adapterUsers = AdapterUsers(
            usersList = usersArrayList,
            usersListAll = originalUsersList,
            context = requireContext(),
            onChatClicked = { userId -> navigateToChat(userId) },
            onProfileClicked = { user -> navigateToSlideProfile(user) },
            onListUpdated = {
                // Lógica de scrollbar que tenías estática, ahora local
                rv?.scrollToPosition((adapterUsers?.itemCount ?: 1) - 1)
            },
            profileUiBinder = profileUiBinder
        )
        rv?.adapter = adapterUsers

    }

    private fun setupSwipeRefresh() {
        swipeRefresh = root!!.findViewById<FixedSwipeRefreshLayout>(R.id.swipe_refresh).apply {
            setRecyclerView(rv)
            setOnRefreshListener {
                loadUsers(isRefresh = true)
                isRefreshing = false // Detenemos la animación visual
            }
        }
    }

    private fun updateToolbarState() {
        val filterActive = userPreferencesRepository.filterSwitch

        // Botón de refrescar
        binding.btnRefresh.setOnClickListener {
            loadUsers(isRefresh = true)
        }

        // Botón de filtro
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Cambiar color si hay filtro activo
        val colorRes = if (filterActive) R.color.accent else R.color.blanco
        binding.btnFilter.setColorFilter(
            requireContext().getColorCompat(colorRes),
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun getColorCompat(resId: Int) = ContextCompat.getColor(this, resId)


    private fun showFilterDialog() {
        val ctx = requireContext()
        val viewFilter = layoutInflater.inflate(R.layout.filter_layout, null)

        val switchEdad = viewFilter.findViewById<SwitchCompat>(R.id.switch_edad)
        val switchOnline = viewFilter.findViewById<SwitchCompat>(R.id.switch_online)
        val spinnerAge = viewFilter.findViewById<Spinner>(R.id.spinner_age)
        val spinnerAge2 = viewFilter.findViewById<Spinner>(R.id.spinner_age2)

        val edades = (18..99).toList().toTypedArray()
        val adapter = ArrayAdapter(ctx, R.layout.tv_spinner_selected, edades).apply {
            setDropDownViewResource(R.layout.tv_spinner_lista)
        }
        spinnerAge.adapter = adapter
        spinnerAge2.adapter = adapter

        // --- Cargar estado actual desde el Repo ---
        if (userPreferencesRepository.filterSwitch) {
            val savedDesde = if (userPreferencesRepository.desdePref < 18) 18 else userPreferencesRepository.desdePref
            val savedHasta = if (userPreferencesRepository.hastaPref < 18) 18 else userPreferencesRepository.hastaPref

            spinnerAge.setSelection(savedDesde - 18)
            spinnerAge2.setSelection(savedHasta - 18)
            switchOnline.isChecked = userPreferencesRepository.checkPref
            switchEdad.isChecked = userPreferencesRepository.edadPref
        }

        spinnerAge.isEnabled = switchEdad.isChecked
        spinnerAge2.isEnabled = switchEdad.isChecked

        // --- Logic de Spinners ---
        setupSpinnerListeners(spinnerAge, spinnerAge2)

        val builder = AlertDialog.Builder(ContextThemeWrapper(ctx, R.style.AlertDialogApp))
            .setView(viewFilter)
            .setPositiveButton("Filtrar") { _, _ ->
                // Guardar en Repo
                userPreferencesRepository.filterSwitch = true
                userPreferencesRepository.checkPref = switchOnline.isChecked
                userPreferencesRepository.edadPref = switchEdad.isChecked

                if (switchEdad.isChecked) {
                    userPreferencesRepository.desdePref = edades[spinnerAge.selectedItemPosition]
                    userPreferencesRepository.hastaPref = edades[spinnerAge2.selectedItemPosition]
                } else {
                    userPreferencesRepository.desdePref = 0
                    userPreferencesRepository.hastaPref = 0
                }

                loadUsers(isRefresh = false)
                updateToolbarState()
            }
            .setNegativeButton(DIALOG_CANCEL) { _, _ -> }
            .setNeutralButton("Quitar filtro") { _, _ ->
                userPreferencesRepository.clearAllData() // Limpia filtros y sesión (revisar si solo quieres limpiar filtros)
                // Si solo quieres limpiar filtros, crea un metod específico en el Repo.

                loadUsers(isRefresh = false)
                updateToolbarState()
            }
            .setCancelable(true)

        val dialog = builder.create()
        setupDialogButtonListeners(dialog, ctx, switchOnline, switchEdad)
        dialog.show()

        // Colores de botones
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ctx.getColor(R.color.blanco))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(ctx.getColor(R.color.blanco))
    }

    /**
     * Carga usuarios usando los filtros actuales del Repositorio.
     */
    fun loadUsers(isRefresh: Boolean) {
        progressbar?.visibility = View.VISIBLE

        FirebaseRefs.refCuentas.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    progressbar?.visibility = View.GONE
                    Toast.makeText(context, "No existen usuarios", Toast.LENGTH_SHORT).show()
                    return
                }

                // Lista temporal para procesar
                val tempList = mutableListOf<Users>()

                // Leemos configuración del repo una sola vez
                val applyAgeFilter = userPreferencesRepository.edadPref
                val applyOnlineFilter = userPreferencesRepository.checkPref
                val minAge = userPreferencesRepository.desdePref
                val maxAge = userPreferencesRepository.hastaPref

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    if (key == myUid) continue

                    val u = child.getValue(Users::class.java) ?: continue

                    // Calcular datos derivados
                    u.age = Utils.calcAge(u.birthDay)
                    u.distance = profileUiBinder.getDistanceMeters(
                        UserRepository.latitude,
                        UserRepository.longitude,
                        u.latitude,
                        u.longitude
                    )

                    // Lógica de Filtrado
                    var isValid = true

                    if (applyOnlineFilter && !u.state) {
                        isValid = false
                    }

                    if (isValid && applyAgeFilter) {
                        if (u.age !in minAge..maxAge) {
                            isValid = false
                        }
                    }

                    if (isValid) {
                        tempList.add(u)
                    }
                }

                // Ordenar
                tempList.sort()

                // Actualizar listas principales
                usersArrayList.clear()
                usersArrayList.addAll(tempList)

                originalUsersList.clear()
                originalUsersList.addAll(tempList) // Mantenemos copia si necesitas filtrado local por texto

                // Notificar al adapter
                if (isRefresh) {
                    adapterUsers?.updateDataUsers(usersArrayList)
                } else {
                    adapterUsers?.notifyDataSetChanged()
                }

                scrollToBottom()
                progressbar?.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                progressbar?.visibility = View.GONE
            }
        })
    }

    private fun scrollToBottom() {
        val count = adapterUsers?.itemCount ?: 0
        if (count > 0) {
            rv?.scrollToPosition(count - 1)
        }
    }

    // --- Helpers para el Dialog ---

    private fun setupSpinnerListeners(spinner1: Spinner, spinner2: Spinner) {
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (spinner2.selectedItemPosition < pos) spinner2.setSelection(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (spinner1.selectedItemPosition > pos) spinner1.setSelection(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupDialogButtonListeners(dialog: AlertDialog, ctx: Context, swOnline: SwitchCompat, swAge: SwitchCompat) {
        dialog.setOnShowListener {
            val neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            if (!userPreferencesRepository.filterSwitch) {
                neutralBtn?.isEnabled = false
                neutralBtn?.setTextColor(Color.GRAY)
            }

            updatePositiveButtonState(dialog, ctx, swOnline.isChecked, swAge.isChecked)
        }

        val listener = { _: Any, isChecked: Boolean ->
            swAge.isEnabled = true // Ensure consistency
            // Logic specific to enabling spinners
            dialog.findViewById<Spinner>(R.id.spinner_age)?.isEnabled = swAge.isChecked
            dialog.findViewById<Spinner>(R.id.spinner_age2)?.isEnabled = swAge.isChecked

            updatePositiveButtonState(dialog, ctx, swOnline.isChecked, swAge.isChecked)
        }

        swOnline.setOnCheckedChangeListener(listener)
        swAge.setOnCheckedChangeListener(listener)
    }

    private fun updatePositiveButtonState(dialog: AlertDialog, ctx: Context, onlineChecked: Boolean, ageChecked: Boolean) {
        val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        if (onlineChecked || ageChecked) {
            positive?.isEnabled = true
            positive?.setTextColor(ctx.getColor(R.color.blanco))
        } else {
            positive?.isEnabled = false
            positive?.setTextColor(Color.GRAY)
        }
    }

    // 1. Navegación al Chat
    private fun navigateToChat(userId: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("id_user", userId)
        startActivity(intent)
    }

    // 2. Navegación al Perfil Deslizable
    private fun navigateToSlideProfile(selectedUser: Users) {
        val intent = Intent(requireContext(), SlideProfileActivity::class.java)

        // Preparamos la lista para el slider (lógica original de revertir)
        // Nota: Accedemos a la lista actual del adapter o tu lista local
        val extra = ArrayList(usersArrayList)
        extra.reverse()

        intent.putExtra("userList", extra)
        intent.putExtra("position", extra.indexOf(selectedUser))
        intent.putExtra("rotation", 0)
        startActivity(intent)
    }

    // --- Search Logic ---

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_exit)?.isVisible = false
        menu.findItem(R.id.action_unlock)?.isVisible = true

        val searchItem = menu.findItem(R.id.action_search)
        searchItem?.isVisible = true

        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapterUsers?.filter?.filter(newText)
        return false
    }

    override fun onResume() {
        super.onResume()
//        mainUiViewModel.showToolbar()
//        mainUiViewModel.hideLayoutSettings()
    }
}