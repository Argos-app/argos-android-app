package com.android.argosapp.view.fragment

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentHomeBinding
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var displayedMonth: YearMonth = YearMonth.from(DEFAULT_SELECTED_DATE)
    private var selectedDate: LocalDate = DEFAULT_SELECTED_DATE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)
        selectedDate = savedInstanceState?.getString(STATE_SELECTED_DATE)
            ?.let(::parseDate)
            ?: DEFAULT_SELECTED_DATE
        displayedMonth = savedInstanceState?.getString(STATE_DISPLAYED_MONTH)
            ?.let(::parseMonth)
            ?: YearMonth.from(selectedDate)

        val userName = arguments?.getString(ARG_USER_NAME).orEmpty()
        binding.homeGreeting.text = if (userName.isBlank()) {
            getString(R.string.home_greeting_fallback)
        } else {
            getString(R.string.home_greeting, userName)
        }

        renderCalendar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_DATE, selectedDate.toString())
        outState.putString(STATE_DISPLAYED_MONTH, displayedMonth.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderCalendar() {
        val monthName = displayedMonth.month
            .getDisplayName(TextStyle.FULL, CALENDAR_LOCALE)
            .capitalized()
        binding.homeCalendarMonth.text = getString(
            R.string.home_calendar_month,
            monthName,
            displayedMonth.year,
        )

        val selectedMonthName = selectedDate.month
            .getDisplayName(TextStyle.FULL, CALENDAR_LOCALE)
            .capitalized()
        binding.homeSelectedDate.text = getString(
            R.string.home_selected_date,
            selectedDate.dayOfMonth,
            selectedMonthName,
            selectedDate.year,
        )

        val grid = binding.homeCalendarGrid
        while (grid.childCount > WEEKDAY_COUNT) {
            grid.removeViewAt(WEEKDAY_COUNT)
        }

        val firstOfMonth = displayedMonth.atDay(1)
        val leadingDayCount = firstOfMonth.dayOfWeek.value % DAYS_PER_WEEK
        val gridStart = firstOfMonth.minusDays(leadingDayCount.toLong())
        val requiredCells = leadingDayCount + displayedMonth.lengthOfMonth()
        val dayCellCount = if (requiredCells <= FIVE_WEEKS) FIVE_WEEKS else SIX_WEEKS
        grid.rowCount = 1 + dayCellCount / DAYS_PER_WEEK

        repeat(dayCellCount) { index ->
            val date = gridStart.plusDays(index.toLong())
            grid.addView(createDayCell(date, YearMonth.from(date) == displayedMonth))
        }
    }

    private fun createDayCell(date: LocalDate, belongsToDisplayedMonth: Boolean): View {
        val dayText = TextView(requireContext()).apply {
            gravity = Gravity.CENTER
            setTextAppearance(R.style.HomeCalendarDay)
            text = date.dayOfMonth.toString()
            contentDescription = date.format(ACCESSIBILITY_DATE_FORMATTER).capitalized()
        }

        if (date == selectedDate) {
            dayText.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            dayText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            dayText.setTypeface(dayText.typeface, Typeface.BOLD)

            return MaterialCardView(requireContext()).apply {
                layoutParams = calendarCellLayoutParams()
                radius = dp(22).toFloat()
                cardElevation = 0f
                strokeWidth = 0
                preventCornerOverlap = false
                useCompatPadding = false
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.argos_green))
                contentDescription = dayText.contentDescription
                stateDescription = getString(R.string.home_calendar_selected)
                addView(dayText)
                bindDateInteraction(this, date)
            }
        }

        dayText.layoutParams = calendarCellLayoutParams()
        dayText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (belongsToDisplayedMonth) R.color.argos_green else R.color.argos_calendar_muted,
            ),
        )
        bindDateInteraction(dayText, date)
        return dayText
    }

    private fun calendarCellLayoutParams() = GridLayout.LayoutParams().apply {
        width = 0
        height = dp(43)
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindDateInteraction(target: View, date: LocalDate) {
        target.isClickable = true
        target.isFocusable = true
        target.setOnClickListener {
            selectedDate = date
            displayedMonth = YearMonth.from(date)
            renderCalendar()
        }

        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onFling(
                    start: MotionEvent?,
                    end: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (start == null) return false
                    val horizontalDistance = end.x - start.x
                    val verticalDistance = end.y - start.y
                    if (abs(horizontalDistance) < dp(MINIMUM_SWIPE_DP) ||
                        abs(horizontalDistance) <= abs(verticalDistance)
                    ) {
                        return false
                    }

                    displayedMonth = displayedMonth.plusMonths(
                        if (horizontalDistance < 0) 1 else -1,
                    )
                    renderCalendar()
                    return true
                }
            },
        )
        target.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun String.capitalized(): String =
        replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(CALENDAR_LOCALE) else character.toString()
        }

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    private fun parseMonth(value: String): YearMonth? =
        runCatching { YearMonth.parse(value) }.getOrNull()

    companion object {
        private const val ARG_USER_NAME = "user_name"
        private const val STATE_SELECTED_DATE = "selected_date"
        private const val STATE_DISPLAYED_MONTH = "displayed_month"
        private const val WEEKDAY_COUNT = 7
        private const val DAYS_PER_WEEK = 7
        private const val FIVE_WEEKS = 35
        private const val SIX_WEEKS = 42
        private const val MINIMUM_SWIPE_DP = 48
        private val DEFAULT_SELECTED_DATE = LocalDate.of(2026, 6, 23)
        private val CALENDAR_LOCALE = Locale("pt", "BR")
        private val ACCESSIBILITY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", CALENDAR_LOCALE)

        fun newInstance(userName: String) = HomeFragment().apply {
            arguments = Bundle().apply { putString(ARG_USER_NAME, userName) }
        }
    }
}
