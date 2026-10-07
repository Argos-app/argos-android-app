package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentHomeApplicationDetailsBinding
import com.android.argosapp.model.ApplicationWithdrawal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

class HomeApplicationDetailsFragment : Fragment(R.layout.fragment_home_application_details) {
    private var _binding: FragmentHomeApplicationDetailsBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeApplicationDetailsBinding.bind(view)

        binding.homeApplicationDetailsBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val arguments = requireArguments()
        val applicationDate = arguments.getString(ARG_APPLICATION_DATE).toLocalDateOrNull()
        val withdrawalEnd = arguments.getString(ARG_WITHDRAWAL_END).toLocalDateOrNull()
        val unknown = getString(R.string.home_calendar_unknown_value)

        binding.homeApplicationDetailsMedicine.text =
            arguments.getString(ARG_MEDICINE_NAME).orEmpty().ifBlank { unknown }
        binding.homeApplicationDetailsLot.text =
            arguments.getString(ARG_LOT_NAME).orEmpty().ifBlank { unknown }
        binding.homeApplicationDetailsApplicationDate.text = applicationDate?.format(DATE_FORMATTER) ?: unknown
        binding.homeApplicationDetailsWithdrawalEnd.text = withdrawalEnd?.format(DATE_FORMATTER) ?: unknown
        binding.homeApplicationDetailsDaysRemaining.text = withdrawalEnd?.let(::formatWithdrawalStatus) ?: unknown
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun formatWithdrawalStatus(withdrawalEnd: LocalDate): String {
        val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), withdrawalEnd)
        return when {
            daysRemaining > 0 -> resources.getQuantityString(
                R.plurals.home_application_details_days_remaining,
                daysRemaining.toInt(),
                daysRemaining,
            )
            daysRemaining == 0L -> getString(R.string.home_application_details_ends_today)
            else -> resources.getQuantityString(
                R.plurals.home_application_details_days_elapsed,
                daysRemaining.absoluteValue.toInt(),
                daysRemaining.absoluteValue,
            )
        }
    }

    private fun String?.toLocalDateOrNull(): LocalDate? =
        this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        const val TAG = "home_application_details"
        private const val ARG_MEDICINE_NAME = "medicine_name"
        private const val ARG_LOT_NAME = "lot_name"
        private const val ARG_APPLICATION_DATE = "application_date"
        private const val ARG_WITHDRAWAL_END = "withdrawal_end"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

        fun newInstance(application: ApplicationWithdrawal) = HomeApplicationDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MEDICINE_NAME, application.medicineName)
                putString(ARG_LOT_NAME, application.lotName)
                putString(ARG_APPLICATION_DATE, application.applicationDate?.toString())
                putString(ARG_WITHDRAWAL_END, application.meatWithdrawalEnd?.toString())
            }
        }
    }
}
