package com.android.argosapp.viewmodel

import android.app.Application as AndroidApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.AnimalExceptionStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.model.AnimalExceptionRecord
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ExceptionsManagementViewModel(application: AndroidApplication) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val repository = (application as ArgosApplication).argosApiRepository
    private val exceptionsStorage = AnimalExceptionStorage(application)
    private var exceptions = emptyList<AnimalExceptionRecord>()
    private val exceptionsCache = CacheDailyRefreshHelper(
        getCachedData = { exceptionsStorage.getAll() },
        getCacheUpdatedAt = { exceptionsStorage.getCreatedAt() },
        updateCache = {
            val bearerToken = tokenStorage.getArgosApiToken()
                ?.takeIf(String::isNotBlank)
                ?: throw SecurityException("Bearer token is unavailable.")
            val allExceptions = mutableListOf<AnimalExceptionRecord>()
            var pageNumber = 0
            while (true) {
                val page = repository.getAnimalExceptionsPage(
                    bearerToken = bearerToken,
                    active = true,
                    page = pageNumber,
                    size = PAGE_SIZE,
                )
                allExceptions += page.data.map { it.toModel() }
                if (page.last) break
                if (page.data.isEmpty() || pageNumber >= page.totalPages - 1) {
                    throw IllegalStateException("Animal exception pagination ended unexpectedly.")
                }
                pageNumber++
            }
            exceptionsStorage.replaceAll(allExceptions)
        },
    )

    val exceptionCount: Int get() = exceptions.size
    val fitCount: Int get() = exceptions.count(AnimalExceptionRecord::eligibleToReturn)
    val unfitCount: Int get() = exceptions.count { !it.eligibleToReturn }

    fun render(
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onExceptionsLoaded: (List<AnimalExceptionRecord>) -> Unit,
        onError: (Exception) -> Unit,
    ) = load( forceRefresh = false, onLoading = onLoading, onRefreshing = onRefreshing, onExceptionsLoaded = onExceptionsLoaded, onError = onError, )

    fun refresh(
        onRefreshing: (Boolean) -> Unit,
        onExceptionsLoaded: (List<AnimalExceptionRecord>) -> Unit,
        onError: (Exception) -> Unit,
    ) = load( forceRefresh = true, onLoading = {}, onRefreshing = onRefreshing, onExceptionsLoaded = onExceptionsLoaded, onError = onError, )

    private fun load(
        forceRefresh: Boolean,
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onExceptionsLoaded: (List<AnimalExceptionRecord>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            var hasLoadedData = false
            onLoading(true)
            try {
                exceptionsCache.getData(forceRefresh).collect { result ->
                    result.data?.let { loaded ->
                        exceptions = loaded
                        onExceptionsLoaded(loaded)
                        hasLoadedData = true
                        onLoading(false)
                    }
                    if (!hasLoadedData && result.refreshing) onLoading(true)
                    onRefreshing(result.refreshing && (hasLoadedData || forceRefresh))
                    result.error?.let { error ->
                        AppLogger.error(AppLogger.REPOSITORY_TAG, "animal_exceptions_load_failure", error)
                        if (result.data == null) onError(error)
                    }
                    if (!result.refreshing && !hasLoadedData) onLoading(false)
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                onLoading(false)
                onRefreshing(false)
            }
        }
    }

    fun filter(filter: ExceptionFilter): List<AnimalExceptionRecord> = when (filter) {
        ExceptionFilter.ALL -> exceptions
        ExceptionFilter.FIT -> exceptions.filter(AnimalExceptionRecord::eligibleToReturn)
        ExceptionFilter.UNFIT -> exceptions.filterNot(AnimalExceptionRecord::eligibleToReturn)
    }

    enum class ExceptionFilter {
        ALL,
        FIT,
        UNFIT,
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}