package com.android.argosapp.view.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.exception.auth.AuthErrorCode
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.ActivityLoginBinding
import com.android.argosapp.exception.auth.messageRes
import com.android.argosapp.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authManager by lazy {
        AuthManager(
            (application as ArgosApplication).argosApiRepository,
            FirebaseRepository(),
            TokenStorage(applicationContext),
            UserStorage(applicationContext),
            MonthApplicationsStorage(applicationContext),
            ApplicationsStorage(applicationContext),
        )
    }
    private val viewModel: LoginViewModel by viewModels {
        viewModelFactory { initializer { LoginViewModel(authManager) } }
    }
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
        binding.googleButton.setOnClickListener(::onGoogleButtonClicked)
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

    private fun showGoogleLoading(loading: Boolean) {
        binding.googleButton.isEnabled = !loading
        binding.googleButton.isClickable = !loading
        binding.googleLoadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.googleButton.text = ""
            binding.googleButton.icon = null
            binding.googleButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.argos_border),
            )
        } else {
            binding.googleButton.setText(R.string.auth_google_login)
            binding.googleButton.setIconResource(R.drawable.google_icon)
            binding.googleButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.argos_transparent),
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onGoogleButtonClicked(view: View) {
        viewModel.startGoogleLogin(
            this,
            getString(R.string.default_web_client_id),
            binding.rememberMeCheck.isChecked,
            ::showGoogleLoading,
            ::showError,
            ::navigateAfterAuthentication,
        )
    }

    private fun showError(code: AuthErrorCode) {
        Toast.makeText(this, getString(code.messageRes()), Toast.LENGTH_LONG).show()
    }
}
