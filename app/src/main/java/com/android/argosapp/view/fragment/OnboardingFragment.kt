package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.databinding.FragmentOnboardingBinding
import com.android.argosapp.util.session.OnboardingProgressStorage
import com.android.argosapp.view.activity.HomeActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val progressStorage by lazy { OnboardingProgressStorage(requireContext().applicationContext) }
    private var step = 0
    private var userEmail = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        step = savedInstanceState?.getInt(STATE_STEP, 0)?.coerceIn(0, LAST_STEP) ?: 0
        renderStep()
        binding.onboardingNextButton.setOnClickListener {
            if (step < LAST_STEP) {
                step++
                renderStep()
            } else {
                completeOnboarding()
            }
        }
        binding.onboardingLaterButton.setOnClickListener { leaveForLater() }
        binding.onboardingStepOne.setOnClickListener { showStep(0) }
        binding.onboardingStepTwo.setOnClickListener { showStep(1) }
        binding.onboardingStepThree.setOnClickListener { showStep(2) }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                userEmail = UserStorage(requireContext().applicationContext).getUser()?.userEmail.orEmpty()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                userEmail = ""
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_STEP, step)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showStep(newStep: Int) {
        step = newStep.coerceIn(0, LAST_STEP)
        renderStep()
    }

    private fun renderStep() = with(binding) {
        onboardingBackdrop.showStep(step)
        onboardingIllustration.setImageResource(
            when (step) {
                0 -> R.drawable.onboarding_step_welcome_tobby
                1 -> R.drawable.onboarding_step_features_tobby
                else -> R.drawable.onboarding_step_ready_tobby
            },
        )
        onboardingIllustration.contentDescription = getString(
            when (step) {
                0 -> R.string.onboarding_art_welcome
                1 -> R.string.onboarding_art_features
                else -> R.string.onboarding_art_ready
            },
        )
        onboardingTitle.setText(
            when (step) {
                0 -> R.string.onboarding_title_welcome
                1 -> R.string.onboarding_title_features
                else -> R.string.onboarding_title_ready
            },
        )
        onboardingDescription.setText(
            when (step) {
                0 -> R.string.onboarding_description_welcome
                1 -> R.string.onboarding_description_features
                else -> R.string.onboarding_description_ready
            },
        )
        onboardingNextButton.setText(if (step == LAST_STEP) R.string.onboarding_register_lot else R.string.onboarding_next)
        onboardingLaterButton.visibility = if (step == LAST_STEP) View.VISIBLE else View.GONE
        updateIndicator(onboardingStepOne, onboardingStepOneDot, 0)
        updateIndicator(onboardingStepTwo, onboardingStepTwoDot, 1)
        updateIndicator(onboardingStepThree, onboardingStepThreeDot, 2)
    }

    private fun updateIndicator(
        indicator: FrameLayout,
        dot: View,
        indicatorStep: Int,
    ) {
        val selected = step == indicatorStep
        dot.setBackgroundResource(
            if (selected) R.drawable.onboarding_step_indicator_selected
            else R.drawable.onboarding_step_indicator_inactive,
        )
        indicator.contentDescription = getString(
            if (selected) R.string.onboarding_step_selected else R.string.onboarding_step_unselected,
            indicatorStep + 1,
        )
        indicator.isSelected = selected
    }

    private fun completeOnboarding() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (userEmail.isBlank()) {
                userEmail = try {
                    UserStorage(requireContext().applicationContext).getUser()?.userEmail.orEmpty()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    ""
                }
            }
            if (userEmail.isNotBlank()) progressStorage.markCompleted(userEmail)
            closeOverlay()
        }
    }

    private fun leaveForLater() = closeOverlay()

    private fun closeOverlay() {
        (activity as? HomeActivity)?.closeOnboardingOverlay(this)
    }

    private companion object {
        const val LAST_STEP = 2
        const val STATE_STEP = "onboarding_step"
    }
}
