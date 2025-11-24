package com.zibete.proyecto1

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.ui.splash.SplashActivity
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.EditProfileFragment
import com.zibete.proyecto1.ui.GruposFragment
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.ui.constants.Constants.CHATWITHUNKNOWN
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.FirebaseRefs.refChatUnknown
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupChat
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import com.zibete.proyecto1.utils.UserMessageUtils
import com.zibete.proyecto1.utils.UserRepository.setUserOffline
import com.zibete.proyecto1.utils.UserRepository.setUserOnline
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.core.content.edit
import androidx.core.view.isGone

class SettingsActivity : AppCompatActivity() {

    // UI
    private lateinit var toolbar: MaterialToolbar
    private lateinit var myEmail: TextView
    private lateinit var btnChangeEmail: LinearLayout
    private lateinit var btnChangePassword: LinearLayout

    private lateinit var linearNotifGrupales: LinearLayout
    private lateinit var linearNotifIndividuales: LinearLayout

    private lateinit var arrowDownChangePass: ImageView
    private lateinit var arrowUpChangePass: ImageView
    private lateinit var arrowDownChangeEmail: ImageView
    private lateinit var arrowUpChangeEmail: ImageView

    private lateinit var btnLogout: View
    private lateinit var btnReport: View
    private lateinit var btnDeleteAccount: View

    private lateinit var btnSavePass: ImageButton
    private lateinit var btnSaveEmail: ImageButton

    private lateinit var linearChangeEmail: LinearLayout
    private lateinit var linearChangePassword: LinearLayout
    private lateinit var edtPasswordEmail: EditText
    private lateinit var edtPasswordPass: EditText
    private lateinit var edtNewMail: EditText
    private lateinit var edtNewPass: EditText

    private lateinit var switchIndividualNotifications: SwitchMaterial
    private lateinit var switchGroupNotifications: SwitchMaterial

    // Estado
    private var newEmail: String? = null
    private var newPassword: String? = null
    private var password: String? = null
    private lateinit var progress: ProgressDialog
    private var provider: String? = null

    private val user get() = currentUser!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (user == null) {
            finish()
            startActivity(Intent(this, SplashActivity::class.java))
            return
        }

