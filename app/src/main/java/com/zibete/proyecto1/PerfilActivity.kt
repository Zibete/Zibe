package com.zibete.proyecto1

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.android.material.appbar.MaterialToolbar
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.ChatUtils
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.utils.DateUtils.calcAge
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.ProfileUiBinder
import com.zibete.proyecto1.utils.UserRepository
import com.zibete.proyecto1.utils.UserRepository.Silent
import com.zibete.proyecto1.utils.UserRepository.setBlockUser
import com.zibete.proyecto1.utils.UserRepository.setUnBlockUser
import com.zibete.proyecto1.utils.UserRepository.setUserOffline
import com.zibete.proyecto1.utils.UserRepository.setUserOnline
import java.math.BigDecimal
import java.math.RoundingMode

class PerfilActivity : AppCompatActivity() {

    // UI
    private lateinit var ftPerfil: ImageView
    private lateinit var iconConectado: ImageView
    private lateinit var iconDesconectado: ImageView
    private lateinit var nameUser: TextView
    private lateinit var tvEstado: TextView
    private lateinit var desc: TextView
    private lateinit var age: TextView
    private lateinit var distanceUser: TextView
    private lateinit var linearImageActivity: LinearLayout
    private lateinit var linearPhotos: LinearLayout
    private lateinit var linearDesc: LinearLayout
    private lateinit var recyclerPhotos: RecyclerView
    private lateinit var perfilFavoriteOn: ImageView
    private lateinit var perfilFavoriteOff: ImageView
    private lateinit var perfilBloq: ImageView
    private lateinit var perfilBloqMe: ImageView
    private lateinit var loadingPhoto: ProgressBar

    private lateinit var floatingActionMenu: FloatingActionMenu
    private lateinit var subMenuChatWith: FloatingActionButton
    private lateinit var subMenuChatWithUnknown: FloatingActionButton

    // Estado / datos
    private lateinit var currentUser: FirebaseUser
    private lateinit var idUser: String
    private var nombre: String? = null
    private var unknownName: String? = null
    private var foto: String? = null

    private val photoList = ArrayList<String>()
    private val receivedPhotos = ArrayList<String>()
    private lateinit var adapterPhotoReceived: AdapterPhotoReceived

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Usuario logueado
        currentUser = FirebaseAuth.getInstance().currentUser
            ?: run {
                finish()
                return
            }

        // id del perfil a mostrar
        idUser = intent.extras?.getString("id_user")
            ?: run {
                finish()
                return
            }

