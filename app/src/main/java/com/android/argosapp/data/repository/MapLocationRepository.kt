package com.android.argosapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import com.android.argosapp.BuildConfig
import com.android.argosapp.model.GeoPoint
import com.android.argosapp.model.MapAddressSuggestion
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MapLocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private var autocompleteSessionToken: AutocompleteSessionToken? = null
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(appContext)
    }

    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(appContext, BuildConfig.MAPS_API_KEY)
        }
        Places.createClient(appContext)
    }

    suspend fun findAddressSuggestions(query: String): List<MapAddressSuggestion> {
        if (query.length < MIN_QUERY_LENGTH || BuildConfig.MAPS_API_KEY.isBlank()) return emptyList()

        val sessionToken = autocompleteSessionToken
            ?: AutocompleteSessionToken.newInstance().also { autocompleteSessionToken = it }
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries(listOf(BRAZIL_COUNTRY_CODE))
            .setRegionCode(BRAZIL_COUNTRY_CODE)
            .setSessionToken(sessionToken)
            .build()

        return placesClient.findAutocompletePredictions(request)
            .await()
            .autocompletePredictions
            .map { prediction ->
                MapAddressSuggestion(
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString(),
                    placeId = prediction.placeId,
                )
            }
    }

    suspend fun findCoordinates(suggestion: MapAddressSuggestion): GeoPoint? {
        val request = FetchPlaceRequest.builder(
            suggestion.placeId,
            listOf(Place.Field.LOCATION),
        )
            .setSessionToken(autocompleteSessionToken)
            .build()

        return try {
            placesClient.fetchPlace(request)
                .await()
                .place
                .location
                ?.let { GeoPoint(it.latitude, it.longitude) }
        } finally {
            autocompleteSessionToken = null
        }
    }

    suspend fun findCoordinates(address: String): GeoPoint? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(appContext)

        geocoder.getFromLocationName(address, 1)
            ?.firstOrNull()
            ?.let { GeoPoint(it.latitude, it.longitude) }
    }

    suspend fun findCurrentLocation(): GeoPoint? {
        if (!hasLocationPermission()) return null

        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    suspend fun findAddress(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(appContext)

        geocoder.getFromLocation(point.latitude, point.longitude, 1)
            ?.firstOrNull()
            ?.let { address ->
                val street = listOfNotNull(
                    address.thoroughfare,
                    address.subThoroughfare,
                ).joinToString(", ")
                val city = address.locality ?: address.subAdminArea
                val segments = listOfNotNull(
                    street.takeIf(String::isNotBlank),
                    address.subLocality,
                    listOfNotNull(city, address.adminArea).joinToString(" - ")
                        .takeIf(String::isNotBlank),
                ).distinct()

                segments.joinToString(" • ")
                    .takeIf(String::isNotBlank)
                    ?: address.getAddressLine(0)?.trim()?.takeIf(String::isNotBlank)
            }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 5
        const val BRAZIL_COUNTRY_CODE = "BR"
    }
}
