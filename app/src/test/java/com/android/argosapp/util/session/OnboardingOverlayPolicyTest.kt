package com.android.argosapp.util.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingOverlayPolicyTest {
    @Test
    fun `pending onboarding is presented when no overlay is restored`() {
        assertTrue(OnboardingOverlayPolicy.shouldAdd(pending = true, alreadyAdded = false))
    }

    @Test
    fun `completed onboarding is not presented`() {
        assertFalse(OnboardingOverlayPolicy.shouldAdd(pending = false, alreadyAdded = false))
    }

    @Test
    fun `restored overlay is not added a second time`() {
        assertFalse(OnboardingOverlayPolicy.shouldAdd(pending = true, alreadyAdded = true))
    }
}
