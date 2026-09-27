package com.android.argosapp.ui.fragment

import android.os.Bundle
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.argosapp.databinding.FragmentResetEmailSentBinding
import com.android.argosapp.R
import com.android.argosapp.ui.activity.PasswordResetActivity

class ResetEmailSentFragment : Fragment() {
    private lateinit var binding: FragmentResetEmailSentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResetEmailSentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        val host = requireActivity() as PasswordResetActivity
        openEmailButton.setOnClickListener { (requireActivity() as PasswordResetActivity).openEmailApp() }
        resendLink.setOnClickListener { host.resendEmail() }
        backButton.setOnClickListener { host.goBack() }
        resendLink.paintFlags = resendLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        host.refreshResendCountdown()
    }

    fun updateResendCountdown(secondsRemaining: Int) = with(binding.resendLink) {
        val canResend = secondsRemaining == 0
        text = getString(R.string.resend_email_countdown, secondsRemaining / 60, secondsRemaining % 60)
        isEnabled = canResend
        isClickable = canResend
        setTextColor(
            requireContext().getColor(
                if (canResend) R.color.argos_green else R.color.argos_loading_track,
            ),
        )
    }

}
