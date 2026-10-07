package com.android.argosapp.view.activity

import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.android.argosapp.R
import com.android.argosapp.databinding.ActivityHomeBinding
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.view.effect.NavBarBlurEffect
import com.android.argosapp.view.fragment.ChatbotFragment
import com.android.argosapp.view.fragment.HomeFragment
import com.android.argosapp.view.fragment.HomeApplicationDetailsFragment
import com.android.argosapp.view.fragment.ProfileEditFragment
import com.android.argosapp.view.fragment.ProfileFragment

class HomeActivity : FragmentActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var currentDestination: Destination? = null
    private lateinit var navBarBlurEffect: NavBarBlurEffect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navBarBlurEffect = NavBarBlurEffect(
            source = binding.homeFragmentContainer,
            backdrop = binding.navigationBlurBackdrop,
        )

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigationBarVisibility()
            binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
        }

        navBarBlurEffect.render()

        binding.navigationHome.setOnClickListener { showDestination(Destination.HOME, animateIndicator = true) }
        binding.navigationChatbot.setOnClickListener { showDestination(Destination.CHATBOT, animateIndicator = true) }
        binding.navigationProfile.setOnClickListener { showDestination(Destination.PROFILE, animateIndicator = true) }

        val destination = savedInstanceState
            ?.getString(STATE_DESTINATION)
            ?.let { runCatching { Destination.valueOf(it) }.getOrNull() }
            ?: Destination.HOME

        if (savedInstanceState == null || supportFragmentManager.fragments.isEmpty()) {
            showDestination(destination, animateIndicator = false)

        } else {
            currentDestination = destination
            updateNavigationSelection(destination, animateIndicator = false)
            navBarBlurEffect.bindSource()

        }
        updateNavigationBarVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentDestination?.let { outState.putString(STATE_DESTINATION, it.name) }
        super.onSaveInstanceState(outState)

    }

    private fun showDestination(destination: Destination, animateIndicator: Boolean) {
        updateNavigationSelection(destination, animateIndicator)

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
            )
        }
        if (currentDestination == destination &&
            supportFragmentManager.findFragmentByTag(destination.name) != null

        ) {
            return
        }

        val fragment = when (destination) {
            Destination.HOME -> HomeFragment()
            Destination.CHATBOT -> ChatbotFragment()
            Destination.PROFILE -> ProfileFragment()

        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeFragmentContainer, fragment, destination.name)
            .commitNow()
        currentDestination = destination
        navBarBlurEffect.bindSource()
    }

    fun showApplicationDetails(application: ApplicationWithdrawal) {
        setNavigationBarVisible(false)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.home_details_enter,
                R.anim.home_details_exit,
                R.anim.home_details_pop_enter,
                R.anim.home_details_pop_exit,
            )
            .replace(
                R.id.homeFragmentContainer,
                HomeApplicationDetailsFragment.newInstance(application),
                HomeApplicationDetailsFragment.TAG,
            )
            .addToBackStack(HomeApplicationDetailsFragment.TAG)
            .commit()
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    fun showProfileEdit() {
        setNavigationBarVisible(false)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.home_details_enter,
                R.anim.home_details_exit,
                R.anim.home_details_pop_enter,
                R.anim.home_details_pop_exit,
            )
            .replace(R.id.homeFragmentContainer, ProfileEditFragment(), ProfileEditFragment.TAG)
            .addToBackStack(ProfileEditFragment.TAG)
            .commit()
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    private fun updateNavigationBarVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.homeFragmentContainer)
        val showingOverlay = currentFragment is HomeApplicationDetailsFragment || currentFragment is ProfileEditFragment
        setNavigationBarVisible(!showingOverlay)
    }

    private fun setNavigationBarVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.homeBottomNavigation.visibility = visibility
        binding.navigationBlurBackdrop.visibility = visibility
    }

    private fun updateNavigationSelection(destination: Destination, animateIndicator: Boolean) {
        binding.navigationHome.isSelected = destination == Destination.HOME
        binding.navigationChatbot.isSelected = destination == Destination.CHATBOT
        binding.navigationProfile.isSelected = destination == Destination.PROFILE

        binding.homeBottomNavigation.post {
            val target = when (destination) {
                Destination.HOME -> binding.navigationHome
                Destination.CHATBOT -> binding.navigationChatbot
                Destination.PROFILE -> binding.navigationProfile
            }

            val targetCenter = (target.parent as View).left + target.left + target.width / 2f
            val indicator = binding.navigationIndicator

            if (indicator.layoutParams.width != target.width) {
                indicator.layoutParams = indicator.layoutParams.apply {
                    width = target.width
                }
            }

            val navigationCenter = binding.homeBottomNavigation.width / 2f
            val targetTranslation = targetCenter - navigationCenter

            indicator.animate().cancel()
            if (animateIndicator) {
                indicator.animate()
                    .translationX(targetTranslation)
                    .setDuration(320L)
                    .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
                    .start()

            } else {
                indicator.translationX = targetTranslation
            }
        }
    }

    override fun onDestroy() {
        navBarBlurEffect.clear()
        super.onDestroy()

    }

    private enum class Destination {
        HOME,
        CHATBOT,
        PROFILE,
    }

    private companion object {
        const val STATE_DESTINATION = "home_destination"
    }
}
