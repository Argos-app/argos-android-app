package com.android.argosapp.ui.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.core.widget.doAfterTextChanged
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentResetChangePasswordBinding
import com.android.argosapp.ui.activity.PasswordResetActivity
import com.android.argosapp.util.PasswordPatternValidator

class ResetChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentResetChangePasswordBinding
    private var newPasswordVisible = false
    private var confirmationVisible = false
    private val passwordValidator = PasswordPatternValidator()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResetChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        newPasswordVisibilityButton.setOnClickListener { toggleNewPasswordVisibility() }
        passwordConfirmationVisibilityButton.setOnClickListener { toggleConfirmationVisibility() }
        changePasswordButton.setOnClickListener { (requireActivity() as PasswordResetActivity).changePassword(newPasswordInput.text.toString(), passwordConfirmationInput.text.toString()) }
        backButton.setOnClickListener { (requireActivity() as PasswordResetActivity).goBack() }
        newPasswordInput.doAfterTextChanged { updatePasswordCriteria(it?.toString().orEmpty()) }
        updatePasswordCriteria(newPasswordInput.text?.toString().orEmpty())
    }

    private fun toggleNewPasswordVisibility() = with(binding) {
        newPasswordVisible = !newPasswordVisible
        newPasswordInput.inputType = inputType(newPasswordVisible)
        newPasswordVisibilityButton.setImageResource(if (newPasswordVisible) R.drawable.open_eye_icon else R.drawable.closed_eye_icon)
    }

    private fun toggleConfirmationVisibility() = with(binding) {
        confirmationVisible = !confirmationVisible
        passwordConfirmationInput.inputType = inputType(confirmationVisible)
        passwordConfirmationVisibilityButton.setImageResource(if (confirmationVisible) R.drawable.open_eye_icon else R.drawable.closed_eye_icon)
    }

    private fun inputType(visible: Boolean) = if (visible) InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

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
}
