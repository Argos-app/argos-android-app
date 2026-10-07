package com.android.argosapp.viewmodel

import android.app.Application as AndroidApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.model.Application as ApplicationModel
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ApplicationsManagementViewModel(application: AndroidApplication) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val repository = (application as ArgosApplication).argosApiRepository
    private var isLoading = false

    fun render(
        onLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationModel>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                onLoading(true)

                val bearerToken = tokenStorage.getArgosApiToken()
                if (bearerToken.isNullOrBlank()) {
                    throw SecurityException("Bearer token is unavailable.")
                }

                val applications = repository.listAllApplications(bearerToken)
                onApplicationsLoaded(applications)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(
                    AppLogger.REPOSITORY_TAG,
                    "application_management_list_load_failed",
                    error,
                    "endpoint" to "application/list-all",
                )
                onError(error)
            } finally {
                isLoading = false
                onLoading(false)
            }
        }
    }
}
