package com.android.argosapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.ComponentActivity
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.auth.manager.AuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.data.api.FirebaseRepository
import com.android.argosapp.databinding.ActivityLoginBinding
import com.android.argosapp.ui.error.messageRes
import com.android.argosapp.ui.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authManager by lazy {
        AuthManager(
            (application as ArgosApplication).argosApiRepository,
            FirebaseRepository(),
            UserSessionStorage(applicationContext),
        )
    }
    private val viewModel by lazy { LoginViewModel(this, authManager) }
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.authSubtitle.text = "O aplicativo que gere, cuida e organiza para você."
        binding.loadingMessage.text = getString(R.string.loading_authenticating)
        binding.passwordVisibilityButton.setOnClickListener { togglePasswordVisibility() }
        binding.loginButton.setOnClickListener { login() }
        binding.forgotPasswordLink.setOnClickListener { startActivity(Intent(this, PasswordResetActivity::class.java)) }
        binding.googleButton.setOnClickListener {
            viewModel.startGoogleLogin(getString(R.string.default_web_client_id), binding.rememberMeCheck.isChecked, ::showLoading, ::showError, ::navigateAfterAuthentication)
        }
        viewModel.restoreSession(::showLoading) { navigateAfterAuthentication(false) }
    }

    private fun togglePasswordVisibility() = with(binding) {
        passwordVisible = !passwordVisible
        passwordInput.inputType = if (passwordVisible) InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordVisibilityButton.setImageResource(if (passwordVisible) R.drawable.open_eye_icon else R.drawable.closed_eye_icon)
    }

    private fun navigateAfterAuthentication(firstAccess: Boolean) {
        startActivity(Intent(this, if (firstAccess) FirstAccessChangePasswordActivity::class.java else HomeActivity::class.java))
        finish()
    }

    private fun login() = viewModel.login(
        binding.emailInput.text.toString(),
        binding.passwordInput.text.toString(),
        binding.rememberMeCheck.isChecked,
        ::showLoading,
        ::showError,
        ::navigateAfterAuthentication,
    )

    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(code: com.android.argosapp.auth.error.AuthErrorCode) {
        android.widget.Toast.makeText(this, getString(code.messageRes()), android.widget.Toast.LENGTH_LONG).show()
    }
}
