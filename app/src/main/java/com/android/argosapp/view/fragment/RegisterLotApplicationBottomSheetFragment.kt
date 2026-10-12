package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.android.argosapp.R
import com.android.argosapp.util.validation.ApplicationFormValidator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RegisterLotApplicationBottomSheetFragment : BottomSheetDialogFragment() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val applicationFormValidator = ApplicationFormValidator()

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.bottom_sheet_register_lot_application, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val doseField = view.findViewById<EditText>(R.id.applicationDoseField)
        val dateValue = view.findViewById<TextView>(R.id.applicationDateValue)
        val today = LocalDate.now()
        dateValue.text = today.format(dateFormatter)
        view.findViewById<View>(R.id.applicationDateField).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                R.style.Theme_Argosapp_DatePickerDialog,
                { _, year, month, day ->
                    dateValue.text = LocalDate.of(year, month + 1, day).format(dateFormatter)
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth,
            ).show()
        }

        doseField.filters = arrayOf(android.text.InputFilter { source, _, _, _, _, _ ->
            val digitsOnly = source.filter { it in '0'..'9' }
            if (digitsOnly.length == source.length) null else digitsOnly
        })
        doseField.doAfterTextChanged { doseField.error = null }
        view.findViewById<View>(R.id.applicationRegisterButton).setOnClickListener {
            if (!applicationFormValidator.isValidDose(doseField.text.toString())) {
                doseField.error = getString(R.string.applications_register_dose_invalid)
            }
        }
        view.findViewById<View>(R.id.applicationLaterButton).setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "register_lot_application"
    }
}
