package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentProfileEditBinding
import com.android.argosapp.model.User
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.viewmodel.ProfileEditViewModel

class ProfileEditFragment : Fragment(R.layout.fragment_profile_edit) {
    private var _binding: FragmentProfileEditBinding? = null
    private val viewModel: ProfileEditViewModel by viewModels {
        viewModelFactory { initializer { ProfileEditViewModel(UserStorage(requireContext())) } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileEditBinding.bind(view)

        _binding?.profileEditBack?.setOnClickListener(::onBackButtonClicked)

        viewModel.loadCachedUser(::populateFields, ::logUserLoadError)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun populateFields(user: User?) {
        _binding?.apply {
            profileEditName.setText(user?.userName.orEmpty())
            profileEditEmail.setText(user?.email.orEmpty())
            profileEditCpf.setText(user?.cpf.orEmpty())
            profileEditAddress.setText(user?.address.orEmpty())
        }
    }

    private fun logUserLoadError(error: Exception) {
        AppLogger.error(AppLogger.SESSION_TAG, "profile_edit_user_read_failed", error)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onBackButtonClicked(view: View) {
        FragmentNavHelper(parentFragmentManager).popBackStack()
    }

    companion object {
        const val TAG = "profile_edit"
    }
}
