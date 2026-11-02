package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.LotStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.dto.LotDto
import com.android.argosapp.data.dto.RegisterAnimalExceptionDto
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateExceptionBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val lotStorage = LotStorage(application)
    private var submitting = false
    private val lotsCache = CacheDailyRefreshHelper(
        getCachedData = {
            lotStorage.getAll().filter { !it.lotId.isNullOrBlank() && it.animalExceptionOutList == null }
        },
        getCacheUpdatedAt = { lotStorage.getCreatedAt() },
        updateCache = { lotStorage.replaceAll(repository.listLots(requireToken())) },
    )

    fun loadLots(
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<LotDto>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            lotsCache.getData().collect { result ->
                result.data?.let(onLoaded)
                onLoading(result.refreshing)
                result.error?.let(onError)
            }
        }
    }

    fun registerException(
        data: RegisterAnimalExceptionDto,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (submitting) return
        submitting = true
        viewModelScope.launch {
            onLoading(true)
            try {
                repository.registerAnimalException(requireToken(), data)
                try {
                    lotStorage.invalidateCache()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLogger.error(AppLogger.REPOSITORY_TAG, "lot_cache_invalidate_failure", error)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error)
                return@launch
            } finally {
                submitting = false
                onLoading(false)
            }
            onSuccess()
        }
    }

    private fun requireToken(): String = tokenStorage.getArgosApiToken()
        ?.takeIf(String::isNotBlank)
        ?: throw SecurityException("Bearer token is unavailable.")
}
