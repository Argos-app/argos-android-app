package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.databinding.FragmentProfileBinding
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

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
}
