package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.data.repository.CloudinaryPhotoRepository
import com.android.argosapp.dto.UpdateCurrentUserDto
import com.android.argosapp.util.auth.AuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(
    private val authManager: AuthManager,
    private val cloudinaryPhotoRepository: CloudinaryPhotoRepository,
) : ViewModel() {

    fun uploadProfilePhoto(
        photoFile: File,
        onLoading: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            onLoading(true)
            try {
                val secureUrl = cloudinaryPhotoRepository.uploadProfilePhoto(photoFile)
                authManager.updateCurrentUser(UpdateCurrentUserDto(photoUrl = secureUrl))
                onSuccess(secureUrl)

            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                onError(error)

            } finally {
                photoFile.delete()
                onLoading(false)

            }
        }
    }

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
