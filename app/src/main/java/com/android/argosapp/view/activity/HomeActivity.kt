package com.android.argosapp.view.activity

import android.content.Context
import android.content.Intent
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
import com.android.argosapp.view.fragment.ApplicationsManagementFragment
import com.android.argosapp.view.fragment.ExceptionsManagementFragment
import com.android.argosapp.view.fragment.ProfileEditFragment
import com.android.argosapp.view.fragment.MapLocationPickerFragment
import com.android.argosapp.view.fragment.ProfileFragment
import com.android.argosapp.util.navigation.FragmentNavHelper

class HomeActivity : FragmentActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val fragmentNavHelper by lazy {
        FragmentNavHelper(supportFragmentManager, R.id.homeFragmentContainer)
    }

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

        val destinationName = intent.getStringExtra(EXTRA_INITIAL_DESTINATION)
            ?: savedInstanceState?.getString(STATE_DESTINATION)

        val destination = Destination.values().firstOrNull { it.name == destinationName }
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

        if (fragmentNavHelper.hasBackStack()) {
            fragmentNavHelper.clearBackStack()
        }
        if (currentDestination == destination &&
            fragmentNavHelper.findFragmentByTag(destination.name) != null

        ) {
            return
        }

        val fragment = when (destination) {
            Destination.HOME -> HomeFragment()
            Destination.CHATBOT -> ChatbotFragment()
            Destination.PROFILE -> ProfileFragment()

        }

        fragmentNavHelper.navigate(
            fragment = fragment,
            tag = destination.name,
            commitNow = true,
        )
        currentDestination = destination
        navBarBlurEffect.bindSource()
    }

    fun showApplicationDetails(application: ApplicationWithdrawal) {
        setNavigationBarVisible(false)
        fragmentNavHelper.navigate(
            fragment = HomeApplicationDetailsFragment.newInstance(application),
            tag = HomeApplicationDetailsFragment.TAG,
            addToBackStack = true,
            animations = overlayAnimations(),
        )
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        if (intent.getStringExtra(EXTRA_INITIAL_DESTINATION) == PROFILE_DESTINATION) {
            showDestination(Destination.PROFILE, animateIndicator = false)
        }
    }

    fun showProfileEdit() {
        setNavigationBarVisible(false)
        fragmentNavHelper.navigate(
            fragment = ProfileEditFragment(),
            tag = ProfileEditFragment.TAG,
            addToBackStack = true,
            animations = overlayAnimations(),
        )
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    fun showProfileLocationPicker(currentAddress: String) {
        setNavigationBarVisible(false)
        fragmentNavHelper.navigate(
            fragment = MapLocationPickerFragment.newInstance(currentAddress),
            tag = MapLocationPickerFragment.TAG,
            addToBackStack = true,
            animations = overlayAnimations(),
        )
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    fun showApplicationsManagement() {
        setNavigationBarVisible(false)
        fragmentNavHelper.navigate(
            fragment = ApplicationsManagementFragment(),
            tag = ApplicationsManagementFragment.TAG,
            addToBackStack = true,
            animations = overlayAnimations(),
        )
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    fun showExceptionsManagement() {
        setNavigationBarVisible(false)
        fragmentNavHelper.navigate(
            fragment = ExceptionsManagementFragment(),
            tag = ExceptionsManagementFragment.TAG,
            addToBackStack = true,
            animations = overlayAnimations(),
        )
        binding.homeFragmentContainer.post { navBarBlurEffect.bindSource() }
    }

    private fun updateNavigationBarVisibility() {
        val currentFragment = fragmentNavHelper.currentFragment()
        val showingOverlay = currentFragment is HomeApplicationDetailsFragment ||
            currentFragment is ProfileEditFragment ||
            currentFragment is MapLocationPickerFragment ||
            currentFragment is ApplicationsManagementFragment ||
            currentFragment is ExceptionsManagementFragment
        setNavigationBarVisible(!showingOverlay)
    }

    private fun setNavigationBarVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.homeBottomNavigation.visibility = visibility
        binding.navigationBlurBackdrop.visibility = visibility
    }

    private fun overlayAnimations() = FragmentNavHelper.Animations(
        enter = R.anim.home_details_enter,
        exit = R.anim.home_details_exit,
        popEnter = R.anim.home_details_pop_enter,
        popExit = R.anim.home_details_pop_exit,
    )

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

    companion object {
        private const val STATE_DESTINATION = "home_destination"
        private const val EXTRA_INITIAL_DESTINATION = "initial_destination"
        private const val PROFILE_DESTINATION = "PROFILE"

        fun createProfileIntent(context: Context): Intent =
            Intent(context, HomeActivity::class.java)
                .putExtra(EXTRA_INITIAL_DESTINATION, PROFILE_DESTINATION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
