package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileEditViewModel(
    private val userStorage: UserStorage,
) : ViewModel() {

    fun loadCachedUser(onLoaded: (User?) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                onLoaded(userStorage.getUser())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error)
            }
        }
    }
}
