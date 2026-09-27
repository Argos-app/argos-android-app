package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.argosapp.databinding.FragmentResetRequestBinding
import com.android.argosapp.view.activity.PasswordResetActivity

class ResetRequestFragment : Fragment() {
    private lateinit var binding: FragmentResetRequestBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResetRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        sendCodeButton.setOnClickListener { (requireActivity() as PasswordResetActivity).requestEmail(emailInput.text.toString()) }
        backButton.setOnClickListener { (requireActivity() as PasswordResetActivity).goBack() }
    }

}
