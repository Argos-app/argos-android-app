package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.dto.RegisterLotDto
import com.android.argosapp.util.validation.LotFormValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreateLotBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val tokenStorage = TokenStorage(application)
    private val validator = LotFormValidator()
    private var submitting = false

    fun invalidFields(
        lotName: String,
        herdName: String,
        category: String,
        headCount: String,
        slaughterDate: String,
        availableCategories: Collection<String>,
    ): Set<LotFormValidator.Field> = validator.invalidFields(
        lotName = lotName,
        herdName = herdName,
        category = category,
        headCount = headCount,
        slaughterDate = slaughterDate,
        availableCategories = availableCategories,
    )

    fun registerLot(
        lotName: String,
        herdName: String,
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
                    herdName = herdName.trim(),
                    category = category.uppercase(),
                    headCount = headCount.toIntOrNull()
                        ?: throw IllegalArgumentException("Head count is invalid."),
                    releaseSlaughterDate = LocalDate.parse(slaughterDate, DATE_FORMATTER).toString(),
                )
                repository.registerLot(requireToken(), data)
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
