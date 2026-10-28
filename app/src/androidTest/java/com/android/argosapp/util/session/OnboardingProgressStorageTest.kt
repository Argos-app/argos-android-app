package com.android.argosapp.util.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingProgressStorageTest {
    @Test
    fun pendingStepPersistsAcrossStorageInstancesAndIsScopedToUser() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val email = "onboarding-${System.nanoTime()}@example.com"
        val otherEmail = "other-${System.nanoTime()}@example.com"
        val storage = OnboardingProgressStorage(context)

        try {
            storage.markPending(email)
            storage.saveStep(email, 1)

            val restoredStorage = OnboardingProgressStorage(context)
            assertTrue(restoredStorage.isPending(email))
            assertEquals(1, restoredStorage.pendingStep(email))
            assertFalse(restoredStorage.isPending(otherEmail))
            assertEquals(0, restoredStorage.pendingStep(otherEmail))

            restoredStorage.saveStep(email, 99)
            assertEquals(2, restoredStorage.pendingStep(email))
            restoredStorage.saveStep(email, -1)
            assertEquals(0, restoredStorage.pendingStep(email))
        } finally {
            storage.markCompleted(email)
            storage.markCompleted(otherEmail)
        }
    }

    @Test
    fun completingOnboardingRemovesPendingFlagAndSavedStep() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val email = "completed-${System.nanoTime()}@example.com"
        val storage = OnboardingProgressStorage(context)

        storage.markPending(email)
        storage.saveStep(email, 2)
        storage.markCompleted(email)

        assertFalse(storage.isPending(email))
        assertEquals(0, storage.pendingStep(email))
    }
}
