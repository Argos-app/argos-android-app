package com.android.argosapp.viewmodel

import android.app.Application as AndroidApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.model.Application as ApplicationModel
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ApplicationsManagementViewModel(application: AndroidApplication) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val repository = (application as ArgosApplication).argosApiRepository
    private val applicationsStorage = ApplicationsStorage(application)
    private var filterJob: Job? = null
    private var textFilter: String? = null
    private var statusFilter: String? = null
    private val applicationsCache = CacheDailyRefreshHelper(
        getCachedData = { applicationsStorage.listAll(textFilter, statusFilter) },
        getCacheUpdatedAt = { applicationsStorage.getCreatedAt() },
        updateCache = {
            val bearerToken = tokenStorage.getArgosApiToken()
                ?.takeIf(String::isNotBlank)
                ?: throw SecurityException("Bearer token is unavailable.")
            applicationsStorage.replaceAll(repository.listAllApplications(bearerToken))
        },
    )

    fun render(
        onFilterLoading: (Boolean) -> Unit,
        onRefreshLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationModel>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        loadApplications(
            forceRefresh = false,
            onFilterLoading = onFilterLoading,
            onRefreshLoading = onRefreshLoading,
            onApplicationsLoaded = onApplicationsLoaded,
            onError = onError,
        )
    }

    fun refresh(
        onRefreshLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationModel>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        loadApplications(
            forceRefresh = true,
            onFilterLoading = {},
            onRefreshLoading = onRefreshLoading,
            onApplicationsLoaded = onApplicationsLoaded,
            onError = onError,
        )
    }

    fun filterApplications(
        text: String?,
        status: String?,
        onLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationModel>) -> Unit,
    ) {
        textFilter = text?.trim()?.takeIf { it.isNotEmpty() }
        statusFilter = status
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            onLoading(true)
            try {
                onApplicationsLoaded(applicationsStorage.listAll(textFilter, statusFilter))
            } finally {
                onLoading(false)
            }
        }
    }

    private fun loadApplications(
        forceRefresh: Boolean,
        onFilterLoading: (Boolean) -> Unit,
        onRefreshLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationModel>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            var hasLoadedData = false
            onFilterLoading(true)
            try {
                applicationsCache.getData(forceRefresh).collect { result ->
                    result.data?.let { applications ->
                        onApplicationsLoaded(applications)
                        hasLoadedData = true
                        onFilterLoading(false)
                    }
                    if (!hasLoadedData && result.refreshing) onFilterLoading(true)
                    onRefreshLoading(result.refreshing && hasLoadedData)
                    result.error?.let { error ->
                        reportLoadFailure(error)
                        if (result.data == null) onError(error)
                    }
                    if (!result.refreshing && !hasLoadedData) onFilterLoading(false)
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                onFilterLoading(false)
                onRefreshLoading(false)
            }
        }
    }

    private fun reportLoadFailure(error: Exception) {
        AppLogger.error(
            AppLogger.REPOSITORY_TAG,
            "application_management_list_load_failed",
            error,
            "endpoint" to "application/list-all",
        )
    }
}
