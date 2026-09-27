package com.android.argosapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.argosapp.databinding.FragmentResetSuccessBinding
import com.android.argosapp.ui.activity.PasswordResetActivity

class PasswordChangedFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentResetSuccessBinding.inflate(inflater, container, false)
        binding.goToLoginButton.setOnClickListener { (requireActivity() as PasswordResetActivity).navigateToLogin() }
        return binding.root
    }
}
