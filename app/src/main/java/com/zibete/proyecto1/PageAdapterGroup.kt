package com.zibete.proyecto1

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.rahimlis.badgedtablayout.BadgedTabLayout
import com.zibete.proyecto1.adapters.ChatListGroupsFragment
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers

class PageAdapterGroup : Fragment() {

    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private lateinit var viewPager: ViewPager
    private lateinit var linearProgressBar: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tabLayout: BadgedTabLayout

    private val fragments: List<Fragment> = listOf(
        PersonsChatGroupFragment(),
        ChatGroupFragment(),
        ChatListGroupsFragment()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.pager_groups_chat, container, false)

        if (user == null) return view

        // Bind views
        viewPager = view.findViewById(R.id.viewPager)
        linearProgressBar = view.findViewById(R.id.linearProgressBar)
        progressBar = view.findViewById(R.id.progressBar)
        tabLayout = view.findViewById(R.id.tab_layout)

        // Loading visible al inicio
        linearProgressBar.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        // Adapter
        val pagerAdapter = MyPagerAdapter(childFragmentManager)
        viewPager.adapter = pagerAdapter

        // Tabs
        tabLayout.setupWithViewPager(viewPager)

        // Arrancamos mostrando la pestaña 2 (lista de grupos)
        viewPager.currentItem = 2

        // Comprobación de red + transición a la pestaña central
        startNetworkCheck()

        // Badge de mensajes no vistos (CHATWITHUNKNOWN)
        setupUnknownChatBadge()

        return view
    }

    // ========== Network + loader ==========

    private fun startNetworkCheck() {
        linearProgressBar.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = cm.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                // Hay conexión: pasamos al tab central y ocultamos loader
                viewPager.currentItem = 1

                Handler(Looper.getMainLooper()).postDelayed({
                    linearProgressBar.visibility = View.GONE
                    progressBar.visibility = View.GONE
                }, 200)
            } else {
                // Sin conexión: mostramos mensaje + opción reintentar
                progressBar.visibility = View.GONE
                linearProgressBar.visibility = View.VISIBLE

                val snack = Snackbar.make(
                    viewPager,
                    "No hay conexión a Internet en este momento",
                    Snackbar.LENGTH_INDEFINITE
                )
                snack.setAction("Reintentar") {
                    snack.dismiss()
                    startNetworkCheck()
                }
                try {
                    snack.setBackgroundTint(resources.getColor(R.color.colorC))
                    val tv = snack.view
                        .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
                } catch (_: Throwable) {}
                snack.show()
            }
        }, 800)
    }

    // ========== Badge de incógnito (CHATWITHUNKNOWN) ==========

    private fun setupUnknownChatBadge() {
        val uid = user?.uid ?: return

        val newQuery: Query = refDatos.child(uid)
            .child(Constants.CHATWITHUNKNOWN)
            .orderByChild("noVisto")
            .startAt(1.0)

        newQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var countMsgUnread = 0
                    for (child in snapshot.children) {
                        val unRead = child.child("noVisto")
                            .getValue(Int::class.java) ?: 0
                        countMsgUnread += unRead
                    }
                    tabLayout.setBadgeText(2, countMsgUnread.toString())
                } else {
                    tabLayout.setBadgeText(2, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ========== Adapter interno ==========

    inner class MyPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private var membersCount: Int = 0

        init {
            // Listener para cantidad de miembros en el grupo actual
            valueEventListenerTitle = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isAdded) {
                        membersCount = snapshot.childrenCount.toInt()
                        notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            // Escuchamos usuarios del grupo actual
            if (UsuariosFragment.groupName.isNotEmpty()) {
                refGroupUsers.child(UsuariosFragment.groupName)
                    .addValueEventListener(valueEventListenerTitle as ValueEventListener)
            }
        }

        override fun getCount(): Int = fragments.size

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            0 -> "(${membersCount}) ${requireContext().getString(R.string.menu_usuarios)}"
            1 -> UsuariosFragment.groupName
            2 -> requireContext().getString(R.string.menu_chat)
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpio listener del título para evitar fugas
        val groupName = UsuariosFragment.groupName
        if (groupName.isNotEmpty() && valueEventListenerTitle != null) {
            refGroupUsers.child(groupName).removeEventListener(valueEventListenerTitle!!)
        }
        valueEventListenerTitle = null
    }

    companion object {
        var valueEventListenerTitle: ValueEventListener? = null
    }
}
