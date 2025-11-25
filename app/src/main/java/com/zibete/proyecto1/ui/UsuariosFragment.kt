package com.zibete.proyecto1.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.FixedSwipeRefreshLayout
import com.zibete.proyecto1.MainActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterUsers
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.DIALOG_CANCEL
import com.zibete.proyecto1.utils.DateUtils.calcAge
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.utils.UserRepository
import java.util.Collections

// ===== UsuariosFragment (sin cambios funcionales, sólo limpieza leve) =====
class UsuariosFragment : Fragment(), SearchView.OnQueryTextListener {

    private var root: View? = null
    private var progressbar: ProgressBar? = null
    private var goChat: ImageButton? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var swipeRefresh: FixedSwipeRefreshLayout? = null
    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val usersArrayList = mutableListOf<Users>()

    private val originalUsersList = mutableListOf<Users>()
    private val usersArrayList2 = mutableListOf<Users>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_usuarios, container, false)
        setHasOptionsMenu(true)

        progressbar = root!!.findViewById(R.id.progressbar)
        goChat = root!!.findViewById(R.id.goChat)

        mLayoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        rv = root!!.findViewById<RecyclerView>(R.id.rv).apply {
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }

        // Filtro inicial
        if (!filterPrefs) {
            editor.putBoolean("checkPref", false)
            editor.putBoolean("edadPref", false)
            editor.putInt("desdePref", 0)
            editor.putInt("hastaPref", 0)
            editor.apply()
            MainActivity.filter?.setColorFilter(
                resources.getColor(R.color.blanco),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            MainActivity.filter?.setColorFilter(
                resources.getColor(R.color.accent),
                PorterDuff.Mode.SRC_IN
            )
        }

        adapterUsers = AdapterUsers(usersArrayList, originalUsersList, requireContext())
        rv!!.adapter = adapterUsers

        swipeRefresh = root!!.findViewById<FixedSwipeRefreshLayout>(R.id.swipe_refresh).apply {
            setRecyclerView(rv)
            setOnRefreshListener {
                loadUsers(checkPref, desdePref, hastaPref, "refresh")
                isRefreshing = false
            }
        }

        // Botón refresh
        MainActivity.refresh?.setOnClickListener {
            loadUsers(checkPref, desdePref, hastaPref, "refresh")
        }

        // Botón filtro (se mantiene lógica original)
        MainActivity.filter?.setOnClickListener {
            showFilterDialog()
        }

        loadUsers(checkPref, desdePref, hastaPref, "load")
        return root!!
    }

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

        if (filterPrefs) {
            spinnerAge.setSelection(desdePref - 18)
            spinnerAge2.setSelection(hastaPref - 18)
            switchOnline.isChecked = checkPref
            switchEdad.isChecked = edadPref
        }

        spinnerAge.isEnabled = switchEdad.isChecked
        spinnerAge2.isEnabled = switchEdad.isChecked

        spinnerAge.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (spinnerAge2.selectedItemPosition < position) {
                    spinnerAge2.setSelection(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerAge2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (spinnerAge.selectedItemPosition > position) {
                    spinnerAge.setSelection(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val builder = AlertDialog.Builder(
            ContextThemeWrapper(ctx, R.style.AlertDialogApp)
        )
            .setView(viewFilter)
            .setPositiveButton("Filtrar") { _, _ ->
                filterPrefs = true
                editor.putBoolean("filterPrefs", true)

                checkPref = switchOnline.isChecked
                editor.putBoolean("checkPref", checkPref)

                edadPref = switchEdad.isChecked
                editor.putBoolean("edadPref", edadPref)

                if (edadPref) {
                    desdePref = edades[spinnerAge.selectedItemPosition]
                    hastaPref = edades[spinnerAge2.selectedItemPosition]
                    editor.putInt("desdePref", desdePref)
                    editor.putInt("hastaPref", hastaPref)
                } else {
                    desdePref = 0
                    hastaPref = 0
                    editor.putInt("desdePref", 0)
                    editor.putInt("hastaPref", 0)
                }

                editor.apply()

                loadUsers(checkPref, desdePref, hastaPref, "filter")
                MainActivity.filter?.setColorFilter(
                    ctx.resources.getColor(R.color.accent),
                    PorterDuff.Mode.SRC_IN
                )
            }
            .setNegativeButton(DIALOG_CANCEL) { _, _ -> }
            .setNeutralButton("Quitar filtro") { _, _ ->
                deletePreferences()
                loadUsers(checkPref, desdePref, hastaPref, "filter")
            }
            .setCancelable(true)

        val dialog = builder.create()

        switchOnline.setOnCheckedChangeListener { _, isChecked ->
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (isChecked || switchEdad.isChecked) {
                positive?.isEnabled = true
                positive?.setTextColor(ctx.resources.getColor(R.color.blanco))
            } else {
                positive?.isEnabled = false
                positive?.setTextColor(Color.GRAY)
            }
        }

        switchEdad.setOnCheckedChangeListener { _, isChecked ->
            spinnerAge.isEnabled = isChecked
            spinnerAge2.isEnabled = isChecked
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (isChecked || switchOnline.isChecked) {
                positive?.isEnabled = true
                positive?.setTextColor(ctx.resources.getColor(R.color.blanco))
            } else {
                positive?.isEnabled = false
                positive?.setTextColor(Color.GRAY)
            }
        }

        dialog.setOnShowListener {
            if (!filterPrefs) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
                    isEnabled = false
                    setTextColor(Color.GRAY)
                }
            }

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (!switchOnline.isChecked && !switchEdad.isChecked) {
                positive?.isEnabled = false
                positive?.setTextColor(Color.GRAY)
            } else {
                positive?.isEnabled = true
                positive?.setTextColor(ctx.resources.getColor(R.color.blanco))
            }
        }

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ctx.resources.getColor(R.color.blanco))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            ?.setTextColor(ctx.resources.getColor(R.color.blanco))
    }

    fun loadUsers(check: Boolean, desde: Int, hasta: Int, flag: String) {
        progressbar?.visibility = View.VISIBLE

        refCuentas.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    progressbar?.visibility = View.GONE
                    Toast.makeText(context, "No existen usuarios", Toast.LENGTH_SHORT).show()
                    return
                }

                if (flag == "load") {
                    usersArrayList.clear()
                } else {
                    usersArrayList2.clear()
                }
                originalUsersList.clear()

                val currentUid = user?.uid

                if (edadPref) {
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        if (key == currentUid) continue

                        val u = child.getValue(Users::class.java) ?: continue
                        val edad = calcAge(u.birthDay)
                        u.age = edad

                        val distanceMeters = ProfileUiBinder.getDistanceMeters(
                            UserRepository.latitude,
                            UserRepository.longitude,
                            u.latitude,
                            u.longitude
                        )
                        u.distance = distanceMeters

                        if (edad in desde..hasta) {
                            if (check && !u.state) continue
                            if (flag == "load") {
                                adapterUsers?.addUser(u)
                            } else {
                                usersArrayList2.add(u)
                            }
                        }
                    }
                } else {
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        if (key == currentUid) continue

                        val u = child.getValue(Users::class.java) ?: continue

                        val distanceMeters = ProfileUiBinder.getDistanceMeters(
                            UserRepository.latitude,
                            UserRepository.longitude,
                            u.latitude,
                            u.longitude
                        )
                        u.distance = distanceMeters

                        if (check && !u.state) continue

                        if (flag == "load") {
                            adapterUsers?.addUser(u)
                        } else {
                            usersArrayList2.add(u)
                        }
                    }
                }

                Collections.sort(usersArrayList2)
                Collections.sort(usersArrayList)
                Collections.sort(originalUsersList)

                if (flag == "load") {
                    adapterUsers?.notifyDataSetChanged()
                } else {
                    adapterUsers?.updateDataUsers(usersArrayList2)
                }

                setScrollbar()
                progressbar?.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                progressbar?.visibility = View.GONE
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val actionSearch = menu.findItem(R.id.action_search)
        val actionUnlock = menu.findItem(R.id.action_unlock)
        val actionExit = menu.findItem(R.id.action_exit)

        actionExit?.isVisible = false
        actionSearch?.isVisible = true
        actionUnlock?.isVisible = true

        val searchView = actionSearch?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        adapterUsers?.filter?.filter(newText)
        return false
    }

    companion object {
        var rv: RecyclerView? = null
        var adapterUsers: AdapterUsers? = null

        @SuppressLint("RestrictedApi")
        private val preferences: SharedPreferences =
            AuthUI.getApplicationContext()
                .getSharedPreferences("FilterUsers", Context.MODE_PRIVATE)
        var editor: SharedPreferences.Editor = preferences.edit()

        var filterPrefs: Boolean = preferences.getBoolean("filterPrefs", false)
        var checkPref: Boolean = preferences.getBoolean("checkPref", false)
        var edadPref: Boolean = preferences.getBoolean("edadPref", false)
        var desdePref: Int = preferences.getInt("desdePref", 0)
        var hastaPref: Int = preferences.getInt("hastaPref", 0)

        var inGroup: Boolean = preferences.getBoolean("inGroup", false)
        @JvmField
        var groupName: String = preferences.getString("groupName", "") ?: ""
        var userName: String = preferences.getString("userName", "") ?: ""
        var userType: Int = preferences.getInt("userType", 2)
        var readGroupMsg: Int = preferences.getInt("readGroupMsg", 0)
        var userDate: String = preferences.getString("userDate", "") ?: ""
        var countGroupBadge: Int = 0

        @JvmField
        var individualNotifications: Boolean =
            preferences.getBoolean("individualNotifications", true)
        @JvmField
        var groupNotifications: Boolean =
            preferences.getBoolean("groupNotifications", true)

        @SuppressLint("RestrictedApi")
        fun deletePreferences() {
            filterPrefs = false
            checkPref = false
            edadPref = false
            desdePref = 0
            hastaPref = 0

            inGroup = false
            groupName = ""
            userName = ""
            userType = 2

            editor.putBoolean("inGroup", false)
            editor.putString("groupName", "")
            editor.putString("userName", "")
            editor.putInt("userType", 2)

            editor.putBoolean("filterPrefs", false)
            editor.putBoolean("checkPref", false)
            editor.putBoolean("edadPref", false)
            editor.putInt("desdePref", 0)
            editor.putInt("hastaPref", 0)

            editor.apply()

            MainActivity.filter?.setColorFilter(
                AuthUI.getApplicationContext().resources.getColor(R.color.blanco),
                PorterDuff.Mode.SRC_IN
            )
        }

        fun setScrollbar() {
            rv?.scrollToPosition((adapterUsers?.itemCount ?: 1) - 1)
        }
    }
}

