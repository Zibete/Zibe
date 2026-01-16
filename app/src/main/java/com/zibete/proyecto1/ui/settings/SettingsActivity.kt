package com.zibete.proyecto1.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.databinding.ActivitySettingsBinding
import com.zibete.proyecto1.ui.base.BaseEdgeToEdgeActivity
import com.zibete.proyecto1.ui.report.ReportActivity
import com.zibete.proyecto1.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : BaseEdgeToEdgeActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var binding: ActivitySettingsBinding

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
                email = edtNewMail.text?.toString().orEmpty()
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
                            UserMessageUtils.showSnack(
                                root = binding.root,
                                message = event.uiText.asString(this@SettingsActivity)
                            )
                        }

                        is SettingsUiEvent.NavigateToSplash -> {
                            startActivity(Intent(applicationContext, SplashActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK }
                            )
                            finish()
                        }
                    }
                }
            }
        }
    }

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
        val title = UiText.StringRes(R.string.dialog_logout_title).asString(this@SettingsActivity)
        val message = UiText.StringRes(R.string.dialog_logout_message).asString(this@SettingsActivity)
        val actionYes = UiText.StringRes(R.string.action_yes).asString(this@SettingsActivity)

        UserMessageUtils.confirm(
            context = this@SettingsActivity,
            title = title,
            message = message,
            positiveText = actionYes,
            onConfirm = { viewModel.onLogoutRequested() }
        )
    }

    private fun showDeleteAccountDialogFlow() {

        val title = UiText.StringRes(R.string.dialog_delete_account_title).asString(this@SettingsActivity)
        val message = UiText.StringRes(R.string.dialog_delete_account_message).asString(this@SettingsActivity)
        val actionYes = UiText.StringRes(R.string.action_yes).asString(this@SettingsActivity)

        UserMessageUtils.confirm(
            context = this,
            title = title,
            message = message,
            positiveText = actionYes,
            onConfirm = {

                val attentionTitle = UiText.StringRes(R.string.attention_title).asString(this@SettingsActivity)
                val finalMessage = UiText.StringRes(R.string.dialog_delete_account_final_message).asString(this@SettingsActivity)
                val deleteAccount = UiText.StringRes(R.string.action_delete_account).asString(this@SettingsActivity)

                UserMessageUtils.confirm(
                    context = this,
                    title = attentionTitle,
                    message = finalMessage,
                    positiveText = actionYes,
                    negativeText = deleteAccount,
                    onConfirm = {
                        if (latestState.requiresPasswordForSensitiveActions) {
//                            showPasswordDialogForDelete()
                        } else {
                            viewModel.deleteAccount(passwordIfNeeded = null)
                        }
                    }
                )
            }
        )
    }

//    private fun showPasswordDialogForDelete() {
//        val deleteAccount = UiText.StringRes(R.string.action_delete_account).asString(this@SettingsActivity)
//        val actionCancel = UiText.StringRes(R.string.action_cancel).asString(this@SettingsActivity)
//        val enterYourPassword = UiText.StringRes(R.string.enter_your_password).asString(this@SettingsActivity)
//
//        val dialogBinding = DialogSetpasswordBinding.inflate(layoutInflater)
//
//        val alert = MaterialAlertDialogBuilder(this)
//            .setTitle(enterYourPassword)
//            .setView(dialogBinding.root)
//            .setPositiveButton(deleteAccount, null)
//            .setNegativeButton(actionCancel, null)
//            .create()
//
//        alert.setOnShowListener {
//            val btn = alert.getButton(AlertDialog.BUTTON_POSITIVE)
//            btn.isEnabled = false
//
//            dialogBinding.edtPassword.doOnTextChanged { text, _, _, _ ->
//                btn.isEnabled = !text.isNullOrBlank()
//            }
//
//            btn.setOnClickListener {
//                viewModel.deleteAccount(passwordIfNeeded = dialogBinding.edtPassword.text?.toString())
//                alert.dismiss()
//            }
//        }
//
//        alert.show()
//    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}