        bindViews()
        setupToolbar()
        setupNotificationSwitches()
        setupAccountInfo()
        setupTextWatchers()
        setupExpandables()
        setupSaveButtons()
        setupDeleteAccount()
        setupLogout()
    }

    // region Setup

    private fun bindViews() {
        arrowDownChangePass = findViewById(R.id.arrow_down_change_pass)
        arrowUpChangePass = findViewById(R.id.arrow_up_change_pass)
        arrowDownChangeEmail = findViewById(R.id.arrow_down_change_email)
        arrowUpChangeEmail = findViewById(R.id.arrow_up_change_email)

        toolbar = findViewById(R.id.toolbar_ajustes)
        myEmail = findViewById(R.id.my_email)
        btnChangeEmail = findViewById(R.id.btn_change_email)
        btnChangePassword = findViewById(R.id.btn_change_password)

        btnSavePass = findViewById(R.id.btn_save_pass)
        btnSaveEmail = findViewById(R.id.btn_save_email)

        btnLogout = findViewById(R.id.btn_logout)
        btnReport = findViewById(R.id.btn_report)
        btnDeleteAccount = findViewById(R.id.btn_delete_account)

        linearChangeEmail = findViewById(R.id.linearChangeEmail)
        linearChangePassword = findViewById(R.id.linearChangePassword)
        edtPasswordEmail = findViewById(R.id.edt_password_email)
        edtPasswordPass = findViewById(R.id.edt_password_pass)
        edtNewMail = findViewById(R.id.edt_new_mail)
        edtNewPass = findViewById(R.id.edt_new_pass)

        switchGroupNotifications = findViewById(R.id.switch_groupNotifications)
        switchIndividualNotifications = findViewById(R.id.switch_individualNotifications)

        linearNotifGrupales = findViewById(R.id.linear_notif_grupales)
        linearNotifIndividuales = findViewById(R.id.linear_notif_individuales)

        progress = ProgressDialog(this, R.style.AlertDialogApp)

        linearNotifGrupales.setOnClickListener {
            switchGroupNotifications.performClick()
        }

        linearNotifIndividuales.setOnClickListener {
            switchIndividualNotifications.performClick()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupNotificationSwitches() {
        val root = findViewById<View>(android.R.id.content)

        // Estado inicial desde prefs
        switchGroupNotifications.isChecked = UsuariosFragment.groupNotifications
        switchIndividualNotifications.isChecked = UsuariosFragment.individualNotifications

        // Grupales
        switchGroupNotifications.setOnClickListener {
            val enabled = switchGroupNotifications.isChecked
            UsuariosFragment.groupNotifications = enabled
            UsuariosFragment.editor.putBoolean("groupNotifications", enabled).apply()

            UserMessageUtils.showInfo(
                root,
                if (enabled)
                    "Notificaciones grupales encendidas"
                else
                    "Notificaciones grupales apagadas"
            )
        }

        // Individuales
        switchIndividualNotifications.setOnClickListener {
            val enabled = switchIndividualNotifications.isChecked
            UsuariosFragment.individualNotifications = enabled
            UsuariosFragment.editor.putBoolean("individualNotifications", enabled).apply()

            UserMessageUtils.showInfo(
                root,
                if (enabled)
                    "Notificaciones individuales encendidas"
                else
                    "Notificaciones individuales apagadas"
            )
        }
    }

    private fun setupAccountInfo() {
        // Detectar proveedor
        FirebaseAuth.getInstance().currentUser?.providerData?.forEach { info ->
            when (info.providerId) {
                "facebook.com" -> provider = "Facebook"
                "google.com" -> provider = "Google"
            }
        }

        val email = user.email.orEmpty()
        myEmail.text = if (provider == null) {
            email
        } else {
            "$email (${provider})"
        }

        btnReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        btnSaveEmail.isEnabled = false
        btnSavePass.isEnabled = false
    }

    private fun setupTextWatchers() {
        // Habilitar / deshabilitar botón guardar contraseña
        edtPasswordPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (edtPasswordPass.length() == 0) btnSavePass.isEnabled = false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSavePass.isEnabled =
                    edtPasswordPass.length() != 0 && edtNewPass.length() != 0
            }

            override fun afterTextChanged(s: Editable?) {
                if (edtPasswordPass.length() == 0) btnSavePass.isEnabled = false
            }
        })

        edtNewPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (edtNewPass.length() == 0) btnSavePass.isEnabled = false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSavePass.isEnabled =
                    edtNewPass.length() != 0 && edtPasswordPass.length() != 0
            }

            override fun afterTextChanged(s: Editable?) {
                if (edtNewPass.length() == 0) btnSavePass.isEnabled = false
            }
        })

        // Habilitar / deshabilitar botón guardar email
        edtPasswordEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (edtPasswordEmail.length() == 0) btnSaveEmail.isEnabled = false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSaveEmail.isEnabled =
                    edtPasswordEmail.length() != 0 && edtNewMail.length() != 0
            }

            override fun afterTextChanged(s: Editable?) {
                if (edtPasswordEmail.length() == 0) btnSaveEmail.isEnabled = false
            }
        })

        edtNewMail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (edtNewMail.length() == 0) btnSaveEmail.isEnabled = false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSaveEmail.isEnabled =
                    edtNewMail.length() != 0 && edtPasswordEmail.length() != 0
            }

            override fun afterTextChanged(s: Editable?) {
                if (edtNewMail.length() == 0) btnSaveEmail.isEnabled = false
            }
        })
    }

    private fun setupExpandables() {
        val root = findViewById<View>(android.R.id.content)

        btnChangeEmail.setOnClickListener {
            if (provider == null) {
                if (linearChangeEmail.isGone) {
                    linearChangeEmail.visibility = View.VISIBLE
                    arrowDownChangeEmail.visibility = View.GONE
                    arrowUpChangeEmail.visibility = View.VISIBLE
                } else {
                    linearChangeEmail.visibility = View.GONE
                    arrowDownChangeEmail.visibility = View.VISIBLE
                    arrowUpChangeEmail.visibility = View.GONE
                }
            } else {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "No disponible para usuarios autenticados con $provider",
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = "OK",
                    action = {}
                )
            }
        }

        btnChangePassword.setOnClickListener {
            if (provider == null) {
                if (!linearChangePassword.isVisible) {
                    linearChangePassword.isVisible = true
                    arrowDownChangePass.isVisible = false
                    arrowUpChangePass.isVisible = true
                } else {
                    linearChangePassword.isVisible = false
                    arrowDownChangePass.isVisible = true
                    arrowUpChangePass.isVisible = false
                }
            } else {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "No disponible para usuarios autenticados con $provider",
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = "OK",
                    action = {}
                )
            }
        }
    }

    private fun setupSaveButtons() {
        btnSaveEmail.setOnClickListener {
            password = edtPasswordEmail.text.toString().trim()
            newEmail = edtNewMail.text.toString().trim()
            checkData(password, newEmail, null)
        }

        btnSavePass.setOnClickListener {
            password = edtPasswordPass.text.toString().trim()
            newPassword = edtNewPass.text.toString().trim()
            checkData(password, null, newPassword)
        }
    }

    private fun setupDeleteAccount() {
        val root = findViewById<View>(android.R.id.content)

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
                .setTitle("Eliminar Cuenta")
                .setMessage("¿Está seguro de eliminar su cuenta?")
                .setCancelable(false)
                .setPositiveButton("Si") { _, _ ->
                    AlertDialog.Builder(
                        ContextThemeWrapper(this, R.style.AlertDialogApp)
                    )
                        .setTitle("Atención")
                        .setMessage(
                            "Si continúa se eliminarán todos sus datos personales, las fotos y las conversaciones. ¿Desea continuar?"
                        )
                        .setCancelable(false)
                        .setPositiveButton("Si") { _, _ ->
                            if (provider == null) {
                                val dialogView = LayoutInflater.from(this)
                                    .inflate(R.layout.dialog_setpassword, null)
                                val edtPassword =
                                    dialogView.findViewById<EditText>(R.id.edt_password)

                                val builder =
                                    AlertDialog.Builder(
                                        ContextThemeWrapper(
                                            this,
                                            R.style.AlertDialogApp
                                        )
                                    )
                                builder.setView(dialogView)
                                    .setTitle("Ingrese su contraseña para finalizar")
                                    .setCancelable(true)
                                    .setPositiveButton("Eliminar cuenta") { _, _ ->
                                        progress.setMessage("Espere...")
                                        progress.setCanceledOnTouchOutside(false)
                                        progress.show()
                                        getCredential(
                                            edtPassword.text.toString(),
                                            null,
                                            null,
                                            edtPassword.text.toString()
                                        )
                                    }
                                    .setNegativeButton("Cancelar", null)

                                val dialog = builder.create()

                                edtPassword.addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        count: Int,
                                        after: Int
                                    ) {
                                        if (edtPassword.length() == 0) {
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                ?.apply {
                                                    isEnabled = false
                                                    setTextColor(Color.GRAY)
                                                }
                                        }
                                    }

                                    override fun onTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        before: Int,
                                        count: Int
                                    ) {
                                        if (edtPassword.length() != 0) {
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                ?.apply {
                                                    isEnabled = true
                                                    setTextColor(Color.WHITE)
                                                }
                                        }
                                    }

                                    override fun afterTextChanged(s: Editable?) {
                                        if (edtPassword.length() == 0) {
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                ?.apply {
                                                    isEnabled = false
                                                    setTextColor(Color.GRAY)
                                                }
                                        }
                                    }
                                })

                                dialog.show()
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                                    isEnabled = false
                                    setTextColor(Color.GRAY)
                                }
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    ?.setTextColor(Color.WHITE)
                            } else {
                                progress.setMessage("Espere...")
                                progress.setCanceledOnTouchOutside(false)
                                progress.show()
                                getCredential(null, null, null, provider)
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun setupLogout() {
        btnLogout.setOnClickListener {
            AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogApp))
                .setTitle("Cerrar sesión")
                .setMessage("¿Está seguro de cerrar su sesión?")
                .setCancelable(false)
                .setPositiveButton("Si") { _, _ -> logOut(null) }
                .setNegativeButton("No", null)
                .show()
        }
    }

    // endregion

    // region Group / Logout

    fun exitGroup() {
        UsuariosFragment.inGroup = false

        // listeners globales definidos en MainActivity
        MainActivity.listenerGroupBadge?.let {
            refGroupChat.child(UsuariosFragment.groupName)
                .removeEventListener(it)
        }

        MainActivity.listenerMsgUnreadBadge?.let {
            val query: Query = refDatos.child(user.uid)
                .child(CHATWITHUNKNOWN)
                .orderByChild("noVisto")
                .startAt(1.0)
            query.removeEventListener(it)
        }

        PageAdapterGroup.valueEventListenerTitle?.let {
            refGroupUsers.child(UsuariosFragment.groupName)
                .removeEventListener(it)
        }

        ChatGroupFragment.listenerGroupChat?.let {
            refGroupChat.child(UsuariosFragment.groupName)
                .removeEventListener(it)
        }

        // Eliminar chats unknown vinculados
        refDatos.child(user.uid).child(CHATWITHUNKNOWN)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val key = snapshot.key ?: continue
                        refChatUnknown.child("${user.uid} <---> $key").removeValue()
                        refChatUnknown.child("$key <---> ${user.uid}").removeValue()
                        refDatos.child(key).child(CHATWITHUNKNOWN)
                            .child(user.uid)
                            .removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        refDatos.child(user.uid).child(CHATWITHUNKNOWN).removeValue()

        @SuppressLint("SimpleDateFormat")
        val dateFormat3 = SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SS")

        val chatmsg = ChatsGroup(
            "abandonó la sala",
            dateFormat3.format(Calendar.getInstance().time),
            UsuariosFragment.userName,
            user.uid,
            0,
            UsuariosFragment.userType
        )
        refGroupChat.child(UsuariosFragment.groupName).push().setValue(chatmsg)

        refGroupUsers.child(UsuariosFragment.groupName)
            .child(user.uid)
            .removeValue()

        // Reset estado local
        UsuariosFragment.inGroup = false
        UsuariosFragment.userName = ""
        UsuariosFragment.groupName = ""
        UsuariosFragment.userType = 2
        UsuariosFragment.readGroupMsg = 0
        UsuariosFragment.userDate = ""

        UsuariosFragment.editor.apply {
            putBoolean("inGroup", false)
            putString("userName", "")
            putString("groupName", "")
            putInt("userType", 2)
            putInt("readGroupMsg", 0)
            putString("userDate", "")
            apply()
        }

        MainActivity.layoutSettings?.visibility = View.GONE
        invalidateOptionsMenu()

        val newFragment = GruposFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, newFragment)
            .commit()

        toolbar.setTitle(R.string.menu_grupos)
    }

    fun logOut(deleteUser: String?) {
        if (deleteUser == null) {
            setUserOffline(applicationContext, user.uid)
        }

        if (UsuariosFragment.inGroup) {
            exitGroup()
        }

        MainActivity.listenerToken?.let {
            FirebaseRefs.refCuentas.child(user.uid)
                .child("installId")
                .removeEventListener(it)
        }

        UsuariosFragment.deletePreferences()
        EditProfileFragment.deleteProfilePreferences(this)

        FirebaseAuth.getInstance().signOut()
        LoginManager.getInstance().logOut()

        finish()
        val intent = Intent(applicationContext, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // endregion

    // region Auth helpers

    private fun getCredential(
        password: String?,
        newEmail: String?,
        newPassword: String?,
        deleteUser: String?
    ) {
        val credential: AuthCredential?

        when (provider) {
            null if password != null -> {
                credential = EmailAuthProvider.getCredential(user.email!!, password)
                reAuthenticate(newEmail, newPassword, deleteUser, credential)
            }
            "Facebook" -> {
                val token = AccessToken.getCurrentAccessToken()
                if (token != null) {
                    credential = FacebookAuthProvider.getCredential(token.token)
                    reAuthenticate(newEmail, newPassword, deleteUser, credential)
                }
            }
            "Google" -> {
                val acct = GoogleSignIn.getLastSignedInAccount(this)
                if (acct != null) {
                    credential = GoogleAuthProvider.getCredential(acct.idToken, null)
                    reAuthenticate(newEmail, newPassword, deleteUser, credential)
                }
            }
        }
    }

    private fun reAuthenticate(
        newEmail: String?,
        newPassword: String?,
        deleteUser: String?,
        credential: AuthCredential
    ) {
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    progress.dismiss()
                    if (provider == null) {
                        Toast.makeText(
                            this,
                            "La contraseña es incorrecta",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        AlertDialog.Builder(
                            ContextThemeWrapper(this, R.style.AlertDialogApp)
                        )
                            .setTitle("Error")
                            .setMessage("Se necesita un inicio de sesión reciente para eliminar la cuenta")
                            .setCancelable(false)
                            .setPositiveButton("Ok") { _, _ ->
                                val zibeAppPrefs =
                                    getSharedPreferences("ZibeAppPrefs", MODE_PRIVATE)
                                zibeAppPrefs.edit {
                                    putBoolean("deleteUser", true)
                                }
                                logOut(null)
                            }
                            .show()
                    }
                } else {
                    when {
                        newEmail != null -> updateEmail(newEmail)
                        newPassword != null -> updatePassword(newPassword)
                        deleteUser != null -> deleteFirebaseUser(deleteUser)
                    }
                }
            }
    }

    private fun deleteFirebaseUser(@Suppress("UNUSED_PARAMETER") deleteUser: String?) {
        val zibeAppPrefs = getSharedPreferences("ZibeAppPrefs", MODE_PRIVATE)
        zibeAppPrefs.edit {
            putBoolean("deleteFirebaseAccount", true)
        }

        refDatos.child(user.uid).removeValue()
        refCuentas.child(user.uid).removeValue()

        FirebaseStorage.getInstance().reference
            .child("Users/imgPerfil/${user.uid}.jpg")
            .delete()

        logOut(deleteUser)
        user.delete()
    }

    private fun updatePassword(newPassword: String) {
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                progress.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Datos actualizados correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "La contraseña debe tener al menos seis caracteres",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateEmail(newEmail: String) {
        user!!.updateEmail(newEmail)
            .addOnCompleteListener { task ->
                progress.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Datos actualizados correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Introduzca un e-mail válido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun checkData(password: String?, newEmail: String?, newPassword: String?) {
        if (password.isNullOrEmpty()) {
            Toast.makeText(this, "Introduzca una contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (newEmail != null && newEmail.isEmpty()) {
            Toast.makeText(this, "Introduzca un e-mail", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != null && newPassword.isEmpty()) {
            Toast.makeText(this, "Introduzca la nueva contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        progress.setMessage("Espere...")
        progress.setCanceledOnTouchOutside(false)
        progress.show()

        getCredential(password, newEmail, newPassword, null)
    }

    // endregion

    // region Lifecycle / Menu

    override fun onPause() {
        super.onPause()
        user?.uid?.let { setUserOffline(applicationContext, it) }
    }

    override fun onResume() {
        super.onResume()
        user?.uid?.let { setUserOnline(applicationContext, it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val RC_SIGN_IN = 1 // hoy no se usa, lo dejamos por compatibilidad
    }

    // endregion
}
