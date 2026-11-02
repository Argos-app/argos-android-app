package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.argosapp.viewmodel.HomeViewModel
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentOnboardingBinding
import com.android.argosapp.view.activity.HomeActivity

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: HomeViewModel by activityViewModels()
    private var step = 0

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
        step = savedInstanceState?.getInt(STATE_STEP, FIRST_STEP)?.coerceIn(FIRST_STEP, LAST_STEP)
            ?: FIRST_STEP
        renderStep()
        binding.onboardingNextButton.setOnClickListener {
            if (step < LAST_STEP) {
                showStep(step + 1)
            } else {
                completeOnboarding()
            }
        }
        binding.onboardingLaterButton.setOnClickListener { leaveForLater() }
        binding.onboardingStepOne.setOnClickListener { showStep(0) }
        binding.onboardingStepTwo.setOnClickListener { showStep(1) }
        binding.onboardingStepThree.setOnClickListener { showStep(2) }
    }

    private fun showStep(newStep: Int) {
        step = newStep.coerceIn(FIRST_STEP, LAST_STEP)
        renderStep()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_STEP, step)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderStep() = with(binding) {
        val content = STEP_CONTENTS[step]
        onboardingIllustration.setImageResource(content.illustration)
        onboardingIllustration.scaleX = content.scale
        onboardingIllustration.scaleY = content.scale
        onboardingIllustration.contentDescription = getString(content.illustrationDescription)
        onboardingTitle.setText(content.title)
        onboardingDescription.setText(content.description)
        onboardingNextButton.setText(
            if (step == LAST_STEP) R.string.onboarding_complete else R.string.onboarding_next,
        )
        onboardingLaterButton.visibility = if (step == LAST_STEP) View.VISIBLE else View.GONE
        updateIndicator(onboardingStepOne, onboardingStepOneDot, 0)
        updateIndicator(onboardingStepTwo, onboardingStepTwoDot, 1)
        updateIndicator(onboardingStepThree, onboardingStepThreeDot, 2)
    }

    private data class StepContent(
        val illustration: Int,
        val illustrationDescription: Int,
        val title: Int,
        val description: Int,
        val scale: Float,
    )

    private fun updateIndicator(
        indicator: FrameLayout,
        dot: View,
        indicatorStep: Int,
    ) {
        val selected = step == indicatorStep
        dot.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(androidx.core.content.ContextCompat.getColor(requireContext(),
                if (selected) R.color.argos_green else R.color.argos_divider))
        }
        indicator.contentDescription = getString(
            if (selected) R.string.onboarding_step_selected else R.string.onboarding_step_unselected,
            indicatorStep + 1,
        )
        indicator.isSelected = selected
    }

    private fun completeOnboarding() {
        viewModel.completeOnboarding()
        closeOverlay()
    }

    private fun leaveForLater() = closeOverlay()

    private fun closeOverlay() {
        (activity as? HomeActivity)?.closeOnboardingOverlay(this)
    }

    companion object {
        private const val FIRST_STEP = 0
        private const val LAST_STEP = 2
        private const val STATE_STEP = "onboarding_step"
        private val STEP_CONTENTS = listOf(
            StepContent(
                R.drawable.onboarding_step_welcome_tobby,
                R.string.onboarding_art_welcome,
                R.string.onboarding_title_welcome,
                R.string.onboarding_description_welcome,
                1f,
            ),
            StepContent(
                R.drawable.onboarding_step_features_tobby,
                R.string.onboarding_art_features,
                R.string.onboarding_title_features,
                R.string.onboarding_description_features,
                1.1f,
            ),
            StepContent(
                R.drawable.onboarding_step_ready_tobby,
                R.string.onboarding_art_ready,
                R.string.onboarding_title_ready,
                R.string.onboarding_description_ready,
                1.12f,
            ),
        )

    }
}