        bindViews()
        setupToolbar()
        setupRecycler()
        setupFabMenu()
        setupFavoriteState()
        setupBlockState()
        loadProfileData()
        setupImageLayout()
        listenIncomingPhotos()
        bindPresence()
    }

    // region Setup

    private fun bindViews() {
        floatingActionMenu = findViewById(R.id.floatingActionMenu)
        subMenuChatWith = findViewById(R.id.subMenu_chatWith)
        subMenuChatWithUnknown = findViewById(R.id.subMenu_chatWithUnknown)

        linearDesc = findViewById(R.id.linear_desc)
        linearImageActivity = findViewById(R.id.linearImageActivity)
        linearPhotos = findViewById(R.id.linearPhotos)
        recyclerPhotos = findViewById(R.id.recyclerPhotos)

        distanceUser = findViewById(R.id.distanceUser)
        ftPerfil = findViewById(R.id.ftPerfil)
        nameUser = findViewById(R.id.nameUser)
        desc = findViewById(R.id.desc)
        age = findViewById(R.id.edad)
        tvEstado = findViewById(R.id.tv_estado)
        iconConectado = findViewById(R.id.icon_conectado)
        iconDesconectado = findViewById(R.id.icon_desconectado)
        perfilFavoriteOff = findViewById(R.id.perfil_favorite_off)
        perfilFavoriteOn = findViewById(R.id.perfil_favorite_on)
        perfilBloq = findViewById(R.id.perfil_bloq)
        perfilBloqMe = findViewById(R.id.perfil_bloq_me)
        loadingPhoto = findViewById(R.id.loadingPhoto)

        floatingActionMenu.setClosedOnTouchOutside(true)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecycler() {
        val layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true).apply {
                stackFromEnd = true
            }

        recyclerPhotos.layoutManager = layoutManager
        adapterPhotoReceived = AdapterPhotoReceived(receivedPhotos, Constants.MAXCHATSIZE, applicationContext)
        recyclerPhotos.adapter = adapterPhotoReceived
    }

    private fun setupFabMenu() {
        // ¿Tiene nombre incógnito en el grupo?
        FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName)
            .child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        unknownName = snapshot.child("user_name").getValue(String::class.java)
                        subMenuChatWithUnknown.labelText =
                            "Chat privado de: ${UsuariosFragment.groupName}"
                    } else {
                        subMenuChatWithUnknown.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        subMenuChatWithUnknown.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("unknownName", unknownName)   // nombre incógnito o UID
                putExtra("idUserUnknown", idUser)      // su UID real
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }

        subMenuChatWith.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("id_user", idUser)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }
    }

    private fun setupFavoriteState() {
        // Estado inicial
        FirebaseRefs.refDatos.child(currentUser.uid).child("FavoriteList").child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isFav = snapshot.exists()
                    perfilFavoriteOn.visibility = if (isFav) View.VISIBLE else View.GONE
                    perfilFavoriteOff.visibility = if (isFav) View.GONE else View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Marcar favorito
        perfilFavoriteOff.setOnClickListener {
            FirebaseRefs.refDatos.child(currentUser.uid)
                .child("FavoriteList")
                .child(idUser)
                .setValue(idUser)
            perfilFavoriteOn.visibility = View.VISIBLE
            perfilFavoriteOff.visibility = View.GONE
            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
        }

        // Quitar favorito
        perfilFavoriteOn.setOnClickListener {
            FirebaseRefs.refDatos.child(currentUser.uid)
                .child("FavoriteList")
                .child(idUser)
                .removeValue()
            perfilFavoriteOn.visibility = View.GONE
            perfilFavoriteOff.visibility = View.VISIBLE
            Toast.makeText(this, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBlockState() {
        // Yo lo bloqueé
        FirebaseRefs.refDatos.child(currentUser.uid)
            .child("ChatWith")
            .child(idUser)
            .child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val state = snapshot.getValue(String::class.java)
                    perfilBloq.visibility = if (state == "bloq") View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Él me bloqueó
        FirebaseRefs.refDatos.child(idUser)
            .child("ChatWith")
            .child(currentUser.uid)
            .child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val state = snapshot.getValue(String::class.java)
                    perfilBloqMe.visibility = if (state == "bloq") View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadProfileData() {
        FirebaseRefs.refCuentas.child(idUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val birthDay = snapshot.child("birthDay").getValue(String::class.java)
                    foto = snapshot.child("foto").getValue(String::class.java)
                    nombre = snapshot.child("nombre").getValue(String::class.java)
                    val descripcion = snapshot.child("descripcion").getValue(String::class.java)
                    val otherLatitude = snapshot.child("latitud").getValue(Double::class.java)
                    val otherLongitude = snapshot.child("longitud").getValue(Double::class.java)

                    // Edad
                    val edad = calcAge(birthDay)
                    age.text = edad?.toString() ?: ""

                    // Distancia
                    if (otherLatitude != null && otherLongitude != null
                    ) {
                        val distanceMeters = ProfileUiBinder.getDistanceMeters(
                            UserRepository.latitude,
                            UserRepository.longitude,
                            otherLatitude,
                            otherLongitude
                        )

                        val text = when {
                            distanceMeters > 10_000 -> {
                                val bd = BigDecimal(distanceMeters / 1000)
                                    .setScale(0, RoundingMode.HALF_UP)
                                "A $bd kilómetros"
                            }

                            distanceMeters > 1_000 -> {
                                val bd = BigDecimal(distanceMeters / 1000)
                                    .setScale(1, RoundingMode.HALF_UP)
                                "A $bd kilómetros"
                            }

                            else -> {
                                val bd = BigDecimal(distanceMeters)
                                    .setScale(0, RoundingMode.HALF_UP)
                                "A $bd metros"
                            }
                        }
                        distanceUser.text = text
                    } else {
                        distanceUser.text = ""
                    }

                    foto?.let { photoList.add(it) }

                    nameUser.text = nombre ?: ""
                    if (!descripcion.isNullOrEmpty()) {
                        linearDesc.visibility = View.VISIBLE
                        desc.text = descripcion
                    } else {
                        linearDesc.visibility = View.GONE
                    }

                    // Foto perfil
                    loadingPhoto.visibility = View.VISIBLE
                    Glide.with(this@PerfilActivity)
                        .load(foto)
                        .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                loadingPhoto.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                loadingPhoto.visibility = View.GONE
                                return false
                            }
                        })
                        .into(ftPerfil)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupImageLayout() {
        val dimension = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dimension)
        val height = dimension.heightPixels

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            height - (height / 4)
        )
        linearImageActivity.layoutParams = layoutParams

        linearImageActivity.setOnClickListener {
            val intent = Intent(this, SlidePhotoActivity::class.java).apply {
                putStringArrayListExtra("photoList", ArrayList(photoList))
                putExtra("position", 0)
                putExtra("rotation", 180)
            }
            startActivity(intent)
        }

        ftPerfil.setOnClickListener {
            // reservado por si querés ampliar o acciones futuras
        }
    }

    private fun listenIncomingPhotos() {
        // Mis chats con esta persona
        FirebaseRefs.refChat.child("${currentUser.uid} <---> $idUser")
            .child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addChat(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

        // Sus chats conmigo
        FirebaseRefs.refChat.child("$idUser <---> ${currentUser.uid}")
            .child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addChat(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun bindPresence() {
        UserRepository.stateUser(
            applicationContext,
            idUser,
            iconConectado,
            iconDesconectado,
            tvEstado,
            Constants.CHATWITH
        )
    }

    // endregion

    // region Chat helper

    private fun addChat(snapshot: DataSnapshot) {
        if (!snapshot.exists()) return

        val type = snapshot.child("type").getValue(Int::class.java)
        val sender = snapshot.child("envia").getValue(String::class.java)
        val message = snapshot.child("mensaje").getValue(String::class.java)

        if (sender != null && sender != currentUser.uid) {
            if (type == Constants.PHOTO || type == Constants.PHOTO_SENDER_DLT) {
                if (!message.isNullOrEmpty()) {
                    adapterPhotoReceived.addString(message)
                    linearPhotos.visibility = View.VISIBLE
                }
            }
        }
    }

    // endregion

    // region Lifecycle

    override fun onPause() {
        super.onPause()
        setUserOffline(applicationContext, currentUser.uid)
    }

    override fun onResume() {
        super.onResume()
        setUserOnline(applicationContext, currentUser.uid)
    }

    // endregion

    // region Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val actionSilent = menu.findItem(R.id.action_silent)
        val actionNotif = menu.findItem(R.id.action_notif)
        val actionBloq = menu.findItem(R.id.action_bloq)
        val actionDesbloq = menu.findItem(R.id.action_desbloq)
        val actionDelete = menu.findItem(R.id.action_delete)

        actionDelete.isVisible = true

        // Actualizar visibilidad según estado en ChatWith
        FirebaseRefs.refDatos.child(currentUser.uid)
            .child("ChatWith")
            .child(idUser)
            .child("estado")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        when (snapshot.getValue(String::class.java)) {
                            "silent" -> {
                                actionSilent.isVisible = false
                                actionNotif.isVisible = true
                                actionDesbloq.isVisible = false
                                actionBloq.isVisible = true
                            }

                            "chat" -> {
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

                            "delete" -> {
                                actionSilent.isVisible = true
                                actionNotif.isVisible = false
                                actionDesbloq.isVisible = false
                                actionBloq.isVisible = true
                            }

                            else -> {
                                actionSilent.isVisible = true
                                actionNotif.isVisible = false
                                actionDesbloq.isVisible = false
                                actionBloq.isVisible = true
                            }
                        }
                    } else {
                        actionSilent.isVisible = true
                        actionNotif.isVisible = false
                        actionDesbloq.isVisible = false
                        actionBloq.isVisible = true
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val view = findViewById<View>(android.R.id.content)
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_silent -> {
                Silent(nombre, idUser, Constants.CHATWITH)
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_notif -> {
                Silent(nombre, idUser, Constants.CHATWITH)
                Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_bloq -> { // Bloquear
                setBlockUser(this, nombre, idUser, view, Constants.CHATWITH)
                true
            }

            R.id.action_desbloq -> { // Desbloquear
                setUnBlockUser(this, idUser, nombre, view, Constants.CHATWITH)
                true
            }

            R.id.action_delete -> { // Eliminar chat
                ChatUtils.deleteChat(this, idUser, nombre, view, Constants.CHATWITH)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // endregion
}
