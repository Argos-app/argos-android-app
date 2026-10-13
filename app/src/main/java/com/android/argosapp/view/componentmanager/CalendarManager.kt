package com.android.argosapp.view.componentmanager

import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.argosapp.R
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class CalendarManager(
    private val monthLabel: TextView,
    private val calendarGrid: GridLayout,
    private val selectedDateLabel: TextView,
) {
    data class NoticeSpec(
        val hasNormalNotice: Boolean = false,
        val hasImportantNotice: Boolean = false,
    )

    var selectedDate: LocalDate = LocalDate.now()
        private set

    var displayedMonth: YearMonth = YearMonth.from(selectedDate)
        private set

    private var selectedDateChangeListener: ((LocalDate) -> Unit)? = null
    private val dateNotices = mutableMapOf<LocalDate, NoticeSpec>()
    private var hasRendered = false
    private val context get() = calendarGrid.context

    fun render(initialDate: LocalDate) {
        val currentMonth = YearMonth.now()
        selectedDate = initialDate.takeIf { YearMonth.from(it) == currentMonth } ?: LocalDate.now()
        displayedMonth = currentMonth
        hasRendered = true

        val monthName = displayedMonth.month
            .getDisplayName(TextStyle.FULL, CALENDAR_LOCALE)
            .capitalized()
        monthLabel.text = context.getString(
            R.string.home_calendar_month,
            monthName,
            displayedMonth.year,
        )

        val selectedMonthName = selectedDate.month
            .getDisplayName(TextStyle.FULL, CALENDAR_LOCALE)
            .capitalized()
        selectedDateLabel.text = context.getString(
            R.string.home_selected_date,
            selectedDate.dayOfMonth,
            selectedMonthName,
            selectedDate.year,
        )

        while (calendarGrid.childCount > WEEKDAY_COUNT) {
            calendarGrid.removeViewAt(WEEKDAY_COUNT)
        }

        val firstOfMonth = displayedMonth.atDay(1)
        val leadingDayCount = firstOfMonth.dayOfWeek.value % DAYS_PER_WEEK
        val gridStart = firstOfMonth.minusDays(leadingDayCount.toLong())
        val requiredCells = leadingDayCount + displayedMonth.lengthOfMonth()
        val dayCellCount = if (requiredCells <= FIVE_WEEKS) FIVE_WEEKS else SIX_WEEKS
        calendarGrid.rowCount = 1 + dayCellCount / DAYS_PER_WEEK

        repeat(dayCellCount) { index ->
            val date = gridStart.plusDays(index.toLong())
            calendarGrid.addView(createDayCell(date, YearMonth.from(date) == displayedMonth))
        }
    }

    fun setOnSelectedDateChange(listener: (LocalDate) -> Unit) {
        selectedDateChangeListener = listener
    }

    fun addNotices(notices: Map<LocalDate, NoticeSpec>) {
        notices.forEach { (date, notice) ->
            val current = dateNotices[date] ?: NoticeSpec()
            dateNotices[date] = NoticeSpec(
                hasNormalNotice = current.hasNormalNotice || notice.hasNormalNotice,
                hasImportantNotice = current.hasImportantNotice || notice.hasImportantNotice,
            )
        }
        if (hasRendered) render(selectedDate)
    }

    private fun createDayCell(date: LocalDate, belongsToDisplayedMonth: Boolean): View {
        val content = createDayContents(date, belongsToDisplayedMonth)
        val accessibilityDescription = date.format(ACCESSIBILITY_DATE_FORMATTER).capitalized()
        val cell = FrameLayout(context).apply {
            layoutParams = calendarCellLayoutParams()
        }

        if (date == selectedDate && belongsToDisplayedMonth) {
            val dayText = content.findViewWithTag<TextView>(DAY_TEXT_TAG)
            dayText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            dayText.setTypeface(dayText.typeface, Typeface.BOLD)

            val selectedCircle = MaterialCardView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dp(CALENDAR_DAY_CELL_SIZE_DP),
                    dp(CALENDAR_DAY_CELL_SIZE_DP),
                    Gravity.CENTER,
                )
                radius = dp(CALENDAR_DAY_CELL_SIZE_DP).toFloat() / 2f
                cardElevation = 0f
                strokeWidth = 0
                preventCornerOverlap = false
                useCompatPadding = false
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.argos_green))
                contentDescription = accessibilityDescription
                stateDescription = context.getString(R.string.home_calendar_selected)
                addView(
                    content,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                bindDateInteraction(this, date, belongsToDisplayedMonth)
            }
            cell.addView(selectedCircle)
            return cell
        }

        content.layoutParams = FrameLayout.LayoutParams(
            dp(CALENDAR_DAY_CELL_SIZE_DP),
            dp(CALENDAR_DAY_CELL_SIZE_DP),
            Gravity.CENTER,
        )
        content.contentDescription = accessibilityDescription
        content.background = ContextCompat.getDrawable(context, R.drawable.calendar_day_ripple)
        bindDateInteraction(content, date, belongsToDisplayedMonth)
        cell.addView(content)
        return cell
    }

    private fun createDayContents(
        date: LocalDate,
        belongsToDisplayedMonth: Boolean,
    ): LinearLayout {
        val dayColor = ContextCompat.getColor(
            context,
            if (belongsToDisplayedMonth) R.color.argos_green else R.color.argos_calendar_muted,
        )
        val dayText = TextView(context).apply {
            tag = DAY_TEXT_TAG
            gravity = Gravity.CENTER
            setTextAppearance(R.style.HomeCalendarDay)
            text = date.dayOfMonth.toString()
            setTextColor(dayColor)
        }
        val noticeSpec = dateNotices[date]
        val hasNotice = belongsToDisplayedMonth &&
            (noticeSpec?.hasNormalNotice == true || noticeSpec?.hasImportantNotice == true)
        val dots = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = if (hasNotice) View.VISIBLE else View.INVISIBLE

            if (belongsToDisplayedMonth && noticeSpec?.hasNormalNotice == true) {
                addNoticeDot(this, ContextCompat.getColor(context, R.color.argos_accent), addGapAfter = true)
            }
            if (belongsToDisplayedMonth && noticeSpec?.hasImportantNotice == true) {
                addNoticeDot(this, ContextCompat.getColor(context, R.color.argos_notice_important), addGapAfter = false)
            }
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(
                dayText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            val dotsLayoutParams = LinearLayout.LayoutParams(
                dp(NOTICE_DOT_ROW_WIDTH_DP),
                dp(NOTICE_DOT_SIZE_DP),
            ).apply {
                topMargin = dp(NOTICE_DOT_TOP_MARGIN_DP)
            }
            addView(dots, dotsLayoutParams)
        }
    }

    private fun addNoticeDot(container: LinearLayout, color: Int, addGapAfter: Boolean) {
        val dot = createNoticeDot(color)
        val layoutParams = dot.layoutParams as LinearLayout.LayoutParams
        if (addGapAfter) layoutParams.marginEnd = dp(NOTICE_DOT_GAP_DP)
        container.addView(dot, layoutParams)
    }

    private fun createNoticeDot(color: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            dp(NOTICE_DOT_SIZE_DP),
            dp(NOTICE_DOT_SIZE_DP),
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun calendarCellLayoutParams() = GridLayout.LayoutParams().apply {
        width = 0
        height = dp(CALENDAR_DAY_CELL_SIZE_DP)
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
    }

    private fun bindDateInteraction(target: View, date: LocalDate, isEnabled: Boolean) {
        if (!isEnabled) {
            target.isClickable = false
            target.isFocusable = false
            return
        }

        target.isClickable = true
        target.isFocusable = true
        target.setOnClickListener {
            selectedDate = date
            render(selectedDate)
            selectedDateChangeListener?.invoke(selectedDate)
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun String.capitalized(): String =
        replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(CALENDAR_LOCALE) else character.toString()
        }

    companion object {
        private const val WEEKDAY_COUNT = 7
        private const val DAYS_PER_WEEK = 7
        private const val FIVE_WEEKS = 35
        private const val SIX_WEEKS = 42
        private const val CALENDAR_DAY_CELL_SIZE_DP = 43
        private const val NOTICE_DOT_SIZE_DP = 6
        private const val NOTICE_DOT_GAP_DP = 4
        private const val NOTICE_DOT_ROW_WIDTH_DP = NOTICE_DOT_SIZE_DP * 2 + NOTICE_DOT_GAP_DP
        private const val NOTICE_DOT_TOP_MARGIN_DP = 1
        private const val DAY_TEXT_TAG = "calendar_day_text"
        private val CALENDAR_LOCALE = Locale("pt", "BR")
        private val ACCESSIBILITY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", CALENDAR_LOCALE)
    }
}
