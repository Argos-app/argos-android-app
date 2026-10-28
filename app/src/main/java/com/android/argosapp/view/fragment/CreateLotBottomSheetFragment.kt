package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.content.res.ColorStateList
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
import com.android.argosapp.view.componentmanager.SelectManager
import com.android.argosapp.util.validation.LotFormValidator
import com.android.argosapp.viewmodel.CreateLotBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreateLotBottomSheetFragment : BottomSheetDialogFragment() {
    private val viewModel: CreateLotBottomSheetViewModel by viewModels()
    private val selectManager = SelectManager()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.bottom_sheet_create_lot, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lotNameField = view.findViewById<EditText>(R.id.createLotNameField)
        val herdNameField = view.findViewById<EditText>(R.id.createLotHerdNameField)
        val headCountField = view.findViewById<EditText>(R.id.createLotHeadCountField)
        val categoryField = view.findViewById<View>(R.id.createLotCategoryField)
        val categoryValue = view.findViewById<TextView>(R.id.createLotCategoryValue)
        val slaughterDateField = view.findViewById<View>(R.id.createLotSlaughterDateField)
        val slaughterDateValue = view.findViewById<TextView>(R.id.createLotSlaughterDateValue)
        val submitButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.createLotSubmitButton)
        val laterButton = view.findViewById<View>(R.id.createLotLaterButton)
        val categories = resources.getStringArray(R.array.lots_categories)

        lotNameField.doAfterTextChanged { lotNameField.error = null }
        herdNameField.doAfterTextChanged { herdNameField.error = null }
        headCountField.doAfterTextChanged { headCountField.error = null }
        categoryField.setOnClickListener {
            selectManager.showOptions(
                anchor = categoryField,
                options = categories.toList(),
                dropdownIcon = view.findViewById(R.id.createLotCategoryDropdownIcon),
                onSelected = { selected ->
                    categoryValue.error = null
                    categoryValue.text = selected
                    categoryValue.setTextColor(requireContext().getColor(R.color.argos_text_primary))
                },
            )
        }
        slaughterDateField.setOnClickListener {
            val today = LocalDate.now()
            DatePickerDialog(
                requireContext(),
                R.style.Theme_Argosapp_DatePickerDialog,
                { _, year, month, day ->
                    slaughterDateValue.error = null
                    slaughterDateValue.text = LocalDate.of(year, month + 1, day).format(dateFormatter)
                    slaughterDateValue.setTextColor(requireContext().getColor(R.color.argos_text_primary))
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth,
            ).show()
        }
        submitButton.setOnClickListener {
            val lotName = lotNameField.text.toString()
            val herdName = herdNameField.text.toString()
            val category = categoryValue.text.toString()
            val headCount = headCountField.text.toString()
            val slaughterDate = slaughterDateValue.text.toString()
            val invalidFields = viewModel.invalidFields(
                lotName = lotName,
                herdName = herdName,
                category = category,
                headCount = headCount,
                slaughterDate = slaughterDate,
                availableCategories = categories.toList(),
            )
            lotNameField.error = if (LotFormValidator.Field.LOT_NAME in invalidFields) {
                getString(R.string.lots_create_name_error)
            } else null
            herdNameField.error = if (LotFormValidator.Field.HERD_NAME in invalidFields) {
                getString(R.string.lots_create_herd_error)
            } else null
            categoryValue.error = if (LotFormValidator.Field.CATEGORY in invalidFields) {
                getString(R.string.lots_create_category_error)
            } else null
            headCountField.error = if (LotFormValidator.Field.HEAD_COUNT in invalidFields) {
                getString(R.string.lots_create_head_count_error)
            } else null
            slaughterDateValue.error = if (LotFormValidator.Field.SLAUGHTER_DATE in invalidFields) {
                getString(R.string.lots_create_slaughter_date_error)
            } else null
            if (invalidFields.isNotEmpty()) return@setOnClickListener

            viewModel.registerLot(
                lotName = lotName,
                herdName = herdName,
                category = category,
                headCount = headCount,
                slaughterDate = slaughterDate,
                onLoading = { loading -> setSubmitting(view, submitButton, laterButton, loading) },
                onSuccess = {
                    Toast.makeText(view.context, R.string.lots_create_success, Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult(REGISTERED_RESULT, Bundle.EMPTY)
                    dismiss()
                },
                onError = {
                    Toast.makeText(view.context, R.string.lots_create_error, Toast.LENGTH_LONG).show()
                },
            )
        }
        laterButton.setOnClickListener { dismiss() }
    }

    private fun setSubmitting(
        view: View,
        submitButton: com.google.android.material.button.MaterialButton,
        laterButton: View,
        submitting: Boolean,
    ) {
        submitButton.isEnabled = !submitting
        submitButton.isClickable = !submitting
        submitButton.text = if (submitting) "" else getString(R.string.lots_create_submit)
        submitButton.backgroundTintList = ColorStateList.valueOf(
            view.context.getColor(if (submitting) R.color.argos_border else R.color.argos_green),
        )
        view.findViewById<View>(R.id.createLotLoadingIndicator).visibility =
            if (submitting) View.VISIBLE else View.GONE
        laterButton.isEnabled = !submitting
        isCancelable = !submitting
    }

    override fun onDestroyView() {
        selectManager.dismiss()
        super.onDestroyView()
    }

    companion object {
        const val TAG = "create_lot"
        const val REGISTERED_RESULT = "lot_registered"
    }
}
