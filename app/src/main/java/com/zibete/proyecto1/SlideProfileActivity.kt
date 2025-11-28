package com.zibete.proyecto1

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.adapters.SliderProfileAdapter
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.ChatUtils
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.utils.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SlideProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var profileUiBinder: ProfileUiBinder

    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var progressbarImage: ProgressBar
    private lateinit var userList: MutableList<Users>
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.slide_activity)

        progressbarImage = findViewById(R.id.progressbarImage)
        progressbarImage.visibility = android.view.View.GONE

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        @Suppress("UNCHECKED_CAST")
        userList = (intent.extras?.getSerializable("userList") as? MutableList<Users>) ?: mutableListOf()
        val position = intent.extras?.getInt("position") ?: 0
        val rotation = intent.extras?.getInt("rotation") ?: 0

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = SliderProfileAdapter(profileUiBinder, this, userList, rotation)
        viewPager.setCurrentItem(position.coerceIn(0, (userList.size - 1).coerceAtLeast(0)), false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val actionSilent = menu.findItem(R.id.action_silent)
        val actionNotif = menu.findItem(R.id.action_notif)
        val actionBloq = menu.findItem(R.id.action_bloq)
        val actionDesbloq = menu.findItem(R.id.action_desbloq)
        val actionDelete = menu.findItem(R.id.action_delete)
        actionDelete.isVisible = true

        val currentIdx = viewPager.currentItem
        val current = userList.getOrNull(currentIdx)
        if (current != null && user != null) {
            FirebaseRefs.refDatos.child(user.uid).child(Constants.CHATWITH).child(current.id)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(ds: DataSnapshot) {
                        val state = ds.child("estado").getValue(String::class.java)
                        when (state) {
                            "silent" -> {
                                actionSilent.isVisible = false
                                actionNotif.isVisible = true
                                actionDesbloq.isVisible = false
                                actionBloq.isVisible = true
                            }
                            Constants.CHATWITH, "delete", null -> {
                                actionSilent.isVisible = true
                                actionNotif.isVisible = false
                                actionDesbloq.isVisible = false
                                actionBloq.isVisible = true
                            }
                            "bloq" -> {
                                actionSilent.isVisible = false
                                actionNotif.isVisible = false
                                actionDesbloq.isVisible = true
                                actionBloq.isVisible = false
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val u = userList.getOrNull(viewPager.currentItem) ?: return super.onOptionsItemSelected(item)
        val root = findViewById<android.view.View>(android.R.id.content)
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
            R.id.action_silent -> {
                UserRepository.silent(u.name, u.id, Constants.CHATWITH)
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_notif -> {
                UserRepository.silent(u.name, u.id, Constants.CHATWITH)
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            }
            R.id.action_bloq -> UserRepository.setBlockUser(this, u.name, u.id, root, Constants.CHATWITH)
            R.id.action_desbloq -> UserRepository.setUnBlockUser(this, u.id, u.name, root, Constants.CHATWITH)
            R.id.action_delete -> ChatUtils.deleteChat(this, u.id, u.name, root, Constants.CHATWITH)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        user?.uid?.let { UserRepository.setUserOffline(applicationContext, it) }
    }

    override fun onResume() {
        super.onResume()
        user?.uid?.let { UserRepository.setUserOnline(applicationContext, it) }
    }
}
