package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.MedicationStockRegistration
import com.android.argosapp.data.repository.MockedArgosApiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicationStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ArgosApplication).argosApiRepository
    private val pager = MedicationStockPager()
    private val mutableState = MutableStateFlow(pager.state)
    val state = mutableState.asStateFlow()
    val integrationPending get() = !repository.isMedicationStockImplemented
    val isMockedRepository get() = repository is MockedArgosApiRepository
    private var pageJob: Job? = null

    init { loadNextPage() }

    fun search(query: String) = updateSelection(query, pager.state.category, debounce = true)
    fun filter(category: StockCategory?) = updateSelection(pager.state.query, category)
    fun refresh() = updateSelection(pager.state.query, pager.state.category, force = true)

    private fun updateSelection(query: String, category: StockCategory?, debounce: Boolean = false, force: Boolean = false) {
        if (!force && query == pager.state.query && category == pager.state.category) return
        pageJob?.cancel()
        pager.reset(query, category)
        mutableState.value = pager.state
        loadNextPage(debounce = debounce)
    }

    fun loadNextPage(retry: Boolean = false, debounce: Boolean = false) {
        if (integrationPending) return
        val request = pager.nextRequest(retry) ?: return
        mutableState.value = pager.state
        pageJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                pager.complete(request, repository.getMedicationStockPage(request.query.trim(), request.category, request.cursor, PAGE_SIZE))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                pager.fail(request)
            }
            mutableState.value = pager.state
        }
    }

    suspend fun getProducts() = if (integrationPending) emptyList() else repository.getMedicationProducts()

    suspend fun registerMedicationStock(registration: MedicationStockRegistration) {
        repository.registerMedicationStock(registration)
        refresh()
    }

    companion object { private const val PAGE_SIZE = 20 }
}
