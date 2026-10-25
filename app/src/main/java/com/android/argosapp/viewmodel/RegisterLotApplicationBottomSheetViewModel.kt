package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.LotStorage
import com.android.argosapp.data.cache.storage.MedicineStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.dto.RegisterLotApplicationDto
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.dto.MedicinePageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class RegisterLotApplicationBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val lotStorage = LotStorage(application)
    private val medicineStorage = MedicineStorage(application)
    private var submitting = false
    private var loadingMoreMedicines = false
    private var refreshingMedicines = false
    private val lotsCache = CacheDailyRefreshHelper(
        getCachedData = { lotStorage.getNamesForSelect() },
        getCacheUpdatedAt = { lotStorage.getCreatedAt() },
        updateCache = { lotStorage.replaceAll(repository.listLots(requireToken())) },
    )
    private val medicinesCache = CacheDailyRefreshHelper(
        getCachedData = {
            medicineStorage.getNamesForSelect() to
                (medicineStorage.getPaginationState()?.isLastPage == false)
        },
        getCacheUpdatedAt = { medicineStorage.getPaginationState()?.createdAt },
        updateCache = {
            val page = repository.getMedicinesPage(
                bearerToken = requireToken(),
                page = 0,
                size = MedicineStorage.PAGE_SIZE,
            )
            medicineStorage.saveFirstPage(page)
        },
    )

    fun loadLots(
        force: Boolean = false,
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<String>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            lotsCache.getData(force).collect { result ->
                result.data?.let(onLoaded)
                onLoading(result.refreshing)
                result.error?.let(onError)
            }
        }
    }

    fun loadMedicines(
        force: Boolean = false,
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<String>, Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            medicinesCache.getData(force).collect { result ->
                refreshingMedicines = result.refreshing
                result.data?.let { (names, hasMore) -> onLoaded(names, hasMore) }
                onLoading(result.refreshing)
                result.error?.let(onError)
            }
        }
    }

    fun loadMoreMedicines(
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<String>, Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (loadingMoreMedicines || refreshingMedicines) {
            onLoading(false)
            return
        }
        loadingMoreMedicines = true
        viewModelScope.launch {
            onLoading(true)
            try {
                val pagination = medicineStorage.getPaginationState()
                    ?: throw IllegalStateException("Medicine pages have not been loaded yet.")
                if (pagination.isLastPage) {
                    onLoaded(emptyList(), false)
                    return@launch
                }
                val page: MedicinePageDto = repository.getMedicinesPage(
                    bearerToken = requireToken(),
                    page = pagination.nextPage,
                    size = MedicineStorage.PAGE_SIZE,
                )
                medicineStorage.appendPage(page, pagination.createdAt)
                onLoaded(page.data.map { it.medicineName }, !page.last)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error)
            } finally {
                loadingMoreMedicines = false
                onLoading(false)
            }
        }
    }

    fun registerApplication(
        data: RegisterLotApplicationDto,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (submitting) return
        submitting = true
        viewModelScope.launch {
            onLoading(true)
            try {
                val user = UserStorage(getApplication()).getUser()
                    ?: throw SecurityException("User session is unavailable.")
                repository.registerLotApplication(requireToken(), user.userId.toString(), data)
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
