package com.zibete.proyecto1.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.databinding.ActivitySettingsBinding
import com.zibete.proyecto1.databinding.DialogSetpasswordBinding
import com.zibete.proyecto1.core.constants.DIALOG_CANCEL
import com.zibete.proyecto1.ui.report.ReportActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var binding: ActivitySettingsBinding
    private var progressDialog: AlertDialog? = null

    private var latestState: SettingsUiState = SettingsUiState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClicks()
        setupTextWatchers()
        observeState()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroy()
    }

    // -------------------------
    // Setup
    // -------------------------

    private fun setupToolbar() {
        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
        binding.materialToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClicks() = with(binding) {
        // Expand/collapse: decide el VM
        btnChangeEmail.setOnClickListener { viewModel.onChangeEmailHeaderClicked() }
        btnChangePassword.setOnClickListener { viewModel.onChangePasswordHeaderClicked() }

        // Switches: decide el VM
        linearNotifGrupales.setOnClickListener { switchGroupNotifications.performClick() }
        linearNotifIndividuales.setOnClickListener { switchIndividualNotifications.performClick() }

        switchGroupNotifications.setOnClickListener {
            viewModel.onGroupNotificationsToggled(switchGroupNotifications.isChecked)
        }
        switchIndividualNotifications.setOnClickListener {
            viewModel.onIndividualNotificationsToggled(switchIndividualNotifications.isChecked)
        }

        // Guardar: VM
        btnSaveEmail.setOnClickListener {
            viewModel.updateEmail(
                password = edtPasswordEmail.text?.toString().orEmpty(),
                newEmail = edtNewMail.text?.toString().orEmpty()
            )
        }
        btnSavePass.setOnClickListener {
            viewModel.updatePassword(
                password = edtPasswordPass.text?.toString().orEmpty(),
                newPassword = edtNewPass.text?.toString().orEmpty()
            )
        }

        btnReport.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, ReportActivity::class.java))
        }

        btnLogout.setOnClickListener { showLogoutDialog() }
        btnDeleteAccount.setOnClickListener { showDeleteAccountDialogFlow() }
    }

    private fun setupTextWatchers() = with(binding) {
        btnSavePass.isEnabled = false
        btnSaveEmail.isEnabled = false

        edtNewPass.doOnTextChanged { text, _, _, _ ->
            btnSavePass.isEnabled = (text?.length ?: 0) >= 6
        }

        val refreshEmailButtonState = {
            val pass = edtPasswordEmail.text?.toString().orEmpty()
            val mail = edtNewMail.text?.toString().orEmpty()
            btnSaveEmail.isEnabled = pass.isNotBlank() && mail.isNotBlank() && mail.contains("@")
        }

        edtPasswordEmail.doOnTextChanged { _, _, _, _ -> refreshEmailButtonState() }
        edtNewMail.doOnTextChanged { _, _, _, _ -> refreshEmailButtonState() }
    }

    // -------------------------
    // Observers
    // -------------------------

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    latestState = state

                    binding.myEmail.text = state.emailDisplay

                    binding.switchGroupNotifications.isChecked = state.groupNotificationsEnabled
                    binding.switchIndividualNotifications.isChecked = state.individualNotificationsEnabled

                    setEmailSectionExpanded(state.isEmailSectionExpanded)
                    setPasswordSectionExpanded(state.isPasswordSectionExpanded)
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {

                        is SettingsUiEvent.ShowSnack -> {
                            val snack = Snackbar.make(
                                binding.root,
                                event.message,
                                Snackbar.LENGTH_LONG
                            )
                            snack.show()
                        }

                        is SettingsUiEvent.ShowProgress -> showProgressDialog(event.message)

                        is SettingsUiEvent.HideProgress -> hideProgressDialog()

                        is SettingsUiEvent.NavigateToSplash -> {
                            startActivity(Intent(applicationContext, SplashActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK }
                            )
                            if (event.finish) finish()
                        }
                    }
                }
            }
        }
    }

    // -------------------------
    // UI helpers
    // -------------------------

    private fun setEmailSectionExpanded(expanded: Boolean) = with(binding) {
        linearChangeEmail.isVisible = expanded
        arrowUpChangeEmail.isVisible = expanded
        arrowDownChangeEmail.isGone = expanded
    }

    private fun setPasswordSectionExpanded(expanded: Boolean) = with(binding) {
        linearChangePassword.isVisible = expanded
        arrowUpChangePass.isVisible = expanded
        arrowDownChangePass.isGone = expanded
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> viewModel.onLogoutRequested() }
            .setNegativeButton(DIALOG_CANCEL, null)
            .show()
    }

    private fun showDeleteAccountDialogFlow() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Seguro quiere eliminar su cuenta?")
            .setMessage("Esta acción eliminará toda la información y no se podrá recuperar")
            .setPositiveButton("Sí") { _, _ ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Atención")
                    .setMessage("Si elimina la cuenta, no podrá recuperar sus datos")
                    .setPositiveButton("Eliminar cuenta") { _, _ ->
                        if (latestState.requiresPasswordForSensitiveActions) {
                            showPasswordDialogForDelete()
                        } else {
                            viewModel.deleteAccount(passwordIfNeeded = null)
                        }
                    }
                    .setNegativeButton(DIALOG_CANCEL, null)
                    .show()
            }
            .setNegativeButton(DIALOG_CANCEL, null)
            .show()
    }

    private fun showPasswordDialogForDelete() {
        val dialogBinding = DialogSetpasswordBinding.inflate(layoutInflater)

        val alert = MaterialAlertDialogBuilder(this)
            .setTitle("Introduzca su contraseña")
            .setView(dialogBinding.root)
            .setPositiveButton("Eliminar cuenta", null)
            .setNegativeButton(DIALOG_CANCEL, null)
            .create()

        alert.setOnShowListener {
            val btn = alert.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.isEnabled = false

            dialogBinding.edtPassword.doOnTextChanged { text, _, _, _ ->
                btn.isEnabled = !text.isNullOrBlank()
            }

            btn.setOnClickListener {
                viewModel.deleteAccount(passwordIfNeeded = dialogBinding.edtPassword.text?.toString())
                alert.dismiss()
            }
        }

        alert.show()
    }

    private fun showProgressDialog(message: String) {
        if (progressDialog?.isShowing == true) return

        val padding = dp(20)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val indicator = CircularProgressIndicator(this).apply {
            isIndeterminate = true
        }

        val tv = TextView(this).apply {
            setPadding(dp(16), 0, 0, 0)
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        container.addView(indicator)
        container.addView(tv)

        progressDialog = MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .setView(container)
            .create()

        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
