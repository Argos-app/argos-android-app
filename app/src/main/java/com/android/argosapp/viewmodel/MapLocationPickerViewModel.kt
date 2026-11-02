package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.data.repository.MapLocationRepository
import com.android.argosapp.model.GeoPoint
import com.android.argosapp.model.MapAddressSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapLocationPickerViewModel(
    private val locationRepository: MapLocationRepository,
) : ViewModel() {

    fun findAddressSuggestions(
        query: String,
        onResult: (List<MapAddressSuggestion>) -> Unit,
    ) {
        viewModelScope.launch {
            delay(SEARCH_DELAY)
            onResult(safely { locationRepository.findAddressSuggestions(query.trim()) }.orEmpty())
        }
    }

    fun findCoordinates(
        address: String,
        onResult: (GeoPoint?) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(safely { locationRepository.findCoordinates(address.trim()) })
        }
    }

    fun findCoordinates(
        suggestion: MapAddressSuggestion,
        onResult: (GeoPoint?) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(safely { locationRepository.findCoordinates(suggestion) })
        }
    }

    fun findCurrentLocation(onResult: (GeoPoint?) -> Unit) {
        viewModelScope.launch {
            onResult(safely { locationRepository.findCurrentLocation() })
        }
    }

    fun findInitialLocation(
        address: String,
        hasLocationPermission: Boolean,
        onResult: (GeoPoint?) -> Unit,
    ) {
        viewModelScope.launch {
            val currentLocation = if (hasLocationPermission) {
                safely { locationRepository.findCurrentLocation() }
            } else null
            val savedLocation = if (currentLocation == null && address.isNotBlank()) {
                safely { locationRepository.findCoordinates(address) }
            } else null
            onResult(currentLocation ?: savedLocation)
        }
    }

    fun findSelectedAddress(point: GeoPoint, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            delay(ADDRESS_DELAY)
            onResult(safely { locationRepository.findAddress(point) })
        }
    }

    fun findAddress(point: GeoPoint, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(safely { locationRepository.findAddress(point) })
        }
    }

    private suspend fun <T> safely(request: suspend () -> T): T? = try {
        request()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val SEARCH_DELAY = 300L
        const val ADDRESS_DELAY = 350L
    }
}
