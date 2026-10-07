package com.android.argosapp.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.ActivityFirstAccessChangePasswordBinding
import com.android.argosapp.exception.auth.messageRes
import com.android.argosapp.viewmodel.FirstAccessChangePasswordViewModel
import com.android.argosapp.util.validation.PasswordPatternValidator
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class FirstAccessChangePasswordActivity : ComponentActivity() {
    private lateinit var binding: ActivityFirstAccessChangePasswordBinding
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
    private val viewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FirstAccessChangePasswordViewModel(authManager) as T
        })[FirstAccessChangePasswordViewModel::class.java]
    }
    private val passwordValidator = PasswordPatternValidator()
    private var newPasswordVisible = false
    private var confirmationVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstAccessChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.newPasswordVisibilityButton.setOnClickListener { toggleNewPasswordVisibility() }
        binding.passwordConfirmationVisibilityButton.setOnClickListener { toggleConfirmationVisibility() }
        binding.changePasswordButton.setOnClickListener { changePassword() }
        binding.backToLoginLink.setOnClickListener { returnToLogin() }
        binding.authSubtitle.setText(R.string.first_access_welcome_fallback)
        lifecycleScope.launch {
            try {
                val userName = UserStorage(applicationContext).getUser()?.userName.orEmpty()
                if (userName.isNotBlank()) {
                    binding.authSubtitle.text = getString(
                        R.string.first_access_welcome,
                        userName.substringBefore(" "),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.SESSION_TAG, "first_access_user_read_failed", error)
            }
        }
        binding.loadingMessage.text = getString(R.string.loading_changing_password)
        binding.newPasswordInput.doAfterTextChanged { updatePasswordCriteria(it?.toString().orEmpty()) }
        updatePasswordCriteria(binding.newPasswordInput.text?.toString().orEmpty())
    }


    private fun toggleNewPasswordVisibility() = with(binding) {
        newPasswordVisible = !newPasswordVisible
        newPasswordInput.inputType = passwordInputType(newPasswordVisible)
        newPasswordVisibilityButton.setImageResource(if (newPasswordVisible) R.drawable.open_eye_icon else R.drawable.closed_eye_icon)
    }

    private fun toggleConfirmationVisibility() = with(binding) {
        confirmationVisible = !confirmationVisible
        passwordConfirmationInput.inputType = passwordInputType(confirmationVisible)
        passwordConfirmationVisibilityButton.setImageResource(if (confirmationVisible) R.drawable.open_eye_icon else R.drawable.closed_eye_icon)
    }

    private fun passwordInputType(visible: Boolean) = if (visible) InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    private fun updatePasswordCriteria(password: String) {
        val validation = passwordValidator.validatePassword(password)
        with(binding) {
            updateCriterion(criteriaLengthIcon, validation.getValue("length"))
            updateCriterion(criteriaCapitalIcon, validation.getValue("minimalCapital"))
            updateCriterion(criteriaLowercaseIcon, validation.getValue("minimalNotCapital"))
            updateCriterion(criteriaNumericIcon, validation.getValue("minimalNumeric"))
            updateCriterion(criteriaSpecialIcon, validation.getValue("minimalSpecial"))
        }
    }

    private fun updateCriterion(icon: ImageView, isMet: Boolean) {
        icon.setImageResource(if (isMet) R.drawable.positive_icon else R.drawable.negative_icon)
        icon.contentDescription = getString(
            if (isMet) R.string.password_criterion_met else R.string.password_criterion_pending,
        )
    }

    private fun returnToLogin() {
        lifecycleScope.launch {
            authManager.clearSession()
            authManager.signOut()
            startActivity(Intent(this@FirstAccessChangePasswordActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun changePassword() = viewModel.changePassword(
        binding.newPasswordInput.text.toString(),
        binding.passwordConfirmationInput.text.toString(),
        ::showLoading,
        { Toast.makeText(this, getString(it.messageRes()), Toast.LENGTH_LONG).show() },
        { startActivity(Intent(this, HomeActivity::class.java)); finish() },
    )

    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
