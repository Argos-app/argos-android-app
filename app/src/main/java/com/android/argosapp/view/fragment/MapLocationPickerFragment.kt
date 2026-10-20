package com.android.argosapp.view.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.data.repository.MapLocationRepository
import com.android.argosapp.databinding.FragmentMapLocationPickerBinding
import com.android.argosapp.model.GeoPoint
import com.android.argosapp.model.MapAddressSuggestion
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.view.adapter.MapAddressSuggestionAdapter
import com.android.argosapp.viewmodel.MapLocationPickerViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapLocationPickerFragment : Fragment(R.layout.fragment_map_location_picker) {
    private var _binding: FragmentMapLocationPickerBinding? = null
    private val binding get() = _binding
    private var googleMap: GoogleMap? = null
    private var isApplyingSuggestion = false
    private lateinit var suggestionAdapter: MapAddressSuggestionAdapter
    private val locationRepository by lazy {
        MapLocationRepository(requireContext().applicationContext)
    }
    private val viewModel: MapLocationPickerViewModel by viewModels {
        viewModelFactory { initializer { MapLocationPickerViewModel(locationRepository) } }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) centerOnCurrentLocation()
        else showMessage(R.string.profile_location_picker_permission_required)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapLocationPickerBinding.bind(view)
        val screen = requireNotNull(binding)

        setupSearch(screen)
        screen.locationPickerBack.setOnClickListener { popBackStack() }
        screen.locationPickerSearchButton.setOnClickListener { searchAddress() }
        screen.locationPickerCurrentLocationIcon.setOnClickListener { requestCurrentLocation() }
        screen.locationPickerConfirm.setOnClickListener { confirmLocation() }

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.locationPickerMap) as? SupportMapFragment
        if (mapFragment == null) showMessage(R.string.profile_location_picker_map_unavailable)
        else mapFragment.getMapAsync(::onMapReady)
    }

    override fun onDestroyView() {
        googleMap = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupSearch(screen: FragmentMapLocationPickerBinding) {
        val field = screen.locationPickerSearchQuery
        field.setText(arguments?.getString(ARG_CURRENT_ADDRESS).orEmpty())
        suggestionAdapter = MapAddressSuggestionAdapter(::selectSuggestion)
        screen.locationPickerSuggestions.layoutManager = LinearLayoutManager(requireContext())
        screen.locationPickerSuggestions.adapter = suggestionAdapter

        field.doAfterTextChanged { text ->
            if (isApplyingSuggestion) return@doAfterTextChanged
            val query = text?.toString()?.trim().orEmpty()
            if (query.length < MIN_QUERY_LENGTH) {
                clearSuggestions()
                return@doAfterTextChanged
            }

            viewModel.findAddressSuggestions(query) { suggestions ->
                val currentBinding = binding ?: return@findAddressSuggestions
                if (currentBinding.locationPickerSearchQuery.text.toString().trim() != query) {
                    return@findAddressSuggestions
                }
                suggestionAdapter.replaceSuggestions(suggestions)
                currentBinding.locationPickerSuggestions.visibility = if (
                    suggestions.isNotEmpty() && currentBinding.locationPickerSearchQuery.hasFocus()
                ) View.VISIBLE else View.GONE
            }
        }
        field.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            screen.locationPickerSuggestions.visibility =
                if (hasFocus && suggestionAdapter.itemCount > 0) View.VISIBLE else View.GONE
        }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            searchAddress()
            true
        }
    }

    private fun selectSuggestion(suggestion: MapAddressSuggestion) {
        val screen = binding ?: return
        if (!screen.locationPickerSearchButton.isEnabled) return
        val field = screen.locationPickerSearchQuery
        clearSuggestions()
        isApplyingSuggestion = true
        field.setText(suggestion.description)
        field.setSelection(field.text.length)
        isApplyingSuggestion = false
        hideKeyboard(field)
        setSearchLoading(true)

        viewModel.findCoordinates(suggestion) { point ->
            val currentBinding = binding ?: return@findCoordinates
            setSearchLoading(false)
            if (currentBinding.locationPickerSearchQuery.text.toString().trim() != suggestion.description) {
                return@findCoordinates
            }
            if (point == null) showMessage(R.string.profile_location_picker_search_not_found)
            else moveCamera(point)
        }
    }

    private fun clearSuggestions() {
        if (::suggestionAdapter.isInitialized) suggestionAdapter.replaceSuggestions(emptyList())
        binding?.locationPickerSuggestions?.visibility = View.GONE
    }

    private fun searchAddress() {
        val field = binding?.locationPickerSearchQuery ?: return
        if (binding?.locationPickerSearchButton?.isEnabled == false) return
        val query = field.text.toString().trim()
        if (query.isBlank()) return
        hideKeyboard(field)
        setSearchLoading(true)

        viewModel.findCoordinates(query) { point ->
            val currentBinding = binding ?: return@findCoordinates
            setSearchLoading(false)
            if (currentBinding.locationPickerSearchQuery.text.toString().trim() != query) {
                return@findCoordinates
            }
            if (point == null) showMessage(R.string.profile_location_picker_search_not_found)
            else moveCamera(point)
        }
    }

    private fun requestCurrentLocation() {
        if (hasLocationPermission()) centerOnCurrentLocation()
        else locationPermissionRequest.launch(LOCATION_PERMISSIONS)
    }

    private fun centerOnCurrentLocation() {
        val screen = binding ?: return
        if (!hasLocationPermission() || !screen.locationPickerCurrentLocation.isEnabled) return
        screen.locationPickerCurrentLocation.isEnabled = false
        screen.locationPickerCurrentLocationProgress.visibility = View.VISIBLE
        screen.locationPickerCurrentLocationIcon.visibility = View.INVISIBLE

        viewModel.findCurrentLocation { point ->
            val currentBinding = binding ?: return@findCurrentLocation
            currentBinding.locationPickerCurrentLocation.isEnabled = true
            currentBinding.locationPickerCurrentLocationProgress.visibility = View.GONE
            currentBinding.locationPickerCurrentLocationIcon.visibility = View.VISIBLE
            if (point == null) {
                showMessage(R.string.profile_location_picker_current_location_unavailable)
            } else {
                moveCamera(point)
            }
        }
    }

    private fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.setOnCameraMoveStartedListener { showAddressLoading() }
        map.setOnCameraIdleListener { updateSelectedAddress(map.cameraPosition.target.toGeoPoint()) }

        viewModel.findInitialLocation(
            arguments?.getString(ARG_CURRENT_ADDRESS).orEmpty(),
            hasLocationPermission(),
        ) { point ->
            if (googleMap !== map) return@findInitialLocation
            val center = point?.toLatLng() ?: DEFAULT_CENTER
            val zoom = if (point == null) DEFAULT_ZOOM else ADDRESS_ZOOM
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
        }
    }

    private fun updateSelectedAddress(point: GeoPoint) {
        showAddressLoading()
        viewModel.findSelectedAddress(point) { address ->
            val currentMapPoint = googleMap?.cameraPosition?.target?.toGeoPoint()
            if (binding == null || currentMapPoint != point) return@findSelectedAddress
            val screen = requireNotNull(binding)
            screen.locationPickerAddressProgress.visibility = View.GONE
            screen.locationPickerSelectedAddressText.text = address
                ?: getString(R.string.profile_location_picker_address_unavailable)
        }
    }

    private fun showAddressLoading() {
        binding?.apply {
            locationPickerAddressProgress.visibility = View.VISIBLE
            locationPickerSelectedAddressText.setText(
                R.string.profile_location_picker_address_loading,
            )
        }
    }

    private fun confirmLocation() {
        val point = googleMap?.cameraPosition?.target?.toGeoPoint() ?: return
        val screen = binding ?: return
        screen.locationPickerConfirm.isEnabled = false
        screen.locationPickerProgress.visibility = View.VISIBLE

        viewModel.findAddress(point) { address ->
            val currentBinding = binding ?: return@findAddress
            currentBinding.locationPickerConfirm.isEnabled = true
            currentBinding.locationPickerProgress.visibility = View.GONE
            if (address.isNullOrBlank()) {
                showMessage(R.string.profile_location_picker_unavailable)
                return@findAddress
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply { putString(RESULT_ADDRESS, address) },
            )
            popBackStack()
        }
    }

    private fun hasLocationPermission() = LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setSearchLoading(isLoading: Boolean) {
        binding?.locationPickerSearchButton?.isEnabled = !isLoading
    }

    private fun moveCamera(point: GeoPoint) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(point.toLatLng(), ADDRESS_ZOOM),
        )
    }

    private fun hideKeyboard(view: View) {
        view.clearFocus()
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }

    private fun popBackStack() = FragmentNavHelper(parentFragmentManager).popBackStack()

    private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
    private fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)

    companion object {
        const val TAG = "profile_location_picker"
        const val RESULT_KEY = "profile_location_picker_result"
        const val RESULT_ADDRESS = "selected_address"

        private const val ARG_CURRENT_ADDRESS = "current_address"
        private const val MIN_QUERY_LENGTH = 5
        private const val DEFAULT_ZOOM = 4.5f
        private const val ADDRESS_ZOOM = 16f
        private val LOCATION_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private val DEFAULT_CENTER = LatLng(-14.235, -51.9253)

        fun newInstance(currentAddress: String) = MapLocationPickerFragment().apply {
            arguments = Bundle().apply { putString(ARG_CURRENT_ADDRESS, currentAddress) }
        }
    }
}
