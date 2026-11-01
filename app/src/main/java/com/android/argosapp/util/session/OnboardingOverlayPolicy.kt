package com.android.argosapp.util.session

/** Avoids adding a duplicate overlay when FragmentManager already restored it. */
object OnboardingOverlayPolicy {
    fun shouldAdd(pending: Boolean, alreadyAdded: Boolean): Boolean = pending && !alreadyAdded
}
