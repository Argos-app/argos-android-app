package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.argosapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreateExceptionBottomSheetFragment : BottomSheetDialogFragment() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.bottom_sheet_create_exception, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dateValue = view.findViewById<TextView>(R.id.exceptionRecordDateValue)
        var selectedDate = LocalDate.now()
        dateValue.text = selectedDate.format(dateFormatter)

        view.findViewById<View>(R.id.exceptionRecordDateField).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                R.style.Theme_Argosapp_DatePickerDialog,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    dateValue.text = selectedDate.format(dateFormatter)
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth,
            ).show()
        }

        view.findViewById<View>(R.id.exceptionLaterButton).setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "create_exception"
    }
}
