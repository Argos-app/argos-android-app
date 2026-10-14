package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.dto.UpdateCurrentUserDto
import com.android.argosapp.model.User
import com.android.argosapp.util.auth.AuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileEditViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    fun loadCachedUser(onLoaded: (User?) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                onLoaded(authManager.getUser())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error)
            }
        }
    }

    fun updateProfile(
        name: String?,
        email: String?,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            onLoading(true)
            try {
                authManager.updateCurrentUser(
                    UpdateCurrentUserDto(
                        name = name?.trim(),
                        email = email?.trim(),
                    ),
                )
                onSuccess()
            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                onError(error)

            } finally {
                onLoading(false)
            }
        }
    }
}
