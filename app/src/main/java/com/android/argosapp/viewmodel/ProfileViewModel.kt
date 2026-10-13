package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.util.auth.AuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    fun logout(onComplete: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                authManager.clearSession()
                authManager.signOut()
                onComplete()

            } catch (error: Exception) {
                onError(error)

            }
        }
    }
}
