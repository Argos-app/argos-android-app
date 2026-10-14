package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentResetSuccessBinding
import com.android.argosapp.view.activity.PasswordResetActivity

class PasswordChangedFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentResetSuccessBinding.inflate(inflater, container, false)
        val host = requireActivity() as PasswordResetActivity

        if (host.isProfileFlow) {
            binding.successMessage.setText(R.string.profile_password_changed)
            binding.goToLoginButton.setText(R.string.profile_password_changed_action)
        }

        binding.goToLoginButton.setOnClickListener { host.finishResetFlow() }
        return binding.root
    }
}
