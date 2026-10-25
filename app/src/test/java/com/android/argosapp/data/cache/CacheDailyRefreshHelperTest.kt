package com.android.argosapp.data.cache

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheDailyRefreshHelperTest {
    @Test
    fun freshCacheIsReturnedWithoutRefreshing() = runBlocking {
        var refreshCount = 0
        val helper = CacheDailyRefreshHelper(
            getCachedData = { "cached" },
            getCacheUpdatedAt = { todayTimestamp() },
            updateCache = { refreshCount++ },
        )

        val result = helper.getData().toList()

        assertEquals(listOf(CacheData("cached", refreshing = false)), result)
        assertEquals(0, refreshCount)
    }

    @Test
    fun missingCacheRefreshesAndReturnsNewData() = runBlocking {
        var cache: String? = null
        var updatedAt: Long? = null
        val helper = CacheDailyRefreshHelper(
            getCachedData = { cache },
            getCacheUpdatedAt = { updatedAt },
            updateCache = {
                cache = "fresh"
                updatedAt = todayTimestamp()
            },
        )

        val result = helper.getData().toList()

        assertEquals(CacheData<String>(null, refreshing = true), result.first())
        assertEquals(CacheData("fresh", refreshing = false), result.last())
    }

    @Test
    fun staleCacheIsEmittedBeforeRefreshing() = runBlocking {
        var cache = "old"
        var updatedAt = yesterdayTimestamp()
        val helper = CacheDailyRefreshHelper(
            getCachedData = { cache },
            getCacheUpdatedAt = { updatedAt },
            updateCache = {
                cache = "fresh"
                updatedAt = todayTimestamp()
            },
        )

        val result = helper.getData().toList()

        assertEquals(CacheData("old", refreshing = true), result.first())
        assertEquals(CacheData("fresh", refreshing = false), result.last())
    }

    @Test
    fun failedRefreshKeepsCachedDataAndReportsError() = runBlocking {
        val helper = CacheDailyRefreshHelper(
            getCachedData = { "cached" },
            getCacheUpdatedAt = { yesterdayTimestamp() },
            updateCache = { throw IllegalStateException("offline") },
        )

        val result = helper.getData().toList()

        assertEquals("cached", result.last().data)
        assertFalse(result.last().refreshing)
        assertNotNull(result.last().error)
        assertEquals("offline", result.last().error?.message)
    }

    @Test
    fun forceRefreshBypassesFreshTimestamp() = runBlocking {
        var refreshCount = 0
        val helper = CacheDailyRefreshHelper(
            getCachedData = { "cached" },
            getCacheUpdatedAt = { todayTimestamp() },
            updateCache = { refreshCount++ },
        )

        val result = helper.getData(force = true).toList()

        assertEquals(2, result.size)
        assertTrue(result.first().refreshing)
        assertFalse(result.last().refreshing)
        assertEquals(1, refreshCount)
        assertNull(result.last().error)
    }

    private fun todayTimestamp(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun yesterdayTimestamp(): Long = LocalDate.now()
        .minusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
