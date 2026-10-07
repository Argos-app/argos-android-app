package com.android.argosapp.viewmodel

import android.app.Application as AndroidApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.model.Application as ApplicationModel
import com.android.argosapp.util.logging.AppLogger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ApplicationsManagementViewModel(application: AndroidApplication) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val repository = (application as ArgosApplication).argosApiRepository
    private val applicationsStorage = ApplicationsStorage(application)
    private var filterJob: Job? = null
    private var textFilter: String? = null
    private var statusFilter: String? = null
    private var isRefreshing = false

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
            var cacheCreatedAt: Long? = null
            onFilterLoading(true)
            try {
                cacheCreatedAt = applicationsStorage.getCreatedAt()
                onApplicationsLoaded(applicationsStorage.listAll(textFilter, statusFilter))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportLoadFailure(error)
                onError(error)
                return@launch
            } finally {
                onFilterLoading(false)
            }

            if (!forceRefresh && !isCacheStale(cacheCreatedAt)) {
                return@launch
            }

            if (isRefreshing) {
                onRefreshLoading(true)
                return@launch
            }

            isRefreshing = true
            onRefreshLoading(true)
            try {
                val bearerToken = tokenStorage.getArgosApiToken()
                if (bearerToken.isNullOrBlank()) {
                    throw SecurityException("Bearer token is unavailable.")
                }

                val applications = repository.listAllApplications(bearerToken)
                applicationsStorage.replaceAll(applications)
                onApplicationsLoaded(applicationsStorage.listAll(textFilter, statusFilter))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportLoadFailure(error)
                if (cacheCreatedAt == null) {
                    onError(error)
                }
            } finally {
                isRefreshing = false
                onRefreshLoading(false)
            }
        }
    }

    private fun isCacheStale(createdAt: Long?): Boolean {
        if (createdAt == null) {
            return true
        }
        val cacheDate = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return cacheDate.isBefore(LocalDate.now())
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
