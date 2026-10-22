package com.android.argosapp.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.databinding.FragmentHomeBinding
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.view.adapter.HomeFragmentApplication
import com.android.argosapp.view.adapter.HomeFragmentApplicationsAdapter
import com.android.argosapp.view.componentmanager.CalendarManager
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.view.activity.MedicationStockActivity
import com.android.argosapp.viewmodel.HomeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: HomeViewModel by viewModels()
    private var calendarManager: CalendarManager? = null
    private var applicationsAdapter: HomeFragmentApplicationsAdapter? = null
    private var selectedDate = LocalDate.now()
    private var monthlyWithdrawals = emptyList<ApplicationWithdrawal>()
    private var hasEventsForSelectedDate = false
    private var isLoadingApplications = true
    private var hasApplicationLoadFailed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.homeManageMedication.setOnClickListener {
            startActivity(Intent(requireContext(), MedicationStockActivity::class.java))
        }

        binding.homeGreeting.setText(R.string.home_greeting_fallback)
        loadUserGreeting(UserStorage(requireContext()))

        val currentMonth = YearMonth.now()
        selectedDate = savedInstanceState?.getString(STATE_SELECTED_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: selectedDate
        selectedDate = selectedDate.takeIf { YearMonth.from(it) == currentMonth } ?: LocalDate.now()

        applicationsAdapter = HomeFragmentApplicationsAdapter(
            onApplicationClick = ::openApplicationDetails,
        )
        binding.homeEventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applicationsAdapter
        }

        calendarManager = CalendarManager(
            monthLabel = binding.homeCalendarMonth,
            calendarGrid = binding.homeCalendarGrid,
            selectedDateLabel = binding.homeSelectedDate,
        ).also { calendar ->
            calendar.setOnSelectedDateChange { date ->
                selectedDate = date
                renderCardsForDate(date)
            }
            calendar.render(selectedDate)
        }

        binding.homeCalendarLoadingOverlay.visibility = View.VISIBLE
        renderCardsForDate(selectedDate)
        viewModel.loadMonthThrough(
            month = currentMonth,
            throughDate = LocalDate.now(),
            onLoading = ::setMonthLoading,
            onApplicationsLoaded = ::showApplications,
            onError = { showApplicationLoadError() },
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_DATE, selectedDate.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding?.homeEventsRecyclerView?.adapter = null
        applicationsAdapter = null
        calendarManager = null
        _binding = null
        super.onDestroyView()
    }

    private fun setMonthLoading(isLoading: Boolean) {
        isLoadingApplications = isLoading
        if (isLoading) hasApplicationLoadFailed = false
        renderCalendarStatus()
        updateEventsEmptyMessage()
    }

    private fun showApplications(applications: List<ApplicationWithdrawal>) {
        if (_binding == null) return
        hasApplicationLoadFailed = false
        monthlyWithdrawals = applications
        calendarManager?.addNotices(noticesFor(applications))
        renderCardsForDate(selectedDate)
    }

    private fun showApplicationLoadError() {
        if (_binding == null) return
        hasApplicationLoadFailed = true
        renderCalendarStatus()
        updateEventsEmptyMessage()
    }

    private fun openApplicationDetails(application: ApplicationWithdrawal) {
        (activity as? HomeActivity)?.showApplicationDetails(application)
    }

    private fun loadUserGreeting(userStorage: UserStorage) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userName = userStorage.getUser()?.userName.orEmpty()
                if (userName.isNotBlank()) {
                    _binding?.homeGreeting?.text = getString(R.string.home_greeting, userName)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.SESSION_TAG, "home_user_read_failed", error)
            }
        }
    }

    private fun renderCardsForDate(date: LocalDate) {
        val cards = monthlyWithdrawals
            .flatMap { application ->
                buildList {
                    if (application.applicationDate == date) add(application.toCard(isWithdrawal = false))
                    if (application.meatWithdrawalEnd == date) add(application.toCard(isWithdrawal = true))
                }
            }
        hasEventsForSelectedDate = cards.isNotEmpty()
        applicationsAdapter?.submitList(cards)
        updateEventsEmptyMessage()
    }

    private fun updateEventsEmptyMessage() {
        val binding = _binding ?: return

        binding.homeEventsEmptyMessage.visibility =
            if (!isLoadingApplications && !hasApplicationLoadFailed && !hasEventsForSelectedDate) {
                View.VISIBLE

            } else {
                View.GONE

            }

        binding.homeEventsEmptyMessage.setText(R.string.home_events_empty)
    }

    private fun renderCalendarStatus() {
        val binding = _binding ?: return
        val showError = hasApplicationLoadFailed && !isLoadingApplications

        binding.homeCalendarContent.visibility =
            if (hasApplicationLoadFailed) View.GONE else View.VISIBLE

        binding.homeCalendarError.visibility =
            if (showError) View.VISIBLE else View.GONE

        binding.homeCalendarLoadingOverlay.visibility =
            if (isLoadingApplications && !hasApplicationLoadFailed) View.VISIBLE else View.GONE
    }

    private fun ApplicationWithdrawal.toCard(isWithdrawal: Boolean): HomeFragmentApplication {
        val unknown = getString(R.string.home_calendar_unknown_value)
        val endDate = meatWithdrawalEnd ?: selectedDate
        val daysUntilEnd = ChronoUnit.DAYS.between(LocalDate.now(), endDate)
        val title = if (!isWithdrawal) {
            getString(R.string.home_application_event_title)
        } else {
            when {
                daysUntilEnd > 0 -> resources.getQuantityString(
                    R.plurals.home_withdrawal_days_remaining,
                    daysUntilEnd.toInt(),
                    daysUntilEnd,
                )
                daysUntilEnd == 0L -> getString(R.string.home_withdrawal_ends_today)
                else -> resources.getQuantityString(
                    R.plurals.home_withdrawal_days_elapsed,
                    daysUntilEnd.absoluteValue.toInt(),
                    daysUntilEnd.absoluteValue,
                )
            }
        }
        val color = ContextCompat.getColor(
            requireContext(),
            if (isWithdrawal) R.color.argos_notice_important else R.color.argos_accent,
        )

        return HomeFragmentApplication(
            title = title,
            detail = getString(R.string.home_withdrawal_medicine, medicineName ?: unknown),
            dateLabel = getString(
                R.string.home_withdrawal_application_date,
                applicationDate?.format(DATE_FORMATTER) ?: unknown,
            ),
            titleColor = color,
            accentColor = color,
            applicationWithdrawal = this,
        )
    }

    private fun noticesFor(withdrawals: List<ApplicationWithdrawal>): Map<LocalDate, CalendarManager.NoticeSpec> =
        buildMap {
            withdrawals.forEach { application ->
                application.applicationDate?.let { date ->
                    mergeNotice(date, normal = true)
                }
                application.meatWithdrawalEnd?.let { date ->
                    mergeNotice(date, important = true)
                }
            }
        }

    private fun MutableMap<LocalDate, CalendarManager.NoticeSpec>.mergeNotice(
        date: LocalDate,
        normal: Boolean = false,
        important: Boolean = false,
    ) {
        val current = get(date) ?: CalendarManager.NoticeSpec()
        put(
            date,
            current.copy(
                hasNormalNotice = current.hasNormalNotice || normal,
                hasImportantNotice = current.hasImportantNotice || important,
            ),
        )
    }

    companion object {
        private const val STATE_SELECTED_DATE = "selected_date"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    }
}
