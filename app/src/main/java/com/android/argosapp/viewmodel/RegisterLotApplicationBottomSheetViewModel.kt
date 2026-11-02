package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.LotStorage
import com.android.argosapp.data.cache.storage.MedicineStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.AnimalExceptionStorage
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.data.dto.RegisterAnimalApplicationDto
import com.android.argosapp.util.logging.AppLogger
import java.math.BigDecimal
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.dto.MedicinePageDto
import com.android.argosapp.model.AnimalExceptionRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class RegisterLotApplicationBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val lotStorage = LotStorage(application)
    private val medicineStorage = MedicineStorage(application)
    private val animalExceptionStorage = AnimalExceptionStorage(application)
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
    private val animalExceptionsCache = CacheDailyRefreshHelper(
        getCachedData = { animalExceptionStorage.getAll() },
        getCacheUpdatedAt = { animalExceptionStorage.getCreatedAt() },
        updateCache = {
            val exceptions = mutableListOf<AnimalExceptionRecord>()
            var pageNumber = 0
            while (true) {
                val page = repository.getAnimalExceptionsPage(requireToken(), true, pageNumber, PAGE_SIZE)
                exceptions += page.data.map { it.toModel() }
                if (page.last) break
                check(page.data.isNotEmpty() && pageNumber < page.totalPages - 1) {
                    "Animal exception pagination ended unexpectedly."
                }
                pageNumber++
            }
            animalExceptionStorage.replaceAll(exceptions)
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

    fun loadAnimalExceptions(
        force: Boolean = false,
        onLoading: (Boolean) -> Unit,
        onLoaded: (List<AnimalExceptionRecord>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            animalExceptionsCache.getData(force).collect { result ->
                result.data?.let(onLoaded)
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

    fun registerAnimalApplication(
        animalExceptionId: String,
        earCode: String,
        medicineName: String,
        dose: BigDecimal,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (submitting) return
        submitting = true
        viewModelScope.launch {
            onLoading(true)
            try {
                val token = requireToken()
                val medicines = medicineStorage.getAll().filter { it.medicineName == medicineName }
                require(medicines.size == 1) { "Medicamento não encontrado ou possui lotes ambíguos." }
                repository.registerAnimalApplication(token, animalExceptionId, RegisterAnimalApplicationDto(
                    earCode = earCode,
                    medicineBatchId = medicines.single().medicineBatchId,
                    dose = dose,
                ))
                try {
                    AnimalExceptionStorage(getApplication()).clear()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLogger.error(AppLogger.REPOSITORY_TAG, "animal_exception_cache_clear_failure", error)
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

    private companion object {
        const val PAGE_SIZE = 100
    }

}
