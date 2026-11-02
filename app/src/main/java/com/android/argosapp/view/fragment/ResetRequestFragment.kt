package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentResetRequestBinding
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.view.activity.PasswordResetActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ResetRequestFragment : Fragment() {
    private lateinit var binding: FragmentResetRequestBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResetRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        val host = requireActivity() as PasswordResetActivity
        sendCodeButton.setOnClickListener { host.requestEmail(emailInput.text.toString()) }
        backButton.setOnClickListener { host.goBack() }

        if (host.isProfileFlow) {
            emailInput.isEnabled = false
            emailInput.setTextColor(ContextCompat.getColor(requireContext(), R.color.argos_muted))
            sendCodeButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    emailInput.setText(host.viewModel.getCurrentUserEmail())
                    sendCodeButton.isEnabled = true

                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLogger.error(AppLogger.RESET_TAG, "password_reset_user_email_load_failed", error)
                    Toast.makeText(requireContext(), R.string.reset_user_email_unavailable, Toast.LENGTH_LONG).show()

                }
            }
        }
    }

}
