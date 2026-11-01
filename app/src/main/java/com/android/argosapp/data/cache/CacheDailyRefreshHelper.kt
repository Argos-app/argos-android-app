package com.android.argosapp.data.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CacheData<T>(
    val data: T?,
    val refreshing: Boolean,
    val error: Exception? = null,
)

class CacheDailyRefreshHelper<T>(
    private val getCachedData: suspend () -> T?,
    private val getCacheUpdatedAt: suspend () -> Long?,
    private val updateCache: suspend () -> Unit,
) {
    private val refreshMutex = Mutex()

    fun getData(force: Boolean = false): Flow<CacheData<T>> = flow {
        refreshMutex.withLock {
            val updatedAt: Long?
            val cachedData: T?
            try {
                updatedAt = getCacheUpdatedAt()
                cachedData = if (updatedAt == null) null else getCachedData()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit(CacheData(data = null, refreshing = false, error = error))
                return@withLock
            }

            val shouldRefresh = force || cachedData == null || isStale(updatedAt)
            emit(CacheData(data = cachedData, refreshing = shouldRefresh))
            if (!shouldRefresh) return@withLock

            try {
                updateCache()
                emit(CacheData(data = getCachedData(), refreshing = false))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit(CacheData(data = cachedData, refreshing = false, error = error))
            }
        }
    }

    private fun isStale(updatedAt: Long?): Boolean {
        if (updatedAt == null) return true
        return Instant.ofEpochMilli(updatedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .isBefore(LocalDate.now())
    }
}
