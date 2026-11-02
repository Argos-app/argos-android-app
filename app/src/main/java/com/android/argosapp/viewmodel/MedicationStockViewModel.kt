package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.MedicineStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.dto.MedicineCatalogDto
import com.android.argosapp.data.dto.MedicineCatalogPageDto
import com.android.argosapp.data.dto.MedicineDto
import com.android.argosapp.data.dto.RegisterMedicineDto
import com.android.argosapp.model.MedicationStockItem
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MedicationStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val medicineStorage = MedicineStorage(application)
    private var loadingMore = false
    private var refreshing = false
    private var registering = false
    private val medicinesCache = CacheDailyRefreshHelper(
        getCachedData = { medicineStorage.getAll() },
        getCacheUpdatedAt = { medicineStorage.getPaginationState()?.createdAt },
        updateCache = {
            medicineStorage.saveFirstPage(repository.getMedicinesPage(requireToken(), 0, MedicineStorage.PAGE_SIZE))
        },
    )

    fun render(
        forceRefresh: Boolean = false,
        onLoading: (Boolean) -> Unit,
        onRefreshing: (Boolean) -> Unit,
        onLoaded: (List<MedicationStockItem>, Boolean) -> Unit,
        onError: (Exception, Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            var hadData = false
            onLoading(true)
            try {
                medicinesCache.getData(forceRefresh).collect { result ->
                    refreshing = result.refreshing
                    result.data?.let { cached ->
                        hadData = true
                        onLoaded(cached.map(MedicineDto::toStockItem), medicineStorage.getPaginationState()?.isLastPage == false)
                    }
                    onLoading(result.refreshing && !hadData)
                    onRefreshing(result.refreshing && hadData)
                    result.error?.let { error ->
                        AppLogger.error(AppLogger.REPOSITORY_TAG, "medication_stock_load_failure", error)
                        onError(error, hadData)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                refreshing = false
                onLoading(false)
                onRefreshing(false)
            }
        }
    }

    fun loadNextPage(
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<MedicationStockItem>, Boolean) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (loadingMore || refreshing) return
        loadingMore = true
        viewModelScope.launch {
            onLoading(true)
            try {
                val pagination = medicineStorage.getPaginationState()
                    ?: throw IllegalStateException("Medicine pages have not been loaded yet.")
                if (pagination.isLastPage) return@launch
                val page = repository.getMedicinesPage(requireToken(), pagination.nextPage, MedicineStorage.PAGE_SIZE)
                medicineStorage.appendPage(page, pagination.createdAt)
                onLoaded(page.data.map(MedicineDto::toStockItem), !page.last)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "medication_stock_page_failure", error)
                onError(error)
            } finally {
                loadingMore = false
                onLoading(false)
            }
        }
    }

    fun loadCatalogPage(page: Int, onResult: (List<MedicineCatalogDto>, Boolean, Exception?) -> Unit) {
        viewModelScope.launch {
            try {
                val result: MedicineCatalogPageDto = repository.getMedicineCatalogPage(requireToken(), page, MedicineStorage.PAGE_SIZE)
                onResult(result.data, !result.last, null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "medicine_catalog_load_failure", error)
                onResult(emptyList(), false, error)
            }
        }
    }

    fun register(data: RegisterMedicineDto, onLoading: (Boolean) -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (registering) return
        registering = true
        viewModelScope.launch {
            onLoading(true)
            try {
                repository.registerMedicine(requireToken(), data)
                medicineStorage.invalidate()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "medicine_registration_failure", error)
                onError(error)
                return@launch
            } finally {
                registering = false
                onLoading(false)
            }
            onSuccess()
        }
    }

    private fun requireToken(): String = tokenStorage.getArgosApiToken()
        ?.takeIf(String::isNotBlank)
        ?: throw SecurityException("Bearer token is unavailable.")
}

private fun MedicineDto.toStockItem() = MedicationStockItem(
    id = medicineBatchId,
    medicineName = medicineName,
    supplier = supplier,
    manufactureDate = manufactureDate,
    expiryDate = expiryDate,
    quantity = quantity,
    dose = dose,
    withdrawalDays = withdrawalDays,
    batch = batch,
    activeIngredient = activeIngredient,
    therapeuticCategory = therapeuticCategory,
)
