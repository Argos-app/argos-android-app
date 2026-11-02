package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.android.argosapp.R
import com.android.argosapp.data.dto.LotDto
import com.android.argosapp.data.dto.RegisterAnimalExceptionDto
import com.android.argosapp.view.componentmanager.SelectManager
import com.android.argosapp.viewmodel.CreateExceptionBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateExceptionBottomSheetFragment : BottomSheetDialogFragment() {
    private val selectManager = SelectManager()
    private val viewModel: CreateExceptionBottomSheetViewModel by viewModels()
    private var lots = emptyList<LotDto>()
    private var selectedLot: LotDto? = null
    private var lotLoadFailed = false
    private var cacheRefreshErrorToastShown = false

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.bottom_sheet_create_exception, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lotField = view.findViewById<View>(R.id.exceptionOriginLotField)
        val lotValue = view.findViewById<TextView>(R.id.exceptionOriginLotValue)
        val reasonField = view.findViewById<EditText>(R.id.exceptionReasonField)
        val earTagField = view.findViewById<EditText>(R.id.exceptionEarTagField)
        val weightField = view.findViewById<EditText>(R.id.exceptionAnimalWeightField)

        fun loadLots() {
            viewModel.loadLots(
                onLoading = { loading ->
                    lotField.isEnabled = !loading
                    lotValue.error = null
                    lotField.findViewById<View>(R.id.exceptionOriginLotLoadingIndicator).visibility =
                        if (loading) View.VISIBLE else View.GONE
                    if (loading) {
                        lotValue.text = getString(R.string.applications_register_options_loading)
                        lotValue.setTextColor(requireContext().getColor(R.color.argos_placeholder))
                        lotField.findViewById<View>(R.id.exceptionOriginLotDropdownIcon).visibility = View.GONE
                        lotField.findViewById<View>(R.id.exceptionOriginLotErrorIcon).visibility = View.GONE
                    }
                },
                onLoaded = { loadedLots ->
                    lots = loadedLots
                    lotLoadFailed = false
                    lotValue.text = selectedLot?.lotName ?: when {
                        lots.isEmpty() -> getString(R.string.applications_register_options_empty)
                        else -> getString(R.string.exceptions_origin_lot_hint)
                    }
                    lotValue.setTextColor(requireContext().getColor(
                        if (lots.isEmpty()) R.color.argos_placeholder else R.color.argos_text_primary,
                    ))
                    lotField.isEnabled = lots.isNotEmpty()
                    lotField.findViewById<View>(R.id.exceptionOriginLotLoadingIndicator).visibility = View.GONE
                    lotField.findViewById<View>(R.id.exceptionOriginLotDropdownIcon).visibility =
                        if (lots.isNotEmpty()) View.VISIBLE else View.GONE
                    lotField.findViewById<View>(R.id.exceptionOriginLotErrorIcon).visibility = View.GONE
                },
                onError = {
                    if (lots.isNotEmpty()) {
                        if (!cacheRefreshErrorToastShown) {
                            cacheRefreshErrorToastShown = true
                            Toast.makeText(
                                requireContext(),
                                R.string.applications_register_cache_refresh_error,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    } else {
                        lotLoadFailed = true
                        lotValue.text = getString(R.string.applications_register_options_error)
                        lotValue.setTextColor(requireContext().getColor(R.color.argos_error))
                        lotField.isEnabled = true
                        lotField.findViewById<View>(R.id.exceptionOriginLotLoadingIndicator).visibility = View.GONE
                        lotField.findViewById<View>(R.id.exceptionOriginLotDropdownIcon).visibility = View.GONE
                        lotField.findViewById<View>(R.id.exceptionOriginLotErrorIcon).visibility = View.VISIBLE
                    }
                },
            )
        }

        lotField.setOnClickListener {
            if (lots.isEmpty()) {
                if (lotLoadFailed) loadLots()
                return@setOnClickListener
            }
            selectManager.showOptions(
                anchor = lotField,
                options = lots.map(LotDto::lotName),
                dropdownIcon = lotField.findViewById(R.id.exceptionOriginLotDropdownIcon),
                onSelected = { name ->
                    selectedLot = lots.firstOrNull { it.lotName == name }
                    lotValue.text = selectedLot?.lotName
                    lotValue.setTextColor(requireContext().getColor(R.color.argos_text_primary))
                    lotValue.error = null
                },
            )
        }
        loadLots()

        val registerButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.exceptionRegisterButton)
        registerButton.setOnClickListener {
            val originLot = selectedLot?.takeIf { lot -> lots.any { it.lotId == lot.lotId } }
            val reason = reasonField.text.toString().trim()
            val earTag = earTagField.text.toString().trim()
            val weight = weightField.text.toString().trim().replace(',', '.').toBigDecimalOrNull()

            if (originLot == null) lotValue.error = getString(R.string.applications_register_lot_required)
            if (reason.isBlank()) reasonField.error = getString(R.string.exceptions_reason_required)
            if (earTag.isBlank()) earTagField.error = getString(R.string.exceptions_ear_tag_required)
            if (weight == null || weight.signum() <= 0) weightField.error = getString(R.string.exceptions_weight_invalid)
            if (originLot == null || reason.isBlank() || earTag.isBlank() || weight == null || weight.signum() <= 0) {
                return@setOnClickListener
            }

            viewModel.registerException(
                data = RegisterAnimalExceptionDto(
                    originLotId = requireNotNull(originLot.lotId),
                    earTag = earTag,
                    exceptionReason = reason,
                    currentWeight = weight,
                ),
                onLoading = { loading ->
                    registerButton.isEnabled = !loading
                    registerButton.isClickable = !loading
                    view.findViewById<View>(R.id.exceptionRegisterLoadingIndicator).visibility =
                        if (loading) View.VISIBLE else View.GONE
                    registerButton.text = if (loading) "" else getString(R.string.exceptions_register)
                    registerButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        view.context.getColor(if (loading) R.color.argos_border else R.color.argos_green),
                    )
                    isCancelable = !loading
                    view.findViewById<View>(R.id.exceptionLaterButton).isEnabled = !loading
                },
                onSuccess = {
                    parentFragmentManager.setFragmentResult(REGISTERED_RESULT, Bundle.EMPTY)
                    Toast.makeText(view.context, R.string.exceptions_register_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                },
                onError = {
                    Toast.makeText(view.context, R.string.applications_register_error, Toast.LENGTH_LONG).show()
                },
            )
        }

        reasonField.doAfterTextChanged { reasonField.error = null }
        earTagField.doAfterTextChanged { earTagField.error = null }
        weightField.doAfterTextChanged { weightField.error = null }
        view.findViewById<View>(R.id.exceptionLaterButton).setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        selectManager.dismiss()
        super.onDestroyView()
    }

    companion object {
        const val TAG = "create_exception"
        const val REGISTERED_RESULT = "animal_exception_registered"
    }
}
