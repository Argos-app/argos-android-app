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
import java.time.LocalDate

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
        onError: (Exception, Boolean) -> Unit,
    ) = load(false, onLoading, onRefreshing, onLotsLoaded, onError)

    fun refresh(
        onRefreshing: (Boolean) -> Unit,
        onLotsLoaded: (List<Lot>) -> Unit,
        onError: (Exception, Boolean) -> Unit,
    ) = load(true, {}, onRefreshing, onLotsLoaded, onError)

    fun filterLots(query: String, category: String?): List<Lot> = lots.filter { lot ->
        val matchesQuery = query.isBlank() || lot.lotName.contains(query, ignoreCase = true) ||
            lot.category.orEmpty().contains(query, ignoreCase = true)
        val matchesCategory = category == null || lot.category.equals(category, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    fun suggestLots(query: String): List<Lot> = findLotSuggestions(lots, query)

    fun isSlaughterAvailable(lot: Lot): Boolean = lot.canSlaughter(LocalDate.now())

    fun slaughter(lot: Lot, onFinished: (Boolean, Boolean) -> Unit) {
        viewModelScope.launch {
            val lotId = lot.lotId
            if (lotId.isNullOrBlank() || !isSlaughterAvailable(lot)) {
                onFinished(false, false)
                return@launch
            }
            try {
                repository.deleteLot(requireToken(), lotId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "lot_slaughter_failure", error)
                onFinished(false, false)
                return@launch
            }
            lots = lots.filterNot { it.lotId == lotId }
            val cacheUpdated = try {
                lotStorage.deleteById(lotId)
                true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "lot_cache_delete_failure", error)
                false
            }
            onFinished(true, cacheUpdated)
        }
    }

    private fun load(
        forceRefresh: Boolean,
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onLotsLoaded: (List<Lot>) -> Unit,
        onError: (Exception, Boolean) -> Unit,
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
                        onError(error, result.data != null)
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

internal fun findLotSuggestions(lots: List<Lot>, query: String): List<Lot> {
    val term = query.trim()
    if (term.isEmpty()) return lots
    val pattern = Regex(Regex.escape(term), RegexOption.IGNORE_CASE)
    return lots.filter { pattern.containsMatchIn(it.lotName) }
}
