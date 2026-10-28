package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.LotStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.model.Lot
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LotsManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val lotStorage = LotStorage(application)
    private var lots = emptyList<Lot>()
    private val lotsCache = CacheDailyRefreshHelper(
        getCachedData = { lotStorage.getAll() },
        getCacheUpdatedAt = { lotStorage.getCreatedAt() },
        updateCache = {
            lotStorage.replaceAll(repository.listLots(requireToken()))
        },
    )

    fun render(
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onLotsLoaded: (List<Lot>) -> Unit,
        onError: (Exception) -> Unit,
    ) = load(false, onLoading, onRefreshing, onLotsLoaded, onError)

    fun refresh(
        onRefreshing: (Boolean) -> Unit,
        onLotsLoaded: (List<Lot>) -> Unit,
        onError: (Exception) -> Unit,
    ) = load(true, {}, onRefreshing, onLotsLoaded, onError)

    fun filterLots(query: String, category: String?): List<Lot> = lots.filter { lot ->
        val matchesQuery = query.isBlank() || lot.lotName.contains(query, ignoreCase = true) ||
            lot.category.orEmpty().contains(query, ignoreCase = true)
        val matchesCategory = category == null || lot.category.equals(category, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    private fun load(
        forceRefresh: Boolean,
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onLotsLoaded: (List<Lot>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            onLoading(true)
            try {
                lotsCache.getData(forceRefresh).collect { result ->
                    result.data?.let { cachedLots ->
                        lots = cachedLots.map { it.toModel() }
                        onLotsLoaded(lots)
                    }
                    onLoading(result.refreshing && result.data == null)
                    onRefreshing(result.refreshing && result.data != null)
                    result.error?.let { error ->
                        AppLogger.error(AppLogger.REPOSITORY_TAG, "lots_load_failure", error)
                        if (result.data == null) onError(error)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                onLoading(false)
                onRefreshing(false)
            }
        }
    }

    private fun requireToken(): String = tokenStorage.getArgosApiToken()
        ?.takeIf(String::isNotBlank)
        ?: throw SecurityException("Bearer token is unavailable.")
}
