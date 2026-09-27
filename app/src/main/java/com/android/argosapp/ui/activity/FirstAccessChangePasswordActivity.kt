package com.android.argosapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged
import com.android.argosapp.R
import com.android.argosapp.auth.manager.AuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.data.api.FirebaseRepository
import com.android.argosapp.databinding.ActivityFirstAccessChangePasswordBinding
import com.android.argosapp.ui.error.messageRes
import com.android.argosapp.ui.viewmodel.FirstAccessChangePasswordViewModel
import com.android.argosapp.util.PasswordPatternValidator

class FirstAccessChangePasswordActivity : ComponentActivity() {
    private lateinit var binding: ActivityFirstAccessChangePasswordBinding
    private val authManager by lazy {
        AuthManager(
            ArgosApiRepository(homologation = true),
            FirebaseRepository(),
            UserSessionStorage(applicationContext),
        )
    }
    private val viewModel by lazy { FirstAccessChangePasswordViewModel(authManager) }
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
        binding.authSubtitle.text = getString(R.string.first_access_welcome, viewModel.userName.substringBefore(" "))
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
        authManager.clearSession()
        authManager.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun changePassword() = viewModel.changePassword(
        binding.newPasswordInput.text.toString(),
        binding.passwordConfirmationInput.text.toString(),
        ::showLoading,
        { android.widget.Toast.makeText(this, getString(it.messageRes()), android.widget.Toast.LENGTH_LONG).show() },
        { startActivity(Intent(this, HomeActivity::class.java)); finish() },
    )

    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
