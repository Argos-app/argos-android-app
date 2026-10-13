package com.android.argosapp.view.fragment

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.FragmentProfileBinding
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.view.activity.LoginActivity
import com.android.argosapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
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
    private val viewModel: ProfileViewModel by viewModels {
        viewModelFactory { initializer { ProfileViewModel(authManager) } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        _binding?.profileEditOption?.setOnClickListener(::onEditOptionClicked)
        _binding?.profileLogoutButton?.setOnClickListener(::onLogoutButtonClicked)

        loadUserDetails()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadUserDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = UserStorage(requireContext()).getUser()
                val unknownValue = getString(R.string.home_calendar_unknown_value)

                _binding?.profileUserName?.text = user?.userName?: unknownValue
                _binding?.profileFarmName?.text = user?.farmName?: unknownValue

            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                AppLogger.error(AppLogger.SESSION_TAG, "profile_user_read_failed", error)

            }
        }
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(requireContext(), LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            ),
        )
        requireActivity().finish()
    }

    private fun onEditOptionClicked(view: View) {
        (activity as? HomeActivity)?.showProfileEdit()
    }

    private fun onLogoutButtonClicked(view: View) {
        viewModel.logout(
            onComplete = ::navigateToLogin,
            onError = ::showLogoutError,
        )
    }

    private fun showLogoutError(error: Exception) {
        AppLogger.error(AppLogger.SESSION_TAG, "profile_logout_failed", error)
        Toast.makeText(requireContext(), R.string.profile_logout_failed, Toast.LENGTH_LONG).show()
    }
}
