package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentApplicationsManagementDetailsBinding
import com.android.argosapp.model.Application
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ApplicationsManagementDetailsFragment :
    Fragment(R.layout.fragment_applications_management_details) {
    private var _binding: FragmentApplicationsManagementDetailsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicationsManagementDetailsBinding.bind(view)

        val binding = requireNotNull(_binding)
        binding.applicationsManagementDetailsBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        renderApplicationDetails(requireArguments())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderApplicationDetails(arguments: Bundle) {
        val binding = _binding ?: return
        val unknown = getString(R.string.home_calendar_unknown_value)
        val applicationDate = arguments.getString(ARG_APPLICATION_DATE).toLocalDateTimeOrNull()
        val applicationTime = arguments.getString(ARG_APPLICATION_TIME).toLocalTimeOrNull()
            ?: applicationDate?.toLocalTime()

        binding.applicationsDetailsMedicine.text = arguments.getString(ARG_MEDICINE_NAME).orUnknown(unknown)
        binding.applicationsDetailsLot.text = (arguments.getString(ARG_LOT_NAME)
            ?: arguments.getString(ARG_LOT_CODE)).orUnknown(unknown)
        binding.applicationsDetailsAnimal.text = arguments.getString(ARG_EAR_CODE).orUnknown(unknown)
        binding.applicationsDetailsApplicationDate.text = applicationDate
            ?.toLocalDate()
            ?.format(DATE_FORMATTER)
            ?: arguments.getString(ARG_APPLICATION_DATE).toLocalDateOrNull()?.format(DATE_FORMATTER)
            ?: unknown
        binding.applicationsDetailsApplicationTime.text = applicationTime?.format(TIME_FORMATTER) ?: unknown
        binding.applicationsDetailsDose.text = formatDose(
            dose = arguments.getString(ARG_DOSE),
            unit = arguments.getString(ARG_UNIT),
            unknown = unknown,
        )
        binding.applicationsDetailsRoute.text = arguments.getString(ARG_ROUTE).orUnknown(unknown)
        binding.applicationsDetailsGoal.text = arguments.getString(ARG_GOAL).orUnknown(unknown)
        binding.applicationsDetailsNotes.text = arguments.getString(ARG_NOTES).orUnknown(unknown)
        binding.applicationsDetailsStatus.text = arguments.getString(ARG_STATUS).orUnknown(unknown)
        binding.applicationsDetailsHeadCount.text = arguments.getString(ARG_HEAD_COUNT).orUnknown(unknown)
        binding.applicationsDetailsWithdrawalEnd.text = arguments.getString(ARG_WITHDRAWAL_END)
            .toLocalDateOrNull()
            ?.format(DATE_FORMATTER)
            ?: unknown
    }

    private fun formatDose(dose: String?, unit: String?, unknown: String): String {
        val doseValue = dose.orUnknown("")
        val unitValue = unit.orUnknown("")
        return listOf(doseValue, unitValue)
            .filter { value -> value.isNotBlank() }
            .joinToString(" ")
            .ifBlank { unknown }
    }

    private fun String?.orUnknown(unknown: String): String {
        return this?.takeIf { value -> value.isNotBlank() } ?: unknown
    }

    private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
        return this?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }
    }

    private fun String?.toLocalDateOrNull(): LocalDate? {
        return this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }

    private fun String?.toLocalTimeOrNull(): LocalTime? {
        return this?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
    }

    companion object {
        const val TAG = "applications_management_details"
        private const val ARG_MEDICINE_NAME = "medicine_name"
        private const val ARG_LOT_NAME = "lot_name"
        private const val ARG_LOT_CODE = "lot_code"
        private const val ARG_EAR_CODE = "ear_code"
        private const val ARG_APPLICATION_DATE = "application_date"
        private const val ARG_APPLICATION_TIME = "application_time"
        private const val ARG_DOSE = "dose"
        private const val ARG_UNIT = "unit"
        private const val ARG_ROUTE = "route"
        private const val ARG_HEAD_COUNT = "head_count"
        private const val ARG_WITHDRAWAL_END = "withdrawal_end"
        private const val ARG_STATUS = "status"
        private const val ARG_GOAL = "goal"
        private const val ARG_NOTES = "notes"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

        fun newInstance(application: Application): ApplicationsManagementDetailsFragment {
            val applicationDate = application.applicationDate?.toString()
            val applicationTime = application.applicationTime?.toString()
            val dose = application.dose?.stripTrailingZeros()?.toPlainString()
            val headCount = application.headCountApplied ?: application.headCount

            return ApplicationsManagementDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEDICINE_NAME, application.medicineName)
                    putString(ARG_LOT_NAME, application.lotName)
                    putString(ARG_LOT_CODE, application.lotCode)
                    putString(ARG_EAR_CODE, application.earCode)
                    putString(ARG_APPLICATION_DATE, applicationDate)
                    putString(ARG_APPLICATION_TIME, applicationTime)
                    putString(ARG_DOSE, dose)
                    putString(ARG_UNIT, application.unit)
                    putString(ARG_ROUTE, application.route)
                    putString(ARG_HEAD_COUNT, headCount?.toString())
                    putString(ARG_WITHDRAWAL_END, application.meatWithdrawalEnd?.toString())
                    putString(ARG_STATUS, application.status)
                    putString(ARG_GOAL, application.goal)
                    putString(ARG_NOTES, application.notes)
                }
            }
        }
    }
}
