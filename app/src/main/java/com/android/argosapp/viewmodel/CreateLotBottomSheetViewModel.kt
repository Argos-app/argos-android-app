package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.CacheDailyRefreshHelper
import com.android.argosapp.data.cache.storage.HerdStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.dto.RegisterLotDto
import com.android.argosapp.util.validation.LotFormValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreateLotBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val herdStorage = HerdStorage(application)
    private val herdsCache = CacheDailyRefreshHelper(
        getCachedData = { herdStorage.getAll() },
        getCacheUpdatedAt = { herdStorage.getCreatedAt() },
        updateCache = { herdStorage.replaceAll(repository.listHerds(requireToken())) },
    )
    private val validator = LotFormValidator()
    private var submitting = false

    fun loadHerds(
        force: Boolean = false,
        onLoading: (Boolean) -> Unit,
        onHerdsLoaded: (List<com.android.argosapp.model.Herd>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                herdsCache.getData(force).collect { result ->
                    result.data?.let(onHerdsLoaded)
                    onLoading(result.refreshing)
                    result.error?.let(onError)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onLoading(false)
                onError(error)
            }
        }
    }

    fun invalidFields(
        lotName: String,
        herdId: String?,
        herdName: String,
        breed: String?,
        category: String,
        headCount: String,
        slaughterDate: String,
        availableCategories: Collection<String>,
    ): Set<LotFormValidator.Field> = validator.invalidFields(
        lotName = lotName,
        herdName = herdName,
        herdId = herdId,
        breed = breed,
        isNewHerd = herdId == null,
        category = category,
        headCount = headCount,
        slaughterDate = slaughterDate,
        availableCategories = availableCategories,
    )

    fun registerLot(
        lotName: String,
        herdId: String?,
        herdName: String,
        breed: String?,
        category: String,
        headCount: String,
        slaughterDate: String,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (submitting) return
        submitting = true
        viewModelScope.launch {
            onLoading(true)
            try {
                val data = RegisterLotDto(
                    lotName = lotName.trim(),
                    herdId = herdId,
                    herdName = herdName.trim(),
                    breed = breed?.trim(),
                    category = when (category) {
                        "Engorda" -> "FATTENING"
                        "Recria" -> "REARING"
                        "Cria" -> "BREEDING"
                        else -> throw IllegalArgumentException("Lot category is invalid.")
                    },
                    headCount = headCount.toInt(),
                    releaseSlaughterDate = LocalDate.parse(slaughterDate, DATE_FORMATTER).toString(),
                )

                repository.registerLot(requireToken(), data)
                herdStorage.clear()
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
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    }
}
