package com.android.argosapp.view.fragment

import android.os.Bundle
import android.content.pm.PackageManager
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.FragmentProfileEditBinding
import com.android.argosapp.model.User
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.util.validation.ProfileEditFormValidator
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.viewmodel.ProfileEditViewModel

class ProfileEditFragment : Fragment(R.layout.fragment_profile_edit) {
    private var _binding: FragmentProfileEditBinding? = null
    private var initialUser: User? = null
    private var initialUserLoadRequested = false
    private val formValidator = ProfileEditFormValidator()
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        openProfileLocationPicker()
    }

    private val authManager by lazy {
        AuthManager(
            (requireActivity().application as ArgosApplication).argosApiRepository,
            FirebaseRepository(),
            TokenStorage(requireContext()),
            UserStorage(requireContext()),
            MonthApplicationsStorage(requireContext()),
            ApplicationsStorage(requireContext()),
        )
    }
    private val viewModel: ProfileEditViewModel by viewModels {
        viewModelFactory { initializer { ProfileEditViewModel(authManager) } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileEditBinding.bind(view)

        _binding?.profileEditBack?.setOnClickListener(::onBackButtonClicked)
        _binding?.profileEditSubmitButton?.setOnClickListener(::onSubmitButtonClicked)
        _binding?.profileEditAddressLocationButton?.setOnClickListener {
            if (hasLocationPermission()) {
                openProfileLocationPicker()
            } else {
                locationPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
        parentFragmentManager.setFragmentResultListener(
            MapLocationPickerFragment.RESULT_KEY,
            this,
        ) { _, result ->
            result.getString(MapLocationPickerFragment.RESULT_ADDRESS)?.let { address ->
                _binding?.profileEditAddress?.setText(address)
            }
        }
        _binding?.apply {
            profileEditName.doAfterTextChanged { clearAttentionIcon(profileEditName) }
            profileEditEmail.doAfterTextChanged { clearAttentionIcon(profileEditEmail) }
            profileEditAddress.doAfterTextChanged { clearAttentionIcon(profileEditAddress) }
        }

        if (!initialUserLoadRequested) {
            initialUserLoadRequested = true
            viewModel.loadCachedUser(::populateFields, ::logUserLoadError)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun populateFields(user: User?) {
        initialUser = user
        _binding?.apply {
            profileEditName.setText(user?.userName.orEmpty())
            profileEditEmail.setText(user?.email.orEmpty())
            profileEditCpf.text = formatCpf(user?.cpf)
            profileEditAddress.setText(user?.address.orEmpty())
        }
    }

    private fun logUserLoadError(error: Exception) {
        AppLogger.error(AppLogger.SESSION_TAG, "profile_edit_user_read_failed", error)
    }

    private fun formatCpf(cpf: String?): String {
        val digits = cpf.orEmpty().filter { it.isDigit() }.take(11)
        return buildString {
            append(digits.take(3))
            if (digits.length > 3) append(".${digits.substring(3, minOf(6, digits.length))}")
            if (digits.length > 6) append(".${digits.substring(6, minOf(9, digits.length))}")
            if (digits.length > 9) append("-${digits.substring(9)}")
        }
    }

    private fun onSubmitButtonClicked(view: View) {
        val binding = _binding ?: return

        val name = binding.profileEditName.text.toString().trim()
        val email = binding.profileEditEmail.text.toString().trim()
        val address = binding.profileEditAddress.text.toString().trim()
        val originalUser = initialUser

        if (originalUser == null) {
            Toast.makeText(requireContext(), R.string.profile_edit_failed, Toast.LENGTH_LONG).show()
            return
        }

        clearAttentionIcon(binding.profileEditName)
        clearAttentionIcon(binding.profileEditEmail)
        clearAttentionIcon(binding.profileEditAddress)

        if (!formValidator.isValidName(name)) {
            showValidationError(binding.profileEditName, R.string.profile_edit_name_invalid)
            return
        }
        if (!formValidator.isValidEmail(email)) {
            showValidationError(binding.profileEditEmail, R.string.profile_edit_email_invalid)
            return
        }
        if (!formValidator.isValidAddress(address)) {
            showValidationError(binding.profileEditAddress, R.string.profile_edit_address_invalid)
            return
        }

        val nameChanged = name != originalUser.userName.trim()
        val emailChanged = email != originalUser.email.trim()
        val addressChanged = address != originalUser.address.orEmpty().trim()

        if (!nameChanged && !emailChanged && !addressChanged) {
            Toast.makeText(requireContext(), R.string.profile_edit_no_changes, Toast.LENGTH_LONG).show()
            return
        }
        viewModel.updateProfile(
            name = name.takeIf { nameChanged },
            email = email.takeIf { emailChanged },
            address = address.takeIf { addressChanged },
            onLoading = ::showSubmitLoading,
            onSuccess = {
                initialUser = originalUser.copy(userName = name, email = email, address = address)
                Toast.makeText(requireContext(), R.string.profile_edit_success, Toast.LENGTH_LONG).show()
            },
            onError = ::showUpdateError,
        )
    }

    private fun showSubmitLoading(loading: Boolean) {
        _binding?.apply {
            profileEditSubmitButton.isEnabled = !loading
            profileEditSubmitButton.isClickable = !loading
            profileEditSubmitButton.text = if (loading) "" else getString(R.string.profile_edit_submit)
            profileEditSubmitButton.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                if (loading) R.color.argos_border else R.color.argos_green,
            )
            profileEditLoadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showValidationError(field: EditText, messageRes: Int) {
        field.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0)
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }

    private fun clearAttentionIcon(field: EditText) {
        field.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
    }

    private fun showUpdateError(error: Exception) {
        AppLogger.error(AppLogger.SESSION_TAG, "profile_edit_update_failed", error)
        Toast.makeText(requireContext(), R.string.profile_edit_failed, Toast.LENGTH_LONG).show()
    }

    private fun hasLocationPermission(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openProfileLocationPicker() {
        (activity as? HomeActivity)?.showProfileLocationPicker(
            _binding?.profileEditAddress?.text?.toString().orEmpty(),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onBackButtonClicked(view: View) {
        FragmentNavHelper(parentFragmentManager).popBackStack()
    }

    companion object {
        const val TAG = "profile_edit"
    }
}